package dev.clojurephant.plugin.clojurescript;

import javax.inject.Inject;

import dev.clojurephant.plugin.clojurescript.internal.DefaultClojureScriptSourceSet;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptCompile;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import dev.clojurephant.plugin.common.internal.ClojureCommonBasePlugin;
import dev.clojurephant.plugin.common.internal.Namespaces;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class ClojureScriptBasePlugin implements Plugin<Project> {
  private final ObjectFactory objects;

  @Inject
  public ClojureScriptBasePlugin(ObjectFactory objects) {
    this.objects = objects;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(ClojureCommonBasePlugin.class);
    ClojureScriptExtension extension = project.getExtensions().create("clojurescript", ClojureScriptExtension.class);
    configureSourceSetDefaults(project, extension);
    configureBuildDefaults(project, extension);
  }

  private void configureSourceSetDefaults(Project project, ClojureScriptExtension extension) {
    project.getExtensions().getByType(SourceSetContainer.class).all((SourceSet sourceSet) -> {
      ClojureScriptSourceSet clojurescriptSourceSet = new DefaultClojureScriptSourceSet("clojurescript", objects);
      new DslObject(sourceSet).getConvention().getPlugins().put("clojurescript", clojurescriptSourceSet);

      clojurescriptSourceSet.getClojureScript().srcDir(String.format("src/%s/clojurescript", sourceSet.getName()));
      clojurescriptSourceSet.getClojureScript().getFilter().include(Namespaces.CLOJURESCRIPT_PATTERNS);
      // in case the clojure source overlaps with the resources source
      sourceSet.getResources().getFilter().exclude(element -> clojurescriptSourceSet.getClojureScript().contains(element.getFile()));
      sourceSet.getAllSource().source(clojurescriptSourceSet.getClojureScript());

      ClojureScriptBuild build = extension.getBuilds().create(sourceSet.getName());
      ClojureScriptSourceSet clojurescript = (ClojureScriptSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojurescript");
      build.getSourceRoots().from(clojurescript.getClojureScript().getSourceDirectories());

      build.getClasspath()
          .from(build.getSourceRoots())
          .from(project.provider(() -> sourceSet.getCompileClasspath()));

      project.getTasks().named(sourceSet.getClassesTaskName(), task -> {
        task.dependsOn(build.getTaskName("compile"));
      });

      Provider<FileCollection> output = project.provider(() -> {
        if (build.isCompilerConfigured()) {
          return project.files(build.getOutputDir());
        } else {
          return clojurescriptSourceSet.getClojureScript().getSourceDirectories();
        }
      });

      ((DefaultSourceSetOutput) sourceSet.getOutput()).getClassesDirs().from(output);

      project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(build.getTaskName("compile"));
    });
  }

  private void configureBuildDefaults(Project project, ClojureScriptExtension extension) {
    extension.getRootOutputDir().set(project.getLayout().getBuildDirectory().dir("clojurescript"));

    extension.getBuilds().configureEach(build -> {
      build.getOutputDir().set(extension.getRootOutputDir().dir(build.getName()));
      build.getCompiler().getBaseOutputDirectory().set(build.getOutputDir());
      build.getCompiler().getModules().configureEach(module -> {
        module.getBaseOutputDirectory().set(build.getCompiler().getBaseOutputDirectory());
      });

      String compileTaskName = build.getTaskName("compile");
      project.getTasks().register(compileTaskName, ClojureScriptCompile.class, task -> {
        task.setDescription(String.format("Compiles the ClojureScript source for the %s build.", build.getName()));
        task.getDestinationDir().set(build.getOutputDir());
        task.setSource(build.getSourceTree());
        task.getClasspath().from(build.getClasspath());
        task.getOptions().set(build.getCompiler());
      });
    });
  }
}
