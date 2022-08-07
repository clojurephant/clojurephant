package dev.clojurephant.plugin.common.internal;

import java.util.HashMap;
import java.util.Map;

import dev.clojurephant.plugin.clojure.ClojureExtension;
import dev.clojurephant.plugin.clojurescript.ClojureScriptExtension;
import dev.clojurephant.plugin.common.ClojurephantModel;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

public class ClojurephantModelBuilder implements ToolingModelBuilder {
  @Override
  public Object buildAll(String modelName, Project project) {
    Map<String, Object> modelData = new HashMap<>();
    ClojureExtension clojure = project.getExtensions().findByType(ClojureExtension.class);
    if (clojure != null) {
      modelData.put("clojure", clojure.getBuilds());
    }

    ClojureScriptExtension clojurescript = project.getExtensions().findByType(ClojureScriptExtension.class);
    if (clojurescript != null) {
      modelData.put("clojurescript", clojurescript.getBuilds());
    }

    Task repl = project.getTasks().findByName(ClojureCommonPlugin.NREPL_TASK_NAME);
    if (repl != null) {
      Map<String, Object> replData = new HashMap<>();
      replData.put("path", repl.getPath());
      replData.put("dir", repl.getTemporaryDir());
      modelData.put("repl", Edn.keywordize(replData));
    }

    return new ClojurephantModel(Edn.print(Edn.keywordize(modelData)));
  }

  @Override
  public boolean canBuild(String modelName) {
    return ClojurephantModel.class.getCanonicalName().equals(modelName);
  }
}
