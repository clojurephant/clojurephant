package dev.clojurephant.plugin.common.attributes.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.clojurephant.plugin.common.attributes.ClojureElements;
import org.gradle.api.Named;
import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;

public class ClojureElementsDisambiguationRule implements AttributeDisambiguationRule<ClojureElements> {
  private static final List<String> PRIORITY = Arrays.asList(ClojureElements.SOURCE, ClojureElements.AOT);

  @Override
  public void execute(MultipleCandidatesDetails<ClojureElements> details) {
    Map<String, ClojureElements> candidates = details.getCandidateValues().stream()
        .collect(Collectors.toMap(Named::getName, Function.identity()));

    PRIORITY.stream()
        .filter(candidates::containsKey)
        .map(candidates::get)
        .findFirst()
        .ifPresent(details::closestMatch);
  }
}
