package crmdna.registration;

import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.sequence.Sequence;
import crmdna.user.User;

import java.util.*;

import static crmdna.common.OfyService.ofy;

public class Discount {

    public static DiscountEntity get(String client, String discountCode) {
        return get(client, discountCode, null);
    }

    public static DiscountEntity get(String client, String discountCode, Long programTypeId) {
        Client.ensureValid(client);

        Query<DiscountEntity> q = ofy(client).load().type(DiscountEntity.class).filter("discountCode", discountCode);

        List<Long> programTypeIds = new ArrayList<>();
        programTypeIds.add(programTypeId);
        if (programTypeId != null) {
            q = q.filter("programTypeIds in", programTypeIds);
        }

        List<DiscountEntity> entities = q.list();

        return (entities.size() > 0) ? entities.get(0) : null;
    }

    public static DiscountProp createDiscountCode(String client, String discountCode,
                                                  Set<Long> programTypeIds, int validTillYYYYMMDD,
                                                  Double percentage, Double amount, String login) {

        User.ensureClientLevelPrivilege(client, login, User.ClientLevelPrivilege.UPDATE_PROGRAM_TYPE);

        DateUtils.ensureFormatYYYYMMDD(validTillYYYYMMDD);
        if (validTillYYYYMMDD < DateUtils.toYYYYMMDD(new Date())) {
            Utils.throwIncorrectSpecException("Validity should not be in the past");
        }

        DiscountEntity entity = get(client, discountCode);
        if (entity != null) {
            Utils.throwAlreadyExistsException("Discount Code [" + discountCode + "] already exists");
        }

        if ((percentage == null) && (amount == null)) {
            Utils.throwIncorrectSpecException("Either percentage or amount must be specified");
        }

        if ((percentage != null) && (amount != null)) {
            Utils.throwIncorrectSpecException("Both percentage and amount must not be specified");
        }

        entity = new DiscountEntity();
        entity.discountId = Sequence.getNext(client, Sequence.SequenceType.DISCOUNT);
        entity.discountCode = discountCode;
        entity.programTypeIds = programTypeIds;
        entity.validTillYYYYMMDD = validTillYYYYMMDD;
        entity.percentage = (percentage != null) ? percentage : -1;
        entity.amount = (amount != null) ? amount : -1;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static DiscountProp updateDiscountCode(String client, String discountCode,
                                                  Set<Long> newProgramTypeIds, Integer newValidTillYYYYMMDD,
                                                  Double newPercentage, Double newAmount, String login) {

        User.ensureClientLevelPrivilege(client, login, User.ClientLevelPrivilege.UPDATE_PROGRAM_TYPE);

        if (newValidTillYYYYMMDD != null) {
            DateUtils.ensureFormatYYYYMMDD(newValidTillYYYYMMDD);

            if (newValidTillYYYYMMDD < DateUtils.toYYYYMMDD(new Date())) {
                Utils.throwIncorrectSpecException("Validity should not be in the past");
            }
        }

        boolean changed = false;
        DiscountEntity entity = get(client, discountCode);
        if (entity == null) {
            Utils.throwNotFoundException("Discount Code [" + discountCode + "] does not exist");
        }

        if ((newPercentage != null) && (newAmount != null)) {
            Utils.throwIncorrectSpecException("Both percentage and amount must not be specified");
        }

        if (newProgramTypeIds != null) {
            entity.programTypeIds = newProgramTypeIds;
            changed = true;
        }

        if (newValidTillYYYYMMDD != null) {
            entity.validTillYYYYMMDD = newValidTillYYYYMMDD;
            changed = true;
        }

        if (newPercentage != null) {
            entity.percentage = newPercentage;
            changed = true;
        }

        if (newAmount != null) {
            entity.amount = newAmount;
            changed = true;
        }

        if (changed) {
            ofy(client).save().entity(entity).now();
        }

        return entity.toProp();
    }

    public static DiscountProp applyDiscount(String client, String discountCode, long programId) {

        ProgramProp programProp = Program.safeGet(client, programId).toProp(client);

        DiscountEntity entity = get(client, discountCode, programProp.programTypeProp.programTypeId);
        if (entity == null) {
            Utils.throwNotFoundException("Invalid Discount Code");
        }

        DiscountProp prop = entity.toProp();
        if (prop.validTillYYYYMMDD < DateUtils.toYYYYMMDD(new Date())) {
            Utils.throwIncorrectSpecException("Expired Discount Code");
        }

        if (prop.amount > 0) {
            prop.discountedAmount = programProp.fee - prop.amount;
        } else if (prop.percentage > 0) {
            prop.discountedAmount = programProp.fee  - (programProp.fee * prop.percentage / 100);
        } else {
            prop.discountedAmount = programProp.fee;
        }

        return prop;
    }

}
