package org.gbif.common.search.solr.builders;

import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.ResourcesUtil;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds an instance of a {@link EmbeddedSolrServer}. The {@link SolrClient} can be created at a specific directory;
 * set by the method withServerHomeDir, is the home directory is not specified a temporary directory is created.
 */
public class EmbeddedServerBuilder {


  private static final Logger LOG = LoggerFactory.getLogger(EmbeddedServerBuilder.class);

  // Default Solr configuration directory
  private static final String SOLR_CONF = "solr/%s/conf/";

  // Solr home system variable
  private static final String SOLR_HOME = "solr.solr.home";

  // Default Solr resources
  private static final String[] SOLR_DEFAULT_RESOURCES = {"synonyms.txt", "protwords.txt", "stopwords.txt"};

  // Default configuration resources
  private static final String[] SOLR_CONF_DEFAULT_RESOURCES = ArrayUtils.addAll(new String[] {"schema.xml",
    "solrconfig.xml"}, SOLR_DEFAULT_RESOURCES);

  // Container name, defaults to collection1
  private String coreName = "collection1";

  // Flag to determine if the solr home directory must be deleted (used by tests cases mostly).
  private boolean deleteOnExit;

  // Solr home directory
  private String serverHomeDir;

  /**
   * Builds a {@link EmbeddedSolrServer} instance. The server is created at the specified directory.
   * If a solrHome directory is not specified a temporary directory is created (used by test cases).
   */
  public SolrClient build() {
    if (Strings.isNullOrEmpty(serverHomeDir)) {
      try {
        serverHomeDir = FileUtils.createTempDir("solr-", "").getAbsolutePath();
        LOG.debug("Using tmp solr server {}", serverHomeDir);
      } catch (IOException e) {
        Throwables.propagate(e);
      }
    }
    // directory solr_test will be created below target in the maven build and populated with the needed files
    File solrHome = new File(serverHomeDir);
    if (deleteOnExit) {
      solrHome.deleteOnExit();
    }
    copyResources(solrHome);
    return builSolrServer(solrHome);
  }


  /**
   * Solr coreName name, defaults to collection1.
   * This is important since 4.4 as the solr directory has to have a subfolder with that name!
   */
  public EmbeddedServerBuilder withCoreName(String coreName) {
    Preconditions.checkNotNull(coreName, "Invalid solr coreName name");
    this.coreName = coreName;
    return this;
  }

  /**
   * Flag to determine if the solr home directory must be deleted (used by tests cases mostly).
   */
  public EmbeddedServerBuilder withDeleteOnExit(boolean deleteOnExit) {
    this.deleteOnExit = deleteOnExit;
    return this;
  }

  /**
   * Solr home root directory. This is the parent folder of the actual core created.
   */
  public EmbeddedServerBuilder withServerHomeDir(String serverHomeDir) {
    this.serverHomeDir = serverHomeDir;
    return this;
  }


  /**
   * Build the {@link SolrClient} instance, pointing to the solrHome directory.
   */
  private SolrClient builSolrServer(File solrHome) {
    // create coreName
    System.setProperty(SOLR_HOME, solrHome.getAbsolutePath());
    CoreContainer cc = new CoreContainer(solrHome.getAbsolutePath());
    cc.load();
    SolrClient solrClient = new EmbeddedSolrServer(cc, coreName);
    LOG.info("Created embedded solr server with solr dir {}", solrHome.getAbsolutePath());
    return solrClient;
  }

  /**
   * Copy default resources into the solrHome directory.
   */
  private void copyResources(File solrHome) {
    try {
      // since solr 4.4 the cores need to reside in a subfolder with that name
      // see http://wiki.apache.org/solr/Solr.xml%204.4%20and%20beyond
      File coreDir = new File(solrHome, coreName);
      // copy solr resource files
      ResourcesUtil.copy(solrHome, "solr/", false, "solr.xml");
      // copy default configurations
      File conf = new File(coreDir, "conf");
      ResourcesUtil.copy(conf, "solr/default/", false, SOLR_DEFAULT_RESOURCES);
      // copy specific configurations, overwriting above defaults
      ResourcesUtil.copy(conf, String.format(SOLR_CONF,coreName), true, SOLR_CONF_DEFAULT_RESOURCES);
      File coreProp = new File(coreDir, "core.properties");
      if(!coreProp.exists()) {
        // create empty core.properties in core dir
        coreProp.createNewFile();
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

}
