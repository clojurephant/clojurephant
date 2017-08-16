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
package gradle_clojure.clojure;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import gradle_clojure.clojure.tasks.ClojureCompile;
import gradle_clojure.clojure.tasks.ClojureSourceSet;
import gradle_clojure.clojure.tasks.ClojureTestRunner;
import gradle_clojure.clojure.tasks.internal.ClojureSourceSetImpl;

public class ClojurePlugin implements Plugin<Project> {
  private static final Logger logger = Logging.getLogger(ClojurePlugin.class);

  @Override
  public void apply(Project project) {
    logger.info("Applying ClojurePlugin");
    project.getPlugins().apply(JavaBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    JavaPluginConvention javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);

    javaPluginConvention.getSourceSets().all(sourceSet -> {
      ClojureCompile compileTask = createCompileTask(project, sourceSet);

      if (SourceSet.TEST_SOURCE_SET_NAME.equals(sourceSet.getName())) {
        ClojureTestRunner testTask = createTestTask(project);

        testTask.getConventionMapping().map("classpath", () -> sourceSet.getRuntimeClasspath());
        testTask.getConventionMapping().map("namespaces", () -> compileTask.findNamespaces());

        testTask.dependsOn(compileTask);
      }
    });
  }

  private ClojureCompile createCompileTask(Project project, SourceSet sourceSet) {
    ProjectInternal projectInternal = (ProjectInternal) project;
    String sourceRootDir = String.format("src/%s/clojure", sourceSet.getName());

    logger.info("Creating DefaultSourceDirectorySet for source set {}", sourceSet);
    ClojureSourceSet clojureSrcSet = new ClojureSourceSetImpl(sourceSet.getName(), projectInternal.getFileResolver());
    SourceDirectorySet clojureDirSet = clojureSrcSet.getClojure();

    new DslObject(sourceSet).getConvention().getPlugins().put("clojure", clojureSrcSet);

    File srcDir = project.file(sourceRootDir);
    logger.info("Creating Clojure SourceDirectorySet for source set " + sourceSet + " with src dir " + srcDir);
    clojureDirSet.srcDir(srcDir);

    logger.info("Adding ClojureSourceDirectorySet " + clojureDirSet + " to source set " + sourceSet);
    sourceSet.getAllSource().source(clojureDirSet);
    sourceSet.getResources().getFilter().exclude(it -> clojureDirSet.contains(it.getFile()));

    String name = sourceSet.getCompileTaskName("clojure");
    Class<ClojureCompile> compilerClass = ClojureCompile.class;
    logger.info("Creating Clojure compile task " + name + " with class " + compilerClass);
    ClojureCompile compile = project.getTasks().create(name, compilerClass);
    compile.setDescription("Compiles the " + sourceSet + " Clojure code");

    Task javaTask = project.getTasks().findByName(sourceSet.getCompileJavaTaskName());
    if (javaTask != null) {
      compile.dependsOn(javaTask);
    }

    project.getTasks().findByName(sourceSet.getClassesTaskName()).dependsOn(compile);

    compile.getConventionMapping().map("classpath", () -> sourceSet.getCompileClasspath());
    compile.getConventionMapping().map("namespaces", () -> compile.findNamespaces());
    compile.getConventionMapping().map("destinationDir", () -> sourceSet.getOutput().getClassesDir());

    compile.source(clojureDirSet);

    return compile;
  }

  private ClojureTestRunner createTestTask(Project project) {
    String name = "testClojure";
    Class<ClojureTestRunner> testRunnerClass = ClojureTestRunner.class;

    ClojureTestRunner testRunner = project.getTasks().create(name, testRunnerClass);
    project.getTasks().findByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(testRunner);
    testRunner.setDescription("Runs the Clojure tests");
    testRunner.setGroup(JavaBasePlugin.VERIFICATION_GROUP);

    testRunner.getOutputs().upToDateWhen(task -> false);

    return testRunner;
  }



}
