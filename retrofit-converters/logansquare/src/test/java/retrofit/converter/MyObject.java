package retrofit.converter;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject
public class MyObject {
  @JsonField
  public String message;

  @JsonField
  public int count;

  public MyObject() {
  }

  public MyObject(String message, int count) {
    this.message = message;
    this.count = count;
  }

  public String getMessage() {
    return message;
  }

  public int getCount() {
    return count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MyObject myObject = (MyObject) o;
    return count == myObject.count
        && !(message != null ? !message.equals(myObject.message) : myObject.message != null);
  }

  @Override
  public int hashCode() {
    int result = message != null ? message.hashCode() : 0;
    result = 31 * result + count;
    return result;
  }
}
