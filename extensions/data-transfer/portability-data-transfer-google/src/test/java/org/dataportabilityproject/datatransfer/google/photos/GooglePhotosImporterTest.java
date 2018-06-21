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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.util.UUID;
import org.dataportabilityproject.cloud.local.LocalJobStore;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.photos.model.GoogleAlbum;
import org.dataportabilityproject.datatransfer.google.photos.model.GoogleMediaItem;
import org.dataportabilityproject.datatransfer.google.photos.model.MediaItemCreationResponse.NewMediaItemResult;
import org.dataportabilityproject.datatransfer.google.photos.model.MediaItemCreationResponse.NewMediaItemResult.Status;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.types.TempPhotosData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class GooglePhotosImporterTest {

  private final String IMG_URI = "image uri";
  private final String JPEG_MEDIA_TYPE = "image/jpeg";
  private final String UPLOAD_TOKEN = "upload token";
  private final String MODEL_ALBUM_ID = "model_album_id";
  private final String GOOGLE_ALBUM_ID = "google_album_id";

  private final UUID uuid = UUID.randomUUID();

  private GooglePhotosImporter googlePhotosImporter;

  private GoogleCredentialFactory credentialFactory;
  private GooglePhotosInterface photosInterface;
  private JobStore jobStore;

  @Before
  public void setUp() throws IOException, ServiceException {
    photosInterface = mock(GooglePhotosInterface.class);
    jobStore = new LocalJobStore();
  }

  @Test
  public void importAlbumAndPhoto() throws IOException, ServiceException {
    // Set up album model
    String albumName = "albumName";
    String albumDescription = "albumDescription";
    PhotoAlbum albumModel = new PhotoAlbum(MODEL_ALBUM_ID, albumName, albumDescription);
    // Set up response item
    GoogleAlbum responseAlbum = new GoogleAlbum();
    responseAlbum.setTitle("copy of " + albumName);
    responseAlbum.setId(GOOGLE_ALBUM_ID);
    // Set up mock
    when(photosInterface.createAlbum(Matchers.any(GoogleAlbum.class))).thenReturn(responseAlbum);

    // Set up photo
    String photoDescription = "photoDescription";
    PhotoModel photoModel = new PhotoModel("", IMG_URI, photoDescription, JPEG_MEDIA_TYPE, null,
        MODEL_ALBUM_ID);
    // Set up response item
    Status status = new Status("0");
    GoogleMediaItem responseMediaItem = new GoogleMediaItem();
    responseMediaItem.setDescription(photoDescription);
    NewMediaItemResult newMediaItemResult = new NewMediaItemResult(UPLOAD_TOKEN, status,
        responseMediaItem);
    // Set up mocks
    when(photosInterface.uploadMedia(IMG_URI)).thenReturn(UPLOAD_TOKEN);
    when(photosInterface.createNewMediaItem(UPLOAD_TOKEN, GOOGLE_ALBUM_ID, photoDescription))
        .thenReturn(newMediaItemResult);

    // Run test
    googlePhotosImporter = new GooglePhotosImporter(null, jobStore, photosInterface);
    googlePhotosImporter.importSingleAlbum(uuid, null, albumModel);
    googlePhotosImporter.importSinglePhoto(uuid, null, photoModel);

    // Check results
    // Verify correct methods were called
    ArgumentCaptor<GoogleAlbum> albumArgumentCaptor = ArgumentCaptor.forClass(GoogleAlbum.class);
    verify(photosInterface).createAlbum(albumArgumentCaptor.capture());
    verify(photosInterface).uploadMedia(IMG_URI);
    verify(photosInterface).createNewMediaItem(UPLOAD_TOKEN, GOOGLE_ALBUM_ID, photoDescription);

    // Check uploaded info
    GoogleAlbum uploadAlbum = albumArgumentCaptor.getValue();
    assertThat(uploadAlbum.getTitle()).isEqualTo("copy of " + albumName);

    // Check jobStore contents
    assertThat(jobStore.findData(uuid, GooglePhotosImporter.createCacheKey(), TempPhotosData.class)
        .lookupNewAlbumId(MODEL_ALBUM_ID)).isEqualTo(GOOGLE_ALBUM_ID);
  }
}
