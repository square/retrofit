// Copyright 2013 Square, Inc.
package retrofit.http.client;

import java.io.IOException;
import java.io.InputStream;
import retrofit.io.TypedInput;

public class TypedInputStream implements TypedInput {

  @Override public String mimeType() {
    return null;
  }

  @Override public long length() {
    return 0;
  }

  @Override public InputStream in() throws IOException {
    return null;
  }
}
