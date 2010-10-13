// Copyright 2010 Square, Inc.
package retrofit.android;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects phone shaking. If > 75% of the samples taken in the past 0.5s are
 * accelerating, the device is a) shaking, or b) free falling 1.84m (h =
 * 1/2*g*t^2*3/4).
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Eric Burke (eric@squareup.com)
 */
public class ShakeDetector implements SensorEventListener {

  /**
   * When the magnitude of total acceleration exceeds this
   * value, the phone is accelerating.
   */
  private static final int ACCELERATION_THRESHOLD = 13;

  /** Listens for shakes. */
  public interface Listener {
    /** Called on the main thread when the device is shaken. */
    void hearShake();
  }

  private final SampleQueue queue = new SampleQueue();
  private final Listener listener;

  private SensorManager sensorManager;
  private Sensor accelerometer;

  public ShakeDetector(Listener listener) {
    this.listener = listener;
  }

  /**
   * Starts listening for shakes on devices with appropriate hardware.
   *
   * @returns true if the device supports shake detection.
   */
  public boolean start(SensorManager sensorManager) {
    // Already started?
    if (accelerometer != null) return true;

    accelerometer = sensorManager.getDefaultSensor(
        Sensor.TYPE_ACCELEROMETER);

    // If this phone has an accelerometer, listen to it.
    if (accelerometer != null) {
      this.sensorManager = sensorManager;
      sensorManager.registerListener(this, accelerometer,
          SensorManager.SENSOR_DELAY_FASTEST);
    }
    return accelerometer != null;
  }

  /** Stops listening, ignored on devices without appropriate hardware. */
  public void stop() {
    if (accelerometer != null) {
      sensorManager.unregisterListener(this, accelerometer);
      sensorManager = null;
      accelerometer = null;
    }
  }

  public void onSensorChanged(SensorEvent event) {
    boolean accelerating = isAccelerating(event);
    long timestamp = event.timestamp;
    queue.add(timestamp, accelerating);
    if (queue.isShaking()) {
      queue.clear();
      listener.hearShake();
    }
  }

  /** Returns true if the device is currently accelerating. */
  private boolean isAccelerating(SensorEvent event) {
    float ax = event.values[0];
    float ay = event.values[1];
    float az = event.values[2];

    final double magnitude = Math.sqrt(ax * ax + ay * ay + az * az);
    return magnitude > ACCELERATION_THRESHOLD;
  }

  /** Queue of samples. Keeps a running average. */
  static class SampleQueue {

    /** Window size in ns. Used to compute the average. */
    private static final long MAX_WINDOW_SIZE = 500000000; // 0.5s
    private static final long MIN_WINDOW_SIZE = MAX_WINDOW_SIZE >> 1; // 0.25s

    /**
     * Ensure the queue size never falls below this size, even if the device
     * fails to deliver this many events during the time window. The LG Ally
     * is one such device.
     */
    private static final int MIN_QUEUE_SIZE = 4;

    private final SamplePool pool = new SamplePool();

    private Sample oldest;
    private Sample newest;
    private int sampleCount;
    private int acceleratingCount;

    /**
     * Adds a sample.
     *
     * @param timestamp    in nanoseconds of sample
     * @param accelerating true if > {@link #ACCELERATION_THRESHOLD}.
     */
    void add(long timestamp, boolean accelerating) {
      // Purge samples that proceed window.
      purge(timestamp - MAX_WINDOW_SIZE);

      // Add the sample to the queue.
      Sample added = pool.acquire();
      added.timestamp = timestamp;
      added.accelerating = accelerating;
      added.next = null;
      if (newest != null) newest.next = added;
      newest = added;
      if (oldest == null) oldest = added;

      // Update running average.
      sampleCount++;
      if (accelerating) acceleratingCount++;
    }

    /**
     * Removes all samples from this queue.
     */
    void clear() {
      while (oldest != null) {
        Sample removed = oldest;
        oldest = removed.next;
        pool.release(removed);
      }
      newest = null;
      sampleCount = 0;
      acceleratingCount = 0;
    }

    /** Purges samples with timestamps older than cutoff. */
    void purge(long cutoff) {
      while (sampleCount >= MIN_QUEUE_SIZE &&
          oldest != null && cutoff - oldest.timestamp > 0) {
        // Remove sample.
        Sample removed = oldest;
        if (removed.accelerating) acceleratingCount--;
        sampleCount--;

        oldest = removed.next;
        if (oldest == null) newest = null;
        pool.release(removed);
      }
    }

    /** Copies the samples into a list, with the oldest entry at index 0. */
    List<Sample> asList() {
      List<Sample> list = new ArrayList<Sample>();
      Sample s = oldest;
      while (s != null) {
        list.add(s);
        s = s.next;
      }
      return list;
    }

    /**
     * Returns true if we have enough samples and more than 3/4 of those samples
     * are accelerating.
     */
    boolean isShaking() {
      return newest != null
          && oldest != null
          && newest.timestamp - oldest.timestamp >= MIN_WINDOW_SIZE
          && acceleratingCount >= (sampleCount >> 1) + (sampleCount >> 2);
    }
  }

  /** An accelerometer sample. */
  static class Sample {
    /** Time sample was taken. */
    long timestamp;

    /** If acceleration > {@link #ACCELERATION_THRESHOLD} */
    boolean accelerating;

    /** Next sample in the queue or pool. */
    Sample next;
  }

  /** Pools samples. Avoids garbage collection. */
  static class SamplePool {
    private Sample head;

    /** Acquires a sample from the pool. */
    Sample acquire() {
      Sample acquired = head;
      if (acquired == null) {
        acquired = new Sample();
      } else {
        // Remove instance from pool.
        head = acquired.next;
      }
      return acquired;
    }

    /** Returns a sample to the pool. */
    void release(Sample sample) {
      sample.next = head;
      head = sample;
    }
  }

  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }
}
