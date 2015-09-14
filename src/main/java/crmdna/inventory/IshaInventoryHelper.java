package crmdna.inventory;

import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.hr.Department;
import crmdna.inventory.MealCount.Meal;
import crmdna.user.User.ResourceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class IshaInventoryHelper {
    public static List<MealCountProp> getKitchen3DailyMealCost(String client, long groupId,
                                                               Integer startYYYYMMDD, Integer endYYYYMMDD, String login) {

        Client.ensureValid(client);

        String groupName = Group.safeGet(client, groupId).toProp().displayName;
        if (!groupName.contains("Mahamudra"))
            throw new APIException("This operation is allowed only for group Mahamudra")
                    .status(Status.ERROR_OPERATION_NOT_ALLOWED);

        List<MealCountEntity> mealCountEntities = MealCount.query(client, startYYYYMMDD, endYYYYMMDD);

        long startMS = DateUtils.toDate(startYYYYMMDD).getTime();
        long endMS = DateUtils.toDate(endYYYYMMDD).getTime();
        StockChangeQueryCondition qc = new StockChangeQueryCondition(groupId, startMS, endMS);
        qc.includeCheckOut = true;
        qc.includeCheckIn = false;

        long departmentId = Department.safeGetByName(client, "kitchen3").toProp().departmentId;
        qc.tags.add(ResourceType.DEPARTMENT + "||" + departmentId);

        List<StockChangeProp> stockChangeProps = InventoryItem.queryStockChanges(client, qc, login);

        Map<Integer, Double> yyyymmddVsBreakfastCost = new HashMap<>();
        Map<Integer, Double> yyyymmddVsLunchCost = new HashMap<>();
        Map<Integer, Double> yyyymmddVsDinnerCost = new HashMap<>();
        Map<Integer, Currency> yyyymmddVsCcy = new HashMap<>();

        for (StockChangeProp prop : stockChangeProps) {
            int yyyymmdd = DateUtils.toYYYYMMDD(prop.timestamp);

            if (!yyyymmddVsCcy.containsKey(prop.ccy))
                yyyymmddVsCcy.put(yyyymmdd, prop.ccy);
            else if (prop.ccy != yyyymmddVsCcy.get(yyyymmdd)) {
                String errMessage =
                        "Found multiple currencies - " + prop.ccy + ", " + yyyymmddVsCcy.get(yyyymmdd)
                                + " when processing checkouts for [" + yyyymmdd + "]";
                throw new APIException(errMessage).status(Status.ERROR_OPERATION_NOT_ALLOWED);
            }

            Map<Integer, Double> map = null;
            if (prop.tags.contains(ResourceType.MEAL + "||" + Meal.BREAKFAST)) {
                map = yyyymmddVsBreakfastCost;
            } else if (prop.tags.contains(ResourceType.MEAL + "||" + Meal.LUNCH)) {
                map = yyyymmddVsLunchCost;
            } else if (prop.tags.contains(ResourceType.MEAL + "||" + Meal.DINNER)) {
                map = yyyymmddVsDinnerCost;
            } else {
                Logger logger = Logger.getLogger(IshaInventoryHelper.class.getName());
                logger.warning("Found inventoryCheckOutEntity [" + prop.checkInOrOutId
                        + "] with possibly invalid tags");
                continue;
            }

            if (!map.containsKey(yyyymmdd))
                map.put(yyyymmdd, 0.0);

            map.put(yyyymmdd, map.get(yyyymmdd) + prop.cost);
        }

        List<MealCountProp> props = new ArrayList<>();
        for (MealCountEntity entity : mealCountEntities) {
            MealCountProp prop = entity.toProp();

            int yyyymmdd = prop.yyyymmdd;
            DateUtils.ensureFormatYYYYMMDD(yyyymmdd);

            prop.totalBreakfastCost = 0.0;
            if (yyyymmddVsBreakfastCost.containsKey(yyyymmdd))
                prop.totalBreakfastCost = yyyymmddVsBreakfastCost.get(yyyymmdd);

            prop.totalLunchCost = 0.0;
            if (yyyymmddVsLunchCost.containsKey(yyyymmdd))
                prop.totalLunchCost = yyyymmddVsLunchCost.get(yyyymmdd);

            prop.totalDinnerCost = 0.0;
            if (yyyymmddVsDinnerCost.containsKey(yyyymmdd))
                prop.totalDinnerCost = yyyymmddVsDinnerCost.get(yyyymmdd);

            props.add(prop);
        }

        return props;
    }
}
