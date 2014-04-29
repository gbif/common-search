package org.gbif.common.search.model;

import java.util.List;

/**
 * A simple facade allowing a property to be a list of complex objects that can be treated as a simple String list
 * for search highlighting. Return this interface in the getter of the annotated result object for the response builder
 * to detect it.
 */
public interface HighlightableList {

  /**
   * Returns a list of string values in the same order as the original wrapped list the individual values are
   * based on.
   * @return a list of string values
   */
  List<String> getValues();

  /**
   * Replaces an existing value in the value list with a new value, e.g. containing markup for highlighting.
   * @param index the position in the list of values
   * @param newValue the new value to set
   */
  void replaceValue(int index, String newValue);
}
