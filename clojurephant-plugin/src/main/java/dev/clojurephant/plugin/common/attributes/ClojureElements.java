package dev.clojurephant.plugin.common.attributes;

import org.gradle.api.Named;
import org.gradle.api.attributes.Attribute;

public interface ClojureElements extends Named {
  Attribute<ClojureElements> CLJELEMENTS_ATTRIBUTE = Attribute.of("dev.clojurephant.alpha.cljelements", ClojureElements.class);

  String SOURCE = "source";
  String AOT = "aot";
}
