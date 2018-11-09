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

package org.datatransferproject.datatransfer.google.tasks;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.TempTasksData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.models.tasks.TaskContainerResource;
import org.datatransferproject.types.transfer.models.tasks.TaskListModel;
import org.datatransferproject.types.transfer.models.tasks.TaskModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleTasksImporter implements Importer<TokensAndUrlAuthData, TaskContainerResource> {

  private final Logger logger = LoggerFactory.getLogger(GoogleTasksImporter.class);

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private Tasks tasksClient;

  public GoogleTasksImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore) {
    this(credentialFactory, jobStore, null);
  }

  @VisibleForTesting
  GoogleTasksImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore,
      Tasks tasksClient) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.tasksClient = tasksClient;
  }

  @Override
  public ImportResult importItem(
      UUID jobId, TokensAndUrlAuthData authData, TaskContainerResource data) throws IOException {

    Tasks tasksService = getOrCreateTasksService(authData);
    TempTasksData tempTasksData = jobStore.findData(jobId, createCacheKey(), TempTasksData.class);
    if (tempTasksData == null) {
      tempTasksData = new TempTasksData(jobId.toString());
      jobStore.create(jobId, createCacheKey(), tempTasksData);
    }

    for (TaskListModel oldTasksList : data.getLists()) {
      // TempTasksData shouldn't be null since we added it.
      tempTasksData = jobStore.findData(jobId, createCacheKey(), TempTasksData.class);
      TaskList newTaskList = new TaskList().setTitle("Imported copy - " + oldTasksList.getName());
      TaskList insertedTaskList;

      insertedTaskList = tasksService.tasklists().insert(newTaskList).execute();

      tempTasksData.addTaskListId(oldTasksList.getId(), insertedTaskList.getId());

      jobStore.update(jobId, createCacheKey(), tempTasksData);
    }

    tempTasksData = jobStore.findData(jobId, createCacheKey(), TempTasksData.class);

    for (TaskModel oldTask : data.getTasks()) {
      Task newTask = new Task().setTitle(oldTask.getText()).setNotes(oldTask.getNotes());
      if (oldTask.getCompletedTime() != null) {
        newTask.setCompleted(new DateTime(oldTask.getCompletedTime().toEpochMilli()));
      }
      if (oldTask.getDueTime() != null) {
        newTask.setDue(new DateTime(oldTask.getDueTime().toEpochMilli()));
      }
      String newTaskListId = tempTasksData.lookupNewTaskListId(oldTask.getTaskListId());
      tasksService.tasks().insert(newTaskListId, newTask).execute();
    }

    return new ImportResult(ResultType.OK);
  }

  private Tasks getOrCreateTasksService(TokensAndUrlAuthData authData) {
    return tasksClient == null ? makeTasksService(authData) : tasksClient;
  }

  private synchronized Tasks makeTasksService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Tasks.Builder(
        credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }

  /**
   * Key for cache of album mappings. TODO: Add a method parameter for a {@code key} for fine
   * grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempTaskData";
  }
}
