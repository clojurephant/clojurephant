package dev.clojurephant.plugin.clojurescript;

import javax.inject.Inject;

import dev.clojurephant.plugin.clojurescript.internal.DefaultClojureScriptSourceSet;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptCompile;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import dev.clojurephant.plugin.clojurescript.tasks.WriteClojureScriptCompileOptions;
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

public class ClojureScriptBasePlugin implements Plugin<Project> {
  private final ObjectFactory objects;

  @Inject
  public ClojureScriptBasePlugin(ObjectFactory objects) {
    this.objects = objects;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(ClojureCommonBasePlugin.class);
    ClojureScriptExtension extension = project.getExtensions().create("clojurescript", ClojureScriptExtension.class, project);
    configureSourceSetDefaults(project, extension);
    configureBuildDefaults(project, extension);
  }

  private void configureSourceSetDefaults(Project project, ClojureScriptExtension extension) {
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all((SourceSet sourceSet) -> {
      ClojureScriptSourceSet clojurescriptSourceSet = new DefaultClojureScriptSourceSet("clojurescript", objects);
      new DslObject(sourceSet).getConvention().getPlugins().put("clojurescript", clojurescriptSourceSet);

      clojurescriptSourceSet.getClojureScript().srcDir(String.format("src/%s/clojurescript", sourceSet.getName()));
      // in case the clojure source overlaps with the resources source
      sourceSet.getResources().getFilter().exclude(element -> clojurescriptSourceSet.getClojureScript().contains(element.getFile()));
      sourceSet.getAllSource().source(clojurescriptSourceSet.getClojureScript());

      ClojureScriptBuild build = extension.getBuilds().create(sourceSet.getName());
      build.getSourceSet().set(sourceSet);

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

    extension.getBuilds().all(build -> {
      String compileTaskName = build.getTaskName("compile");
      ClojureScriptCompile compile = project.getTasks().create(compileTaskName, ClojureScriptCompile.class);
      compile.setDescription(String.format("Compiles the ClojureScript source for the %s build.", build.getName()));
      compile.getDestinationDir().set(build.getOutputDir());
      compile.getSourceRoots().from(build.getSourceRoots());
      compile.getClasspath().from(build.getSourceSet().map(SourceSet::getCompileClasspath));
      compile.setOptions(build.getCompiler());

      String writeOptionsTaskName = build.getTaskName("writeOptions");
      WriteClojureScriptCompileOptions writeOptions = project.getTasks().create(writeOptionsTaskName, WriteClojureScriptCompileOptions.class);
      writeOptions.setDescription(String.format("Writes the configuration options for the %s ClojureScript build.", build.getName()));
      writeOptions.getOptions().set(build.getCompiler());
      writeOptions.getDestinationFile().convention(project.getLayout().getProjectDirectory().file(build.getName() + ".cljs.edn"));
      // FIXME compile should take a compile opts file as input
      compile.dependsOn(writeOptions);
    });
  }
}
