package gradle_clojure.plugin.clojurescript.tasks;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public class ForeignLib {
  private String file;
  private String fileMin;
  private List<String> provides = Collections.emptyList();
  private List<String> requires = Collections.emptyList();
  private ModuleType moduleType;
  private String preprocess;
  private Map<String, String> globalExports = Collections.emptyMap();

  @Input
  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  @Input
  @Optional
  public String getFileMin() {
    return fileMin;
  }

  public void setFileMin(String fileMin) {
    this.fileMin = fileMin;
  }

  @Input
  @Optional
  public List<String> getProvides() {
    return provides;
  }

  public void setProvides(List<String> provides) {
    this.provides = provides;
  }

  @Input
  @Optional
  public List<String> getRequires() {
    return requires;
  }

  public void setRequires(List<String> requires) {
    this.requires = requires;
  }

  @Input
  @Optional
  public ModuleType getModuleType() {
    return moduleType;
  }

  public void setModuleType(ModuleType moduleType) {
    this.moduleType = moduleType;
  }

  @Input
  @Optional
  public String getPreprocess() {
    return preprocess;
  }

  public void setPreprocess(String preprocess) {
    this.preprocess = preprocess;
  }

  @Input
  @Optional
  public Map<String, String> getGlobalExports() {
    return globalExports;
  }

  public void setGlobalExports(Map<String, String> globalExports) {
    this.globalExports = globalExports;
  }
}
