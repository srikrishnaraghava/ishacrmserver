package crmdna.program;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.common.DateUtils;
import crmdna.common.Utils.Currency;
import crmdna.group.Group;
import crmdna.programtype.ProgramType;
import crmdna.teacher.Teacher;
import crmdna.venue.Venue;

import java.util.List;

@Entity
@Cache
public class ProgramEntity {

    @Id
    long programId;

    @Index
    long programTypeId;

    @Index
    int startYYYYMMDD;
    @Index
    int endYYYYMMDD;

    @Index
    long venueId;

    @Index
    long teacherId;

    @Index
    long groupId;

    String description; // free text
    int numBatches;
    int maxParticipants;
    boolean disabled;

    double fee;
    Currency ccy;

    List<String> batch1SessionTimings;
    List<String> batch2SessionTimings;
    List<String> batch3SessionTimings;
    List<String> batch4SessionTimings;
    List<String> batch5SessionTimings;

    String specialInstruction; // free text

    public ProgramProp toProp(String client) {
        //TODO: use batch get to make this more efficient, better to move it into a helper class and support batch operations
        ProgramProp programProp = new ProgramProp();
        programProp.programId = programId;
        programProp.programTypeProp = ProgramType.safeGet(client, programTypeId).toProp(client);
        programProp.groupProp = Group.safeGet(client, groupId).toProp();
        programProp.startYYYYMMDD = startYYYYMMDD;
        programProp.endYYYYMMDD = endYYYYMMDD;
        programProp.venueProp = Venue.safeGet(client, venueId).toProp();
        programProp.teacherProp = Teacher.safeGet(client, teacherId).toProp();
        programProp.description = description;
        programProp.numBatches = numBatches;
        programProp.fee = fee;
        programProp.ccy = ccy;
        programProp.disabled = disabled;

        programProp.batch1SessionTimings = batch1SessionTimings;
        programProp.batch2SessionTimings = batch2SessionTimings;
        programProp.batch3SessionTimings = batch3SessionTimings;
        programProp.batch4SessionTimings = batch4SessionTimings;
        programProp.batch5SessionTimings = batch5SessionTimings;

        programProp.specialInstruction = specialInstruction;
        programProp.maxParticipants = maxParticipants;

        programProp.name =
                programProp.programTypeProp.displayName + " "
                        + DateUtils.getDurationAsString(startYYYYMMDD, endYYYYMMDD) + " @ "
                        + programProp.venueProp.displayName;

        programProp.shortName =
                programProp.programTypeProp.displayName + " "
                        + DateUtils.getDurationAsString(startYYYYMMDD, endYYYYMMDD) + " @ "
                        + programProp.venueProp.shortName;

        // eg: IshaKriya 3 Feb 2014 @ Woodlands CC (Singapore)

        return programProp;
    }
}
