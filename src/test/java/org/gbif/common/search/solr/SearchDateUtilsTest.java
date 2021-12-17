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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Test cases for SearchDateUtils.*/
public class SearchDateUtilsTest {


  /** Tests for common date parsings.*/
  @Test
  public void testSingleDateParsing() {

    //Querying for October is transformed into a range query for the entire month
    assertEquals("[2010-10-01 TO 2010-10-31]", SearchDateUtils.toDateQuery("2010-10"));

    //Querying for 2010 is transformed into a range query for the entire year
    assertEquals("[2010-01-01 TO 2010-12-31]", SearchDateUtils.toDateQuery("2010"));

    //Querying for a specific date produces a query string for that day
    assertEquals("\"2010-10-01\"", SearchDateUtils.toDateQuery("2010-10-01"));
  }

  /** Tries to parse a wrong date.*/
  @Test(expected = IllegalArgumentException.class)
  public void testParsingError() {
    SearchDateUtils.toDateQuery("10-10");
  }
}
