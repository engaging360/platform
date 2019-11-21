package org.exoplatform.platform.common.rest.services.SuggestPeoplePortlet;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.RuntimeDelegate;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Path("/homepage/intranet/people/")
@Produces("application/json")
public class PeopleRestServices implements ResourceContainer {

  private static final int NUMBER_OF_SUGGESTIONS = 10;
  private static Log log = ExoLogger.getLogger(PeopleRestServices.class);

  private static final CacheControl cacheControl;

  private static final String DEFAULT_AVATAR = "/eXoSkin/skin/images/themes/default/social/skin/ShareImages/UserAvtDefault.png";

  private UserACL userACL;

  private IdentityManager identityManager;

  private  RelationshipManager relationshipManager;


  static {
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
  }
  public PeopleRestServices(UserACL userACL, IdentityManager identityManager,  RelationshipManager relationshipManager) {
    this.userACL = userACL;
    this.identityManager = identityManager;
    this.relationshipManager =  relationshipManager;

  }

  @GET
  @Path("contacts/pending")
  public Response getPending(@Context SecurityContext sc, @Context UriInfo uriInfo) {

    try {

      String userId = getUserId(sc, uriInfo);
      if (userId == null) {
        return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
      }

      Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);
      List<Relationship> relations = relationshipManager.getPending(identity);

      JSONArray jsonArray = new JSONArray();

      for (Relationship relation : relations) {

        Identity senderId = relation.getSender();
        Profile senderProfile = senderId.getProfile();
        Identity receiverId = relation.getReceiver();
        Profile receiverProfile = receiverId.getProfile();

        JSONObject json = new JSONObject();
        json.put("senderName", senderProfile.getFullName());
        json.put("senderId", senderId.getId());
        json.put("receiverName", receiverProfile.getFullName());
        json.put("receiverId", receiverId.getId());
        json.put("status", relation.getStatus());
        jsonArray.put(json);
      }

      return Response.ok(jsonArray.toString(), MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();

    } catch (Exception e) {
      log.error("Error in people pending rest service: " + e.getMessage(), e);
      return Response.ok("error").cacheControl(cacheControl).build();
    }
  }


  @GET
  @Path("contacts/incoming")
  public Response getIncoming(@Context SecurityContext sc, @Context UriInfo uriInfo) {

    try {

      String userId = getUserId(sc, uriInfo);
      if (userId == null) {
        return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
      }

      Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);
      List<Relationship> relations = relationshipManager.getIncoming(identity);

      JSONArray jsonArray = new JSONArray();

      for (Relationship relation : relations) {

        Identity senderId = relation.getSender();
        String avatar = senderId.getProfile().getAvatarImageSource();
        if (avatar == null) {
          avatar = "/eXoSkin/skin/images/system/Avatar.gif";
        }

        JSONObject json = new JSONObject();
        json.put("senderName", senderId.getProfile().getFullName());
        json.put("relationId", relation.getId());
        json.put("avatar", avatar);
        json.put("profile", senderId.getProfile().getUrl());
        jsonArray.put(json);
      }

      return Response.ok(jsonArray.toString(), MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
    } catch (Exception e) {
      log.error("Error in people incoming rest service: " + e.getMessage(), e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
    }

  }


  //confirm a request

  @GET
  @Path("contacts/confirm/{relationId}")
  public Response confirm(@PathParam("relationId") String relationId, @Context SecurityContext sc, @Context UriInfo uriInfo) {

    try {

      String userId = getUserId(sc, uriInfo);
      if (userId == null) {
        return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
      }

      Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);

      log.debug("request accepted.");

      relationshipManager.confirm(relationshipManager.getRelationshipById(relationId));

      return Response.ok("Confirmed", MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
    } catch (Exception e) {
      log.error("Error in people accept rest service: " + e.getMessage(), e);
      return Response.ok("error").cacheControl(cacheControl).build();
    }
  }

  @GET
  @Path("contacts/deny/{relationId}")
  public Response deny(@PathParam("relationId") String relationId, @Context SecurityContext sc, @Context UriInfo uriInfo) {

    try {

      String userId = getUserId(sc, uriInfo);
      if (userId == null) {
        return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
      }

      Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);

      relationshipManager.deny(relationshipManager.getRelationshipById(relationId));

      return Response.ok("Denied", MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();

    } catch (Exception e) {
      log.error("Error in people deny rest service: " + e.getMessage(), e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
    }
  }

  @GET
  @Path("contacts/connect/{relationId}")
  public Response connect(@PathParam("relationId") String relationId, @Context SecurityContext sc, @Context UriInfo uriInfo) {

    try {

      String userId = getUserId(sc, uriInfo);
      if (userId == null) {
        return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
      }

      Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);

      relationshipManager.invite(identity, identityManager.getIdentity(relationId));

      return Response.ok("Connected", MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
    } catch (Exception e) {
      log.error("Error in people connect rest service: " + e.getMessage(), e);
      return Response.ok("Error", MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
    }

  }

  @GET
  @Path("contacts/suggestions")
  public Response getSuggestions(@Context SecurityContext sc, @Context UriInfo uriInfo) {

    try {

      String userId = getUserId(sc, uriInfo);
      if (userId == null) {
        return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
      }

      Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, false);

      ListAccess<Identity> connectionList = relationshipManager.getConnections(identity);
      int size = connectionList.getSize();
      Map<Identity, Integer> connectionsSuggestions;
      if (size > 0) {
        connectionsSuggestions = relationshipManager.getSuggestions(identity, 20, 50, 10);
        if (connectionsSuggestions.size() == 1 && connectionsSuggestions.keySet().iterator().next().getRemoteId().equals(userACL.getSuperUser())) {
          // The only suggestion is the super user so we clear the suggestion list
          connectionsSuggestions = Collections.emptyMap();
        }
      } else {
        connectionsSuggestions = Collections.emptyMap();
      }

      JSONObject jsonGlobal = new JSONObject();
      JSONArray jsonArray = new JSONArray();
      Map<Identity, Integer> suggestions = new LinkedHashMap<>(connectionsSuggestions);
      suggestions = suggestions.entrySet()
              .stream()
              .sorted((Map.Entry.<Identity, Integer>comparingByValue().reversed()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

      if (suggestions.size() < NUMBER_OF_SUGGESTIONS) {
        // Returns the last users
        List<Identity> identities = identityManager.getLastIdentities(NUMBER_OF_SUGGESTIONS - suggestions.size());
        for (Identity id : identities) {
          if (identity.equals(id) || relationshipManager.get(identity, id) != null)
            continue;
          suggestions.putIfAbsent(id, 0);
        }
      }
      for (Entry<Identity, Integer> suggestion : suggestions.entrySet()) {
        Identity id = suggestion.getKey();

        if (id.getRemoteId().equals(userACL.getSuperUser())) continue;
        JSONObject json = new JSONObject();
        Profile socialProfile = id.getProfile();
        String avatar = socialProfile.getAvatarUrl();
        if (avatar == null) {
          avatar = DEFAULT_AVATAR;
        }
        String position = socialProfile.getPosition();
        if (position == null) {
          position = "";
        }
        json.put("username", id.getRemoteId());
        json.put("suggestionName", socialProfile.getFullName());
        json.put("suggestionId", id.getId());
        json.put("avatar", avatar);
        json.put("profile", socialProfile.getUrl());
        json.put("title", position);

        //set mutual friend number
        json.put("number", suggestion.getValue());
        json.put("createdDate",socialProfile.getCreatedTime());
        jsonArray.put(json);
      }
      jsonGlobal.put("items",jsonArray);
      jsonGlobal.put("noConnections", size);
      jsonGlobal.put("username", userId);
      return Response.ok(jsonGlobal.toString(), MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
    } catch (Exception e) {
      log.error("Error in getting GS progress: " + e.getMessage(), e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cacheControl).build();
    }

  }


  private String getUserId(SecurityContext sc, UriInfo uriInfo) {
    try {
      return sc.getUserPrincipal().getName();
    } catch (Exception e) {
      return null;
    }
  }

}
