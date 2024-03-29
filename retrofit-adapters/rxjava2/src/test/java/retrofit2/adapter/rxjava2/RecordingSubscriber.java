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
package retrofit2.adapter.rxjava2;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import io.reactivex.Notification;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/** A test {@link Subscriber} and JUnit rule which guarantees all events are asserted. */
final class RecordingSubscriber<T> implements Subscriber<T> {
  private final long initialRequest;
  private final Deque<Notification<T>> events = new ArrayDeque<>();

  private Subscription subscription;

  private RecordingSubscriber(long initialRequest) {
    this.initialRequest = initialRequest;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    this.subscription = subscription;

    subscription.request(initialRequest);
  }

  @Override
  public void onNext(T value) {
    events.add(Notification.createOnNext(value));
  }

  @Override
  public void onComplete() {
    events.add(Notification.createOnComplete());
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
    assertWithMessage("Expected onNext event but was " + notification)
        .that(notification.isOnNext())
        .isTrue();
    return notification.getValue();
  }

  public Throwable takeError() {
    Notification<T> notification = takeNotification();
    assertWithMessage("Expected onError event but was " + notification)
        .that(notification.isOnError())
        .isTrue();
    return notification.getError();
  }

  public RecordingSubscriber<T> assertAnyValue() {
    takeValue();
    return this;
  }

  public RecordingSubscriber<T> assertValue(T value) {
    assertThat(takeValue()).isEqualTo(value);
    return this;
  }

  public void assertComplete() {
    Notification<T> notification = takeNotification();
    assertWithMessage("Expected onCompleted event but was " + notification)
        .that(notification.isOnComplete())
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
      assertThat(throwable).hasMessageThat().isEqualTo(message);
    }
    assertNoEvents();
  }

  public void assertNoEvents() {
    assertWithMessage("Unconsumed events found!").that(events).isEmpty();
  }

  public void request(long amount) {
    if (subscription == null) {
      throw new IllegalStateException("onSubscribe has not been called yet. Did you subscribe()?");
    }
    subscription.request(amount);
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
