package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.program.SessionProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

@Api(name = "program")
public class ProgrameApi {

    @ApiMethod(path = "createProgram", httpMethod = HttpMethod.POST)
    public APIResponse createProgram(@Named("client") String client,
                                     @Named("groupId") long groupId, @Named("programTypeId") long programTypeId,
                                     @Named("venueId") long venueId, @Named("teacherId") long teacherId,
                                     @Named("startYYYYMMDD") int startYYYYMMDD, @Named("endYYYYMMDD") int endYYYYMMDD,
                                     @Named("numBatches") int numBatches, @Nullable @Named("description") String description,
                                     @Named("fees") Double fees, @Named("feeCurrency") Currency ccy,
                                     @Named("maxParticipants") Integer maxParticipants,
                                     @Nullable @Named("specialInstructionsFreeText") String specialInstruction,
                                     @Nullable @Named("batch1SessionTimingsFreeText") List<String> batch1SessionTimings,
                                     @Nullable @Named("batch2SessionTimingsFreeText") List<String> batch2SessionTimings,
                                     @Nullable @Named("batch3SessionTimingsFreeText") List<String> batch3SessionTimings,
                                     @Nullable @Named("batch4SessionTimingsFreeText") List<String> batch4SessionTimings,
                                     @Nullable @Named("batch5SessionTimingsFreeText") List<String> batch5SessionTimings,
                                     @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            if (fees == null)
                fees = 0.0;

            ProgramProp programProp =
                    Program.create(client, groupId, programTypeId, venueId, teacherId, startYYYYMMDD,
                            endYYYYMMDD, numBatches, description, fees, ccy, Utils.getLoginEmail(user));

            programProp =
                    Program.setSpecialInstruction(client, programProp.programId, specialInstruction);

            programProp =
                    Program.setSessionTimings(client, programProp.programId, batch1SessionTimings,
                            batch2SessionTimings, batch3SessionTimings, batch4SessionTimings,
                            batch5SessionTimings);

            programProp = Program.setMaxParticipants(client, programProp.programId, maxParticipants);

            return new APIResponse().status(Status.SUCCESS).object(programProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "queryProgram", httpMethod = HttpMethod.GET)
    public APIResponse queryProgram(@Named("client") String client,
                                    @Nullable @Named("startYYYYMMDD") Integer startYYYYMMDD,
                                    @Nullable @Named("endYYYYMMDD") Integer endYYYYMMDD,
                                    @Nullable @Named("programTypeIds") Set<Long> programTypeIds,
                                    @Nullable @Named("groupIds") Set<Long> groupIds, @Nullable @Named("venueId") Long venueId,
                                    @Nullable @Named("limit") Integer limit,
                                    @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        if (client == null)
            client = "isha";

        try {
            List<ProgramProp> programProps =
                    Program.query(client, startYYYYMMDD, endYYYYMMDD, programTypeIds, groupIds, venueId, limit);

            return new APIResponse().status(Status.SUCCESS).object(programProps);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "updateProgram", httpMethod = HttpMethod.POST)
    public APIResponse updateProgram(@Named("client") String client,
                                     @Named("programId") long programId, @Nullable @Named("newVenueId") Long newVenueId,
                                     @Nullable @Named("newTeacherId") Long newTeacherId,
                                     @Nullable @Named("newStartYYYYMMDD") Integer newStartYYYYMMDD,
                                     @Nullable @Named("newEndYYYYMMDD") Integer newEndYYYYMMDD,
                                     @Nullable @Named("newNumBatches") Integer newNumBatches,
                                     @Nullable @Named("newMaxParticipants") Integer newMaxParticipants,
                                     @Nullable @Named("disabled") Boolean disabled,
                                     @Nullable @Named("newDescription") String newDescription,
                                     @Nullable @Named("fees") Double fees, @Nullable @Named("feeCurrency") Currency ccy,
                                     @Nullable @Named("specialInstructionsFreeText") String specialInstruction,
                                     @Nullable @Named("batch1SessionTimingsFreeText") List<String> batch1SessionTimings,
                                     @Nullable @Named("batch2SessionTimingsFreeText") List<String> batch2SessionTimings,
                                     @Nullable @Named("batch3SessionTimingsFreeText") List<String> batch3SessionTimings,
                                     @Nullable @Named("batch4SessionTimingsFreeText") List<String> batch4SessionTimings,
                                     @Nullable @Named("batch5SessionTimingsFreeText") List<String> batch5SessionTimings,
                                     @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            ProgramProp programProp =
                    Program.update(client, programId, newVenueId, newTeacherId, newStartYYYYMMDD,
                            newEndYYYYMMDD, newNumBatches, newDescription, fees, ccy, Utils.getLoginEmail(user));

            programProp =
                    Program.setSpecialInstruction(client, programProp.programId, specialInstruction);

            programProp =
                    Program.setSessionTimings(client, programProp.programId, batch1SessionTimings,
                            batch2SessionTimings, batch3SessionTimings, batch4SessionTimings,
                            batch5SessionTimings);

            programProp = Program.setMaxParticipants(client, programProp.programId, newMaxParticipants);
            programProp = Program.setDisabled(client, programProp.programId, disabled);

            return new APIResponse().status(Status.SUCCESS).object(programProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getOngoingSessions", httpMethod = HttpMethod.GET)
    public APIResponse getOngoingSessions(@Named("client") String client,
                                          @Named("dateYYYYMMDD") int dateYYYYMMDD,
                                          @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            List<SessionProp> sessionProps = Program.getOngoingSessions(client, dateYYYYMMDD, login);

            return new APIResponse().status(Status.SUCCESS).object(sessionProps);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    public enum IshaKriyaTeachers {
        JEAN_ONG, GOPAL_MM, MUTHU_KUMAR, SONIA_SANT, SHANTANU_SINGH, SRIRAM_SRINIVASAN, THULASIDHAR_KOSALRAM, VASANTH_NAGAPPAN, VATSALA_SRINIVASAN, NARASIMHAN, VANITHA_VISWESVARAN, SHAMLA, BAK_LIANG_LOR, SUNDARA_VADIVEL, BALAJI_SEETHARAMAN, JIANWEN, SATHYA_THILAKAN
    }

}
