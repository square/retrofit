package retrofit;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleResponseDispatcher implements MockService.Dispatcher {

    private Map<Class<?>, Object> responses = new ConcurrentHashMap<Class<?>, Object>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getResponse(final Class<T> returnType, final Method method, final Object[] args) {
        return (T) responses.get(returnType);
    }

    public <T> void setResponse(final Class<T> responseType, final T response) {
        responses.put(responseType, response);
    }

}
