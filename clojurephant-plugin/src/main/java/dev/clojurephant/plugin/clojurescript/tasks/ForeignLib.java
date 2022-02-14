package dev.clojurephant.plugin.clojurescript.tasks;

import org.gradle.api.Named;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public abstract class ForeignLib implements Named {
  @Input
  public abstract Property<String> getFile();

  @Input
  @Optional
  public abstract Property<String> getFileMin();

  @Input
  @Optional
  public abstract ListProperty<String> getProvides();

  @Input
  @Optional
  public abstract ListProperty<String> getRequires();

  @Input
  @Optional
  public abstract Property<String> getModuleType();

  @Input
  @Optional
  public abstract Property<String> getPreprocess();

  @Input
  @Optional
  public abstract MapProperty<String, String> getGlobalExports();
}
