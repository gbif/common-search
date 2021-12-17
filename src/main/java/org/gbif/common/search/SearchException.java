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
package org.gbif.common.search;


/**
 * Generic exception thrown by visible search operations.
 */
public class SearchException extends RuntimeException {

  /**
   * Serial UID.
   */
  private static final long serialVersionUID = -7279148830453546443L;

  /**
   * Default constructor.
   */
  public SearchException() {
    // empty block
  }

  public SearchException(String message) {
    super(message);
  }


  public SearchException(String message, Throwable cause) {
    super(message, cause);
  }

  public SearchException(Throwable cause) {
    super(cause);
  }

}
