package dev.clojurephant.plugin.common.internal;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.file.FileCollection;
import org.gradle.process.JavaForkOptions;

public class PreplSpec {
  private FileCollection classpath;
  private int port;
  private List<Action<JavaForkOptions>> configureFork = new ArrayList<>();

  public FileCollection getClasspath() {
    return classpath;
  }

  public void setClasspath(FileCollection classpath) {
    this.classpath = classpath;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public List<Action<JavaForkOptions>> getConfigureFork() {
    return configureFork;
  }

  public void forkOptions(Action<JavaForkOptions> configureFork) {
    this.configureFork.add(configureFork);
  }
}
