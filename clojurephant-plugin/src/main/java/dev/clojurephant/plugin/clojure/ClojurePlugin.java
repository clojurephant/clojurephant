package dev.clojurephant.plugin.clojure;

import java.util.stream.Collectors;

import dev.clojurephant.plugin.clojure.tasks.ClojureSourceSet;
import dev.clojurephant.plugin.common.attributes.ClojureElements;
import dev.clojurephant.plugin.common.internal.ClojureCommonPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.internal.JavaConfigurationVariantMapping;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

public class ClojurePlugin implements Plugin<Project> {
  public static final String AOT_ELEMENTS_CONFIGURATION_NAME = "aotRuntimeElements";
  public static final String AOT_JAR_NAME = "aotJar";

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureBasePlugin.class);
    project.getPlugins().apply(ClojureCommonPlugin.class);

    ClojureExtension extension = project.getExtensions().getByType(ClojureExtension.class);
    configureBuilds(project, extension);
    configureAotElements(project);
    configureAotVariant(project, extension);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    ClojureCommonPlugin.configureDevSource(javaConvention, sourceSet -> {
      ClojureSourceSet src = (ClojureSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojure");
      return src.getClojure();
    });
  }

  private void configureBuilds(Project project, ClojureExtension extension) {
    ClojureBuild main = extension.getBuilds().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    main.checkAll();

    // any test ns needs this config to work with the Test task
    extension.getBuilds().matching(build -> build.getName().toLowerCase().contains("test")).all(test -> {
      test.aotAll();
      test.getAotNamespaces().add("dev.clojurephant.tools.logger");
      test.getAotNamespaces().add("dev.clojurephant.tools.clojure-test-junit4");
    });

    ClojureBuild dev = extension.getBuilds().getByName(ClojureCommonPlugin.DEV_SOURCE_SET_NAME);
    // REPL crashes if the user namespace doesn't compile, so make sure it does before starting
    // but also have to account project not having a user ns
    dev.getCheckNamespaces().set(dev.getAllNamespaces().map(nses -> {
      return nses.stream()
          .filter("user"::equals)
          .collect(Collectors.toSet());
    }));
  }

  private void configureAotElements(Project project) {
    project.getConfigurations().create(AOT_ELEMENTS_CONFIGURATION_NAME, conf -> {
      conf.setVisible(false);
      conf.setCanBeConsumed(true);
      conf.setCanBeResolved(false);
      conf.setDescription("Elements of runtime for main.");
      conf.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
      conf.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
      conf.getAttributes().attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
      conf.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
      conf.getAttributes().attribute(ClojureElements.CLJELEMENTS_ATTRIBUTE, project.getObjects().named(ClojureElements.class, ClojureElements.AOT));
      // TODO add an attribute for the JVM version?
      conf.extendsFrom(
          project.getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME),
          project.getConfigurations().getByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME),
          project.getConfigurations().getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME));
    });

    project.getConfigurations().named(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, conf -> {
      conf.getAttributes().attribute(ClojureElements.CLJELEMENTS_ATTRIBUTE, project.getObjects().named(ClojureElements.class, ClojureElements.SOURCE));
    });
    project.getConfigurations().named(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, conf -> {
      conf.getAttributes().attribute(ClojureElements.CLJELEMENTS_ATTRIBUTE, project.getObjects().named(ClojureElements.class, ClojureElements.SOURCE));
    });
  }

  private void configureAotVariant(Project project, ClojureExtension extension) {
    ClojureBuild main = extension.getBuilds().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    Configuration aotElements = project.getConfigurations().getByName(ClojurePlugin.AOT_ELEMENTS_CONFIGURATION_NAME);

    TaskProvider<Jar> aotJar = project.getTasks().register(AOT_JAR_NAME, Jar.class, task -> {
      task.setDescription("Assembles a jar archive contain the main AOT classes.");
      task.setGroup(BasePlugin.BUILD_GROUP);
      task.getArchiveClassifier().set("aot");
      task.from(main.getSourceSet().map(SourceSet::getOutput));
      task.from(main.getOutputDir());
    });

    // configure implicit variant
    ConfigurationPublications publications = aotElements.getOutgoing();
    publications.artifact(aotJar);
    publications.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE);

    // TODO find a better way to access this
    AdhocComponentWithVariants component = (AdhocComponentWithVariants) project.getComponents().getByName("java");
    component.addVariantsFromConfiguration(aotElements, new JavaConfigurationVariantMapping("runtime", false));
  }
}
