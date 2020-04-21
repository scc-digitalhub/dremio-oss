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
package com.dremio.dac.resource;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.dremio.common.exceptions.UserException;
import com.dremio.common.util.DremioVersionInfo;
import com.dremio.config.DremioConfig;
import com.dremio.dac.annotations.RestResource;
import com.dremio.dac.model.spaces.HomeName;
import com.dremio.dac.model.spaces.HomePath;
import com.dremio.dac.model.usergroup.SessionPermissions;
import com.dremio.dac.model.usergroup.UserLoginSession;
import com.dremio.dac.server.GenericErrorMessage;
import com.dremio.dac.server.OAuthConfig;
import com.dremio.dac.server.tokens.TokenDetails;
import com.dremio.dac.server.tokens.TokenManager;
import com.dremio.dac.support.SupportService;
import com.dremio.exec.server.SabotContext;
import com.dremio.options.OptionManager;
import com.dremio.service.namespace.NamespaceException;
import com.dremio.service.namespace.NamespaceKey;
import com.dremio.service.namespace.NamespaceNotFoundException;
import com.dremio.service.namespace.NamespaceService;
import com.dremio.service.namespace.space.proto.HomeConfig;
import com.dremio.service.users.SimpleUser;
import com.dremio.service.users.SystemUser;
import com.dremio.service.users.User;
import com.dremio.service.users.UserLoginException;
import com.dremio.service.users.UserNotFoundException;
import com.dremio.service.users.UserService;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * API for OAuth2 access
 */
@RestResource
@Path("/oauth")
public class OAuthResource {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OAuthResource.class);

  private final DremioConfig dremioConfig;
  private final SabotContext dContext;
  private final UserService userService;
  private final SupportService support;
  private final TokenManager tokenManager;
  private final NamespaceService namespaceService;

  private final OAuthConfig oauthConfig;
  private final OAuthApi api;
  private final OAuth20Service service;
  private final SecureRandom generator;

  @Inject
  public OAuthResource(DremioConfig dremioConfig, SabotContext dContext, UserService userService,
      SupportService support, TokenManager tokenManager, NamespaceService namespaceService) {
    this.dremioConfig = dremioConfig;
    this.dContext = dContext;
    this.userService = userService;
    this.support = support;
    this.tokenManager = tokenManager;
    this.namespaceService = namespaceService;

    this.oauthConfig = new OAuthConfig(dremioConfig);
    if (oauthConfig.isValid()) {
      this.api = new OAuthApi(oauthConfig);
      this.service = new ServiceBuilder(oauthConfig.getClientId()).apiSecret(oauthConfig.getClientSecret())
          .defaultScope("openid profile email")
          .callback(oauthConfig.getCallbackUrl() + "/apiv2/oauth/callback").build(api);
    } else {
      this.api = null;
      this.service = null;
    }
    generator = new SecureRandom();
  }

  @GET
  @Path("/login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@Context HttpServletRequest request) {
    logger.info("call to oauth login");
    if (service != null) {
      URI uri = URI.create(service.getAuthorizationUrl());
      return Response.temporaryRedirect(uri).build();
    } else {
      return Response.temporaryRedirect(URI.create("/login")).build();
    }
  }

  @GET
  @Path("/callback")
  @Produces(MediaType.TEXT_HTML)
  public Response callback(@Context HttpServletRequest request) {
    logger.info("call to oauth callback " + request.toString());

    String code = request.getParameter("code");

    if (code == null) {
      logger.error("code is null");
      return Response.serverError().build();
    }

    try {
      // get access token and userInfo
      final OAuth2AccessToken accessToken = service.getAccessToken(code);
      final OAuthRequest req = new OAuthRequest(Verb.GET, oauthConfig.getUserInfoUrl());
      service.signRequest(accessToken, req);
      final com.github.scribejava.core.model.Response res = service.execute(req);

      User userInfo = null;
      String role = "";

      if (res.getCode() == 200) {
        JsonObject json = new JsonParser().parse(res.getBody()).getAsJsonObject();
        userInfo = getUserInfo(json);

        if (oauthConfig.requireRoles()) {
          // fetch role from json
          if (json.has(oauthConfig.getRoleField())) {
            JsonArray roles = json.get(oauthConfig.getRoleField()).getAsJsonArray();
            while (roles.iterator().hasNext()) {
              String r = roles.iterator().next().getAsString();
              if (r.equals(oauthConfig.getRoleUserMapping())) {
                role = "user";
              }
              if (r.equals(oauthConfig.getRoleUserMapping())) {
                role = "admin";
              }
              if (!role.isEmpty()) {
                break;
              }

            }
          }
        }

      } else {
        throw new UserLoginException("", "oauth error");
      }

      if ((userInfo == null) || (userInfo.getUserName().isEmpty() || userInfo.getEmail().isEmpty())) {
        throw new IllegalArgumentException("user name or email cannot be null or empty");
      }

      if (oauthConfig.requireRoles() && role.isEmpty()) {
        throw new UserLoginException("", "role required");
      }

      User user = null;

      // search user in service
      try {
        user = userService.getUser(userInfo.getUserName());
        logger.info("found user " + user.getUserName());

      } catch (UserNotFoundException uex) {
        // create as new with random password
        // leverage token generation function for random string
        String password = newToken();

        // build userConfig from userInfo
        user = userService.createUser(userInfo, password);
        logger.info("created user " + user.getUserName());

        // build namespace
        try {
          namespaceService.addOrUpdateHome(
              new HomePath(HomeName.getUserHomePath(user.getUserName())).toNamespaceKey(),
              new HomeConfig().setCtime(System.currentTimeMillis()).setOwner(user.getUserName()));
        } catch (Exception e) {
          userService.deleteUser(user.getUserName(), user.getVersion());

          throw UserException.unsupportedError().message(
              "Unable to create user '%s'.There may already be a user with the same name but different casing",
              user.getUserName()).addContext("Cause", e.getMessage()).build();

        }
      }

      String userName = user.getUserName();
      logger.info("login user " + userName);

      // create a token for the session
      final String clientAddress = request.getRemoteAddr();
      final TokenDetails tokenDetails = tokenManager.createToken(user.getUserName(), clientAddress);

      // Make sure the logged-in user has a home space. If not create one.
      try {
        final NamespaceService ns = dContext.getNamespaceService(SystemUser.SYSTEM_USERNAME);
        final NamespaceKey homeKey = new HomePath(HomeName.getUserHomePath(user.getUserName()))
            .toNamespaceKey();
        try {
          ns.getHome(homeKey);
        } catch (NamespaceNotFoundException nnfe) {
          // create home
          ns.addOrUpdateHome(homeKey,
              new HomeConfig().setCtime(System.currentTimeMillis()).setOwner(user.getUserName()));
        }
      } catch (NamespaceException ex) {
        logger.error("Failed to make sure the user has home space setup.", ex);
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new GenericErrorMessage(ex.getMessage()))
            .build();
      }

      final OptionManager opt = dContext.getOptionManager();
      SessionPermissions perms = new SessionPermissions(opt.getOption(SupportService.USERS_UPLOAD),
          opt.getOption(SupportService.USERS_DOWNLOAD), opt.getOption(SupportService.USERS_EMAIL),
          opt.getOption(SupportService.USERS_CHAT));

      // build an "internal" session
      // if required check oauth token for role, otherwise
      // flag admin=false for all oauth users
      boolean isAdmin = false;
      if (oauthConfig.requireRoles()) {
        isAdmin = "admin".equals(role);
      }

      UserLoginSession login = new UserLoginSession(tokenDetails.token, user.getUserName(), user.getFirstName(),
          user.getLastName(), tokenDetails.expiresAt, user.getEmail(), user.getUID().getId(), isAdmin,
          user.getCreatedAt(), support.getClusterId().getIdentity(), support.getClusterId().getCreated(),
          true, DremioVersionInfo.getVersion(), perms);

      String response = buildResponse(login);

      return Response.ok().entity(response).type(MediaType.TEXT_HTML).build();
    } catch (IllegalArgumentException | UserLoginException | UserNotFoundException e) {
      return Response.status(UNAUTHORIZED).entity(new GenericErrorMessage(e.getMessage())).build();

    } catch (IOException | InterruptedException | ExecutionException | NullPointerException e) {
      logger.error("server error: "+e.getMessage());
      return Response.serverError().build();

    }

  }

  /*
   * Helpers
   */

  private User createUser(String email, String firstName, String lastName, String userName) {
    long now = System.currentTimeMillis();
    return SimpleUser.newBuilder().setEmail(email).setUserName(userName).setFirstName(firstName)
        .setLastName(lastName).setModifiedAt(now).setCreatedAt(now).build();

  }

  private User getUserInfo(JsonObject json) {
    String email = "";
    String userName = "";
    String firstName = "";
    String lastName = "";

    if (json.has("email")) {
      email = json.get("email").getAsString();
    }
    if (json.has("username")) {
      userName = json.get("username").getAsString();

    } else if (json.has("user_name")) {
      userName = json.get("user_name").getAsString();

    } else if (json.has("preferred_username")) {
      userName = json.get("preferred_username").getAsString();

    } else if (json.has("given_name")) {
      userName = json.get("given_name").getAsString();
    } else {
      // use email as fallback
      userName = email;
    }

    return createUser(email, firstName, lastName, userName);
  }

  private String buildResponse(UserLoginSession login)
      throws JsonGenerationException, JsonMappingException, IOException {
    // ugly, direct write html code for userInfo injection and redirect
    // TODO use a proper template or update user interface in dac-ui

    String redirectUrl = oauthConfig.getCallbackUrl() + "/reload";

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(login);

    StringWriter out = new StringWriter();

    out.write("<!doctype html>\n");
    out.write("<html lang=en><head><meta charset=utf-8>");
    out.write("<script>function load(){window.localStorage.setItem(\"user\",\""
        + StringEscapeUtils.escapeEcmaScript(json) + "\")");
    out.write(",self.location=\"" + redirectUrl + "\"" + "}window.onload=load;</script>");
    out.write("</head><body></html>");
    out.write("\n");

    return out.toString();

  }

  // Copy Dremio TokenManager function for random string
  // From
  // https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
  // ... This works by choosing 130 bits from a cryptographically secure random
  // bit generator, and encoding
  // them in base-32. 128 bits is considered to be cryptographically strong, but
  // each digit in a base 32
  // number can encode 5 bits, so 128 is rounded up to the next multiple of 5 ...
  // Why 32? Because 32 = 2^5;
  // each character will represent exactly 5 bits, and 130 bits can be evenly
  // divided into characters.
  private String newToken() {
    return new BigInteger(130, generator).toString(32);
  }

  protected class OAuthApi extends DefaultApi20 {

    private OAuthConfig config;

    public OAuthApi(OAuthConfig config) {
      this.config = config;
    }

    @Override
    public String getAccessTokenEndpoint() {
      return config.getTokenUrl();
    }

    @Override
    protected String getAuthorizationBaseUrl() {
      return config.getAuthorizationUrl();
    }

  }

}
