package com.dremio.dac.server;

import com.dremio.config.DremioConfig;

public class OAuthConfig {

	private final String authorizationUrl;
	private final String tokenUrl;
	private final String userInfoUrl;
	private final String callbackUrl;

	private final String clientId;
	private final String clientSecret;

	private final DremioConfig config;

	public OAuthConfig(DremioConfig config) {
		// default values
		this(config.getString(DremioConfig.WEB_AUTH_OAUTH_AUTHORIZATION_URL),
				config.getString(DremioConfig.WEB_AUTH_OAUTH_TOKEN_URL),
				config.getString(DremioConfig.WEB_AUTH_OAUTH_USERINFO_URL),
				config.getString(DremioConfig.WEB_AUTH_OAUTH_CALLBACK_URL),
				config.getString(DremioConfig.WEB_AUTH_OAUTH_CLIENT_ID),
				config.getString(DremioConfig.WEB_AUTH_OAUTH_CLIENT_SECRET), config);
	}

	private OAuthConfig(String authorizationUrl, String tokenUrl, String userInfoUrl, String callbackUrl,
			String clientId, String clientSecret, DremioConfig config) {
		super();
		this.authorizationUrl = authorizationUrl;
		this.tokenUrl = tokenUrl;
		this.userInfoUrl = userInfoUrl;
		this.callbackUrl = callbackUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.config = config;
	}

	public boolean isValid() {
		return (!authorizationUrl.isEmpty() && !tokenUrl.isEmpty() && !clientId.isEmpty());
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

	public DremioConfig getConfig() {
		return config;
	}

}
