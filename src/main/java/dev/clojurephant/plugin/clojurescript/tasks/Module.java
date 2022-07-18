package dev.clojurephant.plugin.clojurescript.tasks;

import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

public abstract class Module implements Named {
  @Internal
  @Override
  public abstract String getName();

  @Internal
  public abstract DirectoryProperty getBaseOutputDirectory();

  @Optional
  @Input
  public abstract Property<String> getOutputTo();

  @Input
  @Optional
  public abstract SetProperty<String> getEntries();

  @Input
  @Optional
  public abstract SetProperty<String> getDependsOn();
}
