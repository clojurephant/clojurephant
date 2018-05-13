package gradle_clojure.plugin.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import gradle_clojure.plugin.internal.ClojureExecutor;
import gradle_clojure.plugin.internal.ExperimentalSettings;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.workers.WorkerExecutor;

public class ClojureNRepl extends DefaultTask {
  private static final Logger logger = Logging.getLogger(ClojureNRepl.class);

  private final ClojureExecutor clojureExecutor;
  private final AtomicBoolean failed = new AtomicBoolean(false);

  private ClojureForkOptions forkOptions = new ClojureForkOptions();
  private FileCollection classpath;
  private int port = -1;
  private int controlPort = -1;

  @Inject
  public ClojureNRepl(WorkerExecutor workerExecutor) {
    this.clojureExecutor = new ClojureExecutor(getProject(), workerExecutor);
  }

  @TaskAction
  public void run() throws InterruptedException {
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

    stopOnSignal();
    start();
  }

  private void start() {
    Action<ClojureExecSpec> action = spec -> {
      spec.setClasspath(getClasspath());
      spec.setMain("gradle-clojure.tools.clojure-nrepl");
      spec.setArgs(port, controlPort);
      spec.forkOptions(fork -> {
        fork.setJvmArgs(getForkOptions().getJvmArgs());
        fork.setMinHeapSize(getForkOptions().getMemoryInitialSize());
        fork.setMaxHeapSize(getForkOptions().getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    };
    if (ExperimentalSettings.isUseWorkers()) {
      clojureExecutor.submit(action);
      clojureExecutor.await();
    } else {
      clojureExecutor.exec(action);
    }
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

  @Option(option = "port", description = "Port the nREPL server should listen on.")
  public void setPort(String port) {
    setPort(Integer.parseInt(port));
  }
}
