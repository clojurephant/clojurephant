package dev.clojurephant.plugin.common.internal;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RelativePath;
import org.gradle.api.provider.Provider;

public final class Namespaces {
  public static final Set<String> CLOJURE_PATTERNS = Collections.unmodifiableSet(Stream.of("**/*.clj", "**/*.cljc").collect(Collectors.toSet()));
  public static final Set<String> CLOJURESCRIPT_PATTERNS = Collections.unmodifiableSet(Stream.of("**/*.cljs", "**/*.cljc", "**/*.clj").collect(Collectors.toSet()));

  private Namespaces() {
    // do not instantiate
  }

  public static Provider<Set<String>> findNamespaces(FileTree source) {
    return source.getElements().map(files -> {
      Set<String> namespaces = new HashSet<>();

      source.visit(details -> {
        if (details.isDirectory()) {
          // not a namespace
          return;
        }

        RelativePath relPath = details.getRelativePath();
        String fileBaseName = relPath.getLastName().substring(0, relPath.getLastName().lastIndexOf('.'));

        if (fileBaseName.equals("data_readers")) {
          // these aren't namespaces
          return;
        }

        RelativePath relPathNoExt = relPath.replaceLastName(fileBaseName);
        String namespace = Arrays.stream(relPathNoExt.getSegments())
            .map(Namespaces::demunge)
            .collect(Collectors.joining("."));
        namespaces.add(namespace);
      });

      return namespaces;
    });
  }

  private static final Map<Character, String> CHAR_MAP = new HashMap<>();

  static {
    CHAR_MAP.put('-', "_");
    CHAR_MAP.put(':', "_COLON_");
    CHAR_MAP.put('+', "_PLUS_");
    CHAR_MAP.put('>', "_GT_");
    CHAR_MAP.put('<', "_LT_");
    CHAR_MAP.put('=', "_EQ_");
    CHAR_MAP.put('~', "_TILDE_");
    CHAR_MAP.put('!', "_BANG_");
    CHAR_MAP.put('@', "_CIRCA_");
    CHAR_MAP.put('#', "_SHARP_");
    CHAR_MAP.put('\'', "_SINGLEQUOTE_");
    CHAR_MAP.put('"', "_DOUBLEQUOTE_");
    CHAR_MAP.put('%', "_PERCENT_");
    CHAR_MAP.put('^', "_CARET_");
    CHAR_MAP.put('&', "_AMPERSAND_");
    CHAR_MAP.put('*', "_STAR_");
    CHAR_MAP.put('|', "_BAR_");
    CHAR_MAP.put('{', "_LBRACE_");
    CHAR_MAP.put('}', "_RBRACE_");
    CHAR_MAP.put('[', "_LBRACK_");
    CHAR_MAP.put(']', "_RBRACK_");
    CHAR_MAP.put('/', "_SLASH_");
    CHAR_MAP.put('\\', "_BSLASH_");
    CHAR_MAP.put('?', "_QMARK_");
  }

  private static final Map<String, Character> DEMUNGE_MAP = CHAR_MAP.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  private static final Pattern DEMUNGE_PATTERN = Pattern.compile(DEMUNGE_MAP.keySet().stream()
      .sorted(Comparator.comparingInt(String::length).reversed())
      .map(it -> "\\Q" + it + "\\E")
      .collect(Collectors.joining("|")));

  private static String demunge(String mungedName) {
    StringBuilder sb = new StringBuilder();
    Matcher m = DEMUNGE_PATTERN.matcher(mungedName);
    int lastMatchEnd = 0;
    while (m.find()) {
      int start = m.start();
      int end = m.end();
      // Keep everything before the match
      sb.append(mungedName.substring(lastMatchEnd, start));
      lastMatchEnd = end;
      // Replace the match with DEMUNGE_MAP result
      char origCh = DEMUNGE_MAP.get(m.group());
      sb.append(origCh);
    }
    // Keep everything after the last match
    sb.append(mungedName.substring(lastMatchEnd));
    return sb.toString();
  }
}
