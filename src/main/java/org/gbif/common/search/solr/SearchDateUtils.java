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

import org.gbif.api.util.IsoDateParsingUtils;
import org.gbif.api.util.IsoDateParsingUtils.IsoDateFormat;
import org.gbif.api.util.Range;
import org.gbif.api.util.SearchTypeValidator;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import lombok.experimental.UtilityClass;

import static org.gbif.api.util.IsoDateParsingUtils.getFirstDateFormatMatch;
import static org.gbif.api.util.IsoDateParsingUtils.parseDateRange;
import static org.gbif.common.search.solr.QueryUtils.toDateQueryFormat;
import static org.gbif.common.search.solr.QueryUtils.toPhraseQuery;
import static org.gbif.common.search.solr.SolrConstants.DEFAULT_FILTER_QUERY;
import static org.gbif.common.search.solr.SolrConstants.RANGE_FORMAT;

/**
 * Utility class that contains functions for building occurrence date queries.
 */
@UtilityClass
public class SearchDateUtils {

  /**
   * Converts the value parameter into a date range query.
   */
  public static String toDateQuery(String value) {
    return SearchTypeValidator.isRange(value) ? toRangeDateQueryFormat(value): toSingleDateQueryFormat(value);
  }

  private static LocalDate toLastDayOfMonth(LocalDate date) {
    return date.with(TemporalAdjusters.lastDayOfMonth());
  }

  private static LocalDate toLastDayOfYear(LocalDate date) {
    return date.with(TemporalAdjusters.lastDayOfYear());
  }

  private static String toRangeDateQueryFormat(String value) {
    final Range<LocalDate> dateRange = parseDateRange(value);
    if (!dateRange.hasLowerBound() && !dateRange.hasUpperBound()) {
      return DEFAULT_FILTER_QUERY;
    } else if (dateRange.hasLowerBound() && !dateRange.hasUpperBound()) {
      return String.format(RANGE_FORMAT, toDateQueryFormat(dateRange.lowerEndpoint()), DEFAULT_FILTER_QUERY);
    } else if (!dateRange.hasLowerBound() && dateRange.hasUpperBound()) {
      return String.format(RANGE_FORMAT, DEFAULT_FILTER_QUERY, toDateQueryFormat(dateRange.upperEndpoint()));
    } else {
      return String.format(RANGE_FORMAT, toDateQueryFormat(dateRange.lowerEndpoint()),
                           toDateQueryFormat(dateRange.upperEndpoint()));
    }
  }

  /**
   * Transforms a single ISO date value into a solr query being a range query depending on the precision of the
   * ISO date. For example a single date 1989-09 results in a solr range for the entire month.
   */
  private static String toSingleDateQueryFormat(String value) {
    IsoDateFormat occDateFormat = getFirstDateFormatMatch(value);
    LocalDate lowerDate = IsoDateParsingUtils.parseDate(value);
    String lowerDateStr = toDateQueryFormat(lowerDate);
    if (occDateFormat == IsoDateFormat.YEAR_MONTH) {
      // Generated query: [yyyy-MM-01T00:00:00Z TO yyyy-'MM-LAST_DATE_OF_THE_MONTH'T00:00:00Z]
      return String.format(RANGE_FORMAT, lowerDateStr, toDateQueryFormat(toLastDayOfMonth(lowerDate)));
    } else if (occDateFormat == IsoDateFormat.YEAR) {
      // Generated query: [yyyy-01-01T00:00:00Z TO yyyy-'LAST_DATE_OF_THE_YEAR'T00:00:00Z]
      return String.format(RANGE_FORMAT, lowerDateStr, toDateQueryFormat(toLastDayOfYear(lowerDate)));
    } else {
      return toPhraseQuery(lowerDateStr);
    }
  }
}
