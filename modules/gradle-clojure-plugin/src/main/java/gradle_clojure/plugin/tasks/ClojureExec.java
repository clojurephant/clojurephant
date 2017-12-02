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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.workers.WorkerExecutor;

import gradle_clojure.plugin.internal.ClojureWorkerExecutor;

public class ClojureExec extends DefaultTask {
  private static final Logger logger = Logging.getLogger(ClojureExec.class);

  private final ClojureWorkerExecutor workerExecutor;

  private FileCollection classpath;
  private String namespace;
  private String function;
  private List<Object> args = new ArrayList<>();

  @Inject
  public ClojureExec(WorkerExecutor workerExecutor) {
    this.workerExecutor = new ClojureWorkerExecutor(getProject(), workerExecutor);
  }

  @Classpath
  public FileCollection getClasspath() {
    return classpath;
  }

  public void setClasspath(FileCollection classpath) {
    this.classpath = classpath;
  }

  @Input
  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @Input
  public String getFunction() {
    return function;
  }

  public void setFunction(String function) {
    this.function = function;
  }

  @Input
  public List<Object> getArgs() {
    return args;
  }

  public void setArgs(List<Object> args) {
    this.args = args;
  }

  public void args(Object... args) {
    Collections.addAll(this.args, args);
  }

  @TaskAction
  public void exec() {
    workerExecutor.submit(config -> {
      config.setClasspath(getClasspath());
      config.setNamespace(getNamespace());
      config.setFunction(getFunction());
      config.setArgs(getArgs().toArray(new Object[getArgs().size()]));
      config.forkOptions(fork -> {
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });
  }
}
