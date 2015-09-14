package crmdna.common;

import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.AssertUtils.ensureNotNull;

public class UnitUtils {

    public static double safeGetQtyInReportingUnit(PhysicalQuantity physicalQuantity,
                                                   double qtyInDefaultUnit, ReportingUnit reportingUnit) {

        ensureNotNull(reportingUnit, "reportingUnit is null");
        ensureNotNull(physicalQuantity, "physicalQuantity is null");

        if (physicalQuantity == PhysicalQuantity.WEIGHT) {
            // default unit is gram
            if (reportingUnit == ReportingUnit.KG)
                return qtyInDefaultUnit / 1000;
            else if (reportingUnit == ReportingUnit.GRAM)
                return qtyInDefaultUnit;
            else {
                // should never come here
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                                + physicalQuantity + "]");
            }
        }

        if (physicalQuantity == PhysicalQuantity.VOLUME) {
            // default unit is milli liter
            if (reportingUnit == ReportingUnit.LITRE)
                return qtyInDefaultUnit / 1000;

            if (reportingUnit == ReportingUnit.ML)
                return qtyInDefaultUnit;

            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                            + physicalQuantity + "]");
        }

        if (physicalQuantity == PhysicalQuantity.NUMBER)
            return qtyInDefaultUnit;

        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "Unsupported physical quantity [" + physicalQuantity + "]");
    }

    public static double safeGetPricePerReportingUnit(PhysicalQuantity physicalQuantity,
                                                      double pricePerDefaultUnit, ReportingUnit reportingUnit) {

        ensureNotNull(reportingUnit, "reportingUnit is null");
        ensureNotNull(physicalQuantity, "physicalQuantity is null");

        if (physicalQuantity == PhysicalQuantity.WEIGHT) {
            // default unit is gram
            if (reportingUnit == ReportingUnit.KG)
                return pricePerDefaultUnit * 1000;
            else if (reportingUnit == ReportingUnit.GRAM)
                return pricePerDefaultUnit;
            else {
                // should never come here
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                                + physicalQuantity + "]");
            }
        }

        if (physicalQuantity == PhysicalQuantity.VOLUME) {
            // default unit is milli liter
            if (reportingUnit == ReportingUnit.LITRE)
                return pricePerDefaultUnit * 1000;

            if (reportingUnit == ReportingUnit.ML)
                return pricePerDefaultUnit;

            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                            + physicalQuantity + "]");
        }

        if (physicalQuantity == PhysicalQuantity.NUMBER)
            return pricePerDefaultUnit;

        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "Unsupported physical quantity [" + physicalQuantity + "]");
    }

    public static double safeGetQtyInDefaultUnit(PhysicalQuantity physicalQuantity,
                                                 double qtyInReportingUnit, ReportingUnit reportingUnit) {

        if (physicalQuantity == PhysicalQuantity.WEIGHT) {
            if (reportingUnit == ReportingUnit.KG)
                return qtyInReportingUnit * 1000;
            else if (reportingUnit == ReportingUnit.GRAM)
                return qtyInReportingUnit;
            else {
                // should never come here
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                                + physicalQuantity + "]");
            }
        }

        if (physicalQuantity == PhysicalQuantity.VOLUME) {
            if (reportingUnit == ReportingUnit.LITRE)
                return qtyInReportingUnit * 1000;

            if (reportingUnit == ReportingUnit.ML)
                return qtyInReportingUnit;

            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                            + physicalQuantity + "]");
        }

        if (physicalQuantity == PhysicalQuantity.NUMBER) {
            if (reportingUnit != ReportingUnit.NUMBER) {
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                                + physicalQuantity + "]");
            }

            return qtyInReportingUnit;
        }

        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "Unsupported physical quantity [" + physicalQuantity + "]");
    }

    public static ReportingUnit getDefaultUnit(PhysicalQuantity physicalQuantity) {
        if (physicalQuantity == PhysicalQuantity.NUMBER)
            return ReportingUnit.NUMBER;

        if (physicalQuantity == PhysicalQuantity.WEIGHT)
            return ReportingUnit.GRAM;

        if (physicalQuantity == PhysicalQuantity.VOLUME)
            return ReportingUnit.ML;

        // should never come here
        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "Invalid physical quantity [" + physicalQuantity + "]");
    }

    public static double safeGetPricePerDefaultUnit(PhysicalQuantity physicalQuantity,
                                                    double pricePerReportingUnit, ReportingUnit reportingUnit) {

        if (physicalQuantity == PhysicalQuantity.WEIGHT) {
            if (reportingUnit == ReportingUnit.KG)
                return pricePerReportingUnit / 1000;
            else if (reportingUnit == ReportingUnit.GRAM)
                return pricePerReportingUnit;
            else {
                // should never come here
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                                + physicalQuantity + "]");
            }
        }

        if (physicalQuantity == PhysicalQuantity.VOLUME) {
            if (reportingUnit == ReportingUnit.LITRE)
                return pricePerReportingUnit / 1000;

            if (reportingUnit == ReportingUnit.ML)
                return pricePerReportingUnit;

            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                            + physicalQuantity + "]");
        }

        if (physicalQuantity == PhysicalQuantity.NUMBER) {
            if (reportingUnit != ReportingUnit.NUMBER) {
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                                + physicalQuantity + "]");
            }

            return pricePerReportingUnit;
        }

        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "Unsupported physical quantity [" + physicalQuantity + "]");
    }

    public static void ensureValidReportingUnit(PhysicalQuantity physicalQuantity,
                                                ReportingUnit reportingUnit) {

        ensureNotNull(physicalQuantity, "physicalQuantity is null");
        ensureNotNull(reportingUnit, "reportingUnit is null");

        boolean invalid = false;
        if (physicalQuantity == PhysicalQuantity.WEIGHT) {
            if ((reportingUnit != ReportingUnit.GRAM) && (reportingUnit != ReportingUnit.KG))
                invalid = true;
        } else if (physicalQuantity == PhysicalQuantity.VOLUME) {
            if ((reportingUnit != ReportingUnit.LITRE) && (reportingUnit != ReportingUnit.ML))
                invalid = true;
        } else if (physicalQuantity == PhysicalQuantity.NUMBER) {
            if (reportingUnit != ReportingUnit.NUMBER)
                invalid = true;
        }

        if (invalid)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid reporting unit [" + reportingUnit + "] for physical quantity ["
                            + physicalQuantity + "]");
    }

    public static List<ReportingUnit> getReportingUnitsForPhysicalQuantity(PhysicalQuantity qty) {
        List<ReportingUnit> list = new ArrayList<ReportingUnit>();

        // KG, GRAM, ML, LITRE, NUMBER
        if (qty == PhysicalQuantity.WEIGHT) {
            list.add(ReportingUnit.GRAM);
            list.add(ReportingUnit.KG);
        } else if (qty == PhysicalQuantity.VOLUME) {
            list.add(ReportingUnit.ML);
            list.add(ReportingUnit.LITRE);
        } else {
            list.add(ReportingUnit.NUMBER);
        }

        return list;
    }

    public enum PhysicalQuantity {
        WEIGHT, VOLUME, NUMBER
    }

    public enum ReportingUnit {
        KG, GRAM, ML, LITRE, NUMBER
    }
}
