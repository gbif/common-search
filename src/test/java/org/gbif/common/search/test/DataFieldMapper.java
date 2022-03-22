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
package org.gbif.common.search.test;

import org.gbif.common.search.EsFieldMapper;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableBiMap;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;

public class DataFieldMapper implements EsFieldMapper<DataSearchParameter> {

  private static ImmutableBiMap<DataSearchParameter,String> PARAM_FIELD_MAPPING = ImmutableBiMap.<DataSearchParameter,String>builder()
                                                                                        .put(DataSearchParameter.KEY, "key")
                                                                                        .put(DataSearchParameter.CREATED, "created")
                                                                                        .put(DataSearchParameter.TITLE, "title")
                                                                                        .put(DataSearchParameter.COUNTRY, "country")
                                                                                        .build();

  @Override
  public String get(DataSearchParameter searchParameter) {
    return PARAM_FIELD_MAPPING.get(searchParameter);
  }

  @Override
  public DataSearchParameter get(String esFieldName) {
    return PARAM_FIELD_MAPPING.inverse().get(esFieldName);
  }

  @Override
  public Integer getCardinality(String esFieldName) {
    return null;
  }

  @Override
  public boolean isDateField(String esFieldName) {
    return Date.class.isAssignableFrom(get(esFieldName).type());
  }

  @Override
  public List<String> excludeFields() {
    return Collections.emptyList();
  }

  @Override
  public List<SortOptions> sorts() {
    return Collections.singletonList(SortOptions.of(so -> so.field(fs -> fs.field("key").order(SortOrder.Desc))));
  }
}
