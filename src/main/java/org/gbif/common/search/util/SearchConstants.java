package org.gbif.common.search.util;

/**
 * Class with constants related to search operations. For Solr specific constants see {@link SolrConstants}.
 */
public class SearchConstants {

  public static final String NOT_OP = "!";

  private SearchConstants() {
    throw new UnsupportedOperationException("Can't initialize utils class");
  }
}
