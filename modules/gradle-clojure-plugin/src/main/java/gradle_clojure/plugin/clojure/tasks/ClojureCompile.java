package gradle_clojure.plugin.clojure.tasks;

import static us.bpsm.edn.Keyword.newKeyword;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import gradle_clojure.plugin.common.internal.ClojureExecutor;
import gradle_clojure.plugin.common.internal.ExperimentalSettings;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.workers.WorkerExecutor;

public class ClojureCompile extends AbstractCompile {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);

  private final ClojureExecutor clojureExecutor;

  private final ClojureCompileOptions options;

  private List<String> namespaces = Collections.emptyList();

  @Inject
  public ClojureCompile(WorkerExecutor workerExecutor) {
    this.clojureExecutor = new ClojureExecutor(getProject(), workerExecutor);
    this.options = new ClojureCompileOptions();
  }

  @Nested
  public ClojureCompileOptions getOptions() {
    return options;
  }

  public void options(Action<? super ClojureCompileOptions> configureAction) {
    configureAction.execute(options);
  }

  @Input
  public List<String> getNamespaces() {
    return namespaces;
  }

  public void setNamespaces(List<String> namespaces) {
    this.namespaces = namespaces;
  }

  @Override
  @TaskAction
  public void compile() {
    if (!getProject().delete(getDestinationDir())) {
      throw new GradleException("Cannot clean destination directory: " + getDestinationDir().getAbsolutePath());
    }
    if (!getDestinationDir().mkdirs()) {
      throw new GradleException("Cannot create destination directory: " + getDestinationDir().getAbsolutePath());
    }
    if (!getProject().delete(getTemporaryDir())) {
      throw new GradleException("Cannot clean temporary directory: " + getTemporaryDir().getAbsolutePath());
    }

    if (options.isCopySourceSetToOutput()) {
      getProject().copy(spec -> {
        spec.from(getSource());
        spec.into(getDestinationDir());
      });
    }

    Collection<String> namespaces = getNamespaces();
    if (namespaces.isEmpty()) {
      logger.warn("No Clojure namespaces defined, skipping {}", getName());
      return;
    }

    logger.info("Compiling {}", String.join(", ", namespaces));

    // for non-aot compile we still want to compile as verification, but classes shouldn't be included
    // as an output
    File compileOutputDir = options.isAotCompile() ? getDestinationDir() : getTemporaryDir();

    FileCollection classpath = getClasspath()
        .plus(getProject().files(getSourceRootsFiles()))
        .plus(getProject().files(compileOutputDir));

    Map<Object, Object> config = getOptions().toMap();
    config.put(newKeyword("source-dirs"), getSourceRoots());
    config.put(newKeyword("destination-dir"), compileOutputDir.getAbsolutePath());
    config.put(newKeyword("namespaces"), namespaces);

    Action<ClojureExecSpec> action = spec -> {
      spec.setClasspath(classpath);
      spec.setMain("gradle-clojure.tools.clojure-compiler");
      spec.args(config);
      spec.forkOptions(fork -> {
        fork.setJvmArgs(options.getForkOptions().getJvmArgs());
        fork.setMinHeapSize(options.getForkOptions().getMemoryInitialSize());
        fork.setMaxHeapSize(options.getForkOptions().getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    };

    if (ExperimentalSettings.isUseWorkers()) {
      clojureExecutor.submit(action);
    } else {
      clojureExecutor.exec(action);
    }
  }

  public List<String> findNamespaces() {
    Set<String> roots = getSourceRoots();
    return StreamSupport.stream(getSource().spliterator(), false)
        .filter(it -> it.getName().endsWith(".clj") || it.getName().endsWith(".cljc"))
        .map(it -> findNamespace(it, roots))
        .collect(Collectors.toList());
  }

  private String findNamespace(File file, Set<String> roots) {
    try {
      File current = file.getParentFile();
      String namespace = demunge(file.getName().substring(0, file.getName().lastIndexOf('.')));
      while (current != null) {
        if (roots.contains(current.getCanonicalPath())) {
          return namespace;
        }
        namespace = demunge(current.getName()) + "." + namespace;
        current = current.getParentFile();
      }
      throw new RuntimeException("No source root found for " + file.getCanonicalPath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Set<String> getSourceRoots() {
    return getSourceRootsFiles().stream().map(it -> {
      try {
        return it.getCanonicalPath();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }).collect(Collectors.toSet());
  }

  private List<File> getSourceRootsFiles() {
    // accessing the List<Object> field not the FileTree from getSource
    return source.stream()
        .filter(it -> it instanceof SourceDirectorySet)
        .flatMap(it -> ((SourceDirectorySet) it).getSrcDirs().stream())
        .collect(Collectors.toList());
  }

  private static final Map<Character, String> CHAR_MAP = new HashMap<>();

  static {
    CHAR_MAP.put('-', "_");
    CHAR_MAP.put(':', "_COLON_");
    CHAR_MAP.put('+', "_PLUS_");
    CHAR_MAP.put('>', "_GT_");
    CHAR_MAP.put('<', "_LT_");
    CHAR_MAP.put('=', "_EQ_");
    CHAR_MAP.put('~', "_TILDE_");
    CHAR_MAP.put('!', "_BANG_");
    CHAR_MAP.put('@', "_CIRCA_");
    CHAR_MAP.put('#', "_SHARP_");
    CHAR_MAP.put('\'', "_SINGLEQUOTE_");
    CHAR_MAP.put('"', "_DOUBLEQUOTE_");
    CHAR_MAP.put('%', "_PERCENT_");
    CHAR_MAP.put('^', "_CARET_");
    CHAR_MAP.put('&', "_AMPERSAND_");
    CHAR_MAP.put('*', "_STAR_");
    CHAR_MAP.put('|', "_BAR_");
    CHAR_MAP.put('{', "_LBRACE_");
    CHAR_MAP.put('}', "_RBRACE_");
    CHAR_MAP.put('[', "_LBRACK_");
    CHAR_MAP.put(']', "_RBRACK_");
    CHAR_MAP.put('/', "_SLASH_");
    CHAR_MAP.put('\\', "_BSLASH_");
    CHAR_MAP.put('?', "_QMARK_");
  }

  private static final Map<String, Character> DEMUNGE_MAP = CHAR_MAP.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  private static final Pattern DEMUNGE_PATTERN = Pattern.compile(DEMUNGE_MAP.keySet().stream()
      .sorted(Comparator.comparingInt(String::length).reversed())
      .map(it -> "\\Q" + it + "\\E")
      .collect(Collectors.joining("|")));

  private static String munge(String name) {
    StringBuilder sb = new StringBuilder();
    name.chars().forEach(c -> {
      if (CHAR_MAP.containsKey(c)) {
        sb.append(CHAR_MAP.get(c));
      } else {
        sb.append(c);
      }
    });
    return sb.toString();
  }

  private static String demunge(String mungedName) {
    StringBuilder sb = new StringBuilder();
    Matcher m = DEMUNGE_PATTERN.matcher(mungedName);
    int lastMatchEnd = 0;
    while (m.find()) {
      int start = m.start();
      int end = m.end();
      // Keep everything before the match
      sb.append(mungedName.substring(lastMatchEnd, start));
      lastMatchEnd = end;
      // Replace the match with DEMUNGE_MAP result
      char origCh = DEMUNGE_MAP.get(m.group());
      sb.append(origCh);
    }
    // Keep everything after the last match
    sb.append(mungedName.substring(lastMatchEnd));
    return sb.toString();
  }
}
