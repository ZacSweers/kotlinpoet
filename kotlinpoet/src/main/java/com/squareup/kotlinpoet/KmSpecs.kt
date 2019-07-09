/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.km.ImmutableKmProperty
import com.squareup.kotlinpoet.km.KotlinPoetKm
import com.squareup.kotlinpoet.km.PropertyAccessorFlag
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_EXTERNAL
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_INLINE
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_NOT_DEFAULT
import com.squareup.kotlinpoet.km.hasGetter
import com.squareup.kotlinpoet.km.hasSetter
import com.squareup.kotlinpoet.km.isAbstract
import com.squareup.kotlinpoet.km.isConst
import com.squareup.kotlinpoet.km.isDelegated
import com.squareup.kotlinpoet.km.isExpect
import com.squareup.kotlinpoet.km.isExternal
import com.squareup.kotlinpoet.km.isFinal
import com.squareup.kotlinpoet.km.isInternal
import com.squareup.kotlinpoet.km.isLateinit
import com.squareup.kotlinpoet.km.isOpen
import com.squareup.kotlinpoet.km.isOverride
import com.squareup.kotlinpoet.km.isOverrideProperty
import com.squareup.kotlinpoet.km.isPrivate
import com.squareup.kotlinpoet.km.isProtected
import com.squareup.kotlinpoet.km.isPublic
import com.squareup.kotlinpoet.km.isSealed
import com.squareup.kotlinpoet.km.isSynthesized
import com.squareup.kotlinpoet.km.isVal
import com.squareup.kotlinpoet.km.isVar
import com.squareup.kotlinpoet.km.propertyAccessorFlags
import kotlinx.metadata.Flags

@KotlinPoetKm
private fun ImmutableKmProperty.toPropertySpec(
    typeParamResolver: ((index: Int) -> TypeName)
) = PropertySpec.builder(name, returnType.toTypeName(typeParamResolver))
    .apply {
      addModifiers(flags.visibility)
      addModifiers(*flags.modalities
          .filterNot { it == KModifier.FINAL && !isOverride }
          .toTypedArray())
      if (isOverride) {
        addModifiers(KModifier.OVERRIDE)
      }
      if (isConst) {
        addModifiers(KModifier.CONST)
      }
      if (isVar) {
        mutable(true)
      } else if (isVal) {
        mutable(false)
      }
      if (isDelegated) {
        delegate("") // Placeholder
      }
      if (isExpect) {
        addModifiers(KModifier.EXPECT)
      }
      if (isExternal) {
        addModifiers(KModifier.EXTERNAL)
      }
      if (isLateinit) {
        addModifiers(KModifier.LATEINIT)
      }
      if (isSynthesized) {
        addAnnotation(JvmSynthetic::class)
      }
      if (hasGetter) {
        val visibility = setterFlags.visibility
        val modalities = setterFlags.modalities
            .filterNot { it == KModifier.FINAL && !flags.isOverrideProperty }
        val propertyAccessorFlags = setterFlags.propertyAccessorFlags
        if (visibility != KModifier.PUBLIC || modalities.isNotEmpty() || propertyAccessorFlags.isNotEmpty()) {
          getter(FunSpec.setterBuilder()
              .apply {
                addModifiers(visibility)
                addModifiers(*modalities.toTypedArray())
                addModifiers(*propertyAccessorFlags.toKModifiersArray())
              }
              .build())
        }
      }
      if (hasSetter) {
        val visibility = setterFlags.visibility
        val modalities = setterFlags.modalities
            .filterNot { it == KModifier.FINAL && !flags.isOverrideProperty }
        val propertyAccessorFlags = setterFlags.propertyAccessorFlags
        if (visibility != KModifier.PUBLIC || modalities.isNotEmpty() || propertyAccessorFlags.isNotEmpty()) {
          setter(FunSpec.setterBuilder()
              .apply {
                addModifiers(visibility)
                addModifiers(*modalities.toTypedArray())
                addModifiers(*propertyAccessorFlags.toKModifiersArray())
              }
              .build())
        }
      }
      // TODO Available in tags
      //hasConstant
      //isDeclaration
      //isDelegation
    }
    .tag(this)
    .build()

private fun Set<PropertyAccessorFlag>.toKModifiersArray(): Array<KModifier> {
  return map {
    when (it) {
      IS_EXTERNAL -> KModifier.EXTERNAL
      IS_INLINE -> KModifier.INLINE
      IS_NOT_DEFAULT -> TODO("Wat")
    }
  }.toTypedArray()
}

@KotlinPoetKm
private val Flags.visibility: KModifier
  get() = when {
    isInternal -> KModifier.INTERNAL
    isPrivate -> KModifier.PRIVATE
    isProtected -> KModifier.PROTECTED
    isPublic -> KModifier.PUBLIC
    else -> TODO("Flag not supported!") // IS_PRIVATE_TO_THIS or IS_LOCAL
  }

@KotlinPoetKm
private val Flags.modalities: Set<KModifier>
  get() = setOf {
    if (isFinal) {
      add(KModifier.FINAL)
    }
    if (isOpen) {
      add(KModifier.OPEN)
    }
    if (isAbstract) {
      add(KModifier.ABSTRACT)
    }
    if (isSealed) {
      add(KModifier.SEALED)
    }
  }

private inline fun <E> setOf(body: MutableSet<E>.() -> Unit): Set<E> {
  return mutableSetOf<E>().apply(body).toSet()
}
