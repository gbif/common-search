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

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class SolrModuleTest {

  /**
   * Test module, sets the Solr server type as EMBEDDED.
   */
  public static class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(String.class).annotatedWith(Names.named("solr.server.type")).toInstance("EMBEDDED");
    }

  }

  @Test
  public void testModuleWithoutConfigs() throws IOException, SolrServerException {
    // no properties bound, expect embedded /tmp server as default
    Injector injector = Guice.createInjector(new TestModule(), new SolrModule(new SolrConfig()));
    // a real embedded solr
    SolrClient solr = injector.getInstance(SolrClient.class);
    // do sth with the server to make sure its real and not just lazy
    solr.commit();
    solr.ping();
  }

}
