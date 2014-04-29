/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.common.search.model;

/**
 * WildcardPadding pattern for full text searches.
 * This enumeration determines how a field is used when a search operation is issued.
 * In general this class should be used for explicitly set how the wildcards(*) should be applied.
 */
public enum WildcardPadding {
  /**
   * The search pattern will be transformed into: *inputPattern*.
   */
  BOTH,
  /**
   * The search pattern will be transformed into: *inputPattern.
   */
  LEFT,
  /**
   * The search pattern will be transformed into: inputPattern*.
   */
  RIGHT,
  /**
   * The search pattern will be transformed into: inputPattern. This means exact match.
   */
  NONE
}
