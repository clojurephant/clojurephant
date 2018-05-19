package gradle_clojure.plugin.clojure;

import javax.inject.Inject;

import gradle_clojure.plugin.clojure.internal.DefaultClojureSourceSet;
import gradle_clojure.plugin.clojure.tasks.ClojureCompile;
import gradle_clojure.plugin.clojure.tasks.ClojureSourceSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ComponentModuleMetadataDetails;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;

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
    configureModuleReplacements(project);
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

      DirectoryProperty buildDir = project.getLayout().getBuildDirectory();
      String outputDirPath = String.format("classes/%s/%s", clojureSourceSet.getClojure().getName(), sourceSet.getName());
      Provider<Directory> outputDir = buildDir.dir(outputDirPath);

      clojureSourceSet.getClojure().setOutputDir(outputDir.map(dir -> dir.getAsFile()));
      ((DefaultSourceSetOutput) sourceSet.getOutput()).addClassesDir(() -> outputDir.get().getAsFile());
      compile.getConventionMapping().map("destinationDir", () -> outputDir.get().getAsFile());

      compile.dependsOn(project.getTasks().getByName(sourceSet.getCompileJavaTaskName()));
      compile.dependsOn(project.getTasks().getByName(sourceSet.getProcessResourcesTaskName()));
      project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(compile);
    });
  }

  private void configureModuleReplacements(Project project) {
    project.getDependencies().getModules().module("org.clojure:tools.nrepl", module -> {
      ComponentModuleMetadataDetails details = (ComponentModuleMetadataDetails) module;
      details.replacedBy("nrepl:nrepl", "nREPL was moved out of Clojure Contrib to its own project.");
    });
  }
}
