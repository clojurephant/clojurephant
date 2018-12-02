package gradle_clojure.plugin.clojurescript;

import javax.inject.Inject;

import gradle_clojure.plugin.clojurescript.internal.DefaultClojureScriptSourceSet;
import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptCompile;
import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import gradle_clojure.plugin.common.internal.ClojureCommonBasePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
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
      sourceSet.getOutput().dir(build.getOutputDir());
      project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(build.getTaskName("compile"));

      sourceSet.getOutput().dir(project.provider(() -> {
        if (build.isCompilerConfigured()) {
          return clojurescriptSourceSet.getClojureScript().getSourceDirectories();
        } else {
          return build.getOutputDir();
        }
      }));
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
    });
  }
}
