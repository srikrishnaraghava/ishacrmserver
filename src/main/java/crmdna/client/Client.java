package crmdna.client;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Work;
import crmdna.common.Constants;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.mail2.Mandrill;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.UserCore;
import crmdna.user.UserEntity;
import crmdna.user.UserProp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;
import static crmdna.common.OfyService.ofyCrmDna;

public class Client {
    private static ClientProp create(String name, String displayName) {

        name = name.toLowerCase();
        // truncate name to 8 char
        if (name.length() > 8)
            name = name.substring(0, 8);

        ClientEntity clientEntity = ofyCrmDna().load().type(ClientEntity.class).id(name).now();

        if (null != clientEntity)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a client with name [" + name + "]");

        clientEntity = new ClientEntity();
        clientEntity.name = name;
        clientEntity.displayName = displayName;
        ofyCrmDna().save().entity(clientEntity).now();

        return clientEntity.toProp();
    }

    public static ClientProp create(String name) {
        return create(name, name);
    }

    public static ClientEntity safeGet(String client) {
        if (null == client)
            throw new APIException("Client is null")
                    .status(Status.ERROR_RESOURCE_NOT_FOUND);
        ClientEntity entity = ofyCrmDna().load().type(ClientEntity.class).id(client).now();
        if (null == entity)
            throw new APIException("Client [" + client + "] not found")
                    .status(Status.ERROR_RESOURCE_NOT_FOUND);


        return entity;
    }

    public static ClientEntity get(String client) {
        if (null == client)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Client is null");

        return ofyCrmDna().load().type(ClientEntity.class).id(client).now();
    }

    public static Map<String, ClientEntity> getEntities(Set<String> clients) {
        ensureNotNull(clients, "clients is null");

        return ofyCrmDna().load().type(ClientEntity.class).ids(clients);
    }

    public static List<ClientProp> getAll() {
        List<ClientEntity> entities =
                ofyCrmDna().load().type(ClientEntity.class).orderKey(false).list();

        List<ClientProp> props = new ArrayList<>();
        for (ClientEntity entity : entities) {
            props.add(entity.toProp());
        }

        return props;
    }

    public static ClientProp updateDisplayName(String client, String newDisplayName) {

        ClientEntity clientEntity = ofyCrmDna().load().type(ClientEntity.class).id(client).now();

        if (null == clientEntity)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "Client [" + client + "] does not exist");

        clientEntity.displayName = newDisplayName;
        ofyCrmDna().save().entity(clientEntity).now();

        return clientEntity.toProp();
    }

    public static void ensureValid(String client) {

        if ((client != null) && client.equals(Constants.CLIENT_CRMDNA))
            return;

        safeGet(client);
    }

    static void addUser(String client, String email) {
        email = email.toLowerCase();

        ClientEntity clientEntity = safeGet(client);

        Ref<ClientEntity> clientRef = Ref.create(clientEntity);

        CrmDnaUserEntity crmdnaUserEntity =
                ofyCrmDna().load().type(CrmDnaUserEntity.class).id(email).now();

        if (null == crmdnaUserEntity) {
            crmdnaUserEntity = new CrmDnaUserEntity();
            crmdnaUserEntity.email = email;
            crmdnaUserEntity.clients.add(clientRef);
        } else {
            crmdnaUserEntity.clients.add(clientRef);
        }

        ofyCrmDna().save().entity(crmdnaUserEntity);

        // add as a valid user in the client's namespace
        UserEntity userEntity = ofy(client).load().type(UserEntity.class).id(email).now();

        if (null == userEntity) {
            userEntity = new UserEntity();
            userEntity.email = email;
            ofy(client).save().entity(userEntity);
        }
    }

    static void deleteUser(String client, String email) {
        email = email.toLowerCase();

        ClientEntity clientEntity = safeGet(client);

        Ref<ClientEntity> clientRef = Ref.create(clientEntity);

        CrmDnaUserEntity cmrdnaUserEntity =
                ofyCrmDna().load().type(CrmDnaUserEntity.class).id(email).now();

        if (null != cmrdnaUserEntity) {
            boolean found = cmrdnaUserEntity.clients.remove(clientRef);
            if (found)
                ofyCrmDna().save().entity(cmrdnaUserEntity);

            // remove from client's namespace
            UserEntity userEntity = ofy(client).load().type(UserEntity.class).id(email).now();

            if (null != userEntity) {
                ofy(client).delete().entity(userEntity).now();
            }
        }
    }

    static List<UserEntity> getAllUsers(String client) {
        return ofy(client).load().type(UserEntity.class).orderKey(true).list();
    }

    public static EmailConfig addOrDeleteAllowedEmailSender(final String client,
                                                            final String fromEmail, final String fromName, final boolean add, String login) {

        Client.ensureValid(client);

        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_CLIENT_CONTACT_EMAIL);

        Utils.ensureValidEmail(fromEmail);

        EmailConfig emailConfigProp = ofy(client).transact(new Work<EmailConfig>() {

            @Override
            public EmailConfig run() {
                ClientEntity clientEntity = Client.safeGet(client);

                if (add) {
                    ensureNotNull(fromName, "fromName is null");
                    ensure(!fromName.isEmpty(), "fromName is null");

                    clientEntity.allowedSenderVsName.put(fromEmail.toLowerCase(), fromName);
                } else
                    clientEntity.allowedSenderVsName.remove(fromEmail.toLowerCase());

                ofy(client).save().entity(clientEntity).now();

                EmailConfig emailConfigProp = new EmailConfig();
                emailConfigProp.mandrillApiKey = clientEntity.mandrillApiKey;
                emailConfigProp.allowedFromEmailVsName = clientEntity.allowedSenderVsName;

                return emailConfigProp;
            }
        });

        if (UserCore.isSuperUser(login))
            return emailConfigProp;

        UserProp userProp = User.safeGet(client, login).toProp(client);

        // mask the key if user does not have access
        if (userProp.clientLevelPrivileges.contains(ClientLevelPrivilege.VIEW_API_KEY.toString()))
            emailConfigProp.mandrillApiKey = EmailConfig.TEXT_API_KEY_MASKED;

        return emailConfigProp;
    }

    public static EmailConfig getEmailConfig(String client, String login) {

        ClientEntity clientEntity = safeGet(client);

        User.ensureValidUser(client, login);

        EmailConfig emailConfig = new EmailConfig();
        emailConfig.mandrillApiKey = clientEntity.mandrillApiKey;
        emailConfig.allowedFromEmailVsName = clientEntity.allowedSenderVsName;
        emailConfig.contactEmail = clientEntity.contactEmail;
        emailConfig.contactName = clientEntity.contactName;

        if (UserCore.isSuperUser(login))
            return emailConfig;

        UserProp userProp = User.safeGet(client, login).toProp(client);

        // mask the key if user does not have access
        if (userProp.clientLevelPrivileges.contains(ClientLevelPrivilege.VIEW_API_KEY.toString()))
            emailConfig.mandrillApiKey = EmailConfig.TEXT_API_KEY_MASKED;

        return emailConfig;
    }

    public static ClientProp setContactNameAndEmail(String client, String email, String name,
                                                    String login) {

        ClientEntity clientEntity = safeGet(client);

        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_CLIENT_CONTACT_EMAIL);

        ensureNotNull(email, "email is null");
        ensure(!email.isEmpty(), "email is empty");

        Utils.ensureValidEmail(email);

        ensureNotNull(name, "name is null");
        ensure(!name.isEmpty(), "name is empty");

        ensure(email.length() < 100, "name should be lesser than 100 characters");

        clientEntity.contactEmail = email;
        clientEntity.contactName = name;

        clientEntity.allowedSenderVsName.put(email, name);

        ofyCrmDna().save().entity(clientEntity).now();

        ensureNotNull(clientEntity.contactEmail);

        return clientEntity.toProp();
    }

    public static EmailConfig setMandrillApiKey(final String client, final String apiKey, String login) {

        ensure(UserCore.isSuperUser(login));
        Mandrill.ensureValidApiKey(apiKey);

        return ofy(client).transact(new Work<EmailConfig>() {

            @Override
            public EmailConfig run() {
            ClientEntity clientEntity = safeGet(client);
            clientEntity.mandrillApiKey = apiKey;
            ofy(client).save().entity(clientEntity).now();

            EmailConfig emailConfigProp = new EmailConfig();
            emailConfigProp.mandrillApiKey = clientEntity.mandrillApiKey;
            emailConfigProp.allowedFromEmailVsName = clientEntity.allowedSenderVsName;

            return emailConfigProp;
            }
        });
    }

    public static String safeGetSenderNameFromEmail(String client, String senderEmail) {
        Client.ensureValid(client);

        ensureNotNull(senderEmail, "sender email is null");

        EmailConfig emailConfig = Client.getEmailConfig(client, User.SUPER_USER);

        String senderName = emailConfig.allowedFromEmailVsName.get(senderEmail.toLowerCase());

        if (senderName == null) {
            String message =
                    "[" + senderEmail + "] is not an allowed email sender for client [" + client + "]";
            throw new APIException(message).status(Status.ERROR_RESOURCE_INCORRECT);
        }

        return senderName;
    }
}
