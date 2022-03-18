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

import org.gbif.api.util.VocabularyUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.common.search.es.indexing.EsQueryUtils.STRING_TO_DATE;

@UtilityClass
@Slf4j
public class EsConversionUtils {

  private static final Pattern NESTED_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");
  private static final Predicate<String> IS_NESTED = s -> NESTED_PATTERN.matcher(s).find();

  public static Optional<String> getStringValue(Hit<?> hit, String esField) {
    return getValue(hit, esField, Function.identity());
  }

  public static Optional<Integer> getIntValue(Hit<?> hit, String esField) {
    return getValue(hit, esField, Integer::valueOf);
  }

  public static Optional<Double> getDoubleValue(Hit<?> hit, String esField) {
    return getValue(hit, esField, Double::valueOf);
  }

  public static Optional<Date> getDateValue(Hit<?> hit, String esField) {
    return getValue(hit, esField, STRING_TO_DATE);
  }

  public static  <T extends Enum<?>> Optional<T> getEnumValue(Class<T> enumClass, Hit<?> hit, String esField) {
    return getValue(hit, esField, v -> VocabularyUtils.lookupEnum(v, enumClass));
  }

  public static Optional<List<String>> getListValue(Hit<?> hit, String esField) {
    return Optional.ofNullable(hit.fields().get(esField))
      .map(v -> (List<String>) v)
      .filter(v -> !v.isEmpty());
  }

  public static Optional<List<Map<String, Object>>> getObjectsListValue(
    Hit<?> hit, String esField) {
    return Optional.ofNullable(hit.fields().get(esField))
      .map(v -> (List<Map<String, Object>>) v)
      .filter(v -> !v.isEmpty());
  }

  public static <T> Optional<List<T>> getObjectList(Map<String, JsonData> fields, String field, Function<String,T> mapper) {
    return Optional.ofNullable(fields.get(field))
      .map(v -> (List<String>) v)
      .filter(v -> !v.isEmpty())
      .map(v -> v.stream().map(mapper::apply).collect(Collectors.toList()));
  }

  public static <T> Optional<T> getValue(
    Hit<?> hit, String esField, Function<String, T> mapper) {
    String fieldName = esField;
    Map<String, JsonData> fields = hit.fields();
    if (IS_NESTED.test(esField)) {
      // take all paths till the field name
      String[] paths = esField.split("\\.");
      for (int i = 0; i < paths.length - 1 && fields.containsKey(paths[i]); i++) {
        // update the fields with the current path
        fields = (Map<String, JsonData>) fields.get(paths[i]);
      }
      // the last path is the field name
      fieldName = paths[paths.length - 1];
    }

    return extractValue(fields, fieldName, mapper);
  }

  private static <T> Optional<T> getValue(
    Map<String, JsonData> fields, String esField, Function<String, T> mapper) {
    String fieldName = esField;
    if (IS_NESTED.test(esField)) {
      // take all paths till the field name
      String[] paths = esField.split("\\.");
      for (int i = 0; i < paths.length - 1 && fields.containsKey(paths[i]); i++) {
        // update the fields with the current path
        fields = (Map<String, JsonData>) fields.get(paths[i]);
      }
      // the last path is the field name
      fieldName = paths[paths.length - 1];
    }

    return extractValue(fields, fieldName, mapper);
  }

  protected static <T> Optional<T> extractValue(
    Map<String, JsonData> fields, String fieldName, Function<String, T> mapper) {
    return Optional.ofNullable(fields.get(fieldName))
      .map(String::valueOf)
      .filter(v -> !v.isEmpty())
      .map(
        v -> {
          try {
            return mapper.apply(v);
          } catch (Exception ex) {
            log.error("Error extracting field {} with value {}", fieldName, v);
            return null;
          }
        });
  }

  public static Optional<String> extractStringValue(Map<String, JsonData> fields, String fieldName) {
    return extractValue(fields, fieldName, Function.identity());
  }

  public static <T extends Enum<?>> Optional<List<T>> getEnumListFromOrdinals(Class<T> vocab, Map<String, JsonData> fields, String field) {
    return Optional.ofNullable(fields.get(field))
      .map(v -> (List<Integer>) v)
      .filter(v -> !v.isEmpty())
      .map(v -> v.stream().map(val -> vocab.getEnumConstants()[val]).collect(Collectors.toList()));
  }

  public static <T extends Enum<?>> Optional<T> getEnumFromOrdinal(Class<T> vocab, Map<String, JsonData> fields, String field) {
    return Optional.ofNullable(fields.get(field))
      .map(v -> v.to(Integer.class))
      .map(v -> vocab.getEnumConstants()[v]);
  }

  public static <T extends Enum<?>> Optional<T> getEnumValue(Class<T> vocab, Map<String, JsonData> fields, String field) {
    return Optional.ofNullable(fields.get(field))
            .map(v -> VocabularyUtils.lookupEnum(v.to(String.class), vocab));
  }

  public static Optional<UUID> getUuidValue(Map<String, JsonData> fields, String esField) {
    return getValue(fields, esField, UUID::fromString);
  }

  public static Optional<Boolean> getBooleanValue(Map<String, JsonData> fields, String esField) {
    return getValue(fields, esField, Boolean::valueOf);
  }

  public static Optional<Integer> getIntValue(Map<String, JsonData> fields, String esField) {
    return getValue(fields, esField, Integer::valueOf);
  }

  public static Optional<String> getStringValue(Map<String, JsonData> fields, String esField) {
    return getValue(fields, esField, Function.identity());
  }

  public static Optional<String> getHighlightOrStringValue(
    Map<String, JsonData> fields, Map<String, List<String>> hlFields, String esField) {
    Optional<String> fieldValue = getValue(fields, esField, Function.identity());
    if (Objects.nonNull(hlFields)) {
      Optional<String> hlValue =
        Optional.ofNullable(hlFields.get(esField))
          .map(hlField -> hlField.get(0));
      return hlValue.isPresent() ? hlValue : fieldValue;
    }
    return fieldValue;
  }
}
