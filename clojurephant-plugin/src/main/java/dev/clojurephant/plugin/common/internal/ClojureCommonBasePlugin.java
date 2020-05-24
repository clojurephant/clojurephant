package dev.clojurephant.plugin.common.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import dev.clojurephant.plugin.clojure.ClojureBasePlugin;
import dev.clojurephant.plugin.common.attributes.ClojureElements;
import dev.clojurephant.plugin.common.attributes.internal.ClojureElementsCompatibilityRule;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeMatchingStrategy;
import org.gradle.api.attributes.AttributesSchema;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;

public class ClojureCommonBasePlugin implements Plugin<Project> {
  public static final String TOOLS_CONFIGURATION_NAME = "clojureTools";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    configureAttributes(project);
    configureSourceSets(project, javaConvention);
    configureToolsConfigurations(project);
  }

  private void configureAttributes(Project project) {
    AttributesSchema schema = project.getDependencies().getAttributesSchema();
    schema.attribute(ClojureElements.CLJELEMENTS_ATTRIBUTE);
    AttributeMatchingStrategy<ClojureElements> strategy = schema.getMatchingStrategy(ClojureElements.CLJELEMENTS_ATTRIBUTE);
    strategy.getCompatibilityRules().add(ClojureElementsCompatibilityRule.class);
    // strategy.getDisambiguationRules().add(ClojureElementsDisambiguationRule.class);
  }

  private void configureSourceSets(Project project, JavaPluginConvention javaConvention) {
    javaConvention.getSourceSets().all(sourceSet -> {
      sourceSet.getResources().exclude("**/.keep");
    });
  }

  private void configureToolsConfigurations(Project project) {
    Configuration tools = project.getConfigurations().create(TOOLS_CONFIGURATION_NAME);
    tools.defaultDependencies(deps -> {
      deps.add(project.getDependencies().create("dev.clojurephant:clojurephant-tools:" + getVersion()));
    });

    // TODO does this JAR get included via shadow or application plugins?
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(sourceSet -> {
      project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(tools);
      project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(tools);
    });
  }

  private String getVersion() {
    try (InputStream stream = ClojureBasePlugin.class.getResourceAsStream("/clojurephant.properties")) {
      Properties props = new Properties();
      props.load(stream);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
