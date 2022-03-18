package org.gbif.common;

import org.gbif.common.search.EsResponseParser;
import org.gbif.common.search.test.Data;
import org.gbif.common.search.test.DataFieldMapper;
import org.gbif.common.search.test.DataSearchParameter;
import org.gbif.common.search.test.DataSearchResultConverter;

import org.junit.jupiter.api.Test;

public class EsResponseParserTest {

  private final EsResponseParser<Data, Data, DataSearchParameter> responseParser = new EsResponseParser<>(new DataSearchResultConverter(), new DataFieldMapper());

  @Test
  public void buildTest() {

  }
}
