package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.DateUtils.DateRange;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.email.EmailProp;
import crmdna.email.GAEEmail;
import crmdna.group.Group;
import crmdna.hr.Department;
import crmdna.inventory.*;
import crmdna.inventory.MealCount.Meal;
import crmdna.user.User.ResourceType;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static crmdna.common.AssertUtils.ensure;

@Api(name = "inventory")
public class InventoryItemApi {

    @ApiMethod(path = "createInventoryItem", httpMethod = HttpMethod.POST)
    public APIResponse createInventoryItem(@Named("client") String client,
                                           @Nullable @Named("groupIdOrName") String groupIdOrName,
                                           @Named("inventoryItemTypeIdOrName") String inventoryItemTypeIdOrName,
                                           @Named("displayName") String displayName,
                                           @Named("physicalQuantity") PhysicalQuantity physicalQuantity,
                                           @Named("reportingUnit") ReportingUnit reportingUnit,
                                           @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);

            if (groupIdOrName == null)
                groupIdOrName = "mahamudra";

            long groupId = 0;
            if (Utils.canParseAsLong(groupIdOrName)) {
                groupId = Utils.safeParseAsLong(groupIdOrName);
            } else {
                groupId = Group.safeGetByIdOrName(client, groupIdOrName).toProp().groupId;
            }
            ensure(groupId > 0);

            long inventoryItemTypeId = 0;
            if (Utils.canParseAsLong(inventoryItemTypeIdOrName)) {
                inventoryItemTypeId = Utils.safeParseAsLong(inventoryItemTypeIdOrName);
            } else {
                inventoryItemTypeId =
                        InventoryItemType.safeGetByName(client, inventoryItemTypeIdOrName).toProp().inventoryItemTypeId;
            }
            ensure(inventoryItemTypeId > 0);

            InventoryItemProp prop =
                    InventoryItem.create(client, groupId, inventoryItemTypeId, displayName, physicalQuantity,
                            reportingUnit, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "updateInventoryItem", httpMethod = HttpMethod.POST)
    public APIResponse updateInventoryItem(@Named("client") String client,
                                           @Named("inventoryItemId") long inventoryItemId,
                                           @Nullable @Named("newInventoryItemTypeId") Long newInventoryItemTypeId,
                                           @Nullable @Named("newDisplayName") String newDisplayName,
                                           @Nullable @Named("newReportingUnit") ReportingUnit newReportingUnit,
                                           @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);

            InventoryItemProp prop =
                    InventoryItem.update(client, inventoryItemId, newInventoryItemTypeId, newDisplayName,
                            newReportingUnit, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "checkin", httpMethod = HttpMethod.POST)
    public APIResponse checkin(@Named("client") String client,
                               @Named("inventoryItemIdOrName") String inventoryItemIdOrName,
                               @Named("quantity") double qtyInReportingUnit,
                               @Named("reportingUnit") ReportingUnit reportingUnit,
                               @Named("pricePerReportingUnit") double pricePerReportingUnit,
                               @Nullable @Named("dateDefaultCurrentTimestamp") Date date,
                               @Nullable @Named("currencyDefaultINR") Currency ccy,
                               @Nullable @Named("comment") String comment,
                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (client == null)
            client = "isha";

        if (ccy == null)
            ccy = Currency.INR;

        try {
            long inventoryItemId = 0;
            if (Utils.canParseAsLong(inventoryItemIdOrName)) {
                inventoryItemId = Utils.safeParseAsLong(inventoryItemIdOrName);
            } else {
                inventoryItemId =
                        InventoryItem.safeGetByName(client, inventoryItemIdOrName).toProp().inventoryItemId;
            }

            login = Utils.getLoginEmail(user);
            InventoryCheckInProp prop =
                    InventoryItem.checkIn(client, inventoryItemId, date, qtyInReportingUnit, reportingUnit,
                            pricePerReportingUnit, ccy, comment, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "checkout", httpMethod = HttpMethod.POST)
    public APIResponse checkout(@Named("client") String client,
                                @Named("inventoryItemIdOrName") String inventoryItemIdOrName,
                                @Named("quantity") double qtyInReportingUnit,
                                @Named("reportingUnit") ReportingUnit reportingUnit,
                                @Nullable @Named("dateDefaultCurrentTimestamp") Date date,
                                @Nullable @Named("unitPriceINR") Double pricePerReportingUnit,
                                @Nullable @Named("departmentDropDown") IshaDepartment departmentDropDown,
                                @Nullable @Named("departmentIdOrName") String departmentIdOrName,
                                @Nullable @Named("meal") Meal meal, @Nullable @Named("comment") String comment,
                                @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (client == null)
            client = "isha";

        try {

            long inventoryItemId = 0;

            if (Utils.canParseAsLong(inventoryItemIdOrName)) {
                inventoryItemId = Utils.safeParseAsLong(inventoryItemIdOrName);
            } else {
                inventoryItemId =
                        InventoryItem.safeGetByName(client, inventoryItemIdOrName).toProp().inventoryItemId;
            }

            ensure(
                    (departmentDropDown != null) ^ (departmentIdOrName != null),
                    "Either departmentDropDown or departmentIdOrName should be specified for check out (but not none or both)");

            if (departmentDropDown != null)
                departmentIdOrName = departmentDropDown.toString();

            Long departmentId = null;
            Set<String> tags = new HashSet<>();
            if (departmentIdOrName != null) {
                if (Utils.canParseAsLong(departmentIdOrName)) {
                    departmentId = Utils.safeParseAsLong(departmentIdOrName);
                } else {
                    departmentId = Department.safeGetByName(client, departmentIdOrName).toProp().departmentId;
                }

                tags.add(ResourceType.DEPARTMENT + "||" + departmentId);
            }

            String departmentName = Department.safeGet(client, departmentId).toProp().displayName;
            if (departmentName.toUpperCase().contains("KITCHEN")) {
                ensure(meal != null, "Meal should be specified when checking out for [" + departmentName
                        + "]");
                tags.add(ResourceType.MEAL + "||" + meal);
            }

            login = Utils.getLoginEmail(user);

            InventoryCheckOutProp prop =
                    InventoryItem.checkOut(client, inventoryItemId, date, qtyInReportingUnit, reportingUnit,
                            pricePerReportingUnit, Currency.INR, comment, tags, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getCurrentStockList", httpMethod = HttpMethod.GET)
    public APIResponse getCurrentStockList(@Named("client") String client,
                                           @Nullable @Named("groupIdOrName") String groupIdOrName,
                                           @Nullable @Named("sendEmailDefaultFalse") Boolean email,
                                           @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        if (groupIdOrName == null)
            groupIdOrName = "mahamudra";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);

            long groupId = 0;
            if (Utils.canParseAsLong(groupIdOrName)) {
                groupId = Utils.safeParseAsLong(groupIdOrName);
            } else {
                groupId = Group.safeGetByIdOrName(client, groupIdOrName).toProp().groupId;
            }

            InventoryItemQueryCondition qc = new InventoryItemQueryCondition();
            qc.groupId = groupId;

            List<InventoryItemProp> props = InventoryItem.query(client, qc, login);

            InventoryItemProp.populateDependents(client, props);

            if (email == null)
                email = false;

            if (email) {
                EmailProp emailProp = new EmailProp();
                String groupName = Group.safeGet(client, groupId).toProp().displayName;
                emailProp.attachmentName = "Stock list for " + groupName + ".csv";
                emailProp.bodyHtml =
                        "Stock list for " + groupName + " (as of this email timestamp)" + " is attached.";
                emailProp.csvAttachmentData = InventoryItemProp.toCSV(props);
                emailProp.subject = "Stock list for " + groupName;

                crmdna.user.User.ensureValidUser(client, login);
                emailProp.toEmailAddresses.add(login);
                GAEEmail.send(emailProp);
            }

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteInventoryItem", httpMethod = HttpMethod.DELETE)
    public APIResponse deleteInventoryItem(@Named("client") String client,
                                           @Named("inventoryItemId") long inventoryItemId,
                                           @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);

            InventoryItem.delete(client, inventoryItemId, login);

            return new APIResponse().status(Status.SUCCESS).message(
                    "Inventory item [" + inventoryItemId + "] deleted");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getStockChanges", httpMethod = HttpMethod.GET)
    public APIResponse getStockChanges(@Named("client") String client,
                                       @Nullable @Named("groupIdOrName") String groupIdOrName,
                                       @Nullable @Named("dateRange") DateRange dateRange,
                                       @Nullable @Named("startDateYYYYMMDD") Integer startYYYYMMDD,
                                       @Nullable @Named("endDateYYYYMMDD") Integer endYYYYMMDD,
                                       @Nullable @Named("inventoryItemTypeIdOrName") String inventoryItemTypeIdOrName,
                                       @Nullable @Named("departmentDropDown") IshaDepartment departmentDropDown,
                                       @Nullable @Named("departmentIdOrName") String departmentIdOrName,
                                       @Nullable @Named("meal") Meal meal,
                                       @Nullable @Named("inventoryItemIdOrName") String inventoryItemIdOrName,
                                       @Nullable @Named("sendEmailDefaultFalse") Boolean email,
                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        if (groupIdOrName == null)
            groupIdOrName = "mahamudra";

        try {
            Client.ensureValid(client);
            login = Utils.getLoginEmail(user);

            ensure(((startYYYYMMDD == null) && (endYYYYMMDD == null))
                            || ((startYYYYMMDD != null) && (endYYYYMMDD != null)),
                    "Both startYYYYMMDD and endYYYYMMDD should be specified (if dateRange is not specified)");

            ensure(
                    ((dateRange == null) && (startYYYYMMDD != null))
                            || ((dateRange != null) && (startYYYYMMDD == null)),
                    "Either dateRange should be specified or (startYYYYMMDD and endYYYYMMDD) should be specified but not both");

            long startMS = 0;
            long endMS;
            if (dateRange != null) {
                endMS = new Date().getTime();
                startMS = endMS - DateUtils.getMilliSecondsFromDateRange(dateRange);
            } else {
                DateUtils.ensureFormatYYYYMMDD(startYYYYMMDD);
                DateUtils.ensureFormatYYYYMMDD(endYYYYMMDD);
                ensure(endYYYYMMDD >= startYYYYMMDD, "End date should be greater or equal to start date");

                startMS = DateUtils.toDate(startYYYYMMDD).getTime();
                endMS = DateUtils.toDate(endYYYYMMDD).getTime();
            }

            ensure(startMS != 0, "Start date cannot be 0");
            ensure(endMS != 0, "End date cannot be 0");
            ensure(endMS >= startMS, "End date should be greater or equal to start date");

            long groupId;
            if (Utils.canParseAsLong(groupIdOrName)) {
                groupId = Utils.safeParseAsLong(groupIdOrName);
            } else {
                groupId = Group.safeGetByIdOrName(client, groupIdOrName).toProp().groupId;
            }

            ensure(groupId > 0);

            Long inventoryItemTypeId = null;
            if (inventoryItemTypeIdOrName != null) {
                if (Utils.canParseAsLong(inventoryItemTypeIdOrName)) {
                    inventoryItemTypeId = Utils.safeParseAsLong(inventoryItemTypeIdOrName);
                } else {
                    inventoryItemTypeId =
                            InventoryItemType.safeGetByName(client, inventoryItemTypeIdOrName).toProp().inventoryItemTypeId;
                }

                ensure(inventoryItemTypeId > 0);
            }

            ensure(
                    (departmentDropDown != null) ^ (departmentIdOrName != null),
                    "Either departmentDropDown or departmentIdOrName should be specified for check out (but not none or both)");

            if (departmentDropDown != null)
                departmentIdOrName = departmentDropDown.toString();

            Long departmentId = null;
            Set<String> tags = new HashSet<>();
            if (departmentIdOrName != null) {
                if (Utils.canParseAsLong(departmentIdOrName)) {
                    departmentId = Utils.safeParseAsLong(departmentIdOrName);
                } else {
                    departmentId = Department.safeGetByName(client, departmentIdOrName).toProp().departmentId;
                }

                tags.add(ResourceType.DEPARTMENT + "||" + departmentId);

                ensure(departmentId > 0);
            }

            if (meal != null)
                tags.add(ResourceType.MEAL + "||" + meal);

            Long inventoryItemId = null;
            if (inventoryItemIdOrName != null) {
                if (Utils.canParseAsLong(inventoryItemIdOrName)) {
                    inventoryItemId = Utils.safeParseAsLong(inventoryItemIdOrName);
                } else {
                    inventoryItemId =
                            InventoryItem.safeGetByName(client, inventoryItemIdOrName).toProp().inventoryItemId;
                }

                ensure(inventoryItemId > 0);
            }

            StockChangeQueryCondition qc = new StockChangeQueryCondition(groupId, startMS, endMS);
            if (departmentId != null)
                qc.tags = tags;

            if (inventoryItemId != null)
                qc.inventoryItemIds.add(inventoryItemId);

            if (inventoryItemTypeId != null)
                qc.inventoryItemTypeIds.add(inventoryItemTypeId);

            qc.includeCheckIn = true;
            qc.includeCheckOut = true;

            List<StockChangeProp> props = InventoryItem.queryStockChanges(client, qc, login);

            if (email == null)
                email = false;
            if (email) {
                crmdna.user.User.ensureValidUser(client, login);
                EmailProp emailProp = new EmailProp();
                String groupName = Group.safeGet(client, groupId).toProp().displayName;
                emailProp.attachmentName = "Stock changes for " + groupName + ".csv";
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                emailProp.bodyHtml =
                        "Stock changes for " + groupName
                                + " is attached. <br><br><span style=\"color:grey\">Condition: " + gson.toJson(qc)
                                + "</span>";
                emailProp.csvAttachmentData = StockChangeProp.toCSV(props);
                emailProp.subject = "Stock changes for " + groupName;
                emailProp.toEmailAddresses.add(login);

                GAEEmail.send(emailProp);
            }

            return new APIResponse()
                    .status(Status.SUCCESS)
                    .message(
                            "QueryCondition: [" + new Gson().toJson(qc) + "]. No of changes: " + props.size())
                    .object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "setKitchen3MealCount", httpMethod = HttpMethod.POST)
    public APIResponse setKitchen3MealCount(@Named("client") String client,
                                            @Named("yyyymmdd") int yyyymmdd, @Nullable @Named("breakfastCount") Integer breakfastCount,
                                            @Nullable @Named("lunchCount") Integer lunchCount,
                                            @Nullable @Named("dinnerCount") Integer dinnerCount,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);
            login = Utils.getLoginEmail(user);

            MealCountProp prop =
                    MealCount.setCount(client, yyyymmdd, breakfastCount, lunchCount, dinnerCount, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "queryKitchen3CostPerMeal", httpMethod = HttpMethod.GET)
    public APIResponse queryKitchen3CostPerMeal(@Named("client") String client,
                                                @Nullable @Named("startYYYYMMDD") Integer startYYYYMMDD,
                                                @Nullable @Named("endYYYYMMDD") Integer endYYYYMMDD,
                                                @Nullable @Named("sendEmailDefaultFalse") Boolean email,
                                                @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);

            List<MealCountEntity> entities = MealCount.query(client, startYYYYMMDD, endYYYYMMDD);

            List<MealCountProp> props = new ArrayList<>();
            for (MealCountEntity entity : entities) {
                props.add(entity.toProp());
            }

            long groupId = Group.safeGetByIdOrName(client, "Mahamudra").toProp().groupId;

            props =
                    IshaInventoryHelper.getKitchen3DailyMealCost(client, groupId, startYYYYMMDD, endYYYYMMDD,
                            login);

            if (email == false)
                email = true;

            if (email) {
                EmailProp emailProp = new EmailProp();
                emailProp.attachmentName = "Meal count(s) for Kitchen 3.csv";
                emailProp.bodyHtml =
                        "See attached spreadsheet for kitchen 3 meal count. <br><br>" + "Start date: "
                                + (startYYYYMMDD != null ? startYYYYMMDD : "Not specified") + "<br>" + "End date: "
                                + (endYYYYMMDD != null ? endYYYYMMDD : "Not specified");

                emailProp.csvAttachmentData = MealCountProp.getCSV(props);
                emailProp.subject = "Meal count(s) for Kitchen 3";

                crmdna.user.User.ensureValidUser(client, login);
                emailProp.toEmailAddresses.add(login);

                GAEEmail.send(emailProp);
            }

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    public enum IshaDepartment {
        KITCHEN_3_MAHAMUDRA, NANDI_FOODS
    }
}
