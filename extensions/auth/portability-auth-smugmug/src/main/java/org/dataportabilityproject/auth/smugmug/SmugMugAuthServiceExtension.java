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

package org.dataportabilityproject.auth.smugmug;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.api.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmugMugAuthServiceExtension implements AuthServiceExtension {

  private static final String SMUGMUG_KEY = "SMUGMUG_KEY"; // TODO: Possibly SMUGMUG_API_KEY?
  private static final String SMUGMUG_SECRET = "SMUGMUG_SECRET";
  private static final String SERVICE_ID = "SmugMug";

  private final Logger logger = LoggerFactory.getLogger(SmugMugAuthServiceExtension.class);
  private final List<String> supportedServices = ImmutableList.of("photos");
  private AuthDataGenerator importAuthDataGenerator;
  private AuthDataGenerator exportAuthDataGenerator;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    Preconditions.checkArgument(
        initialized,
        "SmugMugAuthServiceExtension is not initialized!  Unable to retrieve AuthDataGenerator.");
    Preconditions.checkArgument(
        supportedServices.contains(transferDataType),
        "Transfer type [" + transferDataType + "] is not supported in SmugMug.");
    return (mode == AuthMode.IMPORT) ? importAuthDataGenerator : exportAuthDataGenerator;
  }

  @Override
  public List<String> getImportTypes() {
    return supportedServices;
  }

  @Override
  public List<String> getExportTypes() {
    return supportedServices;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      return;
    }

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(SMUGMUG_KEY, SMUGMUG_SECRET);
    } catch (IOException e) {
      logger.warn(
          "Error retrieving SmugMug credentials.  Did you set {} and {}?",
          SMUGMUG_KEY,
          SMUGMUG_SECRET);
      return;
    }
    HttpTransport transport = context.getService(HttpTransport.class);

    importAuthDataGenerator =
        new SmugMugAuthDataGenerator(appCredentials, AuthMode.IMPORT, transport);
    exportAuthDataGenerator =
        new SmugMugAuthDataGenerator(appCredentials, AuthMode.EXPORT, transport);
    initialized = true;
  }
}
