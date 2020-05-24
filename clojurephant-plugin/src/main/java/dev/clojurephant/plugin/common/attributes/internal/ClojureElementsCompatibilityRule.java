package dev.clojurephant.plugin.common.attributes.internal;

import dev.clojurephant.plugin.common.attributes.ClojureElements;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;

public class ClojureElementsCompatibilityRule implements AttributeCompatibilityRule<ClojureElements> {
  @Override
  public void execute(CompatibilityCheckDetails<ClojureElements> details) {
    boolean wantsSource = ClojureElements.SOURCE.equals(details.getConsumerValue().getName());
    boolean producesAot = ClojureElements.AOT.equals(details.getProducerValue().getName());
    if (wantsSource && producesAot) {
      details.incompatible();
    } else {
      details.compatible();
    }
  }
}
