package org.gbif.common.search.model.configuration;

import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.common.search.model.FacetField;

/**
 * Facets configuration class.
 * This class encapsulates the information to build faceted queries of SearchParameter.
 */
public class FacetFieldConfiguration {

  private String field;
  private SearchParameter searchParameter;
  private FacetField.Method method = FacetField.Method.FIELD_CACHE;
  private FacetField.SortOrder sortOrder = FacetField.SortOrder.COUNT;
  private boolean missing;

  /**
   * Default constructor.
   */
  public FacetFieldConfiguration() {

  }
  /**
   * Full constructor.
   */
  public FacetFieldConfiguration(
    String field,
    SearchParameter searchParameter,
    FacetField.Method method,
    FacetField.SortOrder sortOrder,
    boolean missing
  ) {
    this.field = field;
    this.searchParameter = searchParameter;
    this.method = method;
    this.sortOrder = sortOrder;
    this.missing = missing;
  }

  /**
   * Solr/Index field name.
   */
  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  /**
   * Occurrence search parameter.
   */
  public SearchParameter getSearchParameter() {
    return searchParameter;
  }

  public void setSearchParameter(SearchParameter searchParameter) {
    this.searchParameter = searchParameter;
  }

  /**
   * Solr facet method to use.
   */
  public FacetField.Method getMethod() {
    return method;
  }

  public void setMethod(FacetField.Method method) {
    this.method = method;
  }

  /**
   * Solr facet order.
   */
  public FacetField.SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(FacetField.SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  /**
   * Flag to show/hide counts for records missing values for this field.
   */
  public boolean isMissing() {
    return missing;
  }

  public void setMissing(boolean missing) {
    this.missing = missing;
  }
}
