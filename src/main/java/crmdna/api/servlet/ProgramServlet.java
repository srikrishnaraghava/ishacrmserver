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
import java.util.*;

@SuppressWarnings("UnusedParameters")
public class ProgramServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private void getOngoingSessions(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        List<SessionProp> sessionProps =
            Program.getOngoingSessions(client, ServletUtils.getIntParam(request, "dateYYYYMMDD"),
                login);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(sessionProps));
    }

    private void query(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        List<ProgramProp> programProps =
            Program.query(client, ServletUtils.getIntParam(request, "startYYYYMMDD"),
                ServletUtils.getIntParam(request, "endYYYYMMDD"),
                ServletUtils.getLongParamsAsSet(request, "programTypeId"),
                ServletUtils.getLongParamsAsSet(request, "groupId"), null, null);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProps));
    }

    private void queryDetailed(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        List<ProgramProp> programProps =
            Program.query(client, ServletUtils.getIntParam(request, "startYYYYMMDD"),
                ServletUtils.getIntParam(request, "endYYYYMMDD"),
                ServletUtils.getLongParamsAsSet(request, "programTypeId"),
                ServletUtils.getLongParamsAsSet(request, "groupId"), null, null);

        for (ProgramProp programProp : programProps) {
            programProp.regSummary =
                Registration.getSummary(client, programProp.programId, User.SUPER_USER);
        }

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProps));
    }

    private void get(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        ProgramProp programProp =
            Program.safeGet(client, ServletUtils.getLongParam(request, "programId")).toProp(client);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProp));
    }

    private void getAllProgramTypes(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        List<ProgramTypeProp> props = ProgramType.getAll(client);
        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));
    }

    private void getAllPractices(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        List<PracticeProp> props = Practice.getAll(client);
        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));
    }

    private void getAllTeachers(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        List<TeacherProp> props = Teacher.getAll(client);
        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));
    }

    private void getAllVenues(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        List<VenueProp> props = Venue.getAll(client);
        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));
    }

    private void create(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

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

        update(client, programProp, request, response);
    }

    private void update(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        ProgramProp programProp =
            Program.update(client, ServletUtils.getLongParam(request, "programId"),
                ServletUtils.getLongParam(request, "venueId"),
                ServletUtils.getLongParam(request, "teacherId"),
                ServletUtils.getIntParam(request, "startYYYYMMDD"),
                ServletUtils.getIntParam(request, "endYYYYMMDD"), 1, "",
                ServletUtils.getDoubleParam(request, "fee"), Utils.Currency.SGD, login);

        update(client, programProp, request, response);
    }

    private void setDisabled(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        ProgramProp programProp =
            Program.setDisabled(client, ServletUtils.getLongParam(request, "programId"),
                ServletUtils.getBoolParam(request, "disabled"));

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(programProp));
    }

    private void update(String client, ProgramProp programProp,
        HttpServletRequest request, HttpServletResponse response) throws IOException {

        programProp = Program.setSpecialInstruction(client, programProp.programId,
            ServletUtils.getStrParam(request, "specialInstruction"));

        List<String> batch1SessionTimings = new ArrayList<>();
        batch1SessionTimings.addAll(ServletUtils.getStringParamsAsSet(request, "batch1SessionTimings"));
        Collections.sort(batch1SessionTimings);

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
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String client = request.getParameter("client");
        if (client == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FULLY_SPECIFIED));
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils.setJson(response,
                new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
            return;
        }

        String login = ServletUtils.getLogin(request);
        try {
            switch(action) {
                case "getOngoingSessions": getOngoingSessions(client, login, request, response);
                    break;
                case "query": query(client, login, request, response);
                    break;
                case "queryDetailed": queryDetailed(client, login, request, response);
                    break;
                case "get": get(client, login, request, response);
                    break;
                case "getAllProgramTypes": getAllProgramTypes(client, login, request, response);
                    break;
                case "getAllPractices": getAllPractices(client, login, request, response);
                    break;
                case "getAllTeachers": getAllTeachers(client, login, request, response);
                    break;
                case "getAllVenues": getAllVenues(client, login, request, response);
                    break;
                case "create": create(client, login, request, response);
                    break;
                case "update": update(client, login, request, response);
                    break;
                case "setDisabled": setDisabled(client, login, request, response);
                    break;
                default:
                    ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
            }
        } catch (Exception ex) {
            ServletUtils.setJson(response, APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(request).login(login)));
        }
    }
}
