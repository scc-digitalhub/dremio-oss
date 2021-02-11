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

  private final String tenantField;
  private final String scope;

  /*private final String roleField;
  private final String roleUserMapping;
  private final String roleAdminMapping;*/

  private final DremioConfig config;

  public OAuthConfig(DremioConfig config) {
    // default values
    this(config.getString(DremioConfig.WEB_AUTH_OAUTH_AUTHORIZATION_URL),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_TOKEN_URL),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_USERINFO_URL),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_CALLBACK_URL),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_CLIENT_ID),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_CLIENT_SECRET),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_TENANT_FIELD),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_SCOPE),/*
        config.getString(DremioConfig.WEB_AUTH_OAUTH_ROLE_FIELD),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_ROLE_USER),
        config.getString(DremioConfig.WEB_AUTH_OAUTH_ROLE_ADMIN),*/ config);
  }

  private OAuthConfig(String authorizationUrl, String tokenUrl, String userInfoUrl, String callbackUrl,
      String clientId, String clientSecret, String tenantField, String scope, /* String roleField, String roleUserMapping, String roleAdminMapping,*/
      DremioConfig config) {
    super();
    this.authorizationUrl = authorizationUrl;
    this.tokenUrl = tokenUrl;
    this.userInfoUrl = userInfoUrl;
    this.callbackUrl = callbackUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tenantField = tenantField;
    this.scope = scope;
    /*this.roleField = roleField;
    this.roleUserMapping = roleUserMapping;
    this.roleAdminMapping = roleAdminMapping;*/

    this.config = config;
  }

  public boolean isValid() {
    return (!authorizationUrl.isEmpty() && !tokenUrl.isEmpty() && !clientId.isEmpty());
  }

  /*public boolean requireRoles() {
    return (!roleField.isEmpty() && !roleUserMapping.isEmpty() && !roleAdminMapping.isEmpty());
  }*/

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

  public String getTenantField() {
    return tenantField;
  }

  public String getScope() {
    return scope;
  }

  /*public String getRoleField() {
    return roleField;
  }

  public String getRoleUserMapping() {
    return roleUserMapping;
  }

  public String getRoleAdminMapping() {
    return roleAdminMapping;
  }*/

  public DremioConfig getConfig() {
    return config;
  }

}
