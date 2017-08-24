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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import gradle_clojure.plugin.tasks.ClojureCompile;
import gradle_clojure.plugin.tasks.ClojureTest;

public class ClojurePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    configureTestDefaults(project, javaConvention);
    configureTest(project);
  }

  private void configureTestDefaults(Project project, JavaPluginConvention javaConvention) {
    project.getTasks().withType(ClojureTest.class, test -> {
      SourceSet sourceSet = javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
      ClojureCompile compile = (ClojureCompile) project.getTasks().getByName(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getCompileTaskName("clojure"));
      test.getConventionMapping().map("classpath", sourceSet::getRuntimeClasspath);
      test.getConventionMapping().map("namespaces", compile::findNamespaces);
      test.dependsOn(compile);
    });
  }

  private void configureTest(Project project) {
    ClojureTest test = project.getTasks().create("testClojure", ClojureTest.class);
    test.setDescription("Runs the clojure.test tests");
    test.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
    project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(test);
  }
}
