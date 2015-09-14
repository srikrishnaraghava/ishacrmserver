package crmdna.registration;

import crmdna.common.Utils;

public class Invoice {
    public static String getInvoiceNo(String programType, String group,
                                      long registrationId) {
        String invoiceNo = programType + "_" + group + "_" + registrationId;
        return invoiceNo;
    }

    public static long getRegistrationNo(String invoiceNo) {
        // invoice is of the form: programType_group_registrationId
        // eg: Surya Kriya_Singapore_20 where 20 is the registration ID

        if ((invoiceNo == null) || invoiceNo.equals(""))
            Utils.throwIncorrectSpecException("Invalid invoice number ["
                    + invoiceNo + "]");

        String[] parts = invoiceNo.split("_");

        if (parts.length != 3)
            Utils.throwIncorrectSpecException("Invalid format for invoice no ["
                    + invoiceNo
                    + "]. Invoice no should be of the format: <programtype>_group_registrationId. "
                    + "Eg: Surya Kriya_Singapore_20");

        String registrationNoStr = parts[2];
        if (!Utils.canParseAsLong(registrationNoStr))
            Utils.throwIncorrectSpecException("[" + registrationNoStr
                    + "] is not a valid registration no. Invalid invoice no ["
                    + invoiceNo + "]");

        long registrationNo = Utils.safeParseAsLong(registrationNoStr);

        return registrationNo;
    }
}
