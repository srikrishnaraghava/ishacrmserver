package crmdna.api.servlet;

import crmdna.common.DateUtils;
import crmdna.common.DateUtils.DateRange;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.interaction.Interaction;
import crmdna.interaction.Interaction.InteractionType;
import crmdna.interaction.InteractionProp;
import crmdna.interaction.InteractionQueryCondition;
import crmdna.interaction.InteractionQueryResult;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class InteractionServlet extends HttpServlet {

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
                if (action.equals("createInteraction")) {

                    String strInteractionType = ServletUtils.getStrParam(request, "interactionType");
                    InteractionType interactionType =
                            (strInteractionType != null) ? InteractionType.valueOf(strInteractionType) : null;
                    InteractionProp prop =
                            Interaction.createInteraction(client, ServletUtils.getLongParam(request, "memberId"),
                                    ServletUtils.getStrParam(request, "content"), interactionType, new Date(), null, true, login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(prop));

                } else if (action.equals("createSubInteraction")) {

                    Interaction.createSubInteraction(client,
                            ServletUtils.getLongParam(request, "interactionId"),
                            ServletUtils.getStrParam(request, "content"), new Date(), login);
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).message("Added sub interaction"));
                } else if (action.equals("deleteInteraction")) {

                    long interactionId = ServletUtils.getLongParam(request, "interactionId");
                    Interaction.deleteInteraction(client, interactionId, login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object("interaction Id [" + interactionId + "] deleted"));
                } else if (action.equals("deleteSubInteraction")) {

                    Long interactionId = ServletUtils.getLongParam(request, "interactionId");
                    Long subInteractionId = ServletUtils.getLongParam(request, "subInteractionId");
                    Interaction.deleteSubInteraction(client, interactionId, subInteractionId, login);

                    ServletUtils.setJson(response,
                            new APIResponse().status(Status.SUCCESS)
                                    .message("Sub interaction [(" + interactionId + ") " + subInteractionId + "] deleted"));

                } else if (action.equals("updateInteraction")) {

                    String strNewInteractionType = ServletUtils.getStrParam(request, "newInteractionType");
                    InteractionType newInteractionType =
                            (strNewInteractionType != null) ? InteractionType.valueOf(strNewInteractionType)
                                    : null;

                    InteractionProp prop =
                            Interaction.updateInteraction(client,
                                    ServletUtils.getLongParam(request, "interactionId"),
                                    ServletUtils.getLongParam(request, "newMemberId"), newInteractionType,
                                    ServletUtils.getStrParam(request, "newUserEmail"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(prop));

                } else if (action.equals("updateSubInteraction")) {

                    long subInteractionId = ServletUtils.getLongParam(request, "subInteractionId");
                    Interaction.updateSubInteraction(client,
                            ServletUtils.getLongParam(request, "interactionId"), subInteractionId,
                            ServletUtils.getStrParam(request, "content"), login);

                    ServletUtils.setJson(response,
                            new APIResponse().status(Status.SUCCESS)
                                    .message("Sub interaction [" + subInteractionId + "] updated"));

                } else if (action.equals("query")) {

                    String strInteractionType = ServletUtils.getStrParam(request, "interactionType");
                    InteractionType interactionType =
                            (strInteractionType != null) ? InteractionType.valueOf(strInteractionType) : null;

                    String strDateRange = ServletUtils.getStrParam(request, "dateRange");
                    DateRange dateRange = (strDateRange != null) ? DateRange.valueOf(strDateRange) : null;

                    InteractionQueryCondition qc = new InteractionQueryCondition();
                    Long memberId = ServletUtils.getLongParam(request, "memberId");
                    if (memberId != null) {
                        qc.memberIds.add(memberId);
                    }
                    Long userId = ServletUtils.getLongParam(request, "userId");
                    if (userId != null) {
                        qc.userIds.add(userId);
                    }
                    if (interactionType != null) {
                        qc.interactionTypes.add(interactionType.toString());
                    }
                    qc.end = new Date();
                    if (dateRange != null) {
                        qc.start = new Date(qc.end.getTime() - DateUtils.getMilliSecondsFromDateRange(dateRange));
                    }
                    qc.startIndex = ServletUtils.getIntParam(request, "startIndex");
                    qc.numResults = ServletUtils.getIntParam(request, "numResults");
                    InteractionQueryResult result = Interaction.query(client, qc, login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(result));

                } else {
                    ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }

            } catch (Exception ex) {
                ServletUtils.setJson(response,
                        APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }
}
