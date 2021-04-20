package retrofit2

import retrofit2.kotlinx.metadata.Flag
import retrofit2.kotlinx.metadata.KmClassifier
import retrofit2.kotlinx.metadata.KmFunction
import retrofit2.kotlinx.metadata.jvm.KotlinClassHeader
import retrofit2.kotlinx.metadata.jvm.KotlinClassMetadata
import retrofit2.kotlinx.metadata.jvm.signature
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object KotlinMetadata {

    private val kotlinFunctionsMap = ConcurrentHashMap<Class<*>, List<KmFunction>>()

    @JvmStatic fun isReturnTypeNullable(method: Method): Boolean {
        val javaMethodSignature = method.createSignature()
        val kotlinFunctions = loadKotlinFunctions(method.declaringClass)
        val candidates = kotlinFunctions.filter { it.signature?.asString() == javaMethodSignature }

        require(candidates.isNotEmpty()) { "No match found in metadata for '${method}'" }
        require(candidates.size == 1) { "Multiple function matches found in metadata for '${method}'" }
        val match = candidates.first()

        val isNullable = Flag.Type.IS_NULLABLE(match.returnType.flags)
        val isUnit = match.returnType.classifier == KmClassifier.Class("kotlin/Unit")
        return isNullable || isUnit
    }

    private fun Method.createSignature() = buildString {
        append(name)
        append('(')

        parameterTypes.forEach {
            append(it.typeToSignature())
        }

        append(')')

        append(returnType.typeToSignature())
    }

    private fun loadKotlinFunctions(clazz: Class<*>): List<KmFunction> {
        var result = kotlinFunctionsMap[clazz]
        if (result != null) return result

        synchronized(kotlinFunctionsMap) {
            result = kotlinFunctionsMap[clazz]
            if (result == null) {
                result = readFunctionsFromMetadata(clazz)
            }
        }

        return result!!
    }

    private fun readFunctionsFromMetadata(clazz: Class<*>): List<KmFunction> {
        val metadataAnnotation = clazz.getAnnotation(Metadata::class.java)

        val header = KotlinClassHeader(
            kind = metadataAnnotation.kind,
            metadataVersion = metadataAnnotation.metadataVersion,
            data1 = metadataAnnotation.data1,
            data2 = metadataAnnotation.data2,
            extraString = metadataAnnotation.extraString,
            extraInt = metadataAnnotation.extraInt,
            packageName = metadataAnnotation.packageName
        )

        val classMetadata = KotlinClassMetadata.read(header)
        val kmClass = (classMetadata as KotlinClassMetadata.Class).toKmClass()

        return kmClass.functions
    }

    private fun Class<*>.typeToSignature() = when {
        isPrimitive -> javaTypesMap[name]
        isArray -> name.replace('.', '/')
        else -> "L${name.replace('.', '/')};"
    }

    private val javaTypesMap = mapOf(
        "int" to "I",
        "long" to "J",
        "boolean" to "Z",
        "byte" to "B",
        "char" to "C",
        "float" to "F",
        "double" to "D",
        "short" to "S",
        "void" to "V"
    )
}