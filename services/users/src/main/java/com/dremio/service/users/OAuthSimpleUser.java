package com.dremio.service.users;

import java.util.List;
import java.util.Objects;

import com.dremio.service.users.proto.UID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Copy of SimpleUser with roles added
 */
public final class OAuthSimpleUser implements User {
  /**
   * A builder for {@code User}
   */
  public static final class Builder {
    private UID uid;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private long createdAt;
    private long modifiedAt;
    private String version;
    private String extra;
    private List<String> roles;

    private Builder() {}

    private Builder(UID uid, String userName, String firstName, String lastName, String email,
        long createdAt, long modifiedAt, String version, String extra, List<String> roles) {
      super();
      this.uid = uid;
      this.userName = userName;
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
      this.createdAt = createdAt;
      this.modifiedAt = modifiedAt;
      this.version = version;
      this.extra = extra;
      this.roles = roles;
    }

    public UID getUID() {
      return uid;
    }

    public Builder setUID(UID uid) {
      this.uid = uid;
      return this;
    }

    public String getUserName() {
      return userName;
    }

    public Builder setUserName(String userName) {
      this.userName = userName;
      return this;
    }

    public String getFirstName() {
      return firstName;
    }

    public Builder setFirstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public String getLastName() {
      return lastName;
    }

    public Builder setLastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public String getEmail() {
      return email;
    }

    public Builder setEmail(String email) {
      this.email = email;
      return this;
    }

    public long getCreatedAt() {
      return createdAt;
    }

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public long getModifiedAt() {
      return modifiedAt;
    }

    public Builder setModifiedAt(long modifiedAt) {
      this.modifiedAt = modifiedAt;
      return this;
    }

    public String getVersion() {
      return version;
    }

    public Builder setVersion(String version) {
      this.version = version;
      return this;
    }

    public String getExtra() {
      return extra;
    }

    public Builder setExtra(String extra) {
      this.extra = extra;
      return this;
    }

    public List<String> getRoles() {
      return roles;
    }

    public Builder setRoles(List<String> roles) {
      this.roles = roles;
      return this;
    }

    public OAuthSimpleUser build() {
      return new OAuthSimpleUser(uid, userName, firstName, lastName, email, createdAt, modifiedAt, version, extra, roles);
    }
  }

  private final UID uid;
  private final String userName;
  private final String firstName;
  private final String lastName;
  private final String email;
  private final long createdAt;
  private final long modifiedAt;
  private final String version;
  private final String extra;
  private final List<String> roles;

  @JsonCreator
  private OAuthSimpleUser(
      @JsonProperty("uid") UID uid,
      @JsonProperty("userName") String userName,
      @JsonProperty("firstName") String firstName,
      @JsonProperty("lastName") String lastName,
      @JsonProperty("email") String email,
      @JsonProperty("createdAt") long createdAt,
      @JsonProperty("modifiedAt") long modifiedAt,
      @JsonProperty("version") String version,
      @JsonProperty("extra") String extra,
      @JsonProperty("roles") List<String> roles) {
    super();
    this.uid = uid;
    this.userName = userName;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.createdAt = createdAt;
    this.modifiedAt = modifiedAt;
    this.version = version;
    this.extra = extra;
    this.roles = roles;
  }

  @Override
  public UID getUID() {
    return uid;
  }

  @Override
  public String getUserName() {
    return userName;
  }
  @Override
  public String getFirstName() {
    return firstName;
  }

  @Override
  public String getLastName() {
    return lastName;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public long getCreatedAt() {
    return createdAt;
  }

  @Override
  public long getModifiedAt() {
    return modifiedAt;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getExtra() {
    return extra;
  }

  public List<String> getRoles() {
    return roles;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uid, userName, firstName, lastName, email, createdAt, modifiedAt, version, extra, roles);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (!(obj instanceof OAuthSimpleUser)) {
      return false;
    }

    OAuthSimpleUser other = (OAuthSimpleUser) obj;
    return Objects.equals(uid, other.uid)
      && Objects.equals(userName, other.userName)
      && Objects.equals(firstName, other.firstName)
      && Objects.equals(lastName, other.lastName)
      && Objects.equals(email, other.email)
      && Objects.equals(createdAt, other.createdAt)
      && Objects.equals(modifiedAt, other.modifiedAt)
      && Objects.equals(version, other.version)
      && Objects.equals(extra, other.extra)
      && Objects.equals(roles, other.roles);
  }

  @Override
  public String toString() {
    return "OAuthSimpleUser [uid=" + uid + ", userName=" + userName + ", firstName=" + firstName + ", lastName=" + lastName
        + ", email=" + email + ", createdAt=" + createdAt + ", modifiedAt=" + modifiedAt
        + ", version=" + version + ", extra=" + extra + ", roles=" + roles + "]";
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(User user) {
    List<String> roles = null;
    if (user instanceof OAuthSimpleUser) {
      roles = ((OAuthSimpleUser)user).getRoles();
    }
    return new Builder(user.getUID(), user.getUserName(), user.getFirstName(), user.getLastName(),
        user.getEmail(), user.getCreatedAt(), user.getModifiedAt(), user.getVersion(), user.getExtra(), roles);
  }
}
