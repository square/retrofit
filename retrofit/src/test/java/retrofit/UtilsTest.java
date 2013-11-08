// Copyright 2013 Square, Inc.
package retrofit;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import java.io.Serializable;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class UtilsTest {

  @Test public void markerInterfaceIsNotFunctional() {
    assertThat(Utils.isFunctionalInterface(Serializable.class)).isFalse();
  }

  @Test public void singleMethodInterfaceIsFunctional() {
    assertThat(Utils.isFunctionalInterface(Supplier.class)).isTrue();
  }

  @Test public void interfaceOverridingObjectMethodIsStillFunctional() {
    assertThat(Utils.isFunctionalInterface(Function.class)).isTrue();
  }

  @Test public void multiMethodInterfaceIsNotFunctional() {
    assertThat(Utils.isFunctionalInterface(Callback.class)).isFalse();
  }
}
