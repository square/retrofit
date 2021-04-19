/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf

class VersionRequirementTable private constructor(private val infos: List<ProtoBuf.VersionRequirement>) {
    operator fun get(id: Int): ProtoBuf.VersionRequirement? = infos.getOrNull(id)

    companion object {
        val EMPTY = VersionRequirementTable(emptyList())

        fun create(table: ProtoBuf.VersionRequirementTable): VersionRequirementTable =
            if (table.requirementCount == 0) EMPTY else VersionRequirementTable(
                table.requirementList
            )
    }
}
