package gradle_clojure.plugin.clojure.tasks;


import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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

import gradle_clojure.plugin.common.internal.ClojureExecutor;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

public class ClojureCompile extends DefaultTask {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);

  private final ClojureExecutor clojureExecutor;

  private SourceDirectorySet source;
  private final ConfigurableFileCollection classpath;
  private final DirectoryProperty destinationDir;
  private final ClojureCompileOptions options;

  private final ListProperty<String> namespaces;

  public ClojureCompile() {
    this.clojureExecutor = new ClojureExecutor(getProject());
    this.classpath = getProject().files();
    this.destinationDir = getProject().getLayout().directoryProperty();
    this.options = new ClojureCompileOptions();
    this.namespaces = getProject().getObjects().listProperty(String.class);
    namespaces.set(getProject().provider(this::findNamespaces));
  }

  @InputFiles
  @SkipWhenEmpty
  public FileCollection getSource() {
    return source;
  }

  public void setSource(SourceDirectorySet source) {
    this.source = source;
  }

  @Classpath
  public ConfigurableFileCollection getClasspath() {
    return classpath;
  }

  @OutputDirectory
  public DirectoryProperty getDestinationDir() {
    return destinationDir;
  }

  @Nested
  public ClojureCompileOptions getOptions() {
    return options;
  }

  public void options(Action<? super ClojureCompileOptions> configureAction) {
    configureAction.execute(options);
  }

  @Input
  public ListProperty<String> getNamespaces() {
    return namespaces;
  }

  @TaskAction
  public void compile() {
    File outputDir = getDestinationDir().get().getAsFile();
    if (!getProject().delete(outputDir)) {
      throw new GradleException("Cannot clean destination directory: " + outputDir.getAbsolutePath());
    }
    if (!outputDir.mkdirs()) {
      throw new GradleException("Cannot create destination directory: " + outputDir.getAbsolutePath());
    }

    List<String> namespaces = getNamespaces().getOrElse(Collections.emptyList());
    if (namespaces.isEmpty()) {
      logger.warn("No Clojure namespaces defined, skipping {}", getName());
      return;
    }

    logger.info("Compiling {}", String.join(", ", namespaces));

    FileCollection classpath = getClasspath()
        .plus(getProject().files(getSourceRootsFiles()))
        .plus(getProject().files(outputDir));

    clojureExecutor.exec(spec -> {
      spec.setClasspath(classpath);
      spec.setMain("gradle-clojure.tools.clojure-compiler");
      spec.args(getSourceRootsFiles(), outputDir, namespaces, getOptions());
      spec.forkOptions(fork -> {
        fork.setJvmArgs(options.getForkOptions().getJvmArgs());
        fork.setMinHeapSize(options.getForkOptions().getMemoryInitialSize());
        fork.setMaxHeapSize(options.getForkOptions().getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });
  }

  private List<String> findNamespaces() {
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

  private Set<File> getSourceRootsFiles() {
    return source.getSrcDirs();
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
