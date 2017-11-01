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
package gradle_clojure.plugin;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.internal.SourceSetUtil;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.testing.Test;

import gradle_clojure.plugin.tasks.ClojureCompile;

public class ClojurePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    configureTestDefaults(project, javaConvention);
  }

  private void configureTestDefaults(Project project, JavaPluginConvention javaConvention) {
    project.getTasks().withType(Test.class, test -> {
      SourceSet sourceSet = javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
      ClojureCompile compile = (ClojureCompile) project.getTasks().getByName(sourceSet.getCompileTaskName("clojure"));

      compile.getOptions().setAotCompile(true);
      compile.getOptions().forkOptions(fork -> {
        String namespaces = String.join(File.pathSeparator, compile.findNamespaces());
        fork.setJvmArgs(Arrays.asList("-Dgradle-clojure.test-namespaces=" + namespaces));
      });

      Callable<?> namespaces = () -> {
        List<String> nses = new ArrayList<>();
        nses.add("gradle-clojure.tools.clojure-test-junit4");
        nses.addAll(compile.findNamespaces());
        return nses;
      };

      compile.getConventionMapping().map("namespaces", namespaces);
    });
  }
}
