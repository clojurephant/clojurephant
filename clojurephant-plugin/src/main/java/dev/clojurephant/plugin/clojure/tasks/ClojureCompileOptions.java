package dev.clojurephant.plugin.clojure.tasks;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

public abstract class ClojureCompileOptions {
  @Input
  public abstract Property<Boolean> getDisableLocalsClearing();

  @Input
  public abstract ListProperty<String> getElideMeta();

  @Input
  public abstract Property<Boolean> getDirectLinking();
}
