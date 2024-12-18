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
package org.gbif.common.search.es;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.http.HttpHost;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.elasticsearch.indices.update_aliases.AddAction;
import co.elastic.clients.json.JsonpDeserializer;
import co.elastic.clients.json.jackson.JacksonJsonpGenerator;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.json.stream.JsonParser;
import lombok.Data;
import lombok.SneakyThrows;

/** Generic ElasticSearch wrapper client to encapsulate indexing and admin operations. */
@Component
public class EsClient implements Closeable {

  private static final JacksonJsonpMapper MAPPER = new JacksonJsonpMapper();

  @Data
  public static class EsClientConfiguration {
    private String hosts;
    private int connectionTimeOut;
    private int socketTimeOut;
    private int connectionRequestTimeOut;
    private boolean enabled = true;
  }

  private final ElasticsearchClient elasticsearchClient;

  @Autowired
  public EsClient(ElasticsearchClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }

  /**
   * Points the indexName to the alias, and deletes all the indices that were pointing to the alias.
   */
  public void swapAlias(String alias, String indexName) {
    try {
      BooleanResponse aliasExist =
        elasticsearchClient
          .indices()
          .existsAlias(new ExistsAliasRequest.Builder().name(alias).build());

      if (!aliasExist.value()) {
        elasticsearchClient
          .indices()
          .putAlias(new PutAliasRequest.Builder().index(indexName).name(alias).build());
      } else {
        GetAliasResponse getAliasesResponse =
          elasticsearchClient
            .indices()
            .getAlias(new GetAliasRequest.Builder().name(alias).allowNoIndices(true).build());
        Set<String> idxsToDelete = getAliasesResponse.result().keySet();

        elasticsearchClient
          .indices()
          .updateAliases(
            new UpdateAliasesRequest.Builder()
              .actions(
                new Action.Builder()
                  .add(new AddAction.Builder().alias(alias).index(indexName).build())
                  .build())
              .build());
        if (!idxsToDelete.isEmpty()) {
          elasticsearchClient
            .indices()
            .delete(
              new DeleteIndexRequest.Builder().index(new ArrayList<>(idxsToDelete)).build());
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T> T deserializeFromFile(String settingsFile, JsonpDeserializer<T> deserializer) {
    try (final JsonParser jsonParser = MAPPER.jsonProvider().createParser(
      new InputStreamReader(
        new BufferedInputStream(
          EsClient.class.getClassLoader().getResourceAsStream(settingsFile))))) {
        return deserializer.deserialize(jsonParser, MAPPER);
    }
  }

  /** Creates a new index using the indexName, recordType and settings provided. */
  @SneakyThrows
  public void createIndex(
      String indexName,
      TypeMapping mappings,
      IndexSettings settings) {
      CreateIndexRequest.Builder createIndexRequest = new CreateIndexRequest.Builder();
      createIndexRequest
          .index(indexName)
          .settings(new IndexSettings.Builder().index(settings).build())
          .mappings(mappings);
      elasticsearchClient.indices().create(c -> c.index(indexName).settings(settings).mappings(mappings));
  }

  /** Updates the settings of an existing index. */
  public void updateSettings(String indexName, IndexSettings settings) {
    try {
      elasticsearchClient.indices().putSettings(s -> s.index(indexName).settings(settings));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Performs a ElasticSearch {@link BulkRequest}. */
  public BulkResponse bulk(BulkRequest bulkRequest) throws IOException {
    return elasticsearchClient.bulk(bulkRequest);
  }

  private static HttpHost[] getHosts(EsClientConfiguration esClientConfiguration) {
    return Stream.of(esClientConfiguration.hosts.split(","))
                 .map(hostUrl -> {
                                    try {
                                      URL url = new URL(hostUrl);
                                      return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
                                    } catch (MalformedURLException e) {
                                      throw new IllegalArgumentException(hostUrl, e);
                                    }
                                 }).toArray(HttpHost[]::new);
  }

  /** Creates ElasticSearch client using default connection settings. */
  public static ElasticsearchClient provideEsClient(EsClientConfiguration esClientConfiguration) {
    return provideEsClient(esClientConfiguration, new JacksonJsonpMapper());
  }

  /** Creates ElasticSearch client using default connection settings and using a custom Jackson ObjectMapper. */
  public static ElasticsearchClient provideEsClient(EsClientConfiguration esClientConfiguration, ObjectMapper objectMapper) {
    return provideEsClient(esClientConfiguration, new JacksonJsonpMapper(objectMapper));
  }

  /** Creates ElasticSearch client using default connection settings and using a custom JacksonJsonpMapper. */
  public static ElasticsearchClient provideEsClient(EsClientConfiguration esClientConfiguration, JacksonJsonpMapper jacksonJsonpMapper) {
    return new ElasticsearchClient( new RestClientTransport(
      RestClient.builder(getHosts(esClientConfiguration))
        .setRequestConfigCallback(
          requestConfigBuilder ->
            requestConfigBuilder
              .setConnectTimeout(esClientConfiguration.getConnectionTimeOut())
              .setSocketTimeout(esClientConfiguration.getSocketTimeOut())
              .setConnectionRequestTimeout(
                esClientConfiguration.getConnectionRequestTimeOut()))
        .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS)
        .build(), jacksonJsonpMapper));
  }

  @Override
  public void close() {
    // shuts down the ES client
    if (Objects.nonNull(elasticsearchClient)) {
      try {
        elasticsearchClient._transport().close();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  @SneakyThrows
  public static String prettyJsonRequest(SearchRequest request) {
    StringWriter jsonObjectWriter = new StringWriter();
    JsonGenerator generator = MAPPER.objectMapper().writerWithDefaultPrettyPrinter().createGenerator(jsonObjectWriter);
    JacksonJsonpGenerator jacksonJsonpGenerator = new JacksonJsonpGenerator(generator);

    request.serialize(jacksonJsonpGenerator, MAPPER);
    jacksonJsonpGenerator.flush();

    return jsonObjectWriter.toString();
  }
}
