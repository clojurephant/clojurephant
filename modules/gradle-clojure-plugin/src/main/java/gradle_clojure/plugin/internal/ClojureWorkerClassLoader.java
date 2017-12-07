/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gradle_clojure.plugin.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A parent-last classloader that also filters all classes we don't think we'd need from the parent.
 * This is meant to avoid Gradle dirtying up the class loader.
 *
 * Based on https://stackoverflow.com/a/6424879/657880
 */
public class ClojureWorkerClassLoader extends URLClassLoader {
  public ClojureWorkerClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
    Objects.requireNonNull(parent);
  }

  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> clazz = findLoadedClass(name);
    if (clazz == null) {
      try {
        clazz = this.findClass(name);
      } catch (ClassNotFoundException | NoClassDefFoundError | SecurityException e) {
        if (clazz == null && allowedClass(name)) {
          clazz = this.getParent().loadClass(name);
        }

        if (clazz == null) {
          throw e;
        }
      }
    }

    if (resolve) {
      this.resolveClass(clazz);
    }
    return clazz;
  }

  @Override
  public URL getResource(String name) {
    URL url = this.findResource(name);
    if (url == null && allowedClass(name)) {
      url = this.getParent().getResource(name);
    }
    return url;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL> currentUrls = this.findResources(name);
    Enumeration<URL> parentUrls = null;
    if (allowedClass(name)) {
      parentUrls = this.getParent().getResources(name);
    }

    if (parentUrls == null) {
      return currentUrls;
    } else {
      return new CombinedEnumeration(currentUrls, parentUrls);
    }
  }

  private boolean allowedClass(String name) {
    return Stream.of(
        // shimdandy api is loaded outside the shim
        "org.projectodd.shimdandy.",
        // base java classes
        "java.",
        "javax.",
        "jdk.",
        "sun.",
        "com.sun.",
        "org.ietf.",
        "org.omg.",
        "org.w3c.",
        "org.xml.").anyMatch(prefix -> name.startsWith(prefix));
  }

  private static class CombinedEnumeration<T> implements Enumeration<T> {
    private final List<Enumeration<T>> delegates;

    public CombinedEnumeration(Enumeration<T>... delegates) {
      this.delegates = Arrays.asList(delegates);
    }

    @Override
    public boolean hasMoreElements() {
      return delegates.stream()
          .anyMatch(Enumeration::hasMoreElements);
    }

    @Override
    public T nextElement() {
      return delegates.stream()
          .filter(Enumeration::hasMoreElements)
          .findFirst()
          .map(Enumeration::nextElement)
          .orElseThrow(() -> new NoSuchElementException());
    }
  }
}
