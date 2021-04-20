/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package retrofit2.kotlin.metadata.deserialization

import retrofit2.kotlin.metadata.ProtoBuf

fun ProtoBuf.Function.returnType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasReturnType() -> returnType
    hasReturnTypeId() -> typeTable[returnTypeId]
    else -> error("No returnType in ProtoBuf.Function")
}

fun ProtoBuf.Function.receiverType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasReceiverType() -> receiverType
    hasReceiverTypeId() -> typeTable[receiverTypeId]
    else -> null
}

fun ProtoBuf.ValueParameter.type(typeTable: TypeTable): ProtoBuf.Type = when {
    hasType() -> type
    hasTypeId() -> typeTable[typeId]
    else -> error("No type in ProtoBuf.ValueParameter")
}
