package rest;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.BSON;
import org.bson.Document;

import com.google.gson.Gson;

import dbClasses.FriendshipDatabase;
import dbClasses.UserDatabase;
import model.Friendship;
import model.FriendshipStatus;
import model.Group;
import model.NotificationDTO;
import model.NotificationType;
import model.User;

@LocalBean
@Path("/notify")
@Stateless
public class NotificationRest {
	
	@Inject
	private UserDatabase userDatabase;
	
	@Inject
	private FriendshipDatabase friendshipDatabase;
	
	@Inject
	private PushNotifications wsNotifications;
	
	@GET
	@Path("{username}/notifyFriendshipStart/friendship/{fid}")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response notifyWantedFriend(@PathParam("username") String userName, @PathParam("fid") String fid) {
		System.out.println(fid); 
		
		Document foundSender = (Document) friendshipDatabase.getCollection().find(new Document("id", fid)).first();
		
		Gson gson = new Gson();
		Friendship f = gson.fromJson(foundSender.toJson(), Friendship.class);
		 Document foundSender1 = (Document) userDatabase.getCollection().find(new Document("username", f.getSender())).first();
	        Document foundReciver = (Document) userDatabase.getCollection().find(new Document("username", f.getReciever())).first();   

		System.out.println("NOTIFAJUME");
		updateFriendship(FriendshipStatus.PENDING, foundSender1, foundReciver, userName);
		return Response.status(Response.Status.OK).build();
		
	}
	
	@POST
	@Path("{username}/notifyFriendshipEnd")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response notifyFriendshipEnd(@PathParam("username") String userName, Friendship deleted) {
		Document foundSender = (Document) userDatabase.getCollection().find(new Document("username", deleted.getSender())).first();
		Document foundReciver = (Document) userDatabase.getCollection().find(new Document("username", deleted.getSender())).first();
		
		if(foundReciver==null||foundSender==null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}else {
			updateFriendship(FriendshipStatus.DELETED, foundSender, foundReciver, userName);
			return null;
		}
	}
	
	private void updateFriendship(FriendshipStatus f, Document foundSender, Document foundReciver, String userName) {
		Gson gson = new Gson();
	      	User person1 = gson.fromJson(foundSender.toJson(), User.class);   
	      	User person2 = gson.fromJson(foundReciver.toJson(), User.class);  
	      	String username;
	      	if(person1.getUsername().equals(userName))
	      		username=person2.getUsername();
	      	else 
	      		username = person1.getUsername();
	      	
		NotificationDTO n = new NotificationDTO();
		n.setRecieverId(userName);
	
		switch (f) {
		case ACCEPTED:
			n.setType(NotificationType.ACCEPTED);
			break;
		case BLOCKED:
			n.setType(NotificationType.REMOVED);
			
			break;
		case DECLINED:
			n.setType(NotificationType.REMOVED);
	
			break;
		case DELETED:
			n.setType(NotificationType.REMOVED);
	
			break;
		case PENDING:
			n.setType(NotificationType.PENDING);
			break;

		default:
			n.setType(NotificationType.ACCEPTED);
			break;
		}
		
		n.setUserId(username);
		wsNotifications.pushNotification(n);
		
	}
	
	@POST
	@Path("{username}/notifyFriendshipStateChange")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response notifyFriendshiopChanged(@PathParam("userName") String userName, Friendship updated) {
		Document foundSender = (Document) userDatabase.getCollection().find(new Document("username", updated.getSender())).first();
		Document foundReciver = (Document) userDatabase.getCollection().find(new Document("username", updated.getSender())).first();
		
		if(foundReciver==null||foundSender==null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}else {
			updateFriendship(updated.getStatus(), foundSender, foundReciver, userName);
			return null;
		}
	}
	
	private void groupUpdate(String userName, String groupId, NotificationType type) {
		NotificationDTO n = new NotificationDTO();
		n.setRecieverId(userName);
		n.setGroupId(groupId);
		n.setType(type);
		wsNotifications.pushNotification(n);
	}
	
	@POST
	@Path("{userName}/notifyNewGroup")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response notifyNewGroup(@PathParam("userName") String userName, Group g) {
		
		groupUpdate(userName, g.getId(), NotificationType.GROUPADD);
		
		return null;
	}
	
	@POST
	@Path("{userName}/notifyEndGroup")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response notifyEndGroup(@PathParam("userName") String userName, Group g) {
		
		groupUpdate(userName, g.getId(), NotificationType.GROUPREMOVE);
		
		return null;
	}
	
	@POST
	@Path("{username}/notifyNewGroupMember")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response notifyNewGroupMember(@PathParam("username") String userName, Group g) {
		groupUpdate(userName, g.getId(), NotificationType.GROUPNEWUSER);
		return null;
	}
	
	@POST
	@Path("{username}/notifyRemovedUser")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response notifyRemovedUser(@PathParam("username") String userName, Group g) {
		groupUpdate(userName, g.getId(), NotificationType.GROUPREMOVE);
		return null;
	}

}
