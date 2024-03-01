/*
 * Copyright (C) 2024 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.keeper

import com.google.auto.service.AutoService
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.tools.StandardLocation.CLASS_OUTPUT
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING

@AutoService(Processor::class)
@IncrementalAnnotationProcessor(ISOLATING)
class RetrofitResponseTypeKeepProcessor : AbstractProcessor() {
  override fun getSupportedSourceVersion() = SourceVersion.latestSupported()
  override fun getSupportedAnnotationTypes() = setOf(
    "retrofit2.http.DELETE",
    "retrofit2.http.GET",
    "retrofit2.http.HEAD",
    "retrofit2.http.HTTP",
    "retrofit2.http.OPTIONS",
    "retrofit2.http.PATCH",
    "retrofit2.http.POST",
    "retrofit2.http.PUT",
  )

  override fun process(
    annotations: Set<TypeElement>,
    roundEnv: RoundEnvironment,
  ): Boolean {
    val elements = processingEnv.elementUtils
    val types = processingEnv.typeUtils

    val methods = supportedAnnotationTypes
      .mapNotNull(elements::getTypeElement)
      .flatMap(roundEnv::getElementsAnnotatedWith)

    val elementToReferencedTypes = mutableMapOf<TypeElement, MutableSet<String>>()
    for (method in methods) {
      val executableElement = method as ExecutableElement

      val serviceType = method.enclosingElement as TypeElement
      val referenced = elementToReferencedTypes.getOrPut(serviceType, ::LinkedHashSet)

      val returnType = executableElement.returnType as DeclaredType
      returnType.recursiveParameterizedTypesTo(referenced)

      // Retrofit has special support for 'suspend fun' in Kotlin which manifests as a
      // final Continuation parameter whose generic type is the declared return type.
      executableElement.parameters
        .lastOrNull()
        ?.asType()
        ?.takeIf { types.erasure(it).toString() == "kotlin.coroutines.Continuation" }
        ?.let { (it as DeclaredType).typeArguments.single() }
        ?.recursiveParameterizedTypesTo(referenced)
    }

    for ((element, referencedTypes) in elementToReferencedTypes) {
      val typeName = element.qualifiedName.toString()
      val outputFile = "META-INF/proguard/retrofit-response-type-keeper-$typeName.pro"
      val rules = processingEnv.filer.createResource(CLASS_OUTPUT, "", outputFile, element)
      rules.openWriter().buffered().use { w ->
        w.write("# $typeName\n")
        for (referencedType in referencedTypes.sorted()) {
          w.write("-keep,allowobfuscation,allowoptimization class $referencedType\n")
        }
      }
    }
    return false
  }

  private fun TypeMirror.recursiveParameterizedTypesTo(types: MutableSet<String>) {
    when (this) {
      is WildcardType -> {
        extendsBound?.recursiveParameterizedTypesTo(types)
        superBound?.recursiveParameterizedTypesTo(types)
      }
      is DeclaredType -> {
        for (typeArgument in typeArguments) {
          typeArgument.recursiveParameterizedTypesTo(types)
        }
        types += (asElement() as TypeElement).qualifiedName.toString()
      }
    }
  }
}
