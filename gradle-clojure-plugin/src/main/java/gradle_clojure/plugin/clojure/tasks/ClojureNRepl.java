package gradle_clojure.plugin.clojure.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gradle_clojure.plugin.common.internal.ClojureExecutor;
import gradle_clojure.plugin.common.internal.Edn;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.options.Option;

public class ClojureNRepl extends DefaultTask {
  private final ClojureExecutor clojureExecutor;

  private ForkOptions forkOptions = new ForkOptions();
  private FileCollection classpath;
  private int port = -1;
  private int controlPort = -1;
  private final Property<String> handler;
  private final ListProperty<String> userMiddleware;
  private final ListProperty<String> defaultMiddleware;
  private final Map<String, Object> contextData;

  public ClojureNRepl() {
    this.clojureExecutor = new ClojureExecutor(getProject());
    this.handler = getProject().getObjects().property(String.class);
    this.userMiddleware = getProject().getObjects().listProperty(String.class);
    this.defaultMiddleware = getProject().getObjects().listProperty(String.class);
    this.contextData = new HashMap<>();

    // task is never up-to-date, if you ask for REPL, you get REPL
    this.getOutputs().upToDateWhen(t -> false);
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
    if (!getProject().delete(getTemporaryDir())) {
      throw new GradleException("Cannot clean temporary directory: " + getTemporaryDir().getAbsolutePath());
    }

    FileCollection cp = getProject().files(getTemporaryDir(), getClasspath());
    List<String> middleware = Stream.of(defaultMiddleware.getOrElse(Collections.emptyList()), userMiddleware.getOrElse(Collections.emptyList()))
        .flatMap(List::stream)
        .collect(Collectors.toList());
    clojureExecutor.exec(spec -> {
      spec.setClasspath(cp);
      spec.setMain("gradle-clojure.tools.clojure-nrepl");
      spec.setArgs(port, controlPort, handler.getOrNull(), middleware, Edn.keywordize(contextData));
      spec.forkOptions(fork -> {
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
  public ForkOptions getForkOptions() {
    return forkOptions;
  }

  public ClojureNRepl forkOptions(Action<? super ForkOptions> configureAction) {
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

  @org.gradle.api.tasks.Optional
  @Input
  public Property<String> getHandler() {
    return handler;
  }

  @Option(option = "handler", description = "Qualified name of nREPL handler function.")
  public void setHandler(String handler) {
    this.handler.set(handler);
  }

  @Input
  public ListProperty<String> getMiddleware() {
    return userMiddleware;
  }

  @Option(option = "middleware", description = "Qualified names of nREPL middleware functions.")
  public void setMiddleware(List<String> middleware) {
    if (middleware != null) {
      this.userMiddleware.set(middleware);
    }
  }

  @Input
  public ListProperty<String> getDefaultMiddleware() {
    return defaultMiddleware;
  }

  @Internal
  public Map<String, Object> getContextData() {
    return contextData;
  }
}
