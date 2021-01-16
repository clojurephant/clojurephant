package dev.clojurephant.plugin.common.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.gradle.api.Action;
import org.gradle.api.Project;

public class Prepl {
  private final Project project;

  public Prepl(Project project) {
    this.project = project;
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

    new Thread(() -> {
      project.javaexec(spec -> {
        spec.setMain("clojure.main");
        spec.systemProperty("clojure.server.clojurephant", String.format("{:port %d :accept clojure.core.server/io-prepl :client-daemon false}", port));
        spec.args("-e", "(do (ns dev.clojurephant.prepl) (def connected (promise)) @connected nil)");
        spec.setClasspath(preplSpec.getClasspath());
        preplSpec.getConfigureFork().forEach(forkAction -> forkAction.execute(spec));
      });
    }).start();

    return PreplClient.socketConnect(InetAddress.getLoopbackAddress(), port);
  }
}
