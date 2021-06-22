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
package com.dremio.dac.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.SecurityContext;

import com.dremio.common.exceptions.ExecutionSetupException;
import com.dremio.dac.annotations.APIResource;
import com.dremio.dac.annotations.Secured;
import com.dremio.dac.model.spaces.HomeName;
import com.dremio.dac.service.catalog.CatalogServiceHelper;
import com.dremio.dac.service.tenant.MultiTenantServiceHelper;
import com.dremio.service.namespace.NamespaceException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/**
 * Catalog API resource.
 */
@APIResource
@Secured
@RolesAllowed({"user", "admin"})
@Path("/catalog")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class CatalogResource {
  private final CatalogServiceHelper catalogServiceHelper;
  private final SecurityContext securityContext;

  @Inject
  public CatalogResource(CatalogServiceHelper catalogServiceHelper, @Context SecurityContext securityContext) {
    this.catalogServiceHelper = catalogServiceHelper;
    this.securityContext = securityContext;
  }

  @GET
  public ResponseList<? extends CatalogItem> listTopLevelCatalog(@QueryParam("include") final List<String> include) {
    List<CatalogItem> topLevelItems = new ArrayList<>();
    for (CatalogItem item : catalogServiceHelper.getTopLevelCatalogItems(include)) {
      if (isAuthorized("user", item.getPath().get(0))) {
        topLevelItems.add(item);
      }
    }

    return new ResponseList<>(topLevelItems);
  }

  @GET
  @Path("/{id}")
  public CatalogEntity getCatalogItem(@PathParam("id") String id,
                                      @QueryParam("include") final List<String> include) throws NamespaceException {
    Optional<CatalogEntity> entity = catalogServiceHelper.getCatalogEntityById(id, include);

    if (!entity.isPresent()) {
      throw new NotFoundException(String.format("Could not find entity with id [%s]", id));
    }

    if (!isAuthorized("user", getCatalogEntityRoot(entity.get()))) {
      throw new ForbiddenException(String.format("User not authorized to access entity with id [%s].", id));
    }

    return entity.get();
  }

  @POST
  public CatalogEntity createCatalogItem(CatalogEntity entity) throws NamespaceException, BadRequestException {
    String role = "user";
    if (entity instanceof Space) {
      Space space = (Space)entity;
      String prefixedName = MultiTenantServiceHelper.prefixResourceWithTenant(securityContext, space.getName());
      entity = new Space(space.getId(), prefixedName, space.getTag(), space.getCreatedAt(), space.getChildren());
    } else if (entity instanceof Source) {
      String prefixedName = MultiTenantServiceHelper.prefixResourceWithTenant(securityContext, ((Source)entity).getName());
      ((Source)entity).setName(prefixedName);
      role = "admin";
    }

    if (!isAuthorized(role, getCatalogEntityRoot(entity))) {
      throw new ForbiddenException(String.format("User not authorized to create entity with this path. %s",
        MultiTenantServiceHelper.getMessageWithTenant(securityContext.getUserPrincipal().getName())));
    }
    try {
      return catalogServiceHelper.createCatalogItem(entity);
    } catch (UnsupportedOperationException e) {
      throw new BadRequestException(e.getMessage());
    } catch (ExecutionSetupException e) {
      throw new InternalServerErrorException(e);
    }
  }

  @POST
  @Path("/{id}")
  public Dataset promoteToDataset(Dataset dataset, @PathParam("id") String id) throws NamespaceException, BadRequestException {
    if (!isAuthorized("user", getCatalogEntityRoot(dataset))) {
      throw new ForbiddenException(String.format("User not authorized to access dataset with id [%s].", id));
    }

    try {
      return catalogServiceHelper.promoteToDataset(id, dataset);
    } catch (UnsupportedOperationException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @PUT
  @Path("/{id}")
  public CatalogEntity updateCatalogItem(CatalogEntity entity, @PathParam("id") String id) throws NamespaceException, BadRequestException {
    String role = entity instanceof Source ? "admin" : "user";
    if (!isAuthorized(role, getCatalogEntityRoot(entity))) {
      throw new ForbiddenException(String.format("User not authorized to access entity with id [%s].", id));
    }

    try {
      return catalogServiceHelper.updateCatalogItem(entity, id);
    } catch (IllegalArgumentException e) {
      throw new NotFoundException(e.getMessage());
    } catch (ExecutionSetupException | IOException e) {
      throw new InternalServerErrorException(e);
    } catch (UnsupportedOperationException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @DELETE
  @Path("/{id}")
  public void deleteCatalogItem(@PathParam("id") String id, @QueryParam("tag") String tag) throws NamespaceException, BadRequestException {
    Optional<CatalogEntity> entity = catalogServiceHelper.getCatalogEntityById(id, null);
    String role = (entity.isPresent() && entity.get() instanceof Source) ? "admin" : "user";
    if (entity.isPresent() && !isAuthorized(role, getCatalogEntityRoot(entity.get()))) {
      throw new ForbiddenException(String.format("User not authorized to access entity with id [%s].", id));
    }

    try {
      catalogServiceHelper.deleteCatalogItem(id, tag);
    } catch (IllegalArgumentException e) {
      throw new NotFoundException(e.getMessage());
    } catch (UnsupportedOperationException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @POST
  @Path("/{id}/refresh")
  public void refreshCatalogItem(@PathParam("id") String id) throws NamespaceException {
    Optional<CatalogEntity> entity = catalogServiceHelper.getCatalogEntityById(id, null);
    if (entity.isPresent() && !isAuthorized("user", getCatalogEntityRoot(entity.get()))) {
      throw new ForbiddenException(String.format("User not authorized to access entity with id [%s].", id));
    }

    try {
      catalogServiceHelper.refreshCatalogItem(id);
    } catch (IllegalArgumentException e) {
      throw new NotFoundException(e.getMessage());
    } catch (UnsupportedOperationException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @POST
  @Path("/{id}/metadata/refresh")
  public MetadataRefreshResponse refreshCatalogItemMetadata(@PathParam("id") String id,
                                                            @QueryParam("deleteWhenMissing") Boolean delete,
                                                            @QueryParam("forceUpdate") Boolean force,
                                                            @QueryParam("autoPromotion") Boolean promotion) throws NamespaceException {
    Optional<CatalogEntity> entity = catalogServiceHelper.getCatalogEntityById(id, null);
    if (entity.isPresent() && !isAuthorized("user", getCatalogEntityRoot(entity.get()))) {
      throw new ForbiddenException(String.format("User not authorized to access entity with id [%s].", id));
    }

    try {
      boolean changed = false;
      boolean deleted = false;
      switch(catalogServiceHelper.refreshCatalogItemMetadata(id, delete, force, promotion)) {
        case CHANGED:
          changed = true;
          break;
        case UNCHANGED:
          break;
        case DELETED:
          changed = true;
          deleted = true;
          break;
        default:
          throw new IllegalStateException();
      }

      return new MetadataRefreshResponse(changed, deleted);
    } catch (IllegalArgumentException e) {
      throw new NotFoundException(e.getMessage());
    } catch (UnsupportedOperationException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @GET
  @Path("/by-path/{segment:.*}")
  public CatalogEntity getCatalogItemByPath(@PathParam("segment") List<PathSegment> segments) throws NamespaceException, BadRequestException {
    List<String> pathList = new ArrayList<>();

    for (PathSegment segment : segments) {
      pathList.add(segment.getPath());
    }

    Optional<CatalogEntity> entity = catalogServiceHelper.getCatalogEntityByPath(pathList);

    if (!entity.isPresent()) {
      throw new NotFoundException(String.format("Could not find entity with path [%s]", pathList));
    }

    if (!isAuthorized("user", getCatalogEntityRoot(entity.get()))) {
      throw new ForbiddenException(String.format("User not authorized to access entity with path [%s]", pathList));
    }

    return entity.get();
  }

  @GET
  @Path("/search")
  public ResponseList<CatalogItem> search(@QueryParam("query") String query) throws NamespaceException {
    List<CatalogItem> searchResult = new ArrayList<>();
    for (CatalogItem item : catalogServiceHelper.search(query)) {
      if (isAuthorized("user", item.getPath().get(0))) {
        searchResult.add(item);
      }
    }

    ResponseList<CatalogItem> catalogItems = new ResponseList<>(searchResult);

    return catalogItems;
  }

  /**
   * MetadataRefreshResponse class
   */
  public static class MetadataRefreshResponse {
    private final boolean changed;
    private final boolean deleted;

    @JsonCreator
    public MetadataRefreshResponse(
      @JsonProperty("changed") boolean changed,
      @JsonProperty("deleted") boolean deleted
    ) {
      this.changed = changed;
      this.deleted = deleted;
    }

    public boolean getChanged() {
      return changed;
    }

    public boolean getDeleted() {
      return deleted;
    }
  }

  private String getCatalogEntityRoot(CatalogEntity entity) {
    String root = null;
    if (entity instanceof Space) {
      root = ((Space)entity).getName();
    } else if (entity instanceof Source) {
      root = ((Source)entity).getName();
    } else if (entity instanceof Home) {
      root = ((Home)entity).getName();
    } else if (entity instanceof Folder) {
      root = ((Folder)entity).getPath().get(0);
    } else if (entity instanceof File) {
      root = ((File)entity).getPath().get(0);
    } else if (entity instanceof Dataset) {
      root = ((Dataset)entity).getPath().get(0);
    }
    return root;
  }

  private boolean isAuthorized(String role, String root) {
    //short-circuit if the root is the home of the current user
    if (HomeName.getUserHomePath(securityContext.getUserPrincipal().getName()).getName().equals(root)) {
      return true;
    }

    String userTenant = MultiTenantServiceHelper.getUserTenant(securityContext.getUserPrincipal().getName());
    String resourceTenant = MultiTenantServiceHelper.getResourceTenant(root);
    return MultiTenantServiceHelper.hasPermission(securityContext, role, userTenant, resourceTenant);
  }
}
