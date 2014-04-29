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


/**
 * Annotation that defines a field mapping between a java or generic field and a schema field.
 * The field corresponds to the name of it in the index data store.
 * The property is the actual name of java field and the "field" is the name in the schema file.
 * The purpose of this annotation is decoupling property names used in a java application and its
 * correspondent name in the underlying schema.
 */
public @interface Field {

  /**
   * Field name in the schema.
   * 
   * @return name of the field
   */
  String field();


  /**
   * Java/Generic property name.
   * 
   * @return name of the property
   */
  String property();
}
