/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * Annotation that defines a faceted field.
 * The field corresponds to the name of it in the index data store or in the model object.
 * The name is the actual name of the facet, this field is used to naming the facet in consistent way without
 * dependencies of the field name. The intended use of name is that it should contain values coming from literal names
 * of a enumerated type (e.g: Enum.name()).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FacetField {

  /**
   * Valid sort orders.
   */
  enum SortOrder {
    /**
     * Sort the constraints by count (highest count first).
     */
    COUNT,
    /**
     * Sorted in index order (lexicographic by indexed term).
     * For terms in the ascii range, this will be alphabetically sorted.
     */
    INDEX
  }

  /**
   * @return the field name
   */
  String field();

  /**
   * Indicates if count of all matching results which have no value for the field should be included.
   *
   * @return flag that indicates missing facet is included
   */
  boolean missing() default false;

  /**
   * @return the name of the facet exactly as SearchParameter enum specifies
   */
  String name();

  /**
   * @return the sort order for this facet field
   */
  SortOrder sort() default SortOrder.COUNT;
}
