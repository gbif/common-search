package org.gbif.common.search.solr;

import org.gbif.common.search.solr.builders.CloudSolrServerBuilder;
import org.gbif.common.search.solr.builders.EmbeddedServerBuilder;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.net.MalformedURLException;
import java.util.Properties;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configurations for solr with optional injection that allow us to provide defaults for a temporary embedded server.
 */
public class SolrConfig {
  private static final Logger LOG = LoggerFactory.getLogger(SolrConfig.class);
  private static final String P_TYPE = "type";
  private static final String P_HOME= "home";
  private static final String ID_FIELD= "idField";
  private static final String P_COLLECTION = "collection";
  private static final String P_DELETE = "delete";

  public SolrServerType serverType = SolrServerType.EMBEDDED;

  public String serverHome = null;

  public String collection = "collection1";

  public boolean deleteOnExit = false;

  /**
   * IdField is required by SolrCloudClient
   */
  public String idField;


  public Properties toProperties(@Nullable String prefix) {
    Properties props = new Properties();
    setProp(props, prefix, P_TYPE, serverType);
    setProp(props, prefix, P_HOME, serverHome);
    setProp(props, prefix, P_COLLECTION, collection);
    setProp(props, prefix, P_DELETE, deleteOnExit);
    setProp(props, prefix, ID_FIELD, idField);
    return props;
  }

  private void setProp(Properties p, @Nullable String prefix, String name, Object value) {
    if (value != null) {
      p.setProperty(prefix == null ? name : prefix + name, value.toString());
    }
  }

  /**
   * Creates a config instance from properties looking for:
   *   type
   *   home
   *   collection
   *   delete
   *
   * Additional property prefixes can be removed by giving an optional prefix argument.
   *
   * @param properties the properties to convert
   * @param prefix optional property prefix to strip
   */
  public static SolrConfig fromProperties(Properties properties, @Nullable String prefix) {
    Properties props;
    if (prefix != null) {
      props = PropertiesUtil.filterProperties(properties, prefix);
    } else {
      props = properties;
    }
    SolrConfig cfg = new SolrConfig();
    cfg.serverType = SolrServerType.valueOf(props.getProperty(P_TYPE, cfg.serverType.name()));
    cfg.serverHome = props.getProperty(P_HOME, cfg.serverHome);
    cfg.collection = props.getProperty(P_COLLECTION, cfg.collection);
    cfg.idField = props.getProperty(ID_FIELD, cfg.idField);
    cfg.deleteOnExit = Boolean.valueOf(props.getProperty(P_DELETE, String.valueOf(cfg.deleteOnExit)));
    return cfg;
  }

  public SolrClient buildSolr() {
    Preconditions.checkNotNull(serverType, "Solr server type is required");
    switch (serverType) {
      case EMBEDDED:
        LOG.info("Using embedded solr server {} with collection {}", serverHome, collection);
        return new EmbeddedServerBuilder()
            .withServerHomeDir(serverHome)
            .withDeleteOnExit(deleteOnExit)
            .withCoreName(collection)
            .build();

      case HTTP:
        LOG.info("Using remote solr server {}", serverHome);
        return new HttpSolrClient(serverHome);

      case LBHTTP:
        try {
          LOG.info("Using remote load-balanced solr server {}", serverHome);
          return new LBHttpSolrClient(serverHome);
        } catch (MalformedURLException e) {
          throw new IllegalArgumentException(e);
        }

      case CLOUD:
        LOG.info("Using cloud solr server {} with collection {} and idField {}", serverHome, collection, idField);
        return new CloudSolrServerBuilder()
            .withZkHost(serverHome)
            .withDefaultCollection(collection)
            .withIdField(idField)
            .build();

      default:
        // should never get here...
        throw new IllegalArgumentException("Unknown server type " + serverType);
    }
  }
}
