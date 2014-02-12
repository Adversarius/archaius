/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config.scala

/**
 * Utilities for assembling the chains of dynamic properties underlying a [[com.netflix.config.scala.ChainedProperty]].
 */
protected[scala] object ChainMakers {

  /**
   * Assemble a set of property names from
   *
   *  - an optional prefix;
   *  - a name which may be a dot-separated hierarchy of names;
   *  - an optional suffix.
   *
   * The set of names is derived from the hierarchy of the central name.  For example, for prefix x, suffix y, and name a.b.c,
   * the set is
   *
   *  - x.y
   *  - x.a.y
   *  - x.a.b.y
   *  - x.a.b.c.y
   *
   * @param basePropertyPrefix the beginning of the property names.
   * @param name the (optionally dot-separated) name which is processed to create the set of property names.
   * @param basePropertySuffix the end of the property names.
   * @return the set of property names.
   */
  def fanPropertyName(basePropertyPrefix: Option[String], name: String, basePropertySuffix: Option[String]): Iterable[String] = {
    val segments = name.split("\\.")
    (0 to segments.length).map { n =>
      (Seq(basePropertyPrefix).flatten ++ segments.take(n) ++ Seq(basePropertySuffix).flatten).mkString(".")
    }
  }

  /**
   * Create the [[com.netflix.config.ChainedDynamicProperty.ChainLink]]s which underpin a [[com.netflix.config.scala.ChainedProperty]], properly related.
   *
   * @param propertyNames the set of property names for which to create the chain.  The second-most general property is the
   *                      first; the most specific property is last.
   * @param root          the PropertyWrapper for the most general property.
   * @param rootWrap      a function to create the [[com.netflix.config.ChainedDynamicProperty.ChainLink]] which contains the root [[com.netflix.config.PropertyWrapper]].
   * @param linkWrap      a function to create all [[com.netflix.config.ChainedDynamicProperty.ChainLink]]s which contain another [[com.netflix.config.ChainedDynamicProperty.ChainLink]].
   * @tparam T            the type of the value which the chain returns.
   * @tparam PWT          the type of the root PropertyWrapper of the chain.
   * @tparam CLT          the type of all [[com.netflix.config.ChainedDynamicProperty.ChainLink]]s in the chain.
   * @return              a [[com.netflix.config.ChainedDynamicProperty.ChainLink]] which represents the most specific property, containing all other [[com.netflix.config.ChainedDynamicProperty.ChainLink]]s.
   */
  def deriveChain[T, PWT, CLT](
    propertyNames: => Iterable[String],
    root: PWT,
    rootWrap: (String, PWT) => CLT,
    linkWrap: (String, CLT) => CLT): CLT =
  {
    val derivatives = propertyNames.foldLeft[Option[CLT]]( None ) { (chain, propName) =>
      val derivative = chain match {
        case Some(previous) => linkWrap(propName, previous)
        case None           => rootWrap(propName, root)
      }
      Some( derivative )
    }
    derivatives.get
  }
}