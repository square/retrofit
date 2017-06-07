package retrofit2;

public class KeyValue<K, V> {

  public K key;

  public V value;

  public KeyValue(final K key, final V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public String toString() {
    return key.toString() + ":" + value.toString();
  }

  public V getValue() {
    return value;
  }

  public K getKey() {
    return key;
  }
}
