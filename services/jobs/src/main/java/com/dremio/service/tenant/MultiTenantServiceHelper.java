package com.dremio.service.tenant;

import javax.ws.rs.core.SecurityContext;

import com.dremio.service.users.User;

/**
 * Multi-tenancy helper class that is used by the jobs service (com.dremio.service.jobs.LocalJobsService) to verify that a user
 * submitting a query is allowed to access the specified datasets, i.e., user tenant and dataset tenant match.
 * A duplicate of this class, used by the APIs, is com.dremio.dac.service.tenant.MultiTenantServiceHelper.
 */
public class MultiTenantServiceHelper {
  public static final String DEFAULT_USER = "dremio"; //default admin username
  private static final String HOME_PREFIX = "@"; //default home prefix, copied from com.dremio.dac.model.spaces.HomeName
  /**
   * Internal source names, used by Dremio and not by users,
   * e.g. __home is the temporary path root of files while they are being uploaded.
   * These sources must be skipped during tenant check, as they are common to any tenant.
  */
  private static final String[] RESERVED_SOURCES = new String[]{"INFORMATION_SCHEMA", "sys", "__home", "__accelerator", "__jobResultsStore", "$scratch", "__datasetDownload", "__logs", "__support"};

  /**
   * Copied from com.dremio.dac.model.spaces.HomeName to be used outside DAC
   */
  public static String getUserHomePath(String username) {
    return HOME_PREFIX + username;
  }

  /**
   * Used to avoid preventing access to internal sources (e.g. when uploading a file)
   */
  public static boolean isInternalSource(String sourceName) {
    boolean internal = false;
    for (String s : RESERVED_SOURCES) {
      if (s.equals(sourceName)) {
        internal = true;
        break;
      }
    }
    return internal;
  }

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
   * Change string to array, then get resource tenant
   */
  public static String getResourceTenant(String root) {
    String[] rootArr = {root};
    return getResourceTenant(rootArr);
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

  /**
   * Check if user has the required role and its tenant matches the resource tenant
   */
  public static boolean hasPermission(SecurityContext sc, String requiredRole, String userTenant, String resourceTenant) {
    //short-circuit if user is admin
    if(sc.isUserInRole("admin")){
      return true;
    }

    if(userTenant != null && resourceTenant != null && sc.isUserInRole(requiredRole)) {
      return isSameTenant(userTenant, resourceTenant);
    } else {
      return false;
    }
  }
}
