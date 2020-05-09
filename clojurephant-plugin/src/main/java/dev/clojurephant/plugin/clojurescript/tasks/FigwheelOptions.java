package dev.clojurephant.plugin.clojurescript.tasks;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;

public final class FigwheelOptions {
  private final DirectoryProperty destinationDir;
  private final ConfigurableFileCollection watchDirs;
  private final ConfigurableFileCollection cssDirs;
  private String ringHandler;
  private Map<String, Object> ringServerOptions;
  private Boolean rebelReadline;
  private Boolean pprintConfig;
  private String openFileCommand;
  private Boolean figwheelCore;
  private Boolean hotReloadCljs;
  private String connectUrl;
  private Object openUrl;
  private String reloadCljFiles;
  private final RegularFileProperty logFile;
  private String logLevel;
  private String clientLogLevel;
  private String logSyntaxErrorStyle;
  private Boolean loadWarningedCode;
  private Boolean ansiColorOutput;
  private Boolean validateConfig;
  private Boolean launchNode;
  private Boolean inspectNode;
  private String nodeCommand;
  private Boolean cljsDevtools;
  // skipping the "rarely used" options for now

  public FigwheelOptions(Project project, DirectoryProperty destinationDir) {
    this.destinationDir = destinationDir;
    this.watchDirs = project.files();
    this.cssDirs = project.files();
    this.ringServerOptions = new HashMap<>();
    this.logFile = project.getObjects().fileProperty();
  }

  @InputFiles
  public ConfigurableFileCollection getWatchDirs() {
    return watchDirs;
  }

  @InputFiles
  public ConfigurableFileCollection getCssDirs() {
    return cssDirs;
  }

  @Input
  @Optional
  public String getRingHandler() {
    return ringHandler;
  }

  public void setRingHandler(String ringHandler) {
    this.ringHandler = ringHandler;
  }

  @Input
  @Optional
  public Map<String, Object> getRingServerOptions() {
    return ringServerOptions;
  }

  public void setRingServerOptions(Map<String, Object> ringServerOptions) {
    this.ringServerOptions = ringServerOptions;
  }

  @Input
  @Optional
  public Boolean getRebelReadline() {
    return rebelReadline;
  }

  public void setRebelReadline(Boolean rebelReadline) {
    this.rebelReadline = rebelReadline;
  }

  @Input
  @Optional
  public Boolean getPprintConfig() {
    return pprintConfig;
  }

  public void setPprintConfig(Boolean pprintConfig) {
    this.pprintConfig = pprintConfig;
  }

  @Input
  @Optional
  public String getOpenFileCommand() {
    return openFileCommand;
  }

  public void setOpenFileCommand(String openFileCommand) {
    this.openFileCommand = openFileCommand;
  }

  @Input
  @Optional
  public Boolean getFigwheelCore() {
    return figwheelCore;
  }

  public void setFigwheelCore(Boolean figwheelCore) {
    this.figwheelCore = figwheelCore;
  }

  @Input
  @Optional
  public Boolean getHotReloadCljs() {
    return hotReloadCljs;
  }

  public void setHotReloadCljs(Boolean hotReloadCljs) {
    this.hotReloadCljs = hotReloadCljs;
  }

  @Input
  @Optional
  public String getConnectUrl() {
    return connectUrl;
  }

  public void setConnectUrl(String connectUrl) {
    this.connectUrl = connectUrl;
  }

  @Input
  @Optional
  public Object getOpenUrl() {
    return openUrl;
  }

  public void setOpenUrl(String openUrl) {
    this.openUrl = openUrl;
  }

  public void setOpenUrl(boolean openUrl) {
    this.openUrl = openUrl;
  }

  @Input
  @Optional
  public String getReloadCljFiles() {
    return reloadCljFiles;
  }

  public void setReloadCljFiles(String reloadCljFiles) {
    this.reloadCljFiles = reloadCljFiles;
  }

  @OutputFile
  @Optional
  public RegularFileProperty getLogFile() {
    return logFile;
  }

  @Input
  @Optional
  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  @Input
  @Optional
  public String getClientLogLevel() {
    return clientLogLevel;
  }

  public void setClientLogLevel(String clientLogLevel) {
    this.clientLogLevel = clientLogLevel;
  }

  @Input
  @Optional
  public String getLogSyntaxErrorStyle() {
    return logSyntaxErrorStyle;
  }

  public void setLogSyntaxErrorStyle(String logSyntaxErrorStyle) {
    this.logSyntaxErrorStyle = logSyntaxErrorStyle;
  }

  @Input
  @Optional
  public Boolean getLoadWarningedCode() {
    return loadWarningedCode;
  }

  public void setLoadWarningedCode(Boolean loadWarningedCode) {
    this.loadWarningedCode = loadWarningedCode;
  }

  @Input
  @Optional
  public Boolean getAnsiColorOutput() {
    return ansiColorOutput;
  }

  public void setAnsiColorOutput(Boolean ansiColorOutput) {
    this.ansiColorOutput = ansiColorOutput;
  }

  @Input
  @Optional
  public Boolean getValidateConfig() {
    return validateConfig;
  }

  public void setValidateConfig(Boolean validateConfig) {
    this.validateConfig = validateConfig;
  }

  @OutputDirectory
  public Provider<Directory> getTargetDir() {
    return destinationDir;
  }

  @Input
  @Optional
  public Boolean getLaunchNode() {
    return launchNode;
  }

  public void setLaunchNode(Boolean launchNode) {
    this.launchNode = launchNode;
  }

  @Input
  @Optional
  public Boolean getInspectNode() {
    return inspectNode;
  }

  public void setInspectNode(Boolean inspectNode) {
    this.inspectNode = inspectNode;
  }

  @Input
  @Optional
  public String getNodeCommand() {
    return nodeCommand;
  }

  public void setNodeCommand(String nodeCommand) {
    this.nodeCommand = nodeCommand;
  }

  @Input
  @Optional
  public Boolean getCljsDevtools() {
    return cljsDevtools;
  }

  public void setCljsDevtools(Boolean cljsDevtools) {
    this.cljsDevtools = cljsDevtools;
  }
}
