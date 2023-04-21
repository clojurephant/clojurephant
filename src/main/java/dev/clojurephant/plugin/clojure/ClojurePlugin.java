package dev.clojurephant.plugin.clojure;

import java.util.stream.Collectors;

import dev.clojurephant.plugin.common.internal.ClojureCommonPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

public class ClojurePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureBasePlugin.class);
    project.getPlugins().apply(ClojureCommonPlugin.class);

    ClojureExtension extension = project.getExtensions().getByType(ClojureExtension.class);
    configureBuilds(project, extension);
  }

  private void configureBuilds(Project project, ClojureExtension extension) {
    ClojureBuild main = extension.getBuilds().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    main.checkAll();

    // any test ns needs this config to work with the Test task
    extension.getBuilds()
        .matching(build -> build.getName().toLowerCase().contains("test"))
        .all(ClojureBuild::aotAll);

    ClojureBuild dev = extension.getBuilds().getByName(ClojureCommonPlugin.DEV_SOURCE_SET_NAME);
    // REPL crashes if the user namespace doesn't compile, so make sure it does before starting
    // but also have to account project not having a user ns
    dev.getCheckNamespaces().set(dev.getAllNamespaces().map(nses -> nses.stream()
        .filter("user"::equals)
        .collect(Collectors.toSet())));
  }
}
