package retrofit2;

import java.lang.reflect.*;
import java.util.Objects;

abstract class TypeHandler {
  abstract Class<?> getRawType(Type type);

  static class ClassTypeHandler extends TypeHandler {
    @Override
    public Class<?> getRawType(Type type) {
      Objects.requireNonNull(type, "type == null");

      if (type instanceof Class<?>) {
        // Type is a normal class.
        return (Class<?>) type;
      }
      throw new IllegalArgumentException(
        "Expected a Class, but <" + type + "> is of type " + type.getClass().getName());
    }
  }

  // Implementation for handling ParameterizedType type
  static class ParameterizedTypeHandler extends TypeHandler {
    @Override
    public Class<?> getRawType(Type type) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type rawType = parameterizedType.getRawType();
      if (!(rawType instanceof Class)) throw new IllegalArgumentException();
      return (Class<?>) rawType;
    }
  }

  // Implementation for handling GenericArrayType type
  static class GenericArrayTypeHandler extends TypeHandler {
    @Override
    public Class<?> getRawType(Type type) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      return Array.newInstance(getRawType(componentType), 0).getClass();
    }
  }

  // Implementation for handling TypeVariable type
  static class TypeVariableHandler extends TypeHandler {
    @Override
    public Class<?> getRawType(Type type) {
      return Object.class; // More general raw type than necessary
    }
  }

  // Implementation for handling WildcardType type
  static class WildcardTypeHandler extends TypeHandler {
    @Override
    public Class<?> getRawType(Type type) {
      return getRawType(((WildcardType) type).getUpperBounds()[0]);
    }
  }

}

