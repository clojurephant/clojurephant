package dev.clojurephant.plugin.clojurescript.tasks;


import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;

public abstract class FigwheelOptions {
  @OutputDirectory
  public abstract DirectoryProperty getTargetDir();

  @InputFiles
  public abstract ConfigurableFileCollection getWatchDirs();

  @InputFiles
  public abstract ConfigurableFileCollection getCssDirs();

  @Input
  public abstract Property<String> getRingHandler();

  @Input
  public abstract MapProperty<String, Object> getRingServerOptions();

  @Input
  public abstract Property<Boolean> getRebelReadline();

  @Input
  public abstract Property<Boolean> getPprintConfig();

  @Input
  public abstract Property<String> getOpenFileCommand();

  @Input
  public abstract Property<Boolean> getFigwheelCore();

  @Input
  public abstract Property<Boolean> getHotReloadCljs();

  @Input
  public abstract Property<String> getConnectUrl();

  @Input
  public abstract Property<Object> getOpenUrl();

  @Input
  public abstract Property<String> getReloadCljFiles();

  @OutputFile
  public abstract RegularFileProperty getLogFile();

  @Input
  public abstract Property<String> getLogLevel();

  @Input
  public abstract Property<String> getClientLogLevel();

  @Input
  public abstract Property<String> getLogSyntaxErrorStyle();

  @Input
  public abstract Property<Boolean> getLoadWarningedCode();

  @Input
  public abstract Property<Boolean> getAnsiColorOutput();

  @Input
  public abstract Property<Boolean> getValidateConfig();

  @Input
  public abstract Property<Boolean> getValidateCli();

  @Input
  public abstract Property<Boolean> getLaunchNode();

  @Input
  public abstract Property<Boolean> getInspectNode();

  @Input
  public abstract Property<String> getNodeCommand();

  @Input
  public abstract Property<String> getLaunchJs();

  @Input
  public abstract Property<Boolean> getCljsDevtools();

  @Input
  public abstract Property<Boolean> getHelpfulClasspaths();

  // skipping remaining options for now
}
