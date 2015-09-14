package crmdna.inventory;

import crmdna.common.UnitUtils;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils.Currency;

import java.io.Serializable;

public class CheckOutDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    public double checkInPricePerDefaultUnit;
    public Currency checkInCcy;

    public double checkOutPricePerDefaultUnit;
    public Currency checkOutCcy;

    public double qtyInDefaultUnit;

    public String getProfit() {
        if (checkInCcy != checkOutCcy)
            return "NA - profit calculation not implemented when checkin ccy different from checkout ccy";

        double profit = (checkOutPricePerDefaultUnit - checkInPricePerDefaultUnit)
                * qtyInDefaultUnit;
        return profit + "";
    }

    //currently not used - sathya, 7-July-2014
    public String getSummary(PhysicalQuantity physicalQuantity,
                             ReportingUnit reportingUnit) {
        double checkInPricePerReportingUnit = UnitUtils
                .safeGetPricePerReportingUnit(physicalQuantity,
                        checkInPricePerDefaultUnit, reportingUnit);

        double checkOutPricePerReportingUnit = UnitUtils
                .safeGetPricePerReportingUnit(physicalQuantity,
                        checkOutPricePerDefaultUnit, reportingUnit);

        double qtyInReportingUnit = UnitUtils.safeGetQtyInReportingUnit(
                physicalQuantity, qtyInDefaultUnit, reportingUnit);

        StringBuilder sb = new StringBuilder();
        sb.append("In[" + checkInPricePerReportingUnit + " " + checkInCcy
                + " / " + reportingUnit + "] ");
        sb.append("Out[" + checkOutPricePerReportingUnit + " " + checkOutCcy
                + " / " + reportingUnit + "] ");
        sb.append("Qty[" + qtyInReportingUnit + " " + reportingUnit + "] ");
        sb.append("Profit[" + getProfit() + " " + checkInCcy + "]");

        return sb.toString();
    }
}
