package crmdna.api.servlet;

import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.group.Group;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.program.SessionProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.registration.Registration;
import crmdna.teacher.Teacher;
import crmdna.teacher.Teacher.TeacherProp;
import crmdna.user.User;
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProgramServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else {

            String client = request.getParameter("client");
            if (client == null)
                client = "isha";

            String login = ServletUtils.getLogin(request);

            try {
                if (action.equals("getOngoingSessions")) {

                    List<SessionProp> sessionProps =
                            Program.getOngoingSessions(client, ServletUtils.getIntParam(request, "dateYYYYMMDD"),
                                    login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(sessionProps));

                } else if (action.equals("query")) {
                    List<ProgramProp> programProps =
                            Program.query(client, ServletUtils.getIntParam(request, "startYYYYMMDD"),
                                    ServletUtils.getIntParam(request, "endYYYYMMDD"),
                                    ServletUtils.getLongParamsAsSet(request, "programTypeId"),
                                    ServletUtils.getLongParamsAsSet(request, "groupId"), null, null);
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProps));

                } else if (action.equals("queryDetailed")) {
                    List<ProgramProp> _programProps =
                            Program.query(client, ServletUtils.getIntParam(request, "startYYYYMMDD"),
                                    ServletUtils.getIntParam(request, "endYYYYMMDD"),
                                    ServletUtils.getLongParamsAsSet(request, "programTypeId"), null, null, null);

                    ArrayList<ProgramProp> programProps = new ArrayList<>();

                    for (ProgramProp programProp : _programProps) {
                        programProp.regSummary =
                                Registration.getSummary(client, programProp.programId, User.SUPER_USER);

                        programProps.add(programProp);
                    }

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProps));

                } else if (action.equals("get")) {
                    ProgramProp programProp =
                            Program.safeGet(client, ServletUtils.getLongParam(request, "programId")).toProp(
                                    client);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProp));
                } else if (action.equals("getAllProgramTypes")) {

                    List<ProgramTypeProp> props = ProgramType.getAll(client);
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else if (action.equals("getAllPractices")) {

                    List<PracticeProp> props = Practice.getAll(client);
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else if (action.equals("getAllTeachers")) {

                    List<TeacherProp> props = Teacher.getAll(client);
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else if (action.equals("getAllVenues")) {

                    List<VenueProp> props = Venue.getAll(client);
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else if (action.equals("create")) {
                    long groupId = Group.safeGetByIdOrName(
                        client, ServletUtils.getStrParam(request, "group")).toProp().groupId;
                    ProgramProp programProp =
                            Program.create(client, groupId,
                                    ServletUtils.getLongParam(request, "programTypeId"),
                                    ServletUtils.getLongParam(request, "venueId"),
                                    ServletUtils.getLongParam(request, "teacherId"),
                                    ServletUtils.getIntParam(request, "startYYYYMMDD"),
                                    ServletUtils.getIntParam(request, "endYYYYMMDD"), 1, "",
                                    ServletUtils.getDoubleParam(request, "fee"), Utils.Currency.SGD, login);

                    programProp =
                            Program.setSpecialInstruction(client, programProp.programId,
                                    ServletUtils.getStrParam(request, "specialInstruction"));

                    List<String> batch1SessionTimings = new ArrayList<>();
                    for (int i = 0; i < 15; i++) {
                        String timings = ServletUtils.getStrParam(request, "batch1SessionTimings[" + i + "]");
                        if (timings != null) {
                            batch1SessionTimings.add(timings);
                        }
                    }

                    programProp =
                            Program.setSessionTimings(client, programProp.programId, batch1SessionTimings, null,
                                    null, null, null);

                    programProp =
                            Program.setMaxParticipants(client, programProp.programId,
                                    ServletUtils.getIntParam(request, "maxParticipants"));

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProp));

                } else if (action.equals("update")) {

                    ProgramProp programProp =
                            Program.update(client, ServletUtils.getLongParam(request, "programId"),
                                    ServletUtils.getLongParam(request, "venueId"),
                                    ServletUtils.getLongParam(request, "teacherId"),
                                    ServletUtils.getIntParam(request, "startYYYYMMDD"),
                                    ServletUtils.getIntParam(request, "endYYYYMMDD"), 1, "",
                                    ServletUtils.getDoubleParam(request, "fee"), Utils.Currency.SGD, login);

                    List<String> batch1SessionTimings = new ArrayList<>();
                    for (int i = 0; i < 15; i++) {
                        String timings = ServletUtils.getStrParam(request, "batch1SessionTimings[" + i + "]");
                        if (timings != null) {
                            batch1SessionTimings.add(timings);
                        }
                    }

                    programProp =
                            Program.setSessionTimings(client, programProp.programId, batch1SessionTimings, null,
                                    null, null, null);

                    programProp =
                            Program.setMaxParticipants(client, programProp.programId,
                                    ServletUtils.getIntParam(request, "maxParticipants"));

                    programProp =
                            Program.setDisabled(client, programProp.programId,
                                    ServletUtils.getBoolParam(request, "disabled"));

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProp));

                } else if (action.equals("setDisabled")) {

                    ProgramProp programProp =
                            Program.setDisabled(client, ServletUtils.getLongParam(request, "programId"),
                                    ServletUtils.getBoolParam(request, "disabled"));

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProp));

                } else {
                    ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }
            } catch (Exception ex) {
                ServletUtils.setJson(response, APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }
}
