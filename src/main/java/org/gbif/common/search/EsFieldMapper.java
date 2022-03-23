/*
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
package org.gbif.common.search;

import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.util.VocabularyUtils;

import java.util.Collections;
import java.util.List;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;

public interface EsFieldMapper<P extends SearchParameter> {

  /**
   * Looks-up the Elasticsearch field name linked to a search parameter.
   *
   * @param searchParameter to lookup-up
   * @return the associated Elasticsearch field or null otherwise
   */
  String get(P searchParameter);

  /**
   * Looks-up the {@link SearchParameter} linked to a ElasticSearch field.
   *
   * @param esFieldName to look-up
   * @return the search parameter associated to the field
   */
  P get(String esFieldName);

  /**
   * Parses an indexed value into a value expected in the response.
   * @param value to parse
   * @param parameter search parameter
   * @return parsed value
   */
  default String parseIndexedValue(String value, P parameter) {
    return value;
  }

  /**
   * Parses the value of a search parameter to the expected value in the search index.
   * @param value to parse
   * @param parameter search parameter
   * @return parsed value
   */
  default FieldValue parseParamValue(String value, P parameter) {
    if (Enum.class.isAssignableFrom(parameter.type())) {
      return VocabularyUtils.lookup(value, (Class<Enum<?>>) parameter.type())
        .map(e -> FieldValue.of(e.name()))
        .orElse(null);
    }
    if (Boolean.class.isAssignableFrom(parameter.type())) {
      return FieldValue.of(value.toLowerCase());
    }
    if (Integer.class.isAssignableFrom(parameter.type())) {
      return FieldValue.of(Integer.parseInt(value));
    }
    if (Long.class.isAssignableFrom(parameter.type())) {
      return FieldValue.of(Long.parseLong(value));
    }
    if (Double.class.isAssignableFrom(parameter.type())) {
      return FieldValue.of(Double.parseDouble(value));
    }
    if (Float.class.isAssignableFrom(parameter.type())) {
      return FieldValue.of(Float.parseFloat(value));
    }
    return FieldValue.of(value);
  }

  /**
   * Looks-up for the estimate cardinality of ElasticSearch field.
   *
   * @param esFieldName to look-up
   * @return the estimated cardinality
   */
  Integer getCardinality(String esFieldName);

  /**
   * Checks if a ElasticSearch fields is mapped to date data type.
   *
   * @param esFieldName to look-up
   * @return true of the field is date type field, false otherwise
   */
  boolean isDateField(String esFieldName);

  /** @return a list of fields to be excluded in the _source field. */
  List<String> excludeFields();

  /** @return the default sorting of results */
  List<SortOptions> sorts();

  /**
   * Fields to be included in a suggest response. By default only the requested parameter field is
   * returned.
   */
  default List<String> includeSuggestFields(P searchParameter) {
    return Collections.singletonList(get(searchParameter));
  }

  /**
   * Gets the autocomplete field associated to a parameter. By default returns the mapped es field +
   * the "Autocomplete" word.
   */
  default String getAutocompleteField(P searchParameter) {
    return get(searchParameter) + "Autocomplete";
  }

  /** Fields used during to highlight in results. */
  default List<String> highlightingFields() {
    return Collections.emptyList();
  }

  /**
   * List of all ES fields mapped to API responses. Only these fields will be included a in _source
   * field. An empty array means, all fields are mapped and must be included in the _source field.
   *
   * @return
   */
  default List<String> getMappedFields() {
    return Collections.emptyList();
  }


  /** Builds a full text search query builder. */
  default Query fullTextQuery(String q) {
    return Query.of(b -> b.match(QueryBuilders.match().field("all").query(q).build()));
  }

  default boolean isSpatialParameter(P parameter) {
    return false;
  }
}
