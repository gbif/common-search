package org.gbif.common.search.inject;

import java.io.IOException;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;

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
  public void testModuleWithutConfigs() throws IOException, SolrServerException {
    // no properties bound, expect embedded /tmp server as default
    Injector injector = Guice.createInjector(new TestModule(), new SolrModule());
    // a real embedded solr
    SolrServer solr = injector.getInstance(SolrServer.class);
    // do sth with the server to make sure its real and not just lazy
    solr.commit();
    solr.ping();
  }

}
