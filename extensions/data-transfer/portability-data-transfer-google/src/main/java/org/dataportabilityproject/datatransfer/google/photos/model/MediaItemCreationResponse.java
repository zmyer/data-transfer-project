/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.datatransfer.google.photos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MediaItemCreationResponse {

  private NewMediaItemResult[] newMediaItemResults;

  @JsonCreator
  public MediaItemCreationResponse(
      @JsonProperty("newMediaItemResult") NewMediaItemResult[] newMediaItemResults) {
    this.newMediaItemResults = newMediaItemResults;
  }

  public NewMediaItemResult[] getNewMediaItemResults() {
    return newMediaItemResults;
  }

  public static class NewMediaItemResult {

    private String uploadToken;
    private Status status;
    private GoogleMediaItem mediaItem;

    @JsonCreator
    public NewMediaItemResult(@JsonProperty("uploadToken") String uploadToken,
        @JsonProperty("status") Status status,
        @JsonProperty("mediaItem") GoogleMediaItem mediaItem) {
      this.uploadToken = uploadToken;
      this.status = status;
      this.mediaItem = mediaItem;
    }

    public String getUploadToken() {
      return uploadToken;
    }

    public Status getStatus() {
      return status;
    }

    public GoogleMediaItem getMediaItem() {
      return mediaItem;
    }

    public static class Status {

      private String code;

      @JsonCreator
      public Status(@JsonProperty("code") String code) {
        this.code = code;
      }

      public String getCode() {
        return code;
      }
    }
  }
}
