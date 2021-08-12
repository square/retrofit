/*
 * Copyright (C) 2016 Square, Inc.
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
package retrofit2.adapter.rxjava;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Notification;
import rx.Subscriber;

/** A test {@link Subscriber} and JUnit rule which guarantees all events are asserted. */
final class RecordingSubscriber<T> extends Subscriber<T> {
  private final long initialRequest;
  private final Deque<Notification<T>> events = new ArrayDeque<>();

  private RecordingSubscriber(long initialRequest) {
    this.initialRequest = initialRequest;
  }

  @Override
  public void onStart() {
    request(initialRequest);
  }

  @Override
  public void onNext(T value) {
    events.add(Notification.createOnNext(value));
  }

  @Override
  public void onCompleted() {
    events.add(Notification.createOnCompleted());
  }

  @Override
  public void onError(Throwable e) {
    events.add(Notification.createOnError(e));
  }

  private Notification<T> takeNotification() {
    Notification<T> notification = events.pollFirst();
    if (notification == null) {
      throw new AssertionError("No event found!");
    }
    return notification;
  }

  public T takeValue() {
    Notification<T> notification = takeNotification();
    assertThat(notification.isOnNext())
        .overridingErrorMessage("Expected onNext event but was %s", notification)
        .isTrue();
    return notification.getValue();
  }

  public Throwable takeError() {
    Notification<T> notification = takeNotification();
    assertThat(notification.isOnError())
        .overridingErrorMessage("Expected onError event but was %s", notification)
        .isTrue();
    return notification.getThrowable();
  }

  public RecordingSubscriber<T> assertAnyValue() {
    takeValue();
    return this;
  }

  public RecordingSubscriber<T> assertValue(T value) {
    assertThat(takeValue()).isEqualTo(value);
    return this;
  }

  public void assertCompleted() {
    Notification<T> notification = takeNotification();
    assertThat(notification.isOnCompleted())
        .overridingErrorMessage("Expected onCompleted event but was %s", notification)
        .isTrue();
    assertNoEvents();
  }

  public void assertError(Throwable throwable) {
    assertThat(takeError()).isEqualTo(throwable);
  }

  public void assertError(Class<? extends Throwable> errorClass) {
    assertError(errorClass, null);
  }

  public void assertError(Class<? extends Throwable> errorClass, String message) {
    Throwable throwable = takeError();
    assertThat(throwable).isInstanceOf(errorClass);
    if (message != null) {
      assertThat(throwable).hasMessage(message);
    }
    assertNoEvents();
  }

  public void assertNoEvents() {
    assertThat(events).as("Unconsumed events found!").isEmpty();
  }

  public void requestMore(long amount) {
    request(amount);
  }

  public static final class Rule implements TestRule {
    final List<RecordingSubscriber<?>> subscribers = new ArrayList<>();

    public <T> RecordingSubscriber<T> create() {
      return createWithInitialRequest(Long.MAX_VALUE);
    }

    public <T> RecordingSubscriber<T> createWithInitialRequest(long initialRequest) {
      RecordingSubscriber<T> subscriber = new RecordingSubscriber<>(initialRequest);
      subscribers.add(subscriber);
      return subscriber;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          base.evaluate();
          for (RecordingSubscriber<?> subscriber : subscribers) {
            subscriber.assertNoEvents();
          }
        }
      };
    }
  }
}
