package crmdna.inventory;

import crmdna.common.Utils.Currency;

import static crmdna.common.AssertUtils.ensureNoNullElement;


public class MealCountProp {
    public int yyyymmdd;

    public int breakfastCount;
    public int lunchCount;
    public int dinnerCount;

    public double totalBreakfastCost;
    public double totalLunchCost;
    public double totalDinnerCost;

    public String avgBreakfastCostPerMeal;
    public String avgLunchCostPerMeal;
    public String avgDinnerCostPerMeal;
    public Currency ccy;

    public static String getCSV(Iterable<MealCountProp> props) {
        ensureNoNullElement(props);

        StringBuilder builder = new StringBuilder();

        builder.append("YYYYMMDD,Breakfast,Lunch,Dinner");

        for (MealCountProp prop : props) {
            builder.append(prop.yyyymmdd).append(",").append(prop.breakfastCount).append(",")
                    .append(prop.lunchCount).append(",").append(prop.dinnerCount).append(",").append("\n");
        }

        return builder.toString();
    }

    public String getAvgBreakfastCostPerMeal() {
        if (breakfastCount == 0)
            return "";

        return String.format("%.2f", totalBreakfastCost / breakfastCount);
    }

    public String getAvgLunchCostPerMeal() {
        if (lunchCount == 0)
            return "";

        return String.format("%.2f", totalLunchCost / lunchCount);
    }

    public String getAvgDinnerCostPerMeal() {
        if (lunchCount == 0)
            return "";

        return String.format("%.2f", totalDinnerCost / dinnerCount);
    }
}
