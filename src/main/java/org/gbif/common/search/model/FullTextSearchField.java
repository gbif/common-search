/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.common.search.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates setter methods of fields that can be used as part of the full text search.
 * This annotation should be consumed by SearchService implementations.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FullTextSearchField {

  /**
   * Name of the field used for exact matching of tokens, the absence of this field implies that the default field is
   * used.
   */
  String exactMatchField() default "";

  /**
   * Value used for scoring exact matching.
   */
  double exactMatchScore() default 1.0d;

  /**
   * Default field name for full text search potentially doing a wildcard partial match.
   */
  String field();

  /**
   * Name of the highlighted field, is specified this field is used instead of the field attribute.
   */
  String highlightField() default "";

  /**
   * WildcardPadding pattern for the full text field.
   */
  WildcardPadding partialMatching() default WildcardPadding.NONE;

  /**
   * Value for scoring partial matching.
   * This field is set regardless of the "partialMatching" values: BOTH, LEFT and RIGHT.
   */
  double partialMatchScore() default 0.5d;

}
