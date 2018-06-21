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

public class MediaUploadRequest {

  private String albumId;
  private NewMediaItem[] newMediaItems;

  @JsonCreator
  public MediaUploadRequest(@JsonProperty("albumId") String albumId,
      @JsonProperty("newMediaItems") NewMediaItem[] newMediaItems) {
    this.albumId = albumId;
    this.newMediaItems = newMediaItems;
  }

  public String getAlbumId() {
    return albumId;
  }

  public NewMediaItem[] getNewMediaItems() {
    return newMediaItems;
  }

  public static class NewMediaItem {

    private String description;
    private SimpleMediaItem simpleMediaItem;

    @JsonCreator
    public NewMediaItem(@JsonProperty("description") String description,
        @JsonProperty("simpleMediaItem") SimpleMediaItem simpleMediaItem) {
      this.description = description;
      this.simpleMediaItem = simpleMediaItem;
    }

    public String getDescription() {
      return description;
    }

    public SimpleMediaItem getSimpleMediaItem() {
      return simpleMediaItem;
    }

    public static class SimpleMediaItem {

      private String uploadToken;

      @JsonCreator
      public SimpleMediaItem(@JsonProperty("uploadToken") String uploadToken) {
        this.uploadToken = uploadToken;
      }

      public String getUploadToken() {
        return uploadToken;
      }
    }
  }

}
