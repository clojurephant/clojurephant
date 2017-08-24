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
package gradle_clojure.plugin.internal;

import java.io.File;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

public final class ClojureRuntime {
  private static final Pattern JAR_NAME = Pattern.compile("clojure-(?:\\d+\\.)+jar");

  private ClojureRuntime() {
    // do not instantiate
  }

  public static Optional<FileCollection> findClojure(Project project, Iterable<File> files) {
    return StreamSupport.stream(files.spliterator(), false)
        .filter(file -> JAR_NAME.matcher(file.getName()).matches())
        .<FileCollection>map(project::files)
        .findAny();
  }
}
