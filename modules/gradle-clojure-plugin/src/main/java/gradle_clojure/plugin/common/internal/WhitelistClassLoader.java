package gradle_clojure.plugin.common.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A whitelisting classloader that only lets the classes starting with the provided prefixes
 * through.
 */
public class WhitelistClassLoader extends URLClassLoader {
  private final Set<String> prefixes;

  public WhitelistClassLoader(Set<String> prefixes, ClassLoader parent) {
    super(new URL[0], parent);
    this.prefixes = prefixes;
  }

  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (allowedClass(name)) {
      return super.loadClass(name, resolve);
    } else {
      throw new ClassNotFoundException("Class is not in the whitelist: " + name);
    }
  }

  @Override
  public URL getResource(String name) {
    if (allowedClass(name)) {
      return super.getResource(name);
    } else {
      return null;
    }
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    if (allowedClass(name)) {
      return super.getResources(name);
    } else {
      return new EmptyEnumeration<>();
    }
  }

  private boolean allowedClass(String name) {
    return prefixes.stream().anyMatch(name::startsWith);
  }

  private static class EmptyEnumeration<T> implements Enumeration<T> {
    @Override
    public boolean hasMoreElements() {
      return false;
    }

    @Override
    public T nextElement() {
      throw new NoSuchElementException();
    }
  }
}
