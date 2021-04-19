/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.deserialization;

import com.google.protobuf.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.metadata.ProtoBuf;

public class Flags {
    protected Flags() {}

    // Infrastructure

    public static abstract class FlagField<E> {

        public final int offset;
        public final int bitWidth;

        private FlagField(int offset, int bitWidth) {
            this.offset = offset;
            this.bitWidth = bitWidth;
        }

        public abstract E get(int flags);
    }

    @SuppressWarnings("WeakerAccess")
    public static class BooleanFlagField extends FlagField<Boolean> {
        public BooleanFlagField(int offset) {
            super(offset, 1);
        }

        @Override
        @NotNull
        public Boolean get(int flags) {
            return (flags & (1 << offset)) != 0;
        }

        public int invert(int flags) { return (flags ^ (1 << offset)); }
    }
}
