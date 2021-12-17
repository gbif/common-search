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
package org.gbif.common.search.solr;

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

/**
 * Class with constants related to search/solr operations.
 */
@UtilityClass
public class SolrConstants {

  // Static Solr related strings used multiple times
  public static final String DEFAULT_QUERY = "*:*";
  public static final String DEFAULT_FILTER_QUERY = "*";
  public static final String RANGE_FORMAT = "[%s TO %s]";
  // Matches all documents with a value in field
  public static final String RANGE_ALL = "[* TO *]";
  public static final String PARENTHESES_EXP_FORMAT = "(%s)";
  public static final String PARAM_VALUE_SEP = ":";
  public static final String SCORE_OP = "^";
  public static final String PARAM_OR_OP = " OR ";
  public static final String PARAM_AND_OP = " AND ";
  public static final String HTTP_AND_OP = "&";
  public static final String NOT_OP = "-";
  public static final String SOLR_NOT_OP = "NOT";
  public static final String APOSTROPHE = "\"";
  public static final String BLANK = " ";
  public static final String ALT_QUERY_PARAM = "q.alt";
  public static final String LANG_SEPARATOR = "|";
  public static final String FUZZY_OPERATOR = "~";
  public static final String HL_PRE = "<em class=\"gbifHl\">";
  public static final String HL_POST = "</em>";
  public static final Pattern HL_REGEX = Pattern.compile(".*" + HL_PRE + ".*?" + HL_POST + ".*");
  public static final Pattern HL_PRE_REGEX = Pattern.compile("<em class=\"gbifHl\">");
  public static final String GEO_INTERSECTS_QUERY_FMT = "\"Intersects(%s) distErrPct=0\"";

  // Tag expressions used to name a local parameter that contains the facet filters
  public static final String TAG_FIELD_PARAM = "$field";
  public static final String FACET_FILTER_TAG = "{!tag=ffq" + TAG_FIELD_PARAM + "}";
  public static final String FACET_FILTER_EX = "{!ex=ffq" + TAG_FIELD_PARAM + "}" + TAG_FIELD_PARAM;
  // Expression used to validate if a facet name is an exclusion(tagged) expression
  public static final Pattern FACET_FILTER_RGEX = Pattern.compile("\\{!ex=ffq.*\\}.*");
  // This constant is used to extract the field name of facet exclusion filter
  public static final Pattern FACET_FILTER_RGEX_CLEAN = Pattern.compile("\\{!ex=ffq.*\\}");


  /**
   * This is the a system property expected by the Solr server when an embedded instance is created.
   */
  public static final String SOLR_HOME = "solr.solr.home";

  // Keys coming from the configuration (properties) file or from injected parameters.
  public static final String SOLR_SERVER_KEY = "solr.server";
  public static final String SOLR_EMBEDDED_KEY = "solr.embedded";
  public static final String SOLR_REQUEST_HANDLER = "solr.request_handler";

  // Default page size for paginated results
  public static final int DEFAULT_PAGE_SIZE = 20;

  // Default maximum # of results per page, any value greater that this will be override with this value.
  public static final int MAX_PAGE_SIZE = 1000;

  // Maximum number of snippets to retrieve in a highlight response.
  public static final int NUM_HL_SNIPPETS = 10;

  // The size, in characters, of the snippets, 0 indicates that the whole field value should be used.
  public static final int HL_FRAGMENT_SIZE = 0;

  /**
   * Query string parameter.
   * Repeated in WebserviceParameter, couldn't resolve dependencies.
   */
  public static final String PARAM_QUERY_STRING = "q";

  /**
   * Facet missing parameter.
   */
  public static final String PARAM_FACET_MISSING = "facet.missing";

  /**
   * Facet sorting parameter.
   */
  public static final String PARAM_FACET_SORT = "facet.sort";

  public static final String TERMS_COMP_PATH = "/terms";

}
