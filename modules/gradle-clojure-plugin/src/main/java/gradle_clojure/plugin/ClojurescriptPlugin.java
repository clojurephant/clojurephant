package gradle_clojure.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

public class ClojurescriptPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojurescriptBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);
  }
}
