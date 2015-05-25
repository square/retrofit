package retrofit.converter;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject
public class User {
  @JsonField
  String fullName;

  public User() {
  }

  public User(String name) {
    fullName = name;
  }

  public String getFullName() {
    return fullName;
  }
}
