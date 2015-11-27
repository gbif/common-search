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
package org.gbif.common.search.builder;

import org.gbif.common.search.model.FullTextSearchField;
import org.gbif.common.search.model.WildcardPadding;

import java.util.List;

import com.google.common.collect.Lists;


import static org.gbif.common.search.util.AnnotationUtils.initFullTextFieldsPropertiesMap;
import static org.gbif.common.search.util.QueryUtils.PARAMS_JOINER;
import static org.gbif.common.search.util.QueryUtils.getExactMatchField;
import static org.gbif.common.search.util.QueryUtils.getMatchPatternAndScore;
import static org.gbif.common.search.util.SolrConstants.SCORE_OP;


/**
 * Builder class that helps in the creation process of query patterns for classes annotated with
 * {@link FullTextSearchField}.
 */
public class FullTextQueryStringBuilder extends QueryStringBuilderBase {

  /**
   * Private default constructor.
   */
  private FullTextQueryStringBuilder(Class<?> annotatedClass) {
    super(annotatedClass);
  }

  public static FullTextQueryStringBuilder create(Class<?> annotatedClass) {
    return new FullTextQueryStringBuilder(annotatedClass);
  }


  /**
   * Helper method that sets the parameters for full text search param "q".
   * It will also add the optional query function.
   */
  @Override
  protected void initTemplates(Class<?> annotatedClass) {
    List<String> fullTextQueryComponents = Lists.newArrayList();
    List<String> phraseQueryComponents = Lists.newArrayList();

    List<FullTextSearchField> fullTextSearchFields = initFullTextFieldsPropertiesMap(annotatedClass);
    if (!fullTextSearchFields.isEmpty()) {
      for (FullTextSearchField field : fullTextSearchFields) {
        String exactMatchField = getExactMatchField(field);
        String exactMatchSubQuery =
            PARAMS_JOINER.join(exactMatchField, QUERY_PLACE_HOLDER + SCORE_OP + field.exactMatchScore());
        fullTextQueryComponents.add(exactMatchSubQuery);
        phraseQueryComponents.add(exactMatchSubQuery);
        setWildcardFilters(fullTextQueryComponents, phraseQueryComponents, field, exactMatchField);
        highlightedFields.add(field.highlightField().isEmpty() ? field.field() : field.highlightField());
      }
      queryTemplate = buildQueryExpression(fullTextQueryComponents);
      phraseQueryTemplate = buildQueryExpression(phraseQueryComponents);
    }
  }


  /**
   * Sets the filters for wildcard (if applicable) queries.
   */
  private static void setWildcardFilters(List<String> fullTextQueryComponents, List<String> phraseQueryComponents,
                                  FullTextSearchField field, String exactMatchField) {
    if (field.partialMatching() != WildcardPadding.NONE) {
      fullTextQueryComponents
        .add(PARAMS_JOINER.join(field.field(), getMatchPatternAndScore(field, QUERY_PLACE_HOLDER)));
      if (!exactMatchField.equals(field.field())) {
        phraseQueryComponents
          .add(PARAMS_JOINER.join(field.field(), QUERY_PLACE_HOLDER + SCORE_OP + field.partialMatchScore()));
      }
    }
  }
}
