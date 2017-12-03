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
package gradle_clojure.plugin.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.process.JavaForkOptions;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class ClojureWorkerExecutor {
  private static final String SHIMDANDY_VERSION = "1.2.0";
  private static final String NREPL_VERSION = "0.2.12";
  private static final String GRADLE_CLOJURE_VERSION = getVersion();

  private final Project project;
  private final WorkerExecutor workerExecutor;

  public ClojureWorkerExecutor(Project project, WorkerExecutor workerExecutor) {
    this.project = project;
    this.workerExecutor = workerExecutor;
  }

  public void await() {
    workerExecutor.await();
  }

  public void submit(Action<ClojureWorkerConfiguration> action) {
    ClojureWorkerConfiguration config = new ClojureWorkerConfiguration();
    action.execute(config);

    FileCollection realClasspath = config.getClasspath().plus(resolveShim());
    workerExecutor.submit(ClojureWorker.class, worker -> {
      worker.setIsolationMode(IsolationMode.PROCESS);
      worker.params(config.getNamespace(), config.getFunction(), config.getArgs());
      config.getConfigureFork().forEach(worker::forkOptions);
      worker.forkOptions(fork -> fork.systemProperty("shim.classpath", realClasspath.getAsPath()));
      worker.classpath(resolveWorker());
    });
  }

  private FileCollection resolveWorker() {
    Dependency shim = project.getDependencies().create("org.projectodd.shimdandy:shimdandy-api:" + SHIMDANDY_VERSION);
    return project.getConfigurations().detachedConfiguration(shim);
  }

  private FileCollection resolveShim() {
    Dependency shimImpl = project.getDependencies().create("org.projectodd.shimdandy:shimdandy-impl:" + SHIMDANDY_VERSION);
    Dependency tools = project.getDependencies().create("io.github.gradle-clojure:gradle-clojure-tools:" + GRADLE_CLOJURE_VERSION);
    Dependency nrepl = project.getDependencies().create("org.clojure:tools.nrepl:" + NREPL_VERSION);
    return project.getConfigurations().detachedConfiguration(shimImpl, tools, nrepl).setTransitive(false);
  }

  private static String getVersion() {
    try (InputStream stream = ClojureWorkerExecutor.class.getResourceAsStream("/gradle-clojure.properties")) {
      Properties props = new Properties();
      props.load(stream);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static class ClojureWorkerConfiguration {
    private FileCollection classpath;
    private String namespace;
    private String function;
    private Object[] args = new Object[0];
    private List<Action<JavaForkOptions>> configureFork = new ArrayList<>();

    public FileCollection getClasspath() {
      return classpath;
    }

    public void setClasspath(FileCollection classpath) {
      this.classpath = classpath;
    }

    public String getNamespace() {
      return namespace;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public String getFunction() {
      return function;
    }

    public void setFunction(String function) {
      this.function = function;
    }

    public Object[] getArgs() {
      return args;
    }

    public void setArgs(Object... args) {
      this.args = args;
    }

    public List<Action<JavaForkOptions>> getConfigureFork() {
      return configureFork;
    }

    public void forkOptions(Action<JavaForkOptions> configureFork) {
      this.configureFork.add(configureFork);
    }
  }
}
