package org.gbif.common.search.util;

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

  @Test
  public void parseApostrophiedQuery(){
    String newPhraseQuery3His = QueryUtils.parseQueryValue("\"hi   hi\"");
    Assert.assertEquals(newPhraseQuery3His, QueryUtils.parseQueryValue("\"hi hi\""));
  }

}
