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
package org.gbif.common.search.es.indexing;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import lombok.experimental.UtilityClass;

/** Constants used for indexing into Elastisearch. */
@UtilityClass
public class IndexingConstants {


  /** Default/Recommended indexing settings. */
  public static final IndexSettings DEFAULT_INDEXING_SETTINGS = new IndexSettings.Builder()
                                                                  .settings(idx -> idx.refreshInterval(Time.of(t -> t.time("-1")))
                                                                    .numberOfReplicas("0")
                                                                    .numberOfShards("1")
                                                                    .translog(tl -> tl.durability("async")))
                                                                  .build();

  /** Default/recommended setting for search/production mode. */
  public static final IndexSettings DEFAULT_SEARCH_SETTINGS = new IndexSettings.Builder()
    .settings(idx -> idx.refreshInterval(Time.of(t -> t.time("1s")))
      .numberOfReplicas("1")
      .translog(tl -> tl.durability("async")))
    .build();

}
