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

import io.reactivex.CompletableObserver;
import io.reactivex.Notification;
import io.reactivex.disposables.Disposable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/** A test {@link CompletableObserver} and JUnit rule which guarantees all events are asserted. */
final class RecordingCompletableObserver implements CompletableObserver {
  private final Deque<Notification<?>> events = new ArrayDeque<>();

  private RecordingCompletableObserver() {
  }

  @Override public void onSubscribe(Disposable disposable) {
  }

  @Override public void onComplete() {
    events.add(Notification.createOnComplete());
  }

  @Override public void onError(Throwable e) {
    events.add(Notification.createOnError(e));
  }

  private Notification<?> takeNotification() {
    Notification<?> notification = events.pollFirst();
    if (notification == null) {
      throw new AssertionError("No event found!");
    }
    return notification;
  }

  public Throwable takeError() {
    Notification<?> notification = takeNotification();
    assertThat(notification.isOnError())
        .as("Expected onError event but was " + notification)
        .isTrue();
    return notification.getError();
  }

  public void assertComplete() {
    Notification<?> notification = takeNotification();
    assertThat(notification.isOnComplete())
        .as("Expected onCompleted event but was " + notification)
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

  public static final class Rule implements TestRule {
    final List<RecordingCompletableObserver> subscribers = new ArrayList<>();

    public <T> RecordingCompletableObserver create() {
      RecordingCompletableObserver subscriber = new RecordingCompletableObserver();
      subscribers.add(subscriber);
      return subscriber;
    }

    @Override public Statement apply(final Statement base, Description description) {
      return new Statement() {
        @Override public void evaluate() throws Throwable {
          base.evaluate();
          for (RecordingCompletableObserver subscriber : subscribers) {
            subscriber.assertNoEvents();
          }
        }
      };
    }
  }
}
