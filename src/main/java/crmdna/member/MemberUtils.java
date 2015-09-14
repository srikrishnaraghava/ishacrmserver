package crmdna.member;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.gson.Gson;
import crmdna.common.DateUtils;
import crmdna.objectstore.ObjectStore;

import java.util.Date;

import static crmdna.common.AssertUtils.ensureNotNull;

public class MemberUtils {
    public static void queryAsync(MemberQueryCondition condition, String login, Integer partNumber,
        Integer totalParts, Integer resultSize) {

        ensureNotNull(condition.maxResultSize, "Max Result Size should not be null");
        if ((resultSize != null) && (totalParts == null) && (partNumber == null)) {
            totalParts = (int) Math.ceil(((double) resultSize) / condition.maxResultSize);
            partNumber = 1;
        }
        com.google.appengine.api.taskqueue.Queue queue = QueueFactory.getDefaultQueue();
        long tempAccessId = ObjectStore
            .put(condition.client, "TempAccessID", 30, ObjectStore.TimeUnit.SECONDS);
        queue.add(TaskOptions.Builder.withUrl("/member")
            .param("action", "sendReportAsEmail")
            .param("qc", new Gson().toJson(condition))
            .param("email", login)
            .param("emailAttachmentName", "MemberReport-" + DateUtils.toISOString(new Date()))
            .param("accessId", tempAccessId + "").param("partNumber", partNumber + "")
            .param("totalParts", totalParts + ""));
    }
}
