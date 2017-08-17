/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gradle_clojure.clojure.tasks;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

import gradle_clojure.clojure.tasks.internal.ClojureEval;
import gradle_clojure.clojure.tasks.internal.LineProcessingOutputStream;

public class ClojureCompile extends AbstractCompile {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);

  private final WorkerExecutor workerExecutor;

  private final ClojureCompileOptions options = new ClojureCompileOptions();

  private List<String> namespaces = Collections.emptyList();

  @Inject
  public ClojureCompile(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;
  }

  @Nested
  public ClojureCompileOptions getOptions() {
    return options;
  }

  @Input
  public List<String> getNamespaces() {
    return namespaces;
  }

  public void setNamespaces(List<String> namespaces) {
    this.namespaces = namespaces;
  }

  @TaskAction
  @Override
  public void compile() {
    getDestinationDir().mkdirs();

    if (options.isCopySourceSetToOutput()) {
      getProject().copy(spec -> {
        spec.from(getSource());
        spec.into(getDestinationDir());
      });
      return;
    }

    if (options.isAotCompile()) {
      Collection<String> namespaces = getNamespaces();
      if (namespaces.isEmpty()) {
        logger.warn("No Clojure namespaces defined, skipping {}", getName());
        return;
      }

      logger.info("Compiling {}", String.join(", ", namespaces));

      String script;
      try {
        script = Stream.of(
            "(try",
            "  (binding [*compile-path* \"" + getDestinationDir().getCanonicalPath().replace("\\", "\\\\") + "\"",
            "            *warn-on-reflection* " + options.getReflectionWarnings().isEnabled(),
            "            *compiler-options* {:disable-locals-clearing " + options.isDisableLocalsClearing(),
            "                                :elide-meta [" + options.getElideMeta().stream().map(it -> ":" + it).collect(Collectors.joining(" ")) + "]",
            "                                :direct-linking " + options.isDirectLinking() + "}]",
            "    " + namespaces.stream().map(it -> "(compile '" + it + ")").collect(Collectors.joining("\n    ")) + ")",
            "  (catch Throwable e",
            "    (println (.getMessage e))",
            "    (throw e)))").collect(Collectors.joining("\n")) + "\n";
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      LineProcessingOutputStream stdout = new LineProcessingOutputStream() {
        @Override
        protected void processLine(String line) {
          System.out.print(line);
        }
      };

      Set<String> sourceRoots = getSourceRoots();

      // this AtomInteger use is just to get around "effectively final"
      // requirement of anonymous classes. Should find a nicer way to handle
      // this
      AtomicInteger reflectionWarningCount = new AtomicInteger();
      AtomicInteger libraryReflectionWarningCount = new AtomicInteger();

      LineProcessingOutputStream stderr = new LineProcessingOutputStream() {
        @Override
        protected void processLine(String line) {
          if (line.startsWith(REFLECTION_WARNING_PREFIX)) {
            if (options.getReflectionWarnings().isProjectOnly()) {
              int colon = line.indexOf(':');
              String file = line.substring(REFLECTION_WARNING_PREFIX.length(), colon);
              boolean found = sourceRoots.stream().anyMatch(it -> new File(it, file).exists());
              if (found) {
                reflectionWarningCount.incrementAndGet();
                System.err.print(line);
              } else {
                libraryReflectionWarningCount.incrementAndGet();
              }
            } else {
              reflectionWarningCount.incrementAndGet();
              System.err.print(line);
            }
          } else {
            System.err.print(line);
          }
        }
      };

      try {
        executeScript(script, stdout, stderr);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      if (libraryReflectionWarningCount.get() > 0) {
        System.err.println(libraryReflectionWarningCount + " reflection warnings from dependencies");
      }
      if (options.getReflectionWarnings().isAsErrors() && reflectionWarningCount.get() > 0) {
        throw new GradleException(reflectionWarningCount + " reflection warnings found");
      }
    }
  }

  private void executeScript(String script, OutputStream stdout, OutputStream stderr) throws IOException {
    FileCollection classpath = getClasspath()
        .plus(getProject().files(getSourceRootsFiles()))
        .plus(getProject().files(getDestinationDir()));

    workerExecutor.submit(ClojureEval.class, config -> {
      config.setIsolationMode(IsolationMode.PROCESS);
      config.params(script);
      config.params(classpath.getAsPath());
      config.forkOptions(fork -> {
        fork.setJvmArgs(options.getForkOptions().getJvmArgs());
        fork.setMinHeapSize(options.getForkOptions().getMemoryInitialSize());
        fork.setMaxHeapSize(options.getForkOptions().getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });

    stdout.close();
    stderr.close();
  }

  public List<String> findNamespaces() {
    Set<String> roots = getSourceRoots();
    return StreamSupport.stream(getSource().spliterator(), false)
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

  private static final String REFLECTION_WARNING_PREFIX = "Reflection warning, ";

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
