package io.scigraph.services.resources;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The result of a load operation.
 */
public class LoadResult {
  /**
   * Whether the operation succeeded.
   */
  @JsonProperty
  private final boolean success;

  /**
   * The message returned.
   */
  @JsonProperty
  private final String message;

  /**
   * Return whether this was successful.
   */
  public boolean getSuccess() {
    return success;
  }

  /**
   * Return the message.
   */
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return success ? "success" : message;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(success, message);
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof LoadResult)) {
      return false;
    }
    LoadResult other = (LoadResult) obj;
    return Objects.equals(other.success, this.success)
        && Objects.equals(other.message, this.message);
  }

  /**
   * Empty CTOR.
   */
  public LoadResult() {
    this(true, "");
  }

  /**
   * CTOR.
   * @param success did the operation succeed.
   * @param message message given by the loader.
   */
  public LoadResult(boolean success, String message) {
    this.success = success;
    this.message = message;
  }
}

