package org.gbif.common.search.test;

import org.gbif.api.vocabulary.Country;
import org.gbif.common.search.es.EsConversionUtils;

import java.util.function.Function;

import co.elastic.clients.elasticsearch.core.search.Hit;

public class DataSearchResultConverter  implements Function<Hit<Data>,Data> {


  @Override
  public Data apply(Hit<Data> searchHit) {
    Data.DataBuilder build = Data.builder();
    EsConversionUtils.getIntValue(searchHit, "key").ifPresent(build::key);
    EsConversionUtils.getStringValue(searchHit, "title").ifPresent(build::title);
    EsConversionUtils.getDateValue(searchHit, "created").ifPresent(build::created);
    EsConversionUtils.getEnumValue(Country.class, searchHit, "country").ifPresent(build::country);
    return build.build();
  }
}
