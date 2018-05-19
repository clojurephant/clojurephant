package gradle_clojure.plugin.common.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public class PluginMetadata {
  public static final String GRADLE_CLOJURE_VERSION = getVersion();

  private static String getVersion() {
    try (InputStream stream = PluginMetadata.class.getResourceAsStream("/gradle-clojure.properties")) {
      Properties props = new Properties();
      props.load(stream);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
