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
  private boolean enabled;
  private boolean projectOnly;
  private boolean asErrors;

  public ReflectionWarnings(boolean enabled, boolean projectOnly, boolean asErrors) {
    this.enabled = enabled;
    this.projectOnly = projectOnly;
    this.asErrors = asErrors;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean getProjectOnly() {
    return projectOnly;
  }

  public void setProjectOnly(boolean projectOnly) {
    this.projectOnly = projectOnly;
  }

  public boolean getAsErrors() {
    return asErrors;
  }

  public void setAsErrors(boolean asErrors) {
    this.asErrors = asErrors;
  }
}
