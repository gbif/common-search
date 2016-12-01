package org.gbif.common.search.solr;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for class QueryUtils.
 */
public class QueryUtilsTest {


  /**
   * Tests that the utility class handles whitespace correctly.
   */
  @Test
  public void parseQueryValueBlanksTest() {
    String newQuery2His = QueryUtils.parseQueryValue("hi   hi");
    Assert.assertEquals(newQuery2His, QueryUtils.parseQueryValue("hi hi"));

    String newQuery3His = QueryUtils.parseQueryValue("hi   hi        hi");
    Assert.assertEquals(newQuery3His, QueryUtils.parseQueryValue("hi hi hi"));
  }

  /**
   * Asserts that Solr reserved words are escaped.
   */
  @Test
  public void parseSolrReservedWordsTest(){
    Assert.assertEquals(QueryUtils.parseQueryValue("OR"),"\"OR\"");
    Assert.assertEquals(QueryUtils.parseQueryValue("AND"),"\"AND\"");
    Assert.assertEquals(QueryUtils.parseQueryValue("NOT"),"\"NOT\"");
  }

}
