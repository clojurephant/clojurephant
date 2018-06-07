package gradle_clojure.plugin.common.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import gradle_clojure.plugin.clojure.ClojureBasePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ComponentModuleMetadataDetails;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;

public class ClojureCommonBasePlugin implements Plugin<Project> {
  public static final String TOOLS_CONFIGURATION_NAME = "clojureTools";
  public static final String NREPL_CONFIGURATION_NAME = "nrepl";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);
    configureToolsConfigurations(project);
    configureModuleReplacements(project);
  }

  private void configureToolsConfigurations(Project project) {
    Configuration tools = project.getConfigurations().create(TOOLS_CONFIGURATION_NAME);
    tools.defaultDependencies(deps -> {
      deps.add(project.getDependencies().create("io.github.gradle-clojure:gradle-clojure-tools:" + getVersion()));
    });
    Configuration nrepl = project.getConfigurations().create(NREPL_CONFIGURATION_NAME);
    nrepl.defaultDependencies(deps -> {
      deps.add(project.getDependencies().create("nrepl:nrepl:0.3.1"));
    });

    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(sourceSet -> {
      project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(tools);
      project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(tools);

      String classesAotName = String.format("%sAot", sourceSet.getClassesTaskName());
      project.getTasks().create(classesAotName, task -> {
        task.dependsOn(sourceSet.getClassesTaskName());
      });
    });
  }

  private void configureModuleReplacements(Project project) {
    project.getDependencies().getModules().module("org.clojure:tools.nrepl", module -> {
      ComponentModuleMetadataDetails details = (ComponentModuleMetadataDetails) module;
      details.replacedBy("nrepl:nrepl", "nREPL was moved out of Clojure Contrib to its own project.");
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
