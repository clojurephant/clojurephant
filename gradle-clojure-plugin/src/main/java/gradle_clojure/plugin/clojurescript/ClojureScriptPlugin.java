package gradle_clojure.plugin.clojurescript;

import gradle_clojure.plugin.clojure.tasks.ClojureNRepl;
import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import gradle_clojure.plugin.common.internal.ClojureCommonPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPluginConvention;

public class ClojureScriptPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureScriptBasePlugin.class);
    project.getPlugins().apply(ClojureCommonPlugin.class);

    ClojureScriptExtension extension = project.getExtensions().getByType(ClojureScriptExtension.class);
    configureBuilds(project, extension);

    configurePiggieback(project);
    configureFigwheel(project);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    ClojureCommonPlugin.configureDevSource(javaConvention, sourceSet -> {
      ClojureScriptSourceSet src = (ClojureScriptSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojurescript");
      return src.getClojureScript();
    });
  }

  private void configureBuilds(Project project, ClojureScriptExtension extension) {
    ClojureNRepl repl = (ClojureNRepl) project.getTasks().getByName(ClojureCommonPlugin.NREPL_TASK_NAME);
    repl.getContextData().put("cljs-builds", extension.getBuilds());
  }

  private void configurePiggieback(Project project) {
    project.getDependencies().add(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME, "cider:piggieback:0.3.6");

    ClojureNRepl repl = (ClojureNRepl) project.getTasks().getByName(ClojureCommonPlugin.NREPL_TASK_NAME);
    repl.getDefaultMiddleware().add("cider.piggieback/wrap-cljs-repl");
  }

  private void configureFigwheel(Project project) {
    project.getDependencies().add(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME, "com.bhauman:figwheel-main:0.1.2");
  }
}
