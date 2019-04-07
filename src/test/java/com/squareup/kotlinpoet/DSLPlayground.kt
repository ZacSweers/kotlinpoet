package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.CodeBlockClosure.CodeBlockMarker
import com.squareup.kotlinpoet.FunctionClosure.FunctionMarker
import com.squareup.kotlinpoet.ParameterClosure.ParameterMarker
import kotlin.test.Test

class DSLPlayground {

  @Test
  fun codeblockTest() {
    val block = codeBlock {
      controlFlow("val result = when") {
        controlFlow("%S -> ", "Test") {
          "%S"("taco")
        }
        controlFlow("else ->") {
          "%S"("not taco")
        }
      }
      addStatement("println()")
      addCode("// Random code\n")
      "return %T.builder(result)"(FunSpec::class)
    }

    assertThat(block.toString()).isEqualTo("""
      |val result = when {
      |    "Test" ->  {
      |        "taco"
      |    }
      |    else -> {
      |        "not taco"
      |    }
      |}
      |println()
      |// Random code
      |return com.squareup.kotlinpoet.FunSpec.builder(result)
      |""".trimMargin())
  }

  @Test
  fun functionTest() {

  }

}

fun function(name: String, init: FunctionClosure.() -> Unit): FunSpec {
  val block = FunctionClosure(name)
  block.init()
  return block.build()
}

@FunctionMarker
class FunctionClosure(name: String) {
  @DslMarker
  annotation class FunctionMarker

  private val builder = FunSpec.builder(name)

  operator fun String.invoke(vararg args: Any) {
    builder.addStatement(this, *args)
  }

  fun statement(statement: String, vararg args: Any) {
    builder.addStatement(statement, *args)
  }

  fun code(statement: String, vararg args: Any) {
    builder.addCode(statement, *args)
  }

  fun code(init: CodeBlockClosure.() -> Unit) {
    val block = CodeBlockClosure()
    block.init()
    builder.addCode(block.build())
  }

  fun controlFlow(controlFlow: String, vararg args: Any, init: CodeBlockClosure.() -> Unit) {
    builder.beginControlFlow(controlFlow, *args)
    codeBlock(init)
    builder.endControlFlow()
  }

  fun nextControlFlow(controlFlow: String, vararg args: Any, init: CodeBlockClosure.() -> Unit) {
    builder.nextControlFlow(controlFlow, *args)
    codeBlock(init)
  }

  fun build() = builder.build()
}

fun unnamedParameter(type: TypeName, init: ParameterClosure.() -> Unit): ParameterSpec {
  return parameter("", type, init)
}

fun parameter(name: String, type: TypeName, init: ParameterClosure.() -> Unit): ParameterSpec {
  val block = ParameterClosure(name, type)
  block.init()
  return block.build()
}

@ParameterMarker
class ParameterClosure internal constructor(name: String, type: TypeName) {

  @DslMarker
  annotation class ParameterMarker

  private val builder = ParameterSpec.builder(name, type)
  private var defaultValue: CodeBlock?
    get() = builder.defaultValue
    set(value) {
      value?.let { builder.defaultValue(it) }
    }

  fun addKdoc(format: String, vararg args: String) {
    builder.addKdoc(format, *args)
  }

  fun addKdoc(init: CodeBlockClosure.() -> Unit) {
    val block = CodeBlockClosure()
    block.init()
    builder.addKdoc(block.build())
  }

  fun build() = builder.build()
}

fun codeBlock(init: CodeBlockClosure.() -> Unit): CodeBlock {
  val block = CodeBlockClosure()
  block.init()
  return block.build()
}

@CodeBlockMarker
class CodeBlockClosure {
  @DslMarker
  annotation class CodeBlockMarker

  private val builder = CodeBlock.builder()

  operator fun String.invoke(vararg args: Any) {
    builder.addStatement(this, *args)
  }

  class FormatBuilder {
    lateinit var format: String
    var args: Array<Any> = emptyArray()
  }

  fun addStatement(format: String, vararg args: String) {
    builder.addStatement(format, *args)
  }

  fun addStatement(init: FormatBuilder.() -> Unit) {
    val statementBuilder = FormatBuilder()
    statementBuilder.init()
    builder.addStatement(statementBuilder.format, *statementBuilder.args)
  }

  fun addCode(format: String, vararg args: String) {
    builder.add(format, *args)
  }

  fun addCode(init: FormatBuilder.() -> Unit) {
    val statementBuilder = FormatBuilder()
    statementBuilder.init()
    builder.add(statementBuilder.format, *statementBuilder.args)
  }

  fun addCodeBlock(init: CodeBlockClosure.() -> Unit) {
    val block = CodeBlockClosure()
    block.init()
    builder.add(block.build())
  }

  fun controlFlow(controlFlow: String, vararg args: Any, init: CodeBlockClosure.() -> Unit) {
    builder.beginControlFlow(controlFlow, *args)
    init()
    builder.endControlFlow()
  }

  fun nextControlFlow(controlFlow: String, vararg args: Any, init: CodeBlockClosure.() -> Unit) {
    builder.nextControlFlow(controlFlow, *args)
    init()
  }

  fun indent() {
    builder.indent()
  }

  fun unindent() {
    builder.unindent()
  }

  fun build() = builder.build()
}