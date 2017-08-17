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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.inject.Inject;

import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaForkOptions;
import org.gradle.process.ProcessForkOptions;
import org.gradle.process.internal.DefaultJavaForkOptions;

public class ClojureTest extends ConventionTask implements JavaForkOptions {
  private static final Logger logger = Logging.getLogger(ClojureTest.class);

  private final JavaForkOptions forkOptions;
  private FileCollection classpath = getProject().files();
  private Collection<String> namespaces = Collections.emptyList();
  private File junitReport = null;

  @Inject
  public ClojureTest(FileResolver fileResolver) {
    this.forkOptions = new DefaultJavaForkOptions(fileResolver);
  }

  @TaskAction
  public void test() {
    logger.info("Starting ClojureTestRunner task");

    Collection<String> namespaces = getNamespaces();
    if (namespaces.isEmpty()) {
      logger.warn("No Clojure namespaces defined, skipping {}", getName());
      return;
    }

    logger.info("Testing {}", String.join(", ", namespaces));

    String namespaceVec = "'[" + String.join(" ", namespaces) + "]";
    String runnerInvocation;
    if (getJunitReport() != null) {
      runnerInvocation = "(run-tests " + namespaceVec + " \"" + getJunitReport().getAbsolutePath() + "\")";
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

    getProject().javaexec(exec -> {
      copyTo(exec);
      exec.setMain("clojure.main");
      exec.setClasspath(getClasspath());
      exec.setArgs(Arrays.asList("-i", file.toAbsolutePath().toString()));
      exec.setDefaultCharacterEncoding("UTF-8");
    }).assertNormalExitValue();
  }

  private static String getTestRunnerScript() {
    try (
        InputStream stream = ClojureTest.class.getResourceAsStream("/gradle_clojure/test_runner.clj");
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
