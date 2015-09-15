package crmdna.common;

import com.google.appengine.api.NamespaceManager;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import crmdna.attendance.CheckInEntity;
import crmdna.calling.CampaignEntity;
import crmdna.client.ClientEntity;
import crmdna.client.CrmDnaUserEntity;
import crmdna.client.CustomFieldsEntity;
import crmdna.client.isha.IshaConfigEntity;
import crmdna.common.config.ConfigCRMDNAEntity;
import crmdna.counter.ShardEntity;
import crmdna.group.GroupEntity;
import crmdna.helpandsupport.ConfigHelpEntity;
import crmdna.hr.DepartmentEntity;
import crmdna.interaction.InteractionEntity;
import crmdna.interaction.InteractionScoreEntity;
import crmdna.inventory.InventoryCheckInEntity;
import crmdna.inventory.InventoryCheckOutEntity;
import crmdna.inventory.InventoryItemEntity;
import crmdna.inventory.InventoryItemTypeEntity;
import crmdna.inventory.InventoryLocationEntity;
import crmdna.inventory.InventoryTransferEntity;
import crmdna.inventory.MealCountEntity;
import crmdna.inventory.PackagedInventoryBatchEntity;
import crmdna.inventory.PackagedInventoryItemEntity;
import crmdna.inventory.PackagedInventorySalesEntity;
import crmdna.list.ListEntity;
import crmdna.mail2.MailContentEntity;
import crmdna.mail2.SentMailEntity;
import crmdna.mail2.TagSetEntity;
import crmdna.mail2.URLEntity;
import crmdna.member.MemberEntity;
import crmdna.objectstore.ObjectEntity;
import crmdna.participant.ParticipantEntity;
import crmdna.payment.ReceiptEntity;
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
import crmdna.user.UserEntity;
import crmdna.useractivity.UserActivityEntity;
import crmdna.venue.VenueEntity;

public class OfyService {
    static {
        factory().register(ClientEntity.class);
        factory().register(CrmDnaUserEntity.class);
        factory().register(GroupEntity.class);
        factory().register(MemberEntity.class);
        factory().register(ProgramEntity.class);
        factory().register(ProgramTypeEntity.class);
        factory().register(VenueEntity.class);
        factory().register(UserEntity.class);
        factory().register(CustomFieldsEntity.class);
        factory().register(SequenceEntity.class);
        factory().register(UserActivityEntity.class);
        factory().register(InteractionEntity.class);
        factory().register(PracticeEntity.class);
        factory().register(TeacherEntity.class);
        factory().register(DummyTestEntity.class);
        factory().register(ParticipantEntity.class);
        factory().register(ShardEntity.class);
        factory().register(CheckInEntity.class);
        factory().register(IshaConfigEntity.class);
        factory().register(RegistrationEntity.class);
        factory().register(DiscountEntity.class);
        factory().register(TokenEntity.class);
        factory().register(TransactionEntity.class);
        factory().register(ConfigCRMDNAEntity.class);
        factory().register(ConfigHelpEntity.class);
        factory().register(InventoryItemTypeEntity.class);
        factory().register(InventoryItemEntity.class);
        factory().register(InventoryCheckInEntity.class);
        factory().register(InventoryCheckOutEntity.class);
        factory().register(PackagedInventoryItemEntity.class);
        factory().register(PackagedInventoryBatchEntity.class);
        factory().register(PackagedInventorySalesEntity.class);
        factory().register(InventoryLocationEntity.class);
        factory().register(InventoryTransferEntity.class);
        factory().register(DepartmentEntity.class);
        factory().register(ObjectEntity.class);
        factory().register(TagSetEntity.class);
        factory().register(SentMailEntity.class);
        factory().register(MailContentEntity.class);
        factory().register(URLEntity.class);
        factory().register(MealCountEntity.class);
        factory().register(SessionPassEntity.class);
        factory().register(SubscriptionEntity.class);
        factory().register(PaymentEntity.class);
        factory().register(ListEntity.class);
        factory().register(InteractionScoreEntity.class);
        factory().register(CampaignEntity.class);
        factory().register(ReceiptEntity.class);
    }

    public static Objectify ofy(String client) {
        NamespaceManager.set(client);
        return ObjectifyService.ofy();
    }

    public static Objectify ofyCrmDna() {
        NamespaceManager.set(Constants.CLIENT_CRMDNA);
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}
