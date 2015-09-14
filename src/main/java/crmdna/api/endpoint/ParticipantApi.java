package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.appengine.api.users.User;
import crmdna.common.StopWatch;
import crmdna.common.Utils;
import crmdna.common.ValidationResultProp;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.common.contact.Contact;
import crmdna.common.contact.ContactProp;
import crmdna.email.EmailProp;
import crmdna.email.GAEEmail;
import crmdna.gspreadsheet.GSpreadSheet;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.participant.Participant;
import crmdna.participant.ParticipantProp;
import crmdna.participant.UploadReportProp;
import crmdna.program.Program;
import crmdna.program.ProgramProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Api(name = "participant")
public class ParticipantApi {

    public APIResponse uploadParticipants(@Named("client") String client,
                                          @Named("publishedSpreadSheetKey") String gsKey, @Named("programId") long programId,
                                          @Nullable @Named("numLinesExclHeader2500") Integer numLinesExclHeader,
                                          @Nullable @Named("updateMemberProfileDefaultTrue") Boolean updateProfile,
                                          @Nullable @Named("ignoreWarningsDefaultFalse") Boolean ignoreWarnings,
                                          @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            StopWatch sw = StopWatch.createStarted();

            if (numLinesExclHeader == null)
                numLinesExclHeader = 2500;

            login = Utils.getLoginEmail(user);
            List<Map<String, String>> listOfMap =
                    GSpreadSheet.getPublishedSpreasheetAsListOfMap(gsKey, numLinesExclHeader);

            if (ignoreWarnings == null)
                ignoreWarnings = false;

            if (updateProfile == null)
                updateProfile = true;

            List<ContactProp> contactDetailProps = Contact.getContactDetailsFromListOfMap(listOfMap);

            UploadReportProp prop =
                    Participant.uploadAll(client, contactDetailProps, programId, ignoreWarnings,
                            updateProfile, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(prop)
                    .processingTimeInMS(sw.msElapsed())
                    .message("Uploaded data for [" + prop.numParticipants + "] participants");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace,
                    new RequestInfo().client(client).login(login).req(req));
        }
    }

    public APIResponse validateSpreadsheet(@Named("spreadSheetKey") String gsKey,
                                           @Nullable @Named("numLinesToReadExclHeader") Integer numLinesExclHeader,
                                           @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        try {
            StopWatch sw = StopWatch.createStarted();

            if (numLinesExclHeader == null)
                numLinesExclHeader = 2500;

            List<Map<String, String>> listOfMap =
                    GSpreadSheet.getPublishedSpreasheetAsListOfMap(gsKey, numLinesExclHeader);

            List<ContactProp> contactDetailProps = Contact.getContactDetailsFromListOfMap(listOfMap);

            ValidationResultProp prop = Contact.validate(contactDetailProps);

            if (prop.hasErrors())
                return new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT).object(prop);

            return new APIResponse().status(Status.SUCCESS).object(prop)
                    .processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }

    public APIResponse deleteAllParticipants(@Named("client") String client,
                                             @Named("programId") long programId,
                                             @Nullable @Named("showStackTrace") Boolean showStackTrace, User user, HttpServletRequest req) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            StopWatch sw = StopWatch.createStarted();

            login = Utils.getLoginEmail(user);
            int numDeleted = Participant.deleteAll(client, programId, login);

            return new APIResponse().status(Status.SUCCESS).processingTimeInMS(sw.msElapsed())
                    .message("Deleted [" + numDeleted + "] participants");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).client(client)
                    .req(req));
        }
    }

    public APIResponse sendParticipantListAsEmail(@Named("client") String client,
                                                  @Named("programId") long programId, @Named("infoType") MemberInfoType memberInfoType,
                                                  @Nullable @Named("showStackTrace") Boolean showStackTrace, User user, HttpServletRequest req) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            List<ContactProp> contacts = new ArrayList<>();
            if (memberInfoType == MemberInfoType.INFO_ENTERED_DURING_PROGRAM) {
                List<ParticipantProp> participantProps = Participant.getAll(client, programId, login);

                for (ParticipantProp participantProp : participantProps) {
                    contacts.add(participantProp.contactDetail);
                }
            } else if (memberInfoType == MemberInfoType.LATEST) {

                MemberQueryCondition mqc = new MemberQueryCondition(client, 5000);
                mqc.programIds.add(programId);

                List<MemberProp> memberProps = MemberLoader.querySortedProps(mqc, login);

                for (MemberProp memberProp : memberProps) {
                    contacts.add(memberProp.contact);
                }
            } else {
                return new APIResponse().status(Status.ERROR_INVALID_INPUT).message(
                        "Invalid memberInfoType [" + memberInfoType + "]");
            }

            if (contacts.size() == 0)
                return new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                        "No participants found for program Id [" + programId + "]");

            Collections.sort(contacts);

            EmailProp emailProp = new EmailProp();

            ProgramProp programProp = Program.safeGet(client, programId).toProp(client);

            emailProp.toEmailAddresses.add(login);
            emailProp.bodyHtml = programProp.getDetailsAsHtml();
            emailProp.bodyHtml += "<br><br>Participant details are attached.";

            emailProp.csvAttachmentData = Contact.getCSV(contacts);
            emailProp.attachmentName = programProp.getNameWOVenue() + ".csv";
            emailProp.subject = "Participants list for program - " + programProp.getNameWOVenue();

            GAEEmail.send(emailProp);

            return new APIResponse().status(Status.SUCCESS).message("Email sent");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).client(client)
                    .req(req));
        }
    }

    public enum MemberInfoType {
        LATEST, INFO_ENTERED_DURING_PROGRAM
    }
}
