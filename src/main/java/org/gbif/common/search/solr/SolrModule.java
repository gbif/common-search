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

import org.apache.solr.client.solrj.SolrClient;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;


/**
 * Trivial guice module providing a remote or embedded Solr server based on the modules configuration.
 * The only class exposed is SolrClient, optionally bound to a named key.
 *
 */
public class SolrModule extends AbstractModule {

  private final Key<SolrClient> bindingKey;
  private final SolrConfig cfg;

  public SolrModule(SolrConfig cfg) {
    this(cfg, null);
  }

  public SolrModule(SolrConfig cfg, String solrBindingName) {
    this.cfg = cfg;
    if (solrBindingName != null) {
      bindingKey = Key.get(SolrClient.class, Names.named(solrBindingName));
    } else {
      bindingKey = null;
    }
  }

  public Key<SolrClient> bindingKey() {
    return bindingKey;
  }

  @Override
  protected void configure() {
    bind(SolrClient.class).toInstance(cfg.buildSolr());
    if (bindingKey != null) {
      bind(bindingKey).to(SolrClient.class);
    }
  }
}
