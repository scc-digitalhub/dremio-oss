/*
classe statica

 */
package com.dremio.dac.service.tenant;

import com.dremio.service.users.User;

/**
 * Multi-tenancy Helper
 */
public class MultiTenantServiceHelper {

  /**
   * Extract tenant from username, which has the syntax <username>@<tenant>.
   * The tenant is found after the last "@" (username may contain more than one "@", e.g. if it is an email).
   */
  public static String getUserTenant(String username) {
    String tenant = null;
    //split username by @ and take last substring
    if (username != null) {
      String[] substrings = username.split("@");
      if (substrings.length > 0) {
        tenant = substrings[substrings.length -1];
      }
    }

    return tenant;
  }

  /**
   * Get username from user, then extract tenant
   */
  public static String getUserTenant(User user){
    return getUserTenant(user.getUserName());
  }

  /**
   * Split path, then get resource tenant
   */
  public static String getResourceTenant(String path) {
    return getResourceTenant(path.split("\."));
  }

  /**
   * Extract tenant from resource path root, which has the syntax <tenant>__<root>.
   */
  public static String getResourceTenant(String[] path) {
    String tenant = null;
    //split root by __ and take last substring
    if (path != null && path.length > 0) {
      String[] substrings = path[0].split("__");
      if (substrings.length > 1) {
        tenant = substrings[0];
      } else {
        //there is no tenant as root prefix
        tenant = "";
      }
    }

    return tenant;
  }

  /**
   * Case-insensitive comparison between tenants
   */
  public static boolean isSameTenant(String userTenant, String resourceTenant) {
    return userTenant.equalsIgnoreCase(resourceTenant);
  }
}
