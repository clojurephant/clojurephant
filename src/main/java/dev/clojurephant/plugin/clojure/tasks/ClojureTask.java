package dev.clojurephant.plugin.clojure.tasks;

import org.gradle.api.Task;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.jvm.toolchain.JavaLauncher;

public interface ClojureTask extends Task {
  @Nested
  @Optional
  public Property<JavaLauncher> getJavaLauncher();

  @Nested
  public ForkOptions getForkOptions();
}
