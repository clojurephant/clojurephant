package dev.clojurephant.plugin.clojurescript.tasks;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;

public abstract class ClojureScriptCompileOptions {
  @OutputDirectory
  public abstract DirectoryProperty getBaseOutputDirectory();

  @Input
  @Optional
  public abstract Property<String> getAssetPath();

  @Input
  @Optional
  public abstract Property<String> getCheckedArrays();

  @Input
  @Optional
  public abstract ListProperty<String> getExterns();

  @Input
  @Optional
  public abstract NamedDomainObjectContainer<ForeignLib> getForeignLibs();

  @Input
  @Optional
  public abstract Property<Boolean> getInstallDeps();

  @Input
  @Optional
  public abstract Property<String> getMain();

  @Nested
  public abstract NamedDomainObjectContainer<Module> getModules();

  @Input
  @Optional
  public abstract MapProperty<String, String> getNpmDeps();

  @Input
  @Optional
  public abstract Property<String> getOptimizations();

  @Optional
  @Input
  public abstract Property<String> getOutputTo();

  @Optional
  @Input
  public abstract Property<String> getOutputDir();

  @Input
  @Optional
  public abstract ListProperty<String> getPreloads();

  @Input
  @Optional
  public abstract Property<Boolean> getPrettyPrint();

  @Input
  @Optional
  public abstract Property<Object> getSourceMap();

  @Input
  @Optional
  public abstract Property<Boolean> getStablePaths();

  @Input
  @Optional
  public abstract Property<String> getTarget();

  @Console
  public abstract Property<Boolean> getVerbose();
}
