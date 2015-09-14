package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.QueryKeys;
import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.client.Client;
import crmdna.common.StopWatch;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.common.config.ConfigCRMDNA;
import crmdna.datamigration.DataMigration;
import crmdna.member.Member;
import crmdna.member.MemberEntity;
import crmdna.member.MemberLoader;
import crmdna.member.MemberQueryCondition;
import crmdna.participant.ParticipantEntity;
import crmdna.program.Program;
import crmdna.programtype.ProgramType;
import crmdna.registration.RegistrationEntity;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.UserCore;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.OfyService.ofy;

@Api(name = "developersOnly")
public class DevelopersOnly {
    public APIResponse purgeAllMemberData(@Named("client") String client,
                                          @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            crmdna.user.User.ensureClientLevelPrivilege(client, login,
                    ClientLevelPrivilege.PURGE_MEMBER_DATA);

            // ensure dev mode is set
            boolean devMode = ConfigCRMDNA.get().toProp().devMode;
            if (!devMode)
                throw new APIException().status(Status.ERROR_OPERATION_NOT_ALLOWED).message(
                        "This operation is allowed only in dev mode");

            QueryKeys<MemberEntity> memberQueryKeys = ofy(client).load().type(MemberEntity.class).keys();

            QueryKeys<ParticipantEntity> participantQueryKeys =
                    ofy(client).load().type(ParticipantEntity.class).keys();

            QueryKeys<RegistrationEntity> registrationQueryKeys =
                    ofy(client).load().type(RegistrationEntity.class).keys();

            List<Key<MemberEntity>> memberKeys = memberQueryKeys.list();
            List<Key<ParticipantEntity>> participantKeys = participantQueryKeys.list();
            List<Key<RegistrationEntity>> registrationKeys = registrationQueryKeys.list();


            ofy(client).delete().keys(memberKeys);
            ofy(client).delete().keys(participantKeys);
            ofy(client).delete().keys(registrationKeys);

            StringBuilder builder = new StringBuilder();
            builder.append("Deleted [" + memberQueryKeys.list().size() + "] members. ");
            builder.append("Deleted [" + participantQueryKeys.list().size() + "] participants. ");
            builder.append("Deleted [" + registrationQueryKeys.list().size() + "] registrants. ");

            return new APIResponse().status(Status.SUCCESS).message(builder.toString());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(name = "reconstituteKey", httpMethod = HttpMethod.GET)
    public APIResponse reconstituteKey(@Named("webSafeKeyString") String webSafeKey,
                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {
        try {

            StopWatch stopWatch = StopWatch.createStarted();
            com.google.appengine.api.datastore.Key key = KeyFactory.stringToKey(webSafeKey);

            Map<String, String> map = new HashMap<>();
            map.put("Kind", key.getKind());
            map.put("AppId", key.getAppId());
            map.put("Name", key.getName());
            map.put("Namespace", key.getNamespace());
            map.put("Parent", key.getNamespace());
            map.put("Id", key.getId() + "");

            return new APIResponse()
                    .status(Status.SUCCESS)
                    .message(
                            "Data store key successfully reconstituted in [" + stopWatch.nsElapsed() + "] ns. ")
                    .object(map).processingTimeInMS(stopWatch.msElapsed());


        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }

    @ApiMethod(name = "testKeyToIDConversion", httpMethod = HttpMethod.GET)
    public APIResponse testKeyToIDConversion(@Named("client") String client,
                                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            crmdna.user.User.ensureValidUser(client, login);

            StringBuilder message = new StringBuilder();

            StopWatch stopWatch = StopWatch.createStarted();
            List<Key<MemberEntity>> keys =
                    ofy(client).load().type(MemberEntity.class).limit(1000).keys().list();

            message.append("[" + keys.size() + "] member keys loaded in [" + stopWatch.msElapsed()
                    + "] ms. ");

            List<Long> ids = new ArrayList<>();

            stopWatch = StopWatch.createStarted();
            for (Key<MemberEntity> key : keys) {
                ids.add(key.getId());
            }

            message.append("[" + ids.size() + "] ids obtained in [" + stopWatch.nsElapsed() + "] ns. ");

            List<String> keyStrings = new ArrayList<>(keys.size());
            for (Key<MemberEntity> key : keys) {
                keyStrings.add(KeyFactory.keyToString(key.getRaw()));
            }

            return new APIResponse().status(Status.SUCCESS).message(message.toString())
                    .object(keyStrings);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(name = "testMaxDatastoreAsyncQueryLimit", httpMethod = HttpMethod.GET)
    public APIResponse testMaxDatastoreAsyncQueryLimit(@Named("client") String client,
                                                       @Nullable @Named("numQueriesDefault10000") Integer numQueries,
                                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {

            if (numQueries == null)
                numQueries = 10000;

            ensure(numQueries > 0, "Invalid numQueries [" + numQueries + "]. Should be positive.");

            login = Utils.getLoginEmail(user);

            crmdna.user.User.ensureValidUser(client, login);

            Set<String> emails = new HashSet<>(numQueries);
            for (int i = 0; i < numQueries; i++) {
                if (i == 0)
                    emails.add("sathya.t@ishafoundation.org");
                else if (i == 1)
                    emails.add("thulasidhar@gmail.com");
                else
                    emails.add("email" + i + "@invalid.com");
            }

            StopWatch sw = StopWatch.createStarted();
            Map<String, Long> emailVsMemberId = Member.getMemberIdFromEmail(client, emails);

            return new APIResponse()
                    .status(Status.SUCCESS)
                    .object(emailVsMemberId)
                    .message(
                            "Completed [" + numQueries
                                    + "] async datastore queries to get memberId from email for [" + numQueries
                                    + "] email(s)").processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(name = "copyAllEntitiesToAnotherClient", httpMethod = HttpMethod.POST)
    public APIResponse copyAllEntitiesToAnotherClient(@Named("sourceClient") String sourceClient,
                                                      @Named("targetClient") String targetClient,
                                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {


        String login = null;

        try {

            login = Utils.getLoginEmail(user);

            DataMigration.copyAllEntitiesToAnotherClient(sourceClient, targetClient, login);

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace,
                    new RequestInfo().client("multiple").req(req).login(login));
        }
    }

    // To be removed after bhairavi data migration
    @ApiMethod(name = "resaveAllPrograms", httpMethod = HttpMethod.POST)
    public APIResponse resaveAllPrograms(@Named("clientEnum") ClientEnum clientEnum,
                                         @Nullable @Named("clientIfOther") String clientOther,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {


        String login = null;
        String client = EndpointUtils.getClient(clientEnum, clientOther);

        try {

            login = Utils.getLoginEmail(user);

            Program.resaveAll(client, login);

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace,
                    new RequestInfo().client("multiple").req(req).login(login));
        }
    }

    // To be removed after bhairavi data migration
    @ApiMethod(name = "resaveAllProgramTypes", httpMethod = HttpMethod.POST)
    public APIResponse resaveAllProgramTypes(@Named("clientEnum") ClientEnum clientEnum,
                                             @Nullable @Named("clientIfOther") String clientOther,
                                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {


        String login = null;
        String client = EndpointUtils.getClient(clientEnum, clientOther);

        try {

            login = Utils.getLoginEmail(user);

            ProgramType.resaveAll(client, login);

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace,
                    new RequestInfo().client("multiple").req(req).login(login));
        }
    }

    @ApiMethod(name = "resaveAllMembers", httpMethod = HttpMethod.POST)
    public APIResponse resaveAllMembers(@Named("clientEnum") ClientEnum clientEnum,
                                        @Nullable @Named("clientIfOther") String clientOther,
                                        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {


        String login = null;
        String client = EndpointUtils.getClient(clientEnum, clientOther);

        try {

            login = Utils.getLoginEmail(user);

            Client.ensureValid(client);

            ensure(UserCore.isSuperUser(login), "Allowed only for super user");

            List<MemberEntity> all = ofy(client).load().type(MemberEntity.class).list();

            ofy(client).save().entities(all).now();

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace,
                    new RequestInfo().client("multiple").req(req).login(login));
        }
    }

    @ApiMethod(name = "populateSubUnsubGroupIds", httpMethod = HttpMethod.POST)
    public APIResponse populateSubUnsubGroupIds(@Named("clientEnum") ClientEnum clientEnum,
                                        @Nullable @Named("clientOther") String clientOther,
                                        @Named("firstChar") Utils.SingleChar firstChar,
                                        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {


        String login = null;
        String client = EndpointUtils.getClient(clientEnum, clientOther);

        try {

            login = Utils.getLoginEmail(user);

            Client.ensureValid(client);
            ensure(UserCore.isSuperUser(login), "Allowed only for super user");

            MemberQueryCondition mqc = new MemberQueryCondition(client, 20000);
            mqc.nameFirstChar = firstChar.toString();
            List<MemberEntity> memberEntities = MemberLoader.queryEntities(mqc, login);

            Set<Long> memberIds = new HashSet<>();
            for (MemberEntity memberEntity : memberEntities) {
                memberIds.add(memberEntity.getId());
            }

            Set<Long> changedMemberIds = Member.populateSubUnsubGroupIds(client, memberIds, login);
            return new APIResponse().status(Status.SUCCESS)
                    .message("[" + changedMemberIds.size() + "] member entities changed").object(changedMemberIds);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace,
                    new RequestInfo().client(client).req(req).login(login));
        }
    }
}
