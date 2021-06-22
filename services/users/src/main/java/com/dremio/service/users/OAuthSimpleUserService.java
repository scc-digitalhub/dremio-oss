package com.dremio.service.users;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;

import com.dremio.datastore.api.LegacyKVStoreProvider;
import com.dremio.service.users.proto.UserConfig;

/**
 * Extension of SimpleUserService with user roles
 */
public class OAuthSimpleUserService extends SimpleUserService {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OAuthSimpleUserService.class);

  @Inject
  public OAuthSimpleUserService(Provider<LegacyKVStoreProvider> kvStoreProvider) {
    super(kvStoreProvider);
  }

  private void merge(UserConfig newConfig, UserConfig oldConfig) {
    if (newConfig.getRolesList() == null) {
      newConfig.setRolesList(oldConfig.getRolesList());
    }
  }

  //Override update methods to also update roles
  @Override
  public User updateUser(final User userGroup, final String authKey) throws IOException, UserNotFoundException {
    UserConfig userConfig = toUserConfig(userGroup);
    final String userName = userConfig.getUserName();
    final User oldUser = getUser(userName);
    final UserConfig oldUserConfig = toUserConfig(oldUser);
    merge(userConfig, oldUserConfig);

    final User newUser = super.updateUser(fromUserConfig(userConfig), authKey);
    return newUser;
  }

  @Override
  public User updateUserName(final String oldUserName, final String newUserName, final User userGroup, final String authKey)
    throws IOException, IllegalArgumentException, UserNotFoundException {
    final User oldUser = getUser(oldUserName);
    final UserConfig oldUserConfig = toUserConfig(oldUser);
    //leave validation of new username to super method
    UserConfig userConfig = toUserConfig(userGroup);
    merge(userConfig, oldUserConfig);

    final User newUser = super.updateUserName(oldUserName, newUserName, fromUserConfig(userConfig), authKey);
    return newUser;
  }

  @Override
  protected UserConfig toUserConfig(User user) {
    UserConfig newConfig = new UserConfig()
      .setUid(user.getUID())
      .setUserName(user.getUserName())
      .setFirstName(user.getFirstName())
      .setLastName(user.getLastName())
      .setEmail(user.getEmail())
      .setCreatedAt(user.getCreatedAt())
      .setModifiedAt(user.getModifiedAt())
      .setTag(user.getVersion());
    if (user instanceof OAuthSimpleUser) {
      newConfig.setRolesList(((OAuthSimpleUser)user).getRoles());
    }
    return newConfig;
  }

  @Override
  protected User fromUserConfig(UserConfig userConfig) {
    return OAuthSimpleUser.newBuilder()
      .setUID(userConfig.getUid())
      .setUserName(userConfig.getUserName())
      .setFirstName(userConfig.getFirstName())
      .setLastName(userConfig.getLastName())
      .setEmail(userConfig.getEmail())
      .setCreatedAt(userConfig.getCreatedAt())
      .setModifiedAt(userConfig.getModifiedAt())
      .setVersion(userConfig.getTag())
      .setRoles(userConfig.getRolesList())
      .build();
  }
}
