package crmdna.api.servlet;

import crmdna.common.DateUtils;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.group.Group;
import crmdna.hr.Department;
import crmdna.inventory.*;
import crmdna.inventory.MealCount.Meal;
import crmdna.user.User.ResourceType;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static crmdna.common.AssertUtils.ensure;

public class InventoryServlet extends HttpServlet {

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
                if (action.equals("createInventoryItem")) {
                    String group = ServletUtils.getStrParam(request, "groupId");
                    long groupId = Group.safeGetByIdOrName(client, group).toProp().groupId;

                    InventoryItemProp prop = InventoryItem
                            .create(client, groupId,
                                    ServletUtils.getLongParam(request, "inventoryItemTypeId"),
                                    ServletUtils.getStrParam(request, "displayName"),
                                    PhysicalQuantity.valueOf(ServletUtils.getStrParam(request, "physicalQuantity")),
                                    ReportingUnit.valueOf(ServletUtils.getStrParam(request, "reportingUnit")), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(prop));
                } else if (action.equals("updateInventoryItem")) {

                    InventoryItemProp prop = InventoryItem
                            .update(client, ServletUtils.getLongParam(request, "inventoryItemId"),
                                    ServletUtils.getLongParam(request, "newInventoryItemTypeId"),
                                    ServletUtils.getStrParam(request, "newDisplayName"),
                                    ReportingUnit.valueOf(ServletUtils.getStrParam(request, "newReportingUnit")),
                                    login);
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(prop));

                } else if (action.equals("checkIn")) {

                    Long inventoryItemId;
                    for (int i = 0; i < 50; i++) {
                        inventoryItemId =
                                ServletUtils.getLongParam(request, "items[" + i + "].inventoryItemId");
                        if (inventoryItemId == null) {
                            break;
                        }

                        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                        Date date = dateFormat.parse(ServletUtils.getStrParam(request, "yyyymmdd"));
                        InventoryItem.checkIn(client, inventoryItemId, date,
                                ServletUtils.getDoubleParam(request, "items[" + i + "].qtyInReportingUnit"),
                                ReportingUnit
                                        .valueOf(ServletUtils.getStrParam(request, "items[" + i + "].reportingUnit")),
                                ServletUtils.getDoubleParam(request, "items[" + i + "].pricePerReportingUnit"),
                                Currency.INR, ServletUtils.getStrParam(request, "items[" + i + "].comment"), login);
                    }

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("checkOut")) {

                    Set<String> tags = new HashSet<>();

                    Long departmentId = ServletUtils.getLongParam(request, "departmentId");
                    ensure(departmentId != null, "departmentId should be specified for check out");

                    tags.add(ResourceType.DEPARTMENT + "||" + departmentId);

                    String departmentName = Department.safeGet(client, departmentId).toProp().displayName;
                    if (departmentName.toUpperCase().contains("KITCHEN")) {
                        Meal meal = Meal.valueOf(ServletUtils.getStrParam(request, "meal"));
                        tags.add(ResourceType.MEAL + "||" + meal);
                    }

                    Long inventoryItemId;
                    for (int i = 0; i < 50; i++) {
                        inventoryItemId =
                                ServletUtils.getLongParam(request, "items[" + i + "].inventoryItemId");
                        if (inventoryItemId == null) {
                            break;
                        }

                        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                        Date date = dateFormat.parse(ServletUtils.getStrParam(request, "yyyymmdd"));

                        InventoryItem.checkOut(client, inventoryItemId, date,
                                ServletUtils.getDoubleParam(request, "items[" + i + "].qtyInReportingUnit"),
                                ReportingUnit
                                        .valueOf(ServletUtils.getStrParam(request, "items[" + i + "].reportingUnit")),
                                ServletUtils.getDoubleParam(request, "items[" + i + "].pricePerReportingUnit" + i),
                                Currency.INR, ServletUtils.getStrParam(request, "items[" + i + "].comment"), tags,
                                login);
                    }

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("getCurrentStockList")) {

                    InventoryItemQueryCondition qc = new InventoryItemQueryCondition();
                    String groupName = ServletUtils.getStrParam(request, "groupName");
                    if (groupName == null) {
                        groupName = "mahamudra";
                    }

                    qc.groupId = Group.safeGetByIdOrName(client, groupName).toProp().groupId;

                    List<InventoryItemProp> props = InventoryItem.query(client, qc, login);
                    InventoryItemProp.populateDependents(client, props);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else if (action.equals("setKitchen3MealCount")) {

                    MealCountProp prop = MealCount
                            .setCount(client, ServletUtils.getIntParam(request, "yyyymmdd"),
                                    ServletUtils.getIntParam(request, "breakfastCount"),
                                    ServletUtils.getIntParam(request, "lunchCount"),
                                    ServletUtils.getIntParam(request, "dinnerCount"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(prop));

                } else if (action.equals("getAllInventoryItemTypes")) {

                    List<InventoryItemTypeProp> props = InventoryItemType.getAll(client);
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else if (action.equals("createPackagedItems")) {

                    long groupId =
                            Group.safeGetByIdOrName(client, ServletUtils.getStrParam(request, "groupName"))
                                    .toProp().groupId;

                    PackagedInventoryBatchProp batchProp = PackagedInventoryBatch
                            .create(client, ServletUtils.getStrParam(request, "batchName"),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[packingCostAtSrc][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[labellingCostAtSrc][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[transportAtSrc][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[warehouseCost][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[manpowerCost][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[shipmentCost][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[transportAtDst][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[clearanceAtDst][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request, "overheads[GST][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request,
                                    "overheads[other][valueInSGD]", true),
                                ServletUtils.getDoubleParam(request, "forex[USD]"),
                                ServletUtils.getDoubleParam(request, "forex[INR]"),
                                login);

                    int itemsCount = 0, i;
                    for (i = 0; i < 1000; i++) {
                        Long inventoryItemId =
                                ServletUtils.getLongParam(request, "items[" + i + "][inventoryItemId]");
                        if (inventoryItemId == null) {
                            break;
                        }

                        itemsCount += PackagedInventoryItem.create(client, groupId, inventoryItemId,
                                ServletUtils.getIntParam(request, "items[" + i + "][expiryYYYYMMDD]"),
                                ServletUtils.getDoubleParam(request, "items[" + i + "][costPrice]"),
                                ServletUtils.getDoubleParam(request, "items[" + i + "][sellingPrice]"),
                                Utils.Currency
                                        .valueOf(ServletUtils.getStrParam(request, "items[" + i + "][currency]")),
                                ServletUtils.getIntParam(request, "items[" + i + "][qty]"),
                                batchProp.batchId,
                                login);
                    }

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS)
                            .message("Checked in " + itemsCount + " items in " + i + " categories"));

                } else if (action.equals("updatePackagedItem")) {

                    PackagedInventoryItemProp prop = PackagedInventoryItem
                            .update(client, ServletUtils.getLongParam(request, "packagedInventoryItemId"),
                                    ServletUtils.getLongParam(request, "newInventoryItemId"),
                                    ServletUtils.getIntParam(request, "newExpiryYYYYMMDD"),
                                    ServletUtils.getDoubleParam(request, "newCostPrice"),
                                    ServletUtils.getDoubleParam(request, "newSellingPrice"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(prop));
                } else if (action.equals("transferPackagedItems")) {

                    Set<Long> packagedInventoryItemIds = new HashSet<>();
                    for (int i = 0; i < 1000; i++) {
                        Long packagedInventoryItemId = ServletUtils.getLongParam(request, "itemIds[" + i + "]");
                        if (packagedInventoryItemId == null) {
                            break;
                        }
                        packagedInventoryItemIds.add(packagedInventoryItemId);
                    }

                    long groupId =
                            Group.safeGetByIdOrName(client, ServletUtils.getStrParam(request, "groupName"))
                                    .toProp().groupId;
                    PackagedInventoryItem.transfer(client, groupId, packagedInventoryItemIds,
                            ServletUtils.getLongParam(request, "fromLocation"),
                            ServletUtils.getLongParam(request, "toLocation"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));
                } else if (action.equals("updateSoldPackagedItems")) {

                    Map<Long, Double> packagedInventoryItemIdMap = new HashMap<>();
                    for (int i = 0; i < 1000; i++) {
                        Long packagedInventoryItemId = ServletUtils.getLongParam(request, "itemIds[" + i + "]");
                        if (packagedInventoryItemId == null) {
                            break;
                        }
                        Double sellingPrice = ServletUtils.getDoubleParam(request, "sellingPrices[" + i + "]");
                        packagedInventoryItemIdMap.put(packagedInventoryItemId, sellingPrice);
                    }

                    long groupId =
                            Group.safeGetByIdOrName(client, ServletUtils.getStrParam(request, "groupName"))
                                    .toProp().groupId;
                    PackagedInventoryItem.updateSold(client, groupId, packagedInventoryItemIdMap,
                            ServletUtils.getStrParam(request, "salesOrder"),
                            ServletUtils.getBoolParam(request, "paidOnline"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));
                } else if (action.equals("queryPackagedItems")) {

                    PackagedInventoryItemQueryCondition qc = new PackagedInventoryItemQueryCondition();
                    qc.locationId = ServletUtils.getLongParam(request, "locationId");
                    qc.salesId = ServletUtils.getLongParam(request, "salesId");
                    qc.batchId = ServletUtils.getLongParam(request, "batchId");
                    List<PackagedInventoryItemProp> props = PackagedInventoryItem.query(client, qc, login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));
                } else if (action.equals("querySoldPackagedItems")) {

                    PackagedInventorySalesQueryCondition qcSales = new PackagedInventorySalesQueryCondition();
                    qcSales.startMS = DateUtils.toDate(ServletUtils.getIntParam(request, "startYYYYMMDD"))
                            .getTime();
                    qcSales.endMS = DateUtils.toDate(ServletUtils.getIntParam(request, "endYYYYMMDD"))
                            .getTime();
                    List<PackagedInventorySalesProp> salesProps = PackagedInventorySales.query(client,
                            qcSales);

                    for (PackagedInventorySalesProp salesProp : salesProps) {
                        PackagedInventoryItemQueryCondition qc = new PackagedInventoryItemQueryCondition();
                        qc.locationId = ServletUtils.getLongParam(request, "locationId");
                        qc.salesId = salesProp.salesId;
                        qc.batchId = ServletUtils.getLongParam(request, "batchId");
                        salesProp.packagedInventoryItemProps = PackagedInventoryItem.query(client, qc, login);
                    }

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(salesProps));
                } else if (action.equals("getAllLocations")) {

                    List<InventoryLocationProp> props = InventoryLocation
                            .getAll(client, ServletUtils.getBoolParam(request, "populateItemCount"), login);

                    ServletUtils.setJson(response, new APIResponse().object(props).status(Status.SUCCESS));
                } else if (action.equals("getAllBatches")) {

                    List<PackagedInventoryBatchProp> props = PackagedInventoryBatch.getAll(client);

                    ServletUtils.setJson(response, new APIResponse().object(props).status(Status.SUCCESS));
                } else {
                    ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }
            } catch (Exception ex) {
                ServletUtils.setJson(response, APIUtils.toAPIResponse(ex, true,
                        new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }
}
