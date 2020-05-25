package dev.clojurephant.plugin.clojure;

import javax.inject.Inject;

import dev.clojurephant.plugin.clojure.internal.DefaultClojureSourceSet;
import dev.clojurephant.plugin.clojure.tasks.ClojureCheck;
import dev.clojurephant.plugin.clojure.tasks.ClojureCompile;
import dev.clojurephant.plugin.clojure.tasks.ClojureSourceSet;
import dev.clojurephant.plugin.common.internal.ClojureCommonBasePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

public class ClojureBasePlugin implements Plugin<Project> {
  private final ObjectFactory objects;

  @Inject
  public ClojureBasePlugin(ObjectFactory objects) {
    this.objects = objects;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(ClojureCommonBasePlugin.class);
    ClojureExtension extension = project.getExtensions().create("clojure", ClojureExtension.class, project);
    configureSourceSetDefaults(project, extension);
    configureBuildDefaults(project, extension);
  }

  private void configureSourceSetDefaults(Project project, ClojureExtension extension) {
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(sourceSet -> {
      ClojureSourceSet clojureSourceSet = new DefaultClojureSourceSet("clojure", objects);
      new DslObject(sourceSet).getConvention().getPlugins().put("clojure", clojureSourceSet);

      clojureSourceSet.getClojure().srcDir(String.format("src/%s/clojure", sourceSet.getName()));
      // in case the clojure source overlaps with the resources source
      sourceSet.getResources().getFilter().exclude(element -> clojureSourceSet.getClojure().contains(element.getFile()));
      sourceSet.getAllSource().source(clojureSourceSet.getClojure());

      ClojureBuild build = extension.getBuilds().create(sourceSet.getName());
      build.getSourceSet().set(sourceSet);

      Provider<FileCollection> output = project.provider(() -> {
        if (build.isCompilerConfigured()) {
          return project.files(build.getOutputDir());
        } else {
          return clojureSourceSet.getClojure().getSourceDirectories();
        }
      });

      ((DefaultSourceSetOutput) sourceSet.getOutput()).getClassesDirs().from(output);
      project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(build.getTaskName("compile"));
      project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(build.getTaskName("check"));
    });
  }

  private void configureBuildDefaults(Project project, ClojureExtension extension) {
    extension.getRootOutputDir().set(project.getLayout().getBuildDirectory().dir("clojure"));

    extension.getBuilds().all(build -> {
      Provider<FileCollection> classpath = build.getSourceSet().map(sourceSet -> {
        return sourceSet.getCompileClasspath()
            .plus(project.files(sourceSet.getJava().getOutputDir()))
            .plus(project.files(sourceSet.getOutput().getResourcesDir()));
      });

      String checkTaskName = build.getTaskName("check");
      ClojureCheck check = project.getTasks().create(checkTaskName, ClojureCheck.class);
      check.setDescription(String.format("Checks the Clojure source for the %s build.", build.getName()));
      check.getSourceRoots().from(build.getSourceRoots());
      check.getClasspath().from(classpath);
      check.getReflection().set(build.getReflection());
      check.getNamespaces().set(build.getCheckNamespaces());
      check.dependsOn(build.getSourceSet().map(SourceSet::getCompileJavaTaskName));
      check.dependsOn(build.getSourceSet().map(SourceSet::getProcessResourcesTaskName));

      String compileTaskName = build.getTaskName("compile");
      ClojureCompile compile = project.getTasks().create(compileTaskName, ClojureCompile.class);
      compile.setDescription(String.format("Compiles the Clojure source for the %s build.", build.getName()));
      compile.getDestinationDir().set(build.getOutputDir());
      compile.getSourceRoots().from(build.getSourceRoots());
      compile.getClasspath().from(classpath);
      compile.setOptions(build.getCompiler());
      compile.getNamespaces().set(build.getAotNamespaces());
      compile.dependsOn(build.getSourceSet().map(SourceSet::getCompileJavaTaskName));
      compile.dependsOn(build.getSourceSet().map(SourceSet::getProcessResourcesTaskName));
    });
  }
}
