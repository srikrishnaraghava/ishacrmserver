package crmdna.datamigration;

import crmdna.attendance.CheckInEntity;
import crmdna.client.Client;
import crmdna.client.CustomFieldsEntity;
import crmdna.client.isha.IshaConfigEntity;
import crmdna.common.DummyTestEntity;
import crmdna.common.config.ConfigCRMDNAEntity;
import crmdna.counter.ShardEntity;
import crmdna.group.GroupEntity;
import crmdna.helpandsupport.ConfigHelpEntity;
import crmdna.hr.DepartmentEntity;
import crmdna.interaction.InteractionEntity;
import crmdna.inventory.*;
import crmdna.list.ListEntity;
import crmdna.mail2.MailContentEntity;
import crmdna.mail2.SentMailEntity;
import crmdna.mail2.TagSetEntity;
import crmdna.mail2.URLEntity;
import crmdna.member.MemberEntity;
import crmdna.objectstore.ObjectEntity;
import crmdna.participant.ParticipantEntity;
import crmdna.payment.TokenEntity;
import crmdna.payment2.PaymentEntity;
import crmdna.practice.PracticeEntity;
import crmdna.program.ProgramEntity;
import crmdna.programtype.ProgramTypeEntity;
import crmdna.registration.DiscountEntity;
import crmdna.registration.RegistrationEntity;
import crmdna.registration.TransactionEntity;
import crmdna.sequence.SequenceEntity;
import crmdna.sessionpass.SessionPassEntity;
import crmdna.sessionpass.SubscriptionEntity;
import crmdna.teacher.TeacherEntity;
import crmdna.user.UserCore;
import crmdna.user.UserEntity;
import crmdna.useractivity.UserActivityEntity;
import crmdna.venue.VenueEntity;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.OfyService.ofy;

public class DataMigration {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void copyAllEntitiesToAnotherClient(String sourceClient, String targetClient,
                                                      String login) {

        // This is to be used for one time data migration for bhairavi yoga.
        // This code should probably be removed after that

        ensure(UserCore.isSuperUser(login), "This function can be called only by a super user");

        Client.ensureValid(sourceClient);
        Client.ensureValid(targetClient);

        List<Class> types = new ArrayList<>();
        types.add(GroupEntity.class);
        types.add(MemberEntity.class);
        types.add(ProgramEntity.class);
        types.add(ProgramTypeEntity.class);
        types.add(VenueEntity.class);
        types.add(UserEntity.class);
        types.add(CustomFieldsEntity.class);
        types.add(SequenceEntity.class);
        types.add(UserActivityEntity.class);
        types.add(InteractionEntity.class);

        types.add(PracticeEntity.class);
        types.add(TeacherEntity.class);
        types.add(DummyTestEntity.class);
        types.add(ParticipantEntity.class);
        types.add(ShardEntity.class);
        types.add(CheckInEntity.class);
        types.add(IshaConfigEntity.class);
        types.add(RegistrationEntity.class);
        types.add(DiscountEntity.class);
        types.add(TokenEntity.class);
        types.add(TransactionEntity.class);

        types.add(ConfigCRMDNAEntity.class);
        types.add(ConfigHelpEntity.class);
        types.add(InventoryItemTypeEntity.class);
        types.add(InventoryItemEntity.class);
        types.add(PackagedInventoryItemEntity.class);
        types.add(PackagedInventorySalesEntity.class);
        types.add(PackagedInventoryBatchEntity.class);
        types.add(InventoryLocationEntity.class);
        types.add(InventoryTransferEntity.class);
        types.add(InventoryCheckInEntity.class);
        types.add(InventoryCheckOutEntity.class);
        types.add(DepartmentEntity.class);
        types.add(ObjectEntity.class);
        types.add(TagSetEntity.class);
        types.add(SentMailEntity.class);

        types.add(MailContentEntity.class);
        types.add(URLEntity.class);
        types.add(MealCountEntity.class);
        types.add(SessionPassEntity.class);
        types.add(SubscriptionEntity.class);
        types.add(PaymentEntity.class);
        types.add(ListEntity.class);

        for (Class type : types) {
            List<Object> entities = ofy(sourceClient).load().type(type).list();
            ofy(targetClient).save().entities(entities);
        }
    }
}
