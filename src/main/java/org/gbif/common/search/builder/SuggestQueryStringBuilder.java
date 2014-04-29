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

import org.gbif.common.search.model.SuggestMapping;
import org.gbif.common.search.util.QueryUtils;

import java.util.ArrayList;
import java.util.List;

import static org.gbif.common.search.util.QueryUtils.PARAMS_JOINER;
import static org.gbif.common.search.util.QueryUtils.PARAMS_OR_JOINER;
import static org.gbif.common.search.util.QueryUtils.clearConsecutiveBlanks;
import static org.gbif.common.search.util.QueryUtils.toBoostedQuery;
import static org.gbif.common.search.util.QueryUtils.toParenthesesQuery;
import static org.gbif.common.search.util.QueryUtils.toPhraseQuery;

/**
 * Builder class that helps in the creation process of query patterns for classes annotated with
 * {@link org.gbif.common.search.model.SuggestMapping}.
 */
public class SuggestQueryStringBuilder extends QueryStringBuilderBase {

  private static final Integer PHRASE_QUERY_BOOST = 1000;

  private static final Integer PARTIAL_QUERY_BOOST = 300;

  private static final Integer PARTIAL_BOOST_DECREMENT = 100;

  private String startPhraseQueryTemplate;

  /**
   * Private default constructor.
   */
  private SuggestQueryStringBuilder(Class<?> annotatedClass) {
    super(annotatedClass);
  }

  public static SuggestQueryStringBuilder create(Class<?> annotatedClass) {
    return new SuggestQueryStringBuilder(annotatedClass);
  }


  /*
   * (non-Javadoc)
   * @see org.gbif.common.search.builder.QueryStringBuilderBase#build(java.lang.String)
   */
  @Override
  public String build(String q) {
    final String qValue = QueryUtils.emptyToDefaultQuery(q);
    return buildStringQuery(qValue);
  }

  /**
   * Helper method that sets the parameters for full text search param "q".
   * It will also add the optional query function.
   */
  @Override
  protected void initTemplates(Class<?> annotatedClass) {
    if (!annotatedClass.isAnnotationPresent(SuggestMapping.class)) {
      throw new IllegalStateException(
        "Given class " + annotatedClass.getCanonicalName() + " is missing the SuggestField annotation");
    }
    final SuggestMapping annotation = annotatedClass.getAnnotation(SuggestMapping.class);
    final String phraseQueryField = annotation.phraseQueryField();
    final String field = annotation.field();
    final String phraseQuery = PARAMS_JOINER.join(phraseQueryField, toPhraseQuery(QUERY_PLACE_HOLDER));
    phraseQueryTemplate = toBoostedQuery(phraseQuery, PHRASE_QUERY_BOOST, false);
    startPhraseQueryTemplate =
      toBoostedQuery(PARAMS_JOINER.join(phraseQueryField, QUERY_PLACE_HOLDER), PHRASE_QUERY_BOOST, false);
    queryTemplate = PARAMS_JOINER.join(field, QUERY_PLACE_HOLDER);
  }

  /**
   * Creates a query string that matches terms by to the partial phrase query fields.
   * The method takes a query q with the form: "pum conc" and produces a query with the form, for input puma con:
   * "(canonical_name_auto:\"puma con\"^1000 OR canonical_name_auto:puma^1000) OR (canonical_name:puma^300 OR canonical_name:con^200)"
   * 
   * @param q input query
   * @return a string containing a valid Solr query
   */
  private String buildStringQuery(String q) {
    String cleanQ = clearConsecutiveBlanks(q);
    String[] tokens = cleanQ.split("\\ ");
    List<String> query = new ArrayList<String>();
    String[] phraseQuery = new String[2];
    List<String> partialQuery = new ArrayList<String>();
    Integer partialBoost = PARTIAL_QUERY_BOOST;
    if (tokens.length > 1) {
      phraseQuery[0] = phraseQueryTemplate.replace(QUERY_PLACE_HOLDER, cleanQ);
      phraseQuery[1] = startPhraseQueryTemplate.replace(QUERY_PLACE_HOLDER, tokens[0]);
      query.add(toParenthesesQuery(PARAMS_OR_JOINER.join(phraseQuery)));
      for (String token : tokens) {
        partialQuery.add(toBoostedQuery(queryTemplate.replace(QUERY_PLACE_HOLDER, token), partialBoost, false));
        if (partialBoost > PARTIAL_BOOST_DECREMENT) {
          partialBoost -= PARTIAL_BOOST_DECREMENT;
        }
      }
    } else {
      phraseQuery[0] = startPhraseQueryTemplate.replace(QUERY_PLACE_HOLDER, cleanQ);
      query.add(toParenthesesQuery(PARAMS_OR_JOINER.join(phraseQuery)));
      partialQuery.add(toBoostedQuery(queryTemplate.replace(QUERY_PLACE_HOLDER, tokens[0]), partialBoost, false));
    }
    query.add(toParenthesesQuery(PARAMS_OR_JOINER.join(partialQuery)));
    return PARAMS_OR_JOINER.join(query);
  }
}
