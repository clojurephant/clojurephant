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
package gradle_clojure.clojure.tasks.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.inject.Inject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class ClojureEval implements Runnable {
  private final String script;
  private final String classpath;

  @Inject
  public ClojureEval(String script, String classpath) {
    this.script = script;
    this.classpath = classpath;
  }

  @Override
  public void run() {
    URL[] classpathUrls = Arrays.stream(classpath.split(File.pathSeparator))
        .map(ClojureEval::parseUrl)
        .toArray(size -> new URL[size]);

    ClassLoader parent = ClassLoader.getSystemClassLoader();
    ClassLoader loader = new URLClassLoader(classpathUrls, parent);

    Thread.currentThread().setContextClassLoader(loader);
    IFn main = Clojure.var("clojure.main", "main");
    main.invoke("--eval", script);
  }

  private static URL parseUrl(String path) {
    try {
      File file = new File(path);
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      sneakyThrows(e);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void sneakyThrows(Throwable t) throws T {
    throw (T) t;
  }
}
