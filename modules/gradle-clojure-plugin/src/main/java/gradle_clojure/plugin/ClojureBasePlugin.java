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

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.internal.SourceSetUtil;

import gradle_clojure.plugin.internal.DefaultClojureSourceSet;
import gradle_clojure.plugin.tasks.ClojureCompile;
import gradle_clojure.plugin.tasks.ClojureSourceSet;

public class ClojureBasePlugin implements Plugin<Project> {
  private final SourceDirectorySetFactory sourceDirectorySetFactory;

  @Inject
  public ClojureBasePlugin(SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.sourceDirectorySetFactory = sourceDirectorySetFactory;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);

    configureSourceSetDefaults(project);
  }

  private void configureSourceSetDefaults(Project project) {
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(sourceSet -> {
      ClojureSourceSet clojureSourceSet = new DefaultClojureSourceSet("clojure", sourceDirectorySetFactory);
      new DslObject(sourceSet).getConvention().getPlugins().put("clojure", clojureSourceSet);

      clojureSourceSet.getClojure().srcDir(String.format("src/%s/clojure", sourceSet.getName()));
      // in case the clojure source overlaps with the resources source, exclude any clojure code
      // from resources
      sourceSet.getResources().getFilter().exclude(element -> clojureSourceSet.getClojure().contains(element.getFile()));
      sourceSet.getAllSource().source(clojureSourceSet.getClojure());

      String compileTaskName = sourceSet.getCompileTaskName("clojure");
      ClojureCompile compile = project.getTasks().create(compileTaskName, ClojureCompile.class);
      compile.setDescription(String.format("Compiles the %s Clojure source.", sourceSet.getName()));
      compile.setSource(clojureSourceSet.getClojure());

      // TODO presumably at some point this will allow providers, so we should switch to that
      // instead of convention mapping
      compile.getConventionMapping().map("classpath", () -> {
        return sourceSet.getCompileClasspath()
            .plus(project.files(sourceSet.getJava().getOutputDir()))
            .plus(project.files(sourceSet.getOutput().getResourcesDir()));
      });
      // TODO switch to provider
      compile.getConventionMapping().map("namespaces", compile::findNamespaces);

      SourceSetUtil.configureOutputDirectoryForSourceSet(sourceSet, clojureSourceSet.getClojure(), compile, project);

      compile.dependsOn(project.getTasks().getByName(sourceSet.getCompileJavaTaskName()));
      compile.dependsOn(project.getTasks().getByName(sourceSet.getProcessResourcesTaskName()));
      project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(compile);
    });
  }
}
