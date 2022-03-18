package org.gbif.common.search.test;

import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.vocabulary.Country;

import java.util.Date;

public enum DataSearchParameter implements SearchParameter {

  KEY(Integer.class),
  CREATED(Date.class),
  TITLE(String.class),
  COUNTRY(Country.class);

  private final Class<?> type;

  DataSearchParameter(Class<?> type) {
    this.type = type;
  }

  @Override
  public Class<?> type() {
    return type;
  }
}
