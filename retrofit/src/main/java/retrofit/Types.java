/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Static methods for working with types.
 *
 * @author Bob Lee
 * @author Jesse Wilson
 */
final class Types {
  private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

  private Types() {
    // No instances.
  }

  public static Class<?> getRawType(Type type) {
    if (type instanceof Class<?>) {
      // Type is a normal class.
      return (Class<?>) type;

    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;

      // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
      // suspects some pathological case related to nested classes exists.
      Type rawType = parameterizedType.getRawType();
      if (!(rawType instanceof Class)) throw new IllegalArgumentException();
      return (Class<?>) rawType;

    } else if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      return Array.newInstance(getRawType(componentType), 0).getClass();

    } else if (type instanceof TypeVariable) {
      // We could use the variable's bounds, but that won't work if there are multiple. Having a raw
      // type that's more general than necessary is okay.
      return Object.class;

    } else if (type instanceof WildcardType) {
      return getRawType(((WildcardType) type).getUpperBounds()[0]);

    } else {
      String className = type == null ? "null" : type.getClass().getName();
      throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
          + "GenericArrayType, but <" + type + "> is of type " + className);
    }
  }

  /** Returns true if {@code a} and {@code b} are equal. */
  public static boolean equals(Type a, Type b) {
    if (a == b) {
      return true; // Also handles (a == null && b == null).

    } else if (a instanceof Class) {
      return a.equals(b); // Class already specifies equals().

    } else if (a instanceof ParameterizedType) {
      if (!(b instanceof ParameterizedType)) return false;
      ParameterizedType pa = (ParameterizedType) a;
      ParameterizedType pb = (ParameterizedType) b;
      return equal(pa.getOwnerType(), pb.getOwnerType())
          && pa.getRawType().equals(pb.getRawType())
          && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());

    } else if (a instanceof GenericArrayType) {
      if (!(b instanceof GenericArrayType)) return false;
      GenericArrayType ga = (GenericArrayType) a;
      GenericArrayType gb = (GenericArrayType) b;
      return equals(ga.getGenericComponentType(), gb.getGenericComponentType());

    } else if (a instanceof WildcardType) {
      if (!(b instanceof WildcardType)) return false;
      WildcardType wa = (WildcardType) a;
      WildcardType wb = (WildcardType) b;
      return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
          && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());

    } else if (a instanceof TypeVariable) {
      if (!(b instanceof TypeVariable)) return false;
      TypeVariable<?> va = (TypeVariable<?>) a;
      TypeVariable<?> vb = (TypeVariable<?>) b;
      return va.getGenericDeclaration() == vb.getGenericDeclaration()
          && va.getName().equals(vb.getName());

    } else {
      return false; // This isn't a type we support!
    }
  }

  /**
   * Returns the generic supertype for {@code supertype}. For example, given a class {@code
   * IntegerSet}, the result for when supertype is {@code Set.class} is {@code Set<Integer>} and the
   * result when the supertype is {@code Collection.class} is {@code Collection<Integer>}.
   */
  static Type getGenericSupertype(Type context, Class<?> rawType, Class<?> toResolve) {
    if (toResolve == rawType) return context;

    // We skip searching through interfaces if unknown is an interface.
    if (toResolve.isInterface()) {
      Class<?>[] interfaces = rawType.getInterfaces();
      for (int i = 0, length = interfaces.length; i < length; i++) {
        if (interfaces[i] == toResolve) {
          return rawType.getGenericInterfaces()[i];
        } else if (toResolve.isAssignableFrom(interfaces[i])) {
          return getGenericSupertype(rawType.getGenericInterfaces()[i], interfaces[i], toResolve);
        }
      }
    }

    // Check our supertypes.
    if (!rawType.isInterface()) {
      while (rawType != Object.class) {
        Class<?> rawSupertype = rawType.getSuperclass();
        if (rawSupertype == toResolve) {
          return rawType.getGenericSuperclass();
        } else if (toResolve.isAssignableFrom(rawSupertype)) {
          return getGenericSupertype(rawType.getGenericSuperclass(), rawSupertype, toResolve);
        }
        rawType = rawSupertype;
      }
    }

    // We can't resolve this further.
    return toResolve;
  }

  private static int indexOf(Object[] array, Object toFind) {
    for (int i = 0; i < array.length; i++) {
      if (toFind.equals(array[i])) return i;
    }
    throw new NoSuchElementException();
  }

  private static boolean equal(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  private static int hashCodeOrZero(Object o) {
    return o != null ? o.hashCode() : 0;
  }

  public static String typeToString(Type type) {
    return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
  }

  /**
   * Returns the generic form of {@code supertype}. For example, if this is {@code
   * ArrayList<String>}, this returns {@code Iterable<String>} given the input {@code
   * Iterable.class}.
   *
   * @param supertype a superclass of, or interface implemented by, this.
   */
  public static Type getSupertype(Type context, Class<?> contextRawType, Class<?> supertype) {
    if (!supertype.isAssignableFrom(contextRawType)) throw new IllegalArgumentException();
    return resolve(context, contextRawType,
        getGenericSupertype(context, contextRawType, supertype));
  }

  public static Type resolve(Type context, Class<?> contextRawType, Type toResolve) {
    // This implementation is made a little more complicated in an attempt to avoid object-creation.
    while (true) {
      if (toResolve instanceof TypeVariable) {
        TypeVariable<?> typeVariable = (TypeVariable<?>) toResolve;
        toResolve = resolveTypeVariable(context, contextRawType, typeVariable);
        if (toResolve == typeVariable) {
          return toResolve;
        }

      } else if (toResolve instanceof Class && ((Class<?>) toResolve).isArray()) {
        Class<?> original = (Class<?>) toResolve;
        Type componentType = original.getComponentType();
        Type newComponentType = resolve(context, contextRawType, componentType);
        return componentType == newComponentType ? original : new GenericArrayTypeImpl(
            newComponentType);

      } else if (toResolve instanceof GenericArrayType) {
        GenericArrayType original = (GenericArrayType) toResolve;
        Type componentType = original.getGenericComponentType();
        Type newComponentType = resolve(context, contextRawType, componentType);
        return componentType == newComponentType ? original : new GenericArrayTypeImpl(
            newComponentType);

      } else if (toResolve instanceof ParameterizedType) {
        ParameterizedType original = (ParameterizedType) toResolve;
        Type ownerType = original.getOwnerType();
        Type newOwnerType = resolve(context, contextRawType, ownerType);
        boolean changed = newOwnerType != ownerType;

        Type[] args = original.getActualTypeArguments();
        for (int t = 0, length = args.length; t < length; t++) {
          Type resolvedTypeArgument = resolve(context, contextRawType, args[t]);
          if (resolvedTypeArgument != args[t]) {
            if (!changed) {
              args = args.clone();
              changed = true;
            }
            args[t] = resolvedTypeArgument;
          }
        }

        return changed
            ? new ParameterizedTypeImpl(newOwnerType, original.getRawType(), args)
            : original;

      } else if (toResolve instanceof WildcardType) {
        WildcardType original = (WildcardType) toResolve;
        Type[] originalLowerBound = original.getLowerBounds();
        Type[] originalUpperBound = original.getUpperBounds();

        if (originalLowerBound.length == 1) {
          Type lowerBound = resolve(context, contextRawType, originalLowerBound[0]);
          if (lowerBound != originalLowerBound[0]) {
            return new WildcardTypeImpl(new Type[] { Object.class }, new Type[] { lowerBound });
          }
        } else if (originalUpperBound.length == 1) {
          Type upperBound = resolve(context, contextRawType, originalUpperBound[0]);
          if (upperBound != originalUpperBound[0]) {
            return new WildcardTypeImpl(new Type[] { upperBound }, EMPTY_TYPE_ARRAY);
          }
        }
        return original;

      } else {
        return toResolve;
      }
    }
  }

  private static Type resolveTypeVariable(
      Type context, Class<?> contextRawType, TypeVariable<?> unknown) {
    Class<?> declaredByRaw = declaringClassOf(unknown);

    // We can't reduce this further.
    if (declaredByRaw == null) return unknown;

    Type declaredBy = getGenericSupertype(context, contextRawType, declaredByRaw);
    if (declaredBy instanceof ParameterizedType) {
      int index = indexOf(declaredByRaw.getTypeParameters(), unknown);
      return ((ParameterizedType) declaredBy).getActualTypeArguments()[index];
    }

    return unknown;
  }

  /**
   * Returns the declaring class of {@code typeVariable}, or {@code null} if it was not declared by
   * a class.
   */
  private static Class<?> declaringClassOf(TypeVariable<?> typeVariable) {
    GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
    return genericDeclaration instanceof Class ? (Class<?>) genericDeclaration : null;
  }

  private static void checkNotPrimitive(Type type) {
    if (type instanceof Class<?> && ((Class<?>) type).isPrimitive()) {
      throw new IllegalArgumentException();
    }
  }

  private static final class ParameterizedTypeImpl implements ParameterizedType {
    private final Type ownerType;
    private final Type rawType;
    private final Type[] typeArguments;

    public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... typeArguments) {
      // Require an owner type if the raw type needs it.
      if (rawType instanceof Class<?>
          && (ownerType == null) != (((Class<?>) rawType).getEnclosingClass() == null)) {
        throw new IllegalArgumentException();
      }

      this.ownerType = ownerType;
      this.rawType = rawType;
      this.typeArguments = typeArguments.clone();

      for (Type typeArgument : this.typeArguments) {
        if (typeArgument == null) throw new NullPointerException();
        checkNotPrimitive(typeArgument);
      }
    }

    @Override public Type[] getActualTypeArguments() {
      return typeArguments.clone();
    }

    @Override public Type getRawType() {
      return rawType;
    }

    @Override public Type getOwnerType() {
      return ownerType;
    }

    @Override public boolean equals(Object other) {
      return other instanceof ParameterizedType && Types.equals(this, (ParameterizedType) other);
    }

    @Override public int hashCode() {
      return Arrays.hashCode(typeArguments) ^ rawType.hashCode() ^ hashCodeOrZero(ownerType);
    }

    @Override public String toString() {
      StringBuilder result = new StringBuilder(30 * (typeArguments.length + 1));
      result.append(typeToString(rawType));
      if (typeArguments.length == 0) return result.toString();
      result.append("<").append(typeToString(typeArguments[0]));
      for (int i = 1; i < typeArguments.length; i++) {
        result.append(", ").append(typeToString(typeArguments[i]));
      }
      return result.append(">").toString();
    }
  }

  private static final class GenericArrayTypeImpl implements GenericArrayType {
    private final Type componentType;

    public GenericArrayTypeImpl(Type componentType) {
      this.componentType = componentType;
    }

    @Override public Type getGenericComponentType() {
      return componentType;
    }

    @Override public boolean equals(Object o) {
      return o instanceof GenericArrayType
          && Types.equals(this, (GenericArrayType) o);
    }

    @Override public int hashCode() {
      return componentType.hashCode();
    }

    @Override public String toString() {
      return typeToString(componentType) + "[]";
    }
  }

  /**
   * The WildcardType interface supports multiple upper bounds and multiple
   * lower bounds. We only support what the Java 6 language needs - at most one
   * bound. If a lower bound is set, the upper bound must be Object.class.
   */
  private static final class WildcardTypeImpl implements WildcardType {
    private final Type upperBound;
    private final Type lowerBound;

    public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
      if (lowerBounds.length > 1) throw new IllegalArgumentException();
      if (upperBounds.length != 1) throw new IllegalArgumentException();

      if (lowerBounds.length == 1) {
        if (lowerBounds[0] == null) throw new NullPointerException();
        checkNotPrimitive(lowerBounds[0]);
        if (upperBounds[0] != Object.class) throw new IllegalArgumentException();
        this.lowerBound = lowerBounds[0];
        this.upperBound = Object.class;
      } else {
        if (upperBounds[0] == null) throw new NullPointerException();
        checkNotPrimitive(upperBounds[0]);
        this.lowerBound = null;
        this.upperBound = upperBounds[0];
      }
    }

    @Override public Type[] getUpperBounds() {
      return new Type[] { upperBound };
    }

    @Override public Type[] getLowerBounds() {
      return lowerBound != null ? new Type[] { lowerBound } : EMPTY_TYPE_ARRAY;
    }

    @Override public boolean equals(Object other) {
      return other instanceof WildcardType && Types.equals(this, (WildcardType) other);
    }

    @Override public int hashCode() {
      // This equals Arrays.hashCode(getLowerBounds()) ^ Arrays.hashCode(getUpperBounds()).
      return (lowerBound != null ? 31 + lowerBound.hashCode() : 1) ^ (31 + upperBound.hashCode());
    }

    @Override public String toString() {
      if (lowerBound != null) return "? super " + typeToString(lowerBound);
      if (upperBound == Object.class) return "?";
      return "? extends " + typeToString(upperBound);
    }
  }
}
