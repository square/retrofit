package retrofit2

import kotlinx.metadata.Flag.Type.IS_NULLABLE
import kotlinx.metadata.KmFunction
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature
import java.lang.reflect.Method

fun Method.isReturnTypeNullable(): Boolean {
  val kotlinClassMetadata = declaringClass.getAnnotation(Metadata::class.java)?.let { metadata ->
      KotlinClassMetadata.read(
          KotlinClassHeader(
              metadata.kind,
              metadata.metadataVersion,
              metadata.bytecodeVersion,
              metadata.data1,
              metadata.data2,
              metadata.extraString,
              metadata.packageName,
              metadata.extraInt
          )
      )
  }


  if (kotlinClassMetadata is KotlinClassMetadata.Class) {
    val kmClass = kotlinClassMetadata.toKmClass()
    for (function in kmClass.functions) {
      if (function.equalsMethod(this)) {
        if (IS_NULLABLE.invoke(function.returnType.flags)) {
          return true
        }
        break
      }
    }
  }
  return false
}

private fun KmFunction.equalsMethod(method: Method): Boolean {
  if (name != method.name) {
    return false
  }

  val parameters = getParametersTypeNameList()

  if (parameters.size != method.parameterTypes.size) {
    return false
  }

  parameters.zip(method.parameterTypes.map { it.name }).forEach { pair ->
    if (pair.first != pair.second) {
      return false
    }
  }

  return true
}

private fun KmFunction.getParametersTypeNameList(): List<String> {
  val parametersTypeNameList = mutableListOf<String>()

  signature
    ?.desc
    ?.substringAfter('(')
    ?.substringBefore(')')?.let { parametersString ->
      var index = 0
      while (index < parametersString.length) {
        when (parametersString[index]) {
            TypeSignatures.BYTE -> parametersTypeNameList.add(TypeNames.BYTE)
            TypeSignatures.CHAR -> parametersTypeNameList.add(TypeNames.CHAR)
            TypeSignatures.DOUBLE -> parametersTypeNameList.add(TypeNames.DOUBLE)
            TypeSignatures.FLOAT -> parametersTypeNameList.add(TypeNames.FLOAT)
            TypeSignatures.INT -> parametersTypeNameList.add(TypeNames.INT)
            TypeSignatures.LONG -> parametersTypeNameList.add(TypeNames.LONG)
            TypeSignatures.SHORT -> parametersTypeNameList.add(TypeNames.SHORT)
            TypeSignatures.BOOLEAN -> parametersTypeNameList.add(TypeNames.BOOLEAN)
            TypeSignatures.CLASS_REFERENCE -> {
                val endOfClassReferenceIndex = parametersString.indexOf(';', startIndex = index)
                parametersTypeNameList.add(
                    parametersString.substring(
                        startIndex = index + 1,
                        endIndex = endOfClassReferenceIndex
                    ).replace('/', '.')
                )
                index = endOfClassReferenceIndex
            }
            TypeSignatures.ARRAY_REFERENCE -> {
                val endOfClassReferenceIndex = parametersString.indexOf(';', startIndex = index)
                parametersTypeNameList.add(
                    parametersString.substring(
                        startIndex = index,
                        endIndex = endOfClassReferenceIndex + 1
                    ).replace('/', '.')
                )
                index = endOfClassReferenceIndex
            }
        }
        index++
      }
    }
  return parametersTypeNameList
}

object TypeSignatures {
  const val BYTE = 'B'
  const val CHAR = 'C'
  const val DOUBLE = 'D'
  const val FLOAT = 'F'
  const val INT = 'I'
  const val LONG = 'J'
  const val SHORT = 'S'
  const val BOOLEAN = 'Z'
  const val CLASS_REFERENCE = 'L'
  const val ARRAY_REFERENCE = '['
}

object TypeNames {
  const val BYTE = "byte"
  const val CHAR = "char"
  const val DOUBLE = "double"
  const val FLOAT = "float"
  const val INT = "int"
  const val LONG = "long"
  const val SHORT = "short"
  const val BOOLEAN = "boolean"
}