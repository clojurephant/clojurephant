package dev.clojurephant.plugin.common.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gradle.api.Action;
import org.gradle.process.ExecOperations;

public class Prepl {
  private final ExecOperations execOperations;

  public Prepl(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  public PreplClient start(Action<PreplSpec> action) {
    PreplSpec spec = new PreplSpec();
    action.execute(spec);
    return start(spec);
  }

  public PreplClient start(PreplSpec preplSpec) {
    int port;
    if (preplSpec.getPort() == 0) {
      try (ServerSocket socket = new ServerSocket(0)) {
        port = socket.getLocalPort();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    } else {
      port = preplSpec.getPort();
    }

    AtomicBoolean stopper = new AtomicBoolean(false);

    new Thread(() -> {
      try {
        execOperations.javaexec(spec -> {
          if (preplSpec.getJavaLauncher() != null) {
            spec.setExecutable(preplSpec.getJavaLauncher().getExecutablePath());
          }
          spec.getMainClass().set("clojure.main");
          spec.systemProperty("clojure.server.clojurephant", String.format("{:port %d :accept clojure.core.server/io-prepl :client-daemon false}", port));
          spec.args("-e", "(do (ns dev.clojurephant.prepl) (def connected (promise)) @connected nil)");
          spec.setClasspath(preplSpec.getClasspath());
          preplSpec.getConfigureFork().forEach(forkAction -> forkAction.execute(spec));
        }).assertNormalExitValue();
      } catch (Throwable e) {
        stopper.set(true);
      }
    }).start();

    return PreplClient.socketConnect(InetAddress.getLoopbackAddress(), port, stopper);
  }
}
