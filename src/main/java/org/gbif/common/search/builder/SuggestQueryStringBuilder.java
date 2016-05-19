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
import org.gbif.common.search.util.SolrConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;

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

  private static final Pattern QUERY_PLACE_HOLDER_PTRN = Pattern.compile(QUERY_PLACE_HOLDER, Pattern.LITERAL);

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
    return buildStringQuery(QueryUtils.emptyToDefaultQuery(q));
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
    SuggestMapping annotation = annotatedClass.getAnnotation(SuggestMapping.class);
    String phraseQueryField = annotation.phraseQueryField();
    String field = annotation.field();
    String phraseQuery = PARAMS_JOINER.join(phraseQueryField, toPhraseQuery(QUERY_PLACE_HOLDER));
    phraseQueryTemplate = toBoostedQuery(phraseQuery, PHRASE_QUERY_BOOST, false);
    startPhraseQueryTemplate =
      toBoostedQuery(PARAMS_JOINER.join(phraseQueryField, QUERY_PLACE_HOLDER), PHRASE_QUERY_BOOST, false);
    queryTemplate = PARAMS_JOINER.join(field, QUERY_PLACE_HOLDER);
  }

  /**
   * Creates a query string that matches terms by to the partial phrase query fields.
   * The method takes a query q with the form: "pum conc" and produces a query with the form, for input puma con:
   * "(canonical_name_auto:\"puma con\"^1000 OR canonical_name_auto:puma^1000) OR
   * (canonical_name:puma^300 OR canonical_name:con^200)"
   *
   * @param q input query
   * @return a string containing a valid Solr query
   */
  private String buildStringQuery(String q) {
    String cleanQ = clearConsecutiveBlanks(q);
    String[] tokens = cleanQ.split("\\ ");
    Collection<String> partialQuery = new ArrayList<String>(tokens.length + 1);
    Collection<String> query = new ArrayList<String>(4);
    if (tokens.length > 1) {
      Integer partialBoost = PARTIAL_QUERY_BOOST;
      String[] phraseQuery = new String[2];
      phraseQuery[0] = replaceQueryPlaceHolder(phraseQueryTemplate, escapeQueryChars(cleanQ));
      phraseQuery[1] = replaceQueryPlaceHolder(startPhraseQueryTemplate, escapeQueryChars(tokens[0]));
      query.add(toParenthesesQuery(PARAMS_OR_JOINER.join(phraseQuery)));
      for (String token : tokens) {
        partialQuery.add(toBoostedQuery(replaceQueryPlaceHolder(queryTemplate, escapeQueryChars(token)),
            partialBoost, false));
        if (partialBoost > PARTIAL_BOOST_DECREMENT) {
          partialBoost -= PARTIAL_BOOST_DECREMENT;
        }
      }
    } else {
      if (SolrConstants.DEFAULT_FILTER_QUERY.equals(cleanQ)) {
        query.add(toParenthesesQuery(replaceQueryPlaceHolder(startPhraseQueryTemplate, cleanQ)));
        partialQuery.add(toBoostedQuery(replaceQueryPlaceHolder(queryTemplate, tokens[0]), PARTIAL_QUERY_BOOST, false));
      } else {
        query.add(toParenthesesQuery(replaceQueryPlaceHolder(startPhraseQueryTemplate, escapeQueryChars(cleanQ))));
        partialQuery.add(toBoostedQuery(replaceQueryPlaceHolder(queryTemplate, escapeQueryChars(tokens[0])),
                                        PARTIAL_QUERY_BOOST, false));
      }
    }
    query.add(toParenthesesQuery(PARAMS_OR_JOINER.join(partialQuery)));
    return PARAMS_OR_JOINER.join(query);
  }

  /**
   * Utility method that intends to reuse the compiled pattern for the query place holder.
   */
  private static String replaceQueryPlaceHolder(String matcher, String replacement) {
    return  QUERY_PLACE_HOLDER_PTRN.matcher(matcher).replaceAll(Matcher.quoteReplacement(replacement));
  }
}
