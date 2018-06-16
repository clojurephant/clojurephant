package gradle_clojure.plugin.common.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import gradle_clojure.plugin.clojure.ClojureBasePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;

public class ClojureCommonBasePlugin implements Plugin<Project> {
  public static final String TOOLS_CONFIGURATION_NAME = "clojureTools";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    configureSourceSets(project, javaConvention);
    configureToolsConfigurations(project);
  }

  private void configureSourceSets(Project project, JavaPluginConvention javaConvention) {
    javaConvention.getSourceSets().all(sourceSet -> {
      sourceSet.getResources().exclude("**/.keep");
    });
  }

  private void configureToolsConfigurations(Project project) {
    Configuration tools = project.getConfigurations().create(TOOLS_CONFIGURATION_NAME);
    tools.defaultDependencies(deps -> {
      deps.add(project.getDependencies().create("io.github.gradle-clojure:gradle-clojure-tools:" + getVersion()));
    });

    // TODO does this JAR get included via shadow or application plugins?
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(sourceSet -> {
      project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(tools);
      project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(tools);
    });
  }

  private String getVersion() {
    try (InputStream stream = ClojureBasePlugin.class.getResourceAsStream("/gradle-clojure.properties")) {
      Properties props = new Properties();
      props.load(stream);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
