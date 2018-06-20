/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.datatransfer.google.photos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.photos.model.GoogleAlbum;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempPhotosData;
import org.dataportabilityproject.transfer.ImageStreamProvider;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GooglePhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  static final String ALBUM_POST_URL = "https://picasaweb.google.com/data/feed/api/user/default";
  static final String PHOTO_POST_URL_FORMATTER =
      "https://picasaweb.google.com/data/feed/api/user/default/albumid/%s";
  // The default album to upload to if the photo is not associated with an album
  static final String DEFAULT_ALBUM_ID = "default";
  static final Logger logger = LoggerFactory.getLogger(GooglePhotosImporter.class);

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final ImageStreamProvider imageStreamProvider;
  private volatile GooglePhotosInterface photosInterface;

  public GooglePhotosImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore) {
    this(credentialFactory, jobStore, null, new ImageStreamProvider());
  }

  @VisibleForTesting
  GooglePhotosImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      GooglePhotosInterface photosInterface,
      ImageStreamProvider imageStreamProvider) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.photosInterface = photosInterface;
    this.imageStreamProvider = imageStreamProvider;
  }

  @Override
  public ImportResult importItem(
      UUID jobId, TokensAndUrlAuthData authData, PhotosContainerResource data) {
    try {
      for (PhotoAlbum albumModel : data.getAlbums()) {
        importSingleAlbum(jobId, authData, albumModel);
      }
      for (PhotoModel photoModel : data.getPhotos()) {
        importSinglePhoto(jobId, authData, photoModel);
      }
    } catch (IOException e) {
      return new ImportResult(ResultType.ERROR, e.getMessage());
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleAlbum(UUID jobId, TokensAndUrlAuthData authData, PhotoAlbum albumModel)
      throws IOException {
    GoogleAlbum uploadAlbum = new GoogleAlbum();
    uploadAlbum.setTitle(albumModel.getName()); // per spec, only title should be uploaded

    GoogleAlbum resultAlbum = getOrCreatePhotosInterface(authData).createAlbum(uploadAlbum);
    TempPhotosData photosMappings = jobStore
        .findData(jobId, createCacheKey(), TempPhotosData.class);
    if (photosMappings == null) {
      photosMappings = new TempPhotosData(jobId);
      jobStore.create(jobId, createCacheKey(), photosMappings);
    }
    photosMappings.addAlbumId(albumModel.getId(), uploadAlbum.getId());
    jobStore.update(jobId, createCacheKey(), photosMappings);
  }

  @VisibleForTesting
  void importSinglePhoto(UUID jobId, TokensAndUrlAuthData authData, PhotoModel inputPhoto)
      throws IOException {
    // Upload media content


    // Create media item
  }

  private synchronized GooglePhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized GooglePhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    GooglePhotosInterface photosInterface = new GooglePhotosInterface(credential);
    return photosInterface;
  }

  /**
   * Key for cache of album mappings. TODO: Add a method parameter for a {@code key} for fine
   * grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempPhotoData";
  }

}
