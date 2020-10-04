package retrofit2

import org.junit.Test

class MetaDataUtilsTest {

  interface TestFunctionsNonNull {
    fun unit()
    fun string(): String
    fun int(): Int
    fun any(): Any
    fun params(
        byte: Byte,
        char: Char,
        double: Double,
        float: Float,
        int: Int,
        long: Long,
        short: Short,
        boolean: Boolean,
        array: Array<Int>,
        arrayComplex: Array<TestClassComplex<String, TestClassComplex<Int, Float>>>,
        clazz: TestClass,
        clazzComplex: TestClassComplex<String, TestClassComplex<Int, Float>>
    ): String
    fun parameterized(): TestClassComplex<String, TestClassComplex<String, Int>>
  }

  interface TestFunctionsNullable {
    fun string(): String?
    fun int(): Int?
    fun any(): Any?
    fun params(
        byte: Byte,
        char: Char,
        double: Double,
        float: Float,
        int: Int,
        long: Long,
        short: Short,
        boolean: Boolean,
        array: Array<Int>,
        arrayComplex: Array<TestClassComplex<String, TestClassComplex<Int, Float>>>,
        clazz: TestClass,
        clazzComplex: TestClassComplex<String, TestClassComplex<Int, Float>>
    ): String?
    fun parameterized(): TestClassComplex<String, TestClassComplex<String, Int>>?
  }

  class TestClass

  class TestClassComplex<A, B>

  @Test
  fun testNonNull() {
    TestFunctionsNonNull::class.java.declaredMethods.forEach { method ->
      val isReturnTypeNullable = method.isReturnTypeNullable()
      println("NonNull ${method.name}: ${!isReturnTypeNullable}")
      assert(!isReturnTypeNullable)
    }
  }

  @Test
  fun testNullable() {
    TestFunctionsNullable::class.java.declaredMethods.forEach { method ->
      val isReturnTypeNullable = method.isReturnTypeNullable()
      println("Nullable ${method.name}: $isReturnTypeNullable")
      assert(isReturnTypeNullable)
    }
  }
}