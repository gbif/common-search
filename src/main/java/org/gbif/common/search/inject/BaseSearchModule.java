/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.common.search.inject;


import org.gbif.common.search.exception.SearchException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.common.search.util.SolrConstants.SOLR_EMBEDDED_KEY;
import static org.gbif.common.search.util.SolrConstants.SOLR_HOME;
import static org.gbif.common.search.util.SolrConstants.SOLR_SERVER_KEY;

/**
 * A search module that can be configured in one of 2 ways depending on the constructor use.
 * - By using the default constructor, a configuration named *.properties will be expected
 * - By using the properties constructor, properties with the prefix of PROPERTY_PREFIX will be expected
 *
 * @deprecated use PrivateServiceModule instead and the new {@link SolrModule} to provide a flexible server instance.
 */
@Deprecated
public abstract class BaseSearchModule extends AbstractModule {

  private static final Logger LOG = LoggerFactory.getLogger(BaseSearchModule.class);

  private final String configurationFile;
  private final String propertyPrefix;

  private final Map<String, String> properties;

  /**
   * Uses the given properties to configure the service.
   *
   * @param properties to use
   */
  public BaseSearchModule(Properties properties, String propertyPrefix) {
    this.propertyPrefix = propertyPrefix;
    configurationFile = "";
    this.properties = buildProperties(properties);
  }

  /**
   * Uses the clb_search.properties declaration.
   */
  public BaseSearchModule(String configurationFile, String propertyPrefix) {
    this.configurationFile = configurationFile;
    this.propertyPrefix = propertyPrefix;
    properties = buildProperties(getConfigurationFile());
  }

  /**
   * Filters out all properties that don't have a common prefix and removes that prefix for those properties that do
   * have it.
   *
   * @param properties to filter and translate
   *
   * @return immutable Map with the translated and filtered properties
   */
  private Map<String, String> buildProperties(Map<String, String> properties) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      if (entry.getKey().startsWith(propertyPrefix)) {
        builder.put(entry.getKey().substring(propertyPrefix.length()), entry.getValue());
      }
    }

    return builder.build();
  }

  private Map<String, String> buildProperties(Properties properties) {
    return buildProperties(Maps.fromProperties(properties));
  }

  @Override
  protected void configure() {
    Names.bindProperties(binder(), properties);
  }

  /**
   * @return The properties file as named in CONFIGURATION_FILE.
   */
  private Properties getConfigurationFile() {
    FileInputStream fileInputStreamProperties = null;
    Properties confProperties = new Properties();
    try {
      fileInputStreamProperties =
        new FileInputStream(getClass().getClassLoader().getResource(configurationFile).getFile());
      confProperties.load(fileInputStreamProperties);
    } catch (FileNotFoundException e) {
      LOG.error("Configuration file not found", e);
      throw new IllegalStateException(e);
    } catch (IOException e) {
      LOG.error("Error reading the configuration file", e);
      throw new IllegalStateException(e);
    } finally {
      IOUtils.closeQuietly(fileInputStreamProperties);
    }
    return confProperties;
  }

  /**
   * Provider method for creating the SolrServer instance.
   *
   * @param isEmbedded     flag to determine if the SolrServer is an embedded or a remote instance
   * @param solrServerPath the url(remote http url) or path (if is Embedded) to the SolrServer
   */
  @Provides
  @Singleton
  protected SolrClient provideSolrServer(@Named(SOLR_SERVER_KEY) String solrServerPath,
    @Named(SOLR_EMBEDDED_KEY) Boolean isEmbedded) {
    SolrClient sorlClient = null;
    try {
      LOG.info("Creating solr server with path={}", solrServerPath);
      if (isEmbedded) {
        System.setProperty(SOLR_HOME, solrServerPath);
        CoreContainer coreContainer = new CoreContainer(solrServerPath);
        sorlClient = new EmbeddedSolrServer(coreContainer, "");
      } else { // remote instance
        sorlClient = new HttpSolrClient(solrServerPath);
        ((HttpSolrClient) sorlClient).setRequestWriter(new BinaryRequestWriter());
        ((HttpSolrClient) sorlClient).setAllowCompression(true);
      }
      sorlClient.ping();
    } catch (MalformedURLException e) {
      LOG.error("Error reaching remote SolrClient files", e);
      throw new SearchException(e);
    } catch (IOException e) {
      LOG.error("Error accessing SolrClient configuration files", e);
      throw new SearchException(e);
    } catch (SolrException e) {
      LOG.error("Error parsing SolrClient configuration files", e);
      throw new SearchException(e);
    } catch (SolrServerException e) {
      LOG.error("Error creating a SolrClient instance", e);
      throw new SearchException(e);
    }

    return sorlClient;
  }
}
