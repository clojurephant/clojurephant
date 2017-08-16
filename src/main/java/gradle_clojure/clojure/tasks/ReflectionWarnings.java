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
package gradle_clojure.clojure.tasks;

public class ReflectionWarnings {
  private Boolean enabled;
  private Boolean projectOnly;
  private Boolean asErrors;

  public ReflectionWarnings(Boolean enabled, Boolean projectOnly, Boolean asErrors) {
    this.enabled = enabled;
    this.projectOnly = projectOnly;
    this.asErrors = asErrors;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean getProjectOnly() {
    return projectOnly;
  }

  public void setProjectOnly(Boolean projectOnly) {
    this.projectOnly = projectOnly;
  }

  public Boolean getAsErrors() {
    return asErrors;
  }

  public void setAsErrors(Boolean asErrors) {
    this.asErrors = asErrors;
  }
}
