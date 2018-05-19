package gradle_clojure.plugin.clojurescript;

import javax.inject.Inject;

import gradle_clojure.plugin.clojurescript.internal.DefaultClojureScriptSourceSet;
import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptCompile;
import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

public class ClojureScriptBasePlugin implements Plugin<Project> {
  private final SourceDirectorySetFactory sourceDirectorySetFactory;

  @Inject
  public ClojureScriptBasePlugin(SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.sourceDirectorySetFactory = sourceDirectorySetFactory;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);

    configureSourceSetDefaults(project);
  }

  private void configureSourceSetDefaults(Project project) {
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all((SourceSet sourceSet) -> {
      ClojureScriptSourceSet clojurescriptSourceSet = new DefaultClojureScriptSourceSet("clojurescript", sourceDirectorySetFactory);
      new DslObject(sourceSet).getConvention().getPlugins().put("clojurescript", clojurescriptSourceSet);

      clojurescriptSourceSet.getClojureScript().srcDir(String.format("src/%s/clojurescript", sourceSet.getName()));
      // in case the clojure source overlaps with the resources source, exclude any clojure code
      // from resources
      sourceSet.getResources().getFilter().exclude(element -> clojurescriptSourceSet.getClojureScript().contains(element.getFile()));
      sourceSet.getAllSource().source(clojurescriptSourceSet.getClojureScript());

      String compileTaskName = sourceSet.getCompileTaskName("clojurescript");
      ClojureScriptCompile compile = project.getTasks().create(compileTaskName, ClojureScriptCompile.class);
      compile.setDescription(String.format("Compiles the %s ClojureScript source.", sourceSet.getName()));
      compile.setSource(clojurescriptSourceSet.getClojureScript());

      // TODO presumably at some point this will allow providers, so we should switch to that
      // instead of convention mapping
      compile.getConventionMapping().map("classpath", sourceSet::getCompileClasspath);

      DirectoryProperty buildDir = project.getLayout().getBuildDirectory();
      String outputDirPath = String.format("classes/%s/%s", clojurescriptSourceSet.getClojureScript().getName(), sourceSet.getName());
      Provider<Directory> outputDir = buildDir.dir(outputDirPath);

      clojurescriptSourceSet.getClojureScript().setOutputDir(outputDir.map(dir -> dir.getAsFile()));
      ((DefaultSourceSetOutput) sourceSet.getOutput()).addClassesDir(() -> outputDir.get().getAsFile());
      compile.getConventionMapping().map("destinationDir", () -> outputDir.get().getAsFile());

      project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(compile);
    });
  }
}
