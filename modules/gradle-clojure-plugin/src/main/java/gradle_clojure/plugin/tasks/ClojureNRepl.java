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

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.DefaultTask;
import org.gradle.workers.WorkerExecutor;

import gradle_clojure.plugin.internal.ClojureWorkerExecutor;

import java.net.ServerSocket;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ClojureNRepl extends DefaultTask {
  private static final Logger logger = Logging.getLogger(ClojureNRepl.class);

  private final ClojureWorkerExecutor workerExecutor;

  private ClojureForkOptions forkOptions = new ClojureForkOptions();
  private FileCollection classpath;
  private int port = -1;
  private int controlPort = -1;

  @Inject
  public ClojureNRepl(WorkerExecutor workerExecutor) {
    this.workerExecutor = new ClojureWorkerExecutor(getProject(), workerExecutor);
  }

  @TaskAction
  public void run() {
    if (port < 0) {
      try (ServerSocket socket = new ServerSocket(0)) {
        port = socket.getLocalPort();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    try (ServerSocket socket = new ServerSocket(0)) {
      controlPort = socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    start();
    stopOnSignal();
    workerExecutor.await();
  }

  private void start() {
    workerExecutor.submit(config -> {
      config.setClasspath(getClasspath());
      config.setNamespace("gradle-clojure.tools.clojure-nrepl");
      config.setFunction("start!");
      config.setArgs(port, controlPort);
      config.forkOptions(fork -> {
        fork.setJvmArgs(getForkOptions().getJvmArgs());
        fork.setMinHeapSize(getForkOptions().getMemoryInitialSize());
        fork.setMaxHeapSize(getForkOptions().getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });
  }

  /**
   * Waits for a EOF (CTRL+D) signal from System.in. When received it will stop the NREPL server.
   * Using a separate thread, since this loop can prevent seeing errors during REPL startup.
   */
  private void stopOnSignal() {
    Runnable waitLoop = () -> {
      while (true) {
        try {
          int c = System.in.read();
          if (c == -1 || c == 4) {
            // Stop on Ctrl-D or EOF
            stop();
            break;
          }
        } catch (IOException e) {
          stop();
          throw new UncheckedIOException(e);
        }
      }
    };
    new Thread(waitLoop).start();
  }

  private void stop() {
    try (
        SocketChannel socket = SocketChannel.open();
        PrintWriter writer = new PrintWriter(Channels.newWriter(socket, StandardCharsets.UTF_8.name()), true);
        BufferedReader reader = new BufferedReader(Channels.newReader(socket, StandardCharsets.UTF_8.name()))) {
      socket.connect(new InetSocketAddress("localhost", controlPort));
    } catch (IOException e) {
      // bury
    }
  }

  @Nested
  public ClojureForkOptions getForkOptions() {
    return forkOptions;
  }

  public ClojureNRepl forkOptions(Action<? super ClojureForkOptions> configureAction) {
    configureAction.execute(forkOptions);
    return this;
  }

  @Classpath
  public FileCollection getClasspath() {
    return classpath;
  }

  public void setClasspath(FileCollection classpath) {
    this.classpath = classpath;
  }

  @Input
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }
}
