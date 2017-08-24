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
package gradle_clojure.tools.internal;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

public abstract class LineProcessingOutputStream extends OutputStream {
  private final StringBuilder line = new StringBuilder();
  private final ByteBuffer bytes = ByteBuffer.allocate(8192);
  private final CharBuffer chars = CharBuffer.allocate(8192);
  private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

  @Override
  public void write(int b) {
    bytes.put((byte) b);
    process(false);
  }

  @Override
  public void write(byte[] b) {
    bytes.put(b);
    process(false);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    bytes.put(b, off, len);
    process(false);
  }

  private void process(boolean endOfInput) {
    CoderResult result;
    do {
      bytes.flip();
      result = decoder.decode(bytes, chars, endOfInput);
      bytes.compact();

      chars.flip();
      while (chars.remaining() > 0) {
        char ch = chars.get();
        line.append(ch);
        if (ch == '\n') {
          processLine(line.toString());
          line.setLength(0);
        }
      }
      chars.compact();
    } while (result == CoderResult.OVERFLOW);
  }

  @Override
  public void close() {
    process(true);
    if (line.length() > 0) {
      processLine(line.toString() + "\n");
    }
  }

  protected abstract void processLine(String line);
}
