package gradle_clojure.plugin;

import javax.inject.Inject;

import gradle_clojure.plugin.internal.DefaultClojurescriptSourceSet;
import gradle_clojure.plugin.tasks.clojurescript.ClojurescriptCompile;
import gradle_clojure.plugin.tasks.clojurescript.ClojurescriptSourceSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.internal.SourceSetUtil;
import org.gradle.api.tasks.SourceSet;

public class ClojurescriptBasePlugin implements Plugin<Project> {
  private final SourceDirectorySetFactory sourceDirectorySetFactory;

  @Inject
  public ClojurescriptBasePlugin(SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.sourceDirectorySetFactory = sourceDirectorySetFactory;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);

    configureSourceSetDefaults(project);
  }

  private void configureSourceSetDefaults(Project project) {
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all((SourceSet sourceSet) -> {
      ClojurescriptSourceSet clojurescriptSourceSet = new DefaultClojurescriptSourceSet("clojurescript", sourceDirectorySetFactory);
      new DslObject(sourceSet).getConvention().getPlugins().put("clojurescript", clojurescriptSourceSet);

      clojurescriptSourceSet.getClojurescript().srcDir(String.format("src/%s/clojurescript", sourceSet.getName()));
      // in case the clojure source overlaps with the resources source, exclude any clojure code
      // from resources
      sourceSet.getResources().getFilter().exclude(element -> clojurescriptSourceSet.getClojurescript().contains(element.getFile()));
      sourceSet.getAllSource().source(clojurescriptSourceSet.getClojurescript());

      String compileTaskName = sourceSet.getCompileTaskName("clojurescript");
      ClojurescriptCompile compile = project.getTasks().create(compileTaskName, ClojurescriptCompile.class);
      compile.setDescription(String.format("Compiles the %s Clojurescript source.", sourceSet.getName()));
      compile.setSource(clojurescriptSourceSet.getClojurescript());

      // TODO presumably at some point this will allow providers, so we should switch to that
      // instead of convention mapping
      compile.getConventionMapping().map("classpath", sourceSet::getCompileClasspath);

      SourceSetUtil.configureOutputDirectoryForSourceSet(sourceSet, clojurescriptSourceSet.getClojurescript(), compile, project);

      project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(compile);
    });
  }
}
