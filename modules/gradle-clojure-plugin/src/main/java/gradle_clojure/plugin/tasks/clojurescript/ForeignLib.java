/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gradle_clojure.plugin.tasks.clojurescript;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ForeignLib implements Serializable {
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
