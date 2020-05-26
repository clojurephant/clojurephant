package dev.clojurephant.plugin.clojurescript.tasks;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.util.ConfigureUtil;

public final class ClojureScriptCompileOptions {
  private final Project project;
  private final DirectoryProperty destinationDir;
  private final RegularFileProperty outputTo;
  private final DirectoryProperty outputDir;
  private Optimizations optimizations;
  private String main;
  private String assetPath;
  private final RegularFileProperty sourceMapFile;
  private Boolean sourceMapEnabled;
  private Boolean verbose;
  private Boolean prettyPrint;
  private Target target;
  private List<ForeignLib> foreignLibs = new ArrayList<>();
  private List<String> externs = new ArrayList<>();
  private Map<String, Module> modules = new HashMap<>();
  private List<String> preloads;
  private Map<String, String> npmDeps = new HashMap<>();
  private Boolean installDeps;
  private CheckedArrays checkedArrays;
  // private String sourceMapPath;
  // private String sourceMapAssetPath;
  // private Boolean sourceMapTimestamp;
  // private Boolean cacheAnalysis;
  // private Boolean recompileDependents;
  // private Boolean staticFns;
  // private Boolean fnInvokeDirect;

  private Boolean devcards;

  public ClojureScriptCompileOptions(Project project, DirectoryProperty destinationDir) {
    this.project = project;
    this.destinationDir = destinationDir;
    this.outputTo = project.getObjects().fileProperty();
    this.outputDir = project.getObjects().directoryProperty();
    this.sourceMapFile = project.getObjects().fileProperty();
  }

  @OutputFile
  @Optional
  public RegularFileProperty getOutputTo() {
    return outputTo;
  }

  public void setOutputTo(String outputTo) {
    this.outputTo.set(destinationDir.file(outputTo));
  }

  public void setOutputTo(File outputTo) {
    this.outputTo.set(outputTo);
  }

  @Optional
  @OutputDirectory
  public DirectoryProperty getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(String outputDir) {
    this.outputDir.set(destinationDir.dir(outputDir));
  }

  public void setOutputDir(File outputDir) {
    this.outputDir.set(outputDir);
  }

  @Input
  @Optional
  public Optimizations getOptimizations() {
    return optimizations;
  }

  public void setOptimizations(Optimizations optimizations) {
    this.optimizations = optimizations;
  }

  @Input
  @Optional
  public String getMain() {
    return main;
  }

  public void setMain(String main) {
    this.main = main;
  }

  @Input
  @Optional
  public String getAssetPath() {
    return assetPath;
  }

  public void setAssetPath(String assetPath) {
    this.assetPath = assetPath;
  }

  @Internal
  public Object getSourceMap() {
    File sourceMap = sourceMapFile.getAsFile().getOrNull();
    if (sourceMap == null) {
      return sourceMapEnabled;
    } else {
      return sourceMap;
    }
  }

  @OutputFile
  @Optional
  public RegularFileProperty getSourceMapPath() {
    return sourceMapFile;
  }

  public void setSourceMap(String sourceMap) {
    this.sourceMapEnabled = true;
    this.sourceMapFile.set(destinationDir.file((String) sourceMap));
  }

  public void setSourceMap(File sourceMap) {
    this.sourceMapEnabled = true;
    this.sourceMapFile.set((File) sourceMap);
  }

  public void setSourceMap(boolean sourceMap) {
    this.sourceMapEnabled = (Boolean) sourceMap;
    this.sourceMapFile.set((File) null);
  }

  @Console
  public Boolean getVerbose() {
    return verbose;
  }

  public void setVerbose(Boolean verbose) {
    this.verbose = verbose;
  }

  @Input
  @Optional
  public Boolean getPrettyPrint() {
    return prettyPrint;
  }

  public void setPrettyPrint(Boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  @Input
  @Optional
  public Target getTarget() {
    return target;
  }

  public void setTarget(Target target) {
    this.target = target;
  }

  @Input
  @Optional
  public List<ForeignLib> getForeignLibs() {
    return foreignLibs;
  }

  public ClojureScriptCompileOptions foreignLib(Closure<?> configureAction) {
    ForeignLib lib = new ForeignLib();
    ConfigureUtil.configure(configureAction, lib);
    this.foreignLibs.add(lib);
    return this;
  }

  public void setForeignLibs(List<ForeignLib> foreignLibs) {
    this.foreignLibs = foreignLibs;
  }

  @Input
  @Optional
  public List<String> getExterns() {
    return externs;
  }

  public void setExterns(List<String> externs) {
    this.externs = externs;
  }

  @Nested
  @Optional
  public Map<String, Module> getModules() {
    return modules;
  }

  public ClojureScriptCompileOptions module(String name, Closure<?> configureAction) {
    Module module = new Module(project, destinationDir);
    ConfigureUtil.configure(configureAction, module);
    this.modules.put(name, module);
    return this;
  }

  public void setModules(Map<String, Module> modules) {
    this.modules = modules;
  }

  @Input
  @Optional
  public List<String> getPreloads() {
    return preloads;
  }

  public void setPreloads(List<String> preloads) {
    this.preloads = preloads;
  }

  @Input
  @Optional
  public Map<String, String> getNpmDeps() {
    return npmDeps;
  }

  public void setNpmDeps(Map<String, String> npmDeps) {
    this.npmDeps = npmDeps;
  }

  @Input
  @Optional
  public Boolean getInstallDeps() {
    return installDeps;
  }

  public void setInstallDeps(Boolean installDeps) {
    this.installDeps = installDeps;
  }

  @Input
  @Optional
  public CheckedArrays getCheckedArrays() {
    return checkedArrays;
  }

  public void setCheckedArrays(CheckedArrays checkedArrays) {
    this.checkedArrays = checkedArrays;
  }

  // @Input
  // @Optional
  // public String getSourceMapPath() {
  // return sourceMapPath;
  // }
  //
  // public void setSourceMapPath(String sourceMapPath) {
  // this.sourceMapPath = sourceMapPath;
  // }
  //
  // @Input
  // @Optional
  // public String getSourceMapAssetPath() {
  // return sourceMapAssetPath;
  // }
  //
  // public void setSourceMapAssetPath(String sourceMapAssetPath) {
  // this.sourceMapAssetPath = sourceMapAssetPath;
  // }
  //
  // @Input
  // @Optional
  // public Boolean getSourceMapTimestamp() {
  // return sourceMapTimestamp;
  // }
  //
  // public void setSourceMapTimestamp(Boolean sourceMapTimestamp) {
  // this.sourceMapTimestamp = sourceMapTimestamp;
  // }
  //
  // @Input
  // @Optional
  // public Boolean getCacheAnalysis() {
  // return cacheAnalysis;
  // }
  //
  // public void setCacheAnalysis(Boolean cacheAnalysis) {
  // this.cacheAnalysis = cacheAnalysis;
  // }
  //
  // @Input
  // @Optional
  // public Boolean getRecompileDependents() {
  // return recompileDependents;
  // }
  //
  // public void setRecompileDependents(Boolean recompileDependents) {
  // this.recompileDependents = recompileDependents;
  // }
  //
  // @Input
  // @Optional
  // public Boolean getStaticFns() {
  // return staticFns;
  // }
  //
  // public void setStaticFns(Boolean staticFns) {
  // this.staticFns = staticFns;
  // }
  //
  // @Input
  // @Optional
  // public Boolean getFnInvokeDirect() {
  // return fnInvokeDirect;
  // }
  //
  // public void setFnInvokeDirect(Boolean fnInvokeDirect) {
  // this.fnInvokeDirect = fnInvokeDirect;
  // }

  @Input
  @Optional
  public Boolean getDevcards() {
    return devcards;
  }

  public void setDevcards(Boolean devcards) {
    this.devcards = devcards;
  }
}
