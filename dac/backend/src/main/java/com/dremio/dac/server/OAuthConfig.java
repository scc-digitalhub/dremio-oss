/*
 * Copyright (C) 2017-2019 Dremio Corporation
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
package com.dremio.dac.server;

import com.dremio.config.DremioConfig;

/**
 * Configuration object for OAuth
 */
public class OAuthConfig {

  private final String authorizationUrl;
  private final String tokenUrl;
  private final String userInfoUrl;
  private final String callbackUrl;

  private final String clientId;
  private final String clientSecret;

  private final String roleField;
  private final String roleUserMapping;
  private final String roleAdminMapping;

  private final DremioConfig config;

  public OAuthConfig(DremioConfig config) {
    // default values
    this(config.getString(DremioConfig.WEB_AUTH_OAUTH_AUTHORIZATION_URL),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_TOKEN_URL),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_USERINFO_URL),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_CALLBACK_URL),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_CLIENT_ID),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_CLIENT_SECRET),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_ROLE_FIELD),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_ROLE_USER),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_ROLE_ADMIN), config);
  }

  private OAuthConfig(String authorizationUrl, String tokenUrl, String userInfoUrl, String callbackUrl,
      String clientId, String clientSecret, String roleField, String roleUserMapping, String roleAdminMapping,
      DremioConfig config) {
    super();
    this.authorizationUrl = authorizationUrl;
    this.tokenUrl = tokenUrl;
    this.userInfoUrl = userInfoUrl;
    this.callbackUrl = callbackUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.roleField = roleField;
    this.roleUserMapping = roleUserMapping;
    this.roleAdminMapping = roleAdminMapping;

    this.config = config;
  }

  public boolean isValid() {
    return (!authorizationUrl.isEmpty() && !tokenUrl.isEmpty() && !clientId.isEmpty());
  }

  public boolean requireRoles() {
    return (!roleField.isEmpty() && !roleUserMapping.isEmpty() && !roleAdminMapping.isEmpty());
  }

  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  public String getTokenUrl() {
    return tokenUrl;
  }

  public String getUserInfoUrl() {
    return userInfoUrl;
  }

  public String getCallbackUrl() {
    return callbackUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getRoleField() {
    return roleField;
  }

  public String getRoleUserMapping() {
    return roleUserMapping;
  }

  public String getRoleAdminMapping() {
    return roleAdminMapping;
  }

  public DremioConfig getConfig() {
    return config;
  }

}
