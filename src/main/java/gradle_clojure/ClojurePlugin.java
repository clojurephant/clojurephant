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
package gradle_clojure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaForkOptions;
import org.gradle.process.ProcessForkOptions;
import org.gradle.process.internal.DefaultJavaForkOptions;
import org.gradle.process.internal.ExecException;
import org.gradle.process.internal.JavaExecHandleBuilder;

public class ClojurePlugin implements Plugin<Project> {
  private static final Logger logger = Logging.getLogger(ClojurePlugin.class);

  @Override
  public void apply(Project project) {
    logger.info("Applying ClojurePlugin");
    project.getPlugins().apply(JavaBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    JavaPluginConvention javaPluginConvention =
        project.getConvention().getPlugin(JavaPluginConvention.class);

    javaPluginConvention
        .getSourceSets()
        .all(
            sourceSet -> {
              ClojureCompile compileTask = createCompileTask(project, sourceSet);

              if (SourceSet.TEST_SOURCE_SET_NAME.equals(sourceSet.getName())) {
                ClojureTestRunner testTask = createTestTask(project);

                testTask
                    .getConventionMapping()
                    .map("classpath", () -> sourceSet.getRuntimeClasspath());
                testTask
                    .getConventionMapping()
                    .map("namespaces", () -> compileTask.findNamespaces());

                testTask.dependsOn(compileTask);
              }
            });
  }

  private ClojureCompile createCompileTask(Project project, SourceSet sourceSet) {
    ProjectInternal projectInternal = (ProjectInternal) project;
    String sourceRootDir = String.format("src/%s/clojure", sourceSet.getName());

    logger.info("Creating DefaultSourceDirectorySet for source set {}", sourceSet);
    ClojureSourceSet clojureSrcSet =
        new ClojureSourceSetImpl(sourceSet.getName(), projectInternal.getFileResolver());
    SourceDirectorySet clojureDirSet = clojureSrcSet.getClojure();

    new DslObject(sourceSet).getConvention().getPlugins().put("clojure", clojureSrcSet);

    File srcDir = project.file(sourceRootDir);
    logger.info(
        "Creating Clojure SourceDirectorySet for source set "
            + sourceSet
            + " with src dir "
            + srcDir);
    clojureDirSet.srcDir(srcDir);

    logger.info(
        "Adding ClojureSourceDirectorySet " + clojureDirSet + " to source set " + sourceSet);
    sourceSet.getAllSource().source(clojureDirSet);
    sourceSet.getResources().getFilter().exclude(it -> clojureDirSet.contains(it.getFile()));

    String name = sourceSet.getCompileTaskName("clojure");
    Class<ClojureCompile> compilerClass = ClojureCompile.class;
    logger.info("Creating Clojure compile task " + name + " with class " + compilerClass);
    ClojureCompile compile = project.getTasks().create(name, compilerClass);
    compile.setDescription("Compiles the " + sourceSet + " Clojure code");

    Task javaTask = project.getTasks().findByName(sourceSet.getCompileJavaTaskName());
    if (javaTask != null) {
      compile.dependsOn(javaTask);
    }

    project.getTasks().findByName(sourceSet.getClassesTaskName()).dependsOn(compile);

    compile.getConventionMapping().map("classpath", () -> sourceSet.getCompileClasspath());
    compile.getConventionMapping().map("namespaces", () -> compile.findNamespaces());
    compile
        .getConventionMapping()
        .map("destinationDir", () -> sourceSet.getOutput().getClassesDir());

    compile.source(clojureDirSet);

    return compile;
  }

  private ClojureTestRunner createTestTask(Project project) {
    String name = "testClojure";
    Class<ClojureTestRunner> testRunnerClass = ClojureTestRunner.class;

    ClojureTestRunner testRunner = project.getTasks().create(name, testRunnerClass);
    project.getTasks().findByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(testRunner);
    testRunner.setDescription("Runs the Clojure tests");
    testRunner.setGroup(JavaBasePlugin.VERIFICATION_GROUP);

    testRunner.getOutputs().upToDateWhen(task -> false);

    return testRunner;
  }

  public interface ClojureSourceSet {
    SourceDirectorySet getClojure();

    ClojureSourceSet clojure(Action<? super SourceDirectorySet> configureAction);
  }

  public static class ClojureSourceSetImpl implements ClojureSourceSet {
    private final String displayName;
    private final FileResolver resolver;
    private final SourceDirectorySet clojure;

    public ClojureSourceSetImpl(String displayName, FileResolver resolver) {
      this.displayName = displayName;
      this.resolver = resolver;
      this.clojure =
          new DefaultSourceDirectorySet(
              displayName, resolver, new DefaultDirectoryFileTreeFactory());
      this.clojure.getFilter().include("**/*.clj", "**/*.cljc");
    }

    @Override
    public SourceDirectorySet getClojure() {
      return clojure;
    }

    @Override
    public ClojureSourceSet clojure(Action<? super SourceDirectorySet> configureAction) {
      configureAction.execute(clojure);
      return this;
    }
  }

  public static class ReflectionWarnings {
    private Boolean enabled;
    private Boolean projectOnly;
    private Boolean asErrors;

    public ReflectionWarnings(Boolean enabled, Boolean projectOnly, Boolean asErrors) {
      this.enabled = enabled;
      this.projectOnly = projectOnly;
      this.asErrors = asErrors;
    }

    public Boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    public Boolean getProjectOnly() {
      return projectOnly;
    }

    public void setProjectOnly(Boolean projectOnly) {
      this.projectOnly = projectOnly;
    }

    public Boolean getAsErrors() {
      return asErrors;
    }

    public void setAsErrors(Boolean asErrors) {
      this.asErrors = asErrors;
    }
  }

  public static class ClojureCompile extends AbstractCompile implements JavaForkOptions {
    private final FileResolver fileResolver;
    private final JavaForkOptions forkOptions;

    private Boolean aotCompile = false;
    private Boolean copySourceSetToOutput = null;
    private ReflectionWarnings reflectionWarnings = new ReflectionWarnings(false, false, false);

    private Boolean disableLocalsClearing = false;
    private Collection<String> elideMeta = Collections.emptyList();
    private Boolean directLinking = false;

    private Collection<String> namespaces = Collections.emptyList();

    @Inject
    public ClojureCompile(FileResolver fileResolver) {
      this.fileResolver = fileResolver;
      this.forkOptions = new DefaultJavaForkOptions(fileResolver);
    }

    @TaskAction
    @Override
    public void compile() {
      logger.info("Starting ClojureCompiler task");

      File tmpDestinationDir = new File(getTemporaryDir(), "classes");
      removeObsoleteClassFiles(getDestinationDir().toPath(), tmpDestinationDir.toPath());

      try {
        if (Files.exists(tmpDestinationDir.toPath())) {
          Files.walkFileTree(
              tmpDestinationDir.toPath(),
              new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                  Files.delete(file);
                  return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                  Files.delete(dir);
                  return FileVisitResult.CONTINUE;
                }
              });
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      tmpDestinationDir.mkdirs();
      getDestinationDir().mkdirs();

      if (getCopySourceSetToOutput() == null ? !getAotCompile() : getCopySourceSetToOutput()) {
        getProject()
            .copy(
                spec -> {
                  spec.from(getSource()).into(tmpDestinationDir);
                });
        // copy to destination
        getProject()
            .copy(
                spec -> {
                  spec.from(tmpDestinationDir);
                  spec.into(getDestinationDir());
                });
        return;
      }

      if (getAotCompile()) {
        logger.info("Destination: {}", getDestinationDir());

        Collection<String> namespaces = getNamespaces();
        if (namespaces.isEmpty()) {
          logger.info("No Clojure namespaces defined, skipping {}", getName());
          return;
        }

        logger.info("Compiling {}", String.join(", ", namespaces));

        String script;
        try {
          script =
              Stream.of(
                      "(try",
                      "  (binding [*compile-path* \""
                          + tmpDestinationDir.getCanonicalPath().replace("\\", "\\\\")
                          + "\"",
                      "            *warn-on-reflection* " + getReflectionWarnings().getEnabled(),
                      "            *compiler-options* {:disable-locals-clearing "
                          + getDisableLocalsClearing(),
                      "                                :elide-meta ["
                          + getElideMeta()
                              .stream()
                              .map(it -> ":" + it)
                              .collect(Collectors.joining(" "))
                          + "]",
                      "                                :direct-linking "
                          + getDirectLinking()
                          + "}]",
                      "    "
                          + namespaces
                              .stream()
                              .map(it -> "(compile '" + it + ")")
                              .collect(Collectors.joining("\n    "))
                          + ")",
                      "  (catch Throwable e",
                      "    (.printStackTrace e)",
                      "    (System/exit 1)))",
                      "(System/exit 0)")
                  .collect(Collectors.joining("\n"));
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }

        LineProcessingOutputStream stdout =
            new LineProcessingOutputStream() {
              @Override
              protected void processLine(String line) {
                System.out.print(line);
              }
            };

        Set<String> sourceRoots = getSourceRoots();

        // this AtomInteger use is just to get around "effectively final" requirement of anonymous classes. Should find a nicer way to handle this
        AtomicInteger reflectionWarningCount = new AtomicInteger();
        AtomicInteger libraryReflectionWarningCount = new AtomicInteger();

        LineProcessingOutputStream stderr =
            new LineProcessingOutputStream() {
              @Override
              protected void processLine(String line) {
                if (line.startsWith(REFLECTION_WARNING_PREFIX)) {
                  if (getReflectionWarnings().getProjectOnly()) {
                    int colon = line.indexOf(':');
                    String file = line.substring(REFLECTION_WARNING_PREFIX.length(), colon);
                    boolean found =
                        sourceRoots.stream().anyMatch(it -> new File(it, file).exists());
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

        // copy to destination
        getProject()
            .copy(
                spec -> {
                  spec.from(tmpDestinationDir);
                  spec.into(getDestinationDir());
                });

        if (libraryReflectionWarningCount.get() > 0) {
          System.err.println(
              libraryReflectionWarningCount + " reflection warnings from dependencies");
        }
        if (getReflectionWarnings().getAsErrors() && reflectionWarningCount.get() > 0) {
          throw new ExecException(reflectionWarningCount + " reflection warnings found");
        }
      }
    }

    private void removeObsoleteClassFiles(Path destinationDir, Path tmpDestinationDir) {
      try {
        if (!Files.exists(tmpDestinationDir)) {
          return;
        }
        Files.walkFileTree(
            tmpDestinationDir,
            new SimpleFileVisitor<Path>() {
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                  throws IOException {
                Path destinationFile = destinationDir.resolve(tmpDestinationDir.relativize(file));
                if (Files.exists(destinationFile)) {
                  Files.delete(destinationFile);
                }
                return FileVisitResult.CONTINUE;
              }
            });
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    private void executeScript(String script, OutputStream stdout, OutputStream stderr)
        throws IOException {
      Path file = Files.createTempFile(getTemporaryDir().toPath(), "clojure-compiler", ".clj");
      Files.write(file, (script + "\n").getBytes(StandardCharsets.UTF_8));

      JavaExecHandleBuilder exec = new JavaExecHandleBuilder(fileResolver);
      copyTo(exec);
      exec.setMain("clojure.main");
      exec.setClasspath(
          getClasspath()
              .plus(new SimpleFileCollection(getSourceRootsFiles()))
              .plus(new SimpleFileCollection(getDestinationDir())));
      exec.setArgs(Arrays.asList("-i", file.toAbsolutePath().toString()));
      exec.setDefaultCharacterEncoding("UTF-8");

      exec.setStandardOutput(stdout);
      exec.setErrorOutput(stderr);

      ExecResult result = exec.build().start().waitForFinish();

      stdout.close();
      stderr.close();

      result.assertNormalExitValue();
    }

    private List<String> findNamespaces() {
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
      return getSourceRootsFiles()
          .stream()
          .map(
              it -> {
                try {
                  return it.getCanonicalPath();
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              })
          .collect(Collectors.toSet());
    }

    private List<File> getSourceRootsFiles() {
      // accessing the List<Object> field not the FileTree from getSource
      return source
          .stream()
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

    private static final Map<String, Character> DEMUNGE_MAP =
        CHAR_MAP
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private static final Pattern DEMUNGE_PATTERN =
        Pattern.compile(
            DEMUNGE_MAP
                .keySet()
                .stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(it -> "\\Q" + it + "\\E")
                .collect(Collectors.joining("|")));

    private static final String REFLECTION_WARNING_PREFIX = "Reflection warning, ";

    private static String munge(String name) {
      StringBuilder sb = new StringBuilder();
      name.chars()
          .forEach(
              c -> {
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

    public Boolean getAotCompile() {
      return aotCompile;
    }

    public void setAotCompile(Boolean aotCompile) {
      this.aotCompile = aotCompile;
    }

    public Boolean getCopySourceSetToOutput() {
      return copySourceSetToOutput;
    }

    public void setCopySourceSetToOutput(Boolean copySourceSetToOutput) {
      this.copySourceSetToOutput = copySourceSetToOutput;
    }

    public ReflectionWarnings getReflectionWarnings() {
      return reflectionWarnings;
    }

    public void setReflectionWarnings(ReflectionWarnings reflectionWarnings) {
      this.reflectionWarnings = reflectionWarnings;
    }

    public ReflectionWarnings reflectionWarnings(Action<ReflectionWarnings> configureAction) {
      configureAction.execute(reflectionWarnings);
      return reflectionWarnings;
    }

    public Boolean getDisableLocalsClearing() {
      return disableLocalsClearing;
    }

    public void setDisableLocalsClearing(Boolean disableLocalsClearing) {
      this.disableLocalsClearing = disableLocalsClearing;
    }

    public Collection<String> getElideMeta() {
      return elideMeta;
    }

    public void setElideMeta(Collection<String> elideMeta) {
      this.elideMeta = elideMeta;
    }

    public Boolean getDirectLinking() {
      return directLinking;
    }

    public void setDirectLinking(Boolean directLinking) {
      this.directLinking = directLinking;
    }

    public Collection<String> getNamespaces() {
      return namespaces;
    }

    public void setNamespaces(Collection<String> namespaces) {
      this.namespaces = namespaces;
    }

    public FileResolver getFileResolver() {
      return fileResolver;
    }

    public JavaForkOptions getForkOptions() {
      return forkOptions;
    }

    public JavaForkOptions bootstrapClasspath(Object... arg0) {
      return forkOptions.bootstrapClasspath(arg0);
    }

    public JavaForkOptions copyTo(JavaForkOptions arg0) {
      return forkOptions.copyTo(arg0);
    }

    public ProcessForkOptions copyTo(ProcessForkOptions arg0) {
      return forkOptions.copyTo(arg0);
    }

    public ProcessForkOptions environment(Map<String, ?> arg0) {
      return forkOptions.environment(arg0);
    }

    public ProcessForkOptions environment(String arg0, Object arg1) {
      return forkOptions.environment(arg0, arg1);
    }

    public ProcessForkOptions executable(Object arg0) {
      return forkOptions.executable(arg0);
    }

    public List<String> getAllJvmArgs() {
      return forkOptions.getAllJvmArgs();
    }

    public FileCollection getBootstrapClasspath() {
      return forkOptions.getBootstrapClasspath();
    }

    public boolean getDebug() {
      return forkOptions.getDebug();
    }

    public String getDefaultCharacterEncoding() {
      return forkOptions.getDefaultCharacterEncoding();
    }

    public boolean getEnableAssertions() {
      return forkOptions.getEnableAssertions();
    }

    public Map<String, Object> getEnvironment() {
      return forkOptions.getEnvironment();
    }

    public String getExecutable() {
      return forkOptions.getExecutable();
    }

    public List<String> getJvmArgs() {
      return forkOptions.getJvmArgs();
    }

    public String getMaxHeapSize() {
      return forkOptions.getMaxHeapSize();
    }

    public String getMinHeapSize() {
      return forkOptions.getMinHeapSize();
    }

    public Map<String, Object> getSystemProperties() {
      return forkOptions.getSystemProperties();
    }

    public File getWorkingDir() {
      return forkOptions.getWorkingDir();
    }

    public JavaForkOptions jvmArgs(Iterable<?> arg0) {
      return forkOptions.jvmArgs(arg0);
    }

    public JavaForkOptions jvmArgs(Object... arg0) {
      return forkOptions.jvmArgs(arg0);
    }

    public void setAllJvmArgs(List<String> arg0) {
      forkOptions.setAllJvmArgs(arg0);
    }

    public void setAllJvmArgs(Iterable<?> arg0) {
      forkOptions.setAllJvmArgs(arg0);
    }

    public void setBootstrapClasspath(FileCollection arg0) {
      forkOptions.setBootstrapClasspath(arg0);
    }

    public void setDebug(boolean arg0) {
      forkOptions.setDebug(arg0);
    }

    public void setDefaultCharacterEncoding(String arg0) {
      forkOptions.setDefaultCharacterEncoding(arg0);
    }

    public void setEnableAssertions(boolean arg0) {
      forkOptions.setEnableAssertions(arg0);
    }

    public void setEnvironment(Map<String, ?> arg0) {
      forkOptions.setEnvironment(arg0);
    }

    public void setExecutable(String arg0) {
      forkOptions.setExecutable(arg0);
    }

    public void setExecutable(Object arg0) {
      forkOptions.setExecutable(arg0);
    }

    public void setJvmArgs(List<String> arg0) {
      forkOptions.setJvmArgs(arg0);
    }

    public void setJvmArgs(Iterable<?> arg0) {
      forkOptions.setJvmArgs(arg0);
    }

    public void setMaxHeapSize(String arg0) {
      forkOptions.setMaxHeapSize(arg0);
    }

    public void setMinHeapSize(String arg0) {
      forkOptions.setMinHeapSize(arg0);
    }

    public void setSystemProperties(Map<String, ?> arg0) {
      forkOptions.setSystemProperties(arg0);
    }

    public void setWorkingDir(File arg0) {
      forkOptions.setWorkingDir(arg0);
    }

    public void setWorkingDir(Object arg0) {
      forkOptions.setWorkingDir(arg0);
    }

    public JavaForkOptions systemProperties(Map<String, ?> arg0) {
      return forkOptions.systemProperties(arg0);
    }

    public JavaForkOptions systemProperty(String arg0, Object arg1) {
      return forkOptions.systemProperty(arg0, arg1);
    }

    public ProcessForkOptions workingDir(Object arg0) {
      return forkOptions.workingDir(arg0);
    }
  }

  public static class ClojureTestRunner extends ConventionTask implements JavaForkOptions {
    private final FileResolver fileResolver;
    private final JavaForkOptions forkOptions;
    private FileCollection classpath = new SimpleFileCollection();
    private Collection<String> namespaces = Collections.emptyList();
    private File junitReport = null;

    @Inject
    public ClojureTestRunner(FileResolver fileResolver) {
      this.fileResolver = fileResolver;
      this.forkOptions = new DefaultJavaForkOptions(fileResolver);
    }

    @TaskAction
    public void test() {
      logger.info("Starting ClojureTestRunner task");

      Collection<String> namespaces = getNamespaces();
      if (namespaces.isEmpty()) {
        logger.info("No Clojure namespaces defined, skipping {}", getName());
        return;
      }

      logger.info("Testing {}", String.join(", ", namespaces));

      String namespaceVec = "'[" + String.join(" ", namespaces) + "]";
      String runnerInvocation;
      if (getJunitReport() != null) {
        runnerInvocation =
            "(run-tests " + namespaceVec + " \"" + getJunitReport().getAbsolutePath() + "\")";
      } else {
        runnerInvocation = "(run-tests " + namespaceVec + ")";
      }

      String script = getTestRunnerScript() + "\n" + runnerInvocation;

      try {
        executeScript(script);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    private void executeScript(String script) throws IOException {
      Path file = Files.createTempFile(getTemporaryDir().toPath(), "clojure-test-runner", ".clj");
      Files.write(file, (script + "\n").getBytes(StandardCharsets.UTF_8));

      JavaExecHandleBuilder exec = new JavaExecHandleBuilder(fileResolver);
      copyTo(exec);
      exec.setMain("clojure.main");
      exec.setClasspath(getClasspath());
      exec.setArgs(Arrays.asList("-i", file.toAbsolutePath().toString()));
      exec.setDefaultCharacterEncoding("UTF-8");

      exec.build().start().waitForFinish().assertNormalExitValue();
    }

    private static String getTestRunnerScript() {
      try (InputStream stream =
              ClojureTestRunner.class.getResourceAsStream("/gradle_clojure/test_runner.clj");
          Scanner scanner = new Scanner(stream)) {
        scanner.useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public FileCollection getClasspath() {
      return classpath;
    }

    public void setClasspath(FileCollection classpath) {
      this.classpath = classpath;
    }

    public Collection<String> getNamespaces() {
      return namespaces;
    }

    public void setNamespaces(Collection<String> namespaces) {
      this.namespaces = namespaces;
    }

    public File getJunitReport() {
      return junitReport;
    }

    public void setJunitReport(File junitReport) {
      this.junitReport = junitReport;
    }

    public JavaForkOptions bootstrapClasspath(Object... arg0) {
      return forkOptions.bootstrapClasspath(arg0);
    }

    public JavaForkOptions copyTo(JavaForkOptions arg0) {
      return forkOptions.copyTo(arg0);
    }

    public ProcessForkOptions copyTo(ProcessForkOptions arg0) {
      return forkOptions.copyTo(arg0);
    }

    public ProcessForkOptions environment(Map<String, ?> arg0) {
      return forkOptions.environment(arg0);
    }

    public ProcessForkOptions environment(String arg0, Object arg1) {
      return forkOptions.environment(arg0, arg1);
    }

    public ProcessForkOptions executable(Object arg0) {
      return forkOptions.executable(arg0);
    }

    public List<String> getAllJvmArgs() {
      return forkOptions.getAllJvmArgs();
    }

    public FileCollection getBootstrapClasspath() {
      return forkOptions.getBootstrapClasspath();
    }

    public boolean getDebug() {
      return forkOptions.getDebug();
    }

    public String getDefaultCharacterEncoding() {
      return forkOptions.getDefaultCharacterEncoding();
    }

    public boolean getEnableAssertions() {
      return forkOptions.getEnableAssertions();
    }

    public Map<String, Object> getEnvironment() {
      return forkOptions.getEnvironment();
    }

    public String getExecutable() {
      return forkOptions.getExecutable();
    }

    public List<String> getJvmArgs() {
      return forkOptions.getJvmArgs();
    }

    public String getMaxHeapSize() {
      return forkOptions.getMaxHeapSize();
    }

    public String getMinHeapSize() {
      return forkOptions.getMinHeapSize();
    }

    public Map<String, Object> getSystemProperties() {
      return forkOptions.getSystemProperties();
    }

    public File getWorkingDir() {
      return forkOptions.getWorkingDir();
    }

    public JavaForkOptions jvmArgs(Iterable<?> arg0) {
      return forkOptions.jvmArgs(arg0);
    }

    public JavaForkOptions jvmArgs(Object... arg0) {
      return forkOptions.jvmArgs(arg0);
    }

    public void setAllJvmArgs(List<String> arg0) {
      forkOptions.setAllJvmArgs(arg0);
    }

    public void setAllJvmArgs(Iterable<?> arg0) {
      forkOptions.setAllJvmArgs(arg0);
    }

    public void setBootstrapClasspath(FileCollection arg0) {
      forkOptions.setBootstrapClasspath(arg0);
    }

    public void setDebug(boolean arg0) {
      forkOptions.setDebug(arg0);
    }

    public void setDefaultCharacterEncoding(String arg0) {
      forkOptions.setDefaultCharacterEncoding(arg0);
    }

    public void setEnableAssertions(boolean arg0) {
      forkOptions.setEnableAssertions(arg0);
    }

    public void setEnvironment(Map<String, ?> arg0) {
      forkOptions.setEnvironment(arg0);
    }

    public void setExecutable(String arg0) {
      forkOptions.setExecutable(arg0);
    }

    public void setExecutable(Object arg0) {
      forkOptions.setExecutable(arg0);
    }

    public void setJvmArgs(List<String> arg0) {
      forkOptions.setJvmArgs(arg0);
    }

    public void setJvmArgs(Iterable<?> arg0) {
      forkOptions.setJvmArgs(arg0);
    }

    public void setMaxHeapSize(String arg0) {
      forkOptions.setMaxHeapSize(arg0);
    }

    public void setMinHeapSize(String arg0) {
      forkOptions.setMinHeapSize(arg0);
    }

    public void setSystemProperties(Map<String, ?> arg0) {
      forkOptions.setSystemProperties(arg0);
    }

    public void setWorkingDir(File arg0) {
      forkOptions.setWorkingDir(arg0);
    }

    public void setWorkingDir(Object arg0) {
      forkOptions.setWorkingDir(arg0);
    }

    public JavaForkOptions systemProperties(Map<String, ?> arg0) {
      return forkOptions.systemProperties(arg0);
    }

    public JavaForkOptions systemProperty(String arg0, Object arg1) {
      return forkOptions.systemProperty(arg0, arg1);
    }

    public ProcessForkOptions workingDir(Object arg0) {
      return forkOptions.workingDir(arg0);
    }
  }
}
