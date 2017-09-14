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
package gradle_clojure.plugin.tasks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.*;
import org.gradle.process.JavaForkOptions;
import org.gradle.process.ProcessForkOptions;
import org.gradle.process.internal.DefaultJavaForkOptions;
import org.gradle.workers.WorkerExecutor;

import gradle_clojure.plugin.internal.ClojureWorkerExecutor;

public class ClojureTest extends ConventionTask implements JavaForkOptions {
  private static final Logger logger = Logging.getLogger(ClojureTest.class);

  private final ClojureWorkerExecutor workerExecutor;
  private final JavaForkOptions forkOptions;
  private FileCollection classpath = getProject().files();
  private Collection<String> namespaces = Collections.emptyList();
  private File junitReport = null;

  @Inject
  public ClojureTest(WorkerExecutor workerExecutor, FileResolver fileResolver) {
    this.workerExecutor = new ClojureWorkerExecutor(getProject(), workerExecutor);
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

    workerExecutor.submit(config -> {
      config.setClasspath(getClasspath());
      config.setNamespace("gradle-clojure.tools.clojure-test");
      config.setFunction("run-tests");
      config.setArgs(namespaces, getJunitReport());
      config.forkOptions(fork -> {
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
        this.copyTo(fork);
      });
    });
  }

  @Classpath
  public FileCollection getClasspath() {
    return classpath;
  }

  public void setClasspath(FileCollection classpath) {
    this.classpath = classpath;
  }

  @Input
  public Collection<String> getNamespaces() {
    return namespaces;
  }

  public void setNamespaces(Collection<String> namespaces) {
    this.namespaces = namespaces;
  }

  @Optional
  @OutputFile
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

  @Input
  public Map<String, Object> getEnvironment() {
    return forkOptions.getEnvironment();
  }

  @Input
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

  @Internal
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
