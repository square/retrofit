package retrofit2.converter.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

interface GsonAdapterProvider {
  <T> TypeAdapter<T> get(TypeToken<T> type);
}
