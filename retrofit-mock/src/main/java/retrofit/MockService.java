package retrofit;

import rx.Observable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public final class MockService {

    public interface Dispatcher {
        <T> T getResponse(Class<T> returnType, Method method, final Object[] args);
    }

    static public <T> T create(final MockRestAdapter mockRestAdapter, final Class<T> retrofitApi, final Dispatcher dispatcher) {
        return mockRestAdapter.create(retrofitApi, create(retrofitApi,dispatcher));
    }

    @SuppressWarnings("unchecked")
    static private <T> T create(final Class<T> retrofitApi, final Dispatcher dispatcher) {
        return (T) Proxy.newProxyInstance(retrofitApi.getClassLoader(), new Class<?>[] {retrofitApi},
                                          new MockServiceHandler(dispatcher));
    }

    static public class MockServiceHandler implements InvocationHandler {
        private final Map<Method, MethodInfo> methodInfoCache;
        private final Dispatcher dispatcher;

        public MockServiceHandler(final Dispatcher dispatcher) {
            this(new HashMap<Method, MethodInfo>(), dispatcher);
        }

        public MockServiceHandler(final Map<Method, MethodInfo> methodInfoCache, final Dispatcher dispatcher) {
            this.methodInfoCache = methodInfoCache;
            this.dispatcher = dispatcher;
        }

        private MethodInfo getMethodInfo(Method method) {
            synchronized (methodInfoCache) {
                MethodInfo methodInfo = methodInfoCache.get(method);
                if (methodInfo == null) {
                    methodInfo = new MethodInfo(method);
                    methodInfoCache.put(method, methodInfo);
                }
                return methodInfo;
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, final Object[] args) throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            // Load or create the details cache for the current method.
            final MethodInfo methodInfo = getMethodInfo(method);
            final Object response;
            if (methodInfo.responseObjectType instanceof Class) {
                response = dispatcher.getResponse((Class) (methodInfo.responseObjectType), method, args);
            } else {
                throw new ClassCastException("'methodInfo.responseObjectType' is not an instance of 'Class'");
            }
            if (methodInfo.executionType == MethodInfo.ExecutionType.SYNC) {
                return response;
            }
            if (methodInfo.executionType == MethodInfo.ExecutionType.ASYNC) {
                final Callback callback = (Callback) args[args.length - 1];
                callback.success(response, null);
                return null;
            }
            if (methodInfo.executionType == MethodInfo.ExecutionType.RX) {
                return Observable.just(response);
            }
            throw new IllegalArgumentException("executionType must be one of: SYNC, ASYNC, RX");
        }
    }
}
