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
    Assert.assertEquals("\"OR\"", QueryUtils.parseQueryValue("OR"));
    Assert.assertEquals("\"AND\"", QueryUtils.parseQueryValue("AND"));
    Assert.assertEquals("\"NOT\"", QueryUtils.parseQueryValue("NOT"));
  }

}
