package crmdna.common.contact;

import crmdna.common.Utils;
import crmdna.common.ValidationResultProp;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.util.*;
import java.util.logging.Logger;

import static crmdna.common.AssertUtils.ensureNotNull;

public class Contact {
    final static String EMAIL = "email";
    final static String FIRSTNAME = "firstname";
    final static String LASTNAME = "lastname";
    final static String NICKNAME = "nickname";
    final static String HOMEPHONE = "homephone";
    final static String OFFICEPHONE = "officephone";
    final static String MOBILEPHONE = "mobilephone";
    final static String GENDER = "gender";
    final static String CITY = "city";
    final static String COUNTRY = "country";
    final static String HOMEADDRESS = "homeaddress";
    final static String OFFICEADDRESS = "officeaddress";
    final static String HOMEPOSTALCODE = "homepostalcode";
    final static String OFFICEPOSTALCODE = "officepostalcode";

    public static Gender getGender(String gender) {

        if ((null == gender) || gender.equals(""))
            return null;

        gender = gender.toUpperCase();
        if (gender.equals("M") || gender.equals("MALE") || gender.equals("MAN"))
            return Gender.MALE;

        if (gender.equals("F") || gender.equals("FEMALE") || gender.equals("WOMAN"))
            return Gender.FEMALE;

        Logger logger = Logger.getLogger(Contact.class.getName());
        logger.warning("Invalid gender [" + gender + "]");
        return null;
    }

    public static ValidationResultProp validate(List<ContactProp> contacts) {

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Map<String, List<Integer>> firstName3CharVsIndex = new HashMap<>();

        for (int i = 0; i < contacts.size(); i++) {
            ContactProp c = contacts.get(i);

            // throw error if first name is not specified
            if ((c.firstName == null) || c.firstName.equals("")) {
                String message = "Error in line no [" + (i + 2) + "]: First name is missing";
                errors.add(message);
                continue;
            }

            if (c.firstName.length() < 3) {
                String message =
                        "Warning in line no [" + (i + 2) + "]: First name [" + c.firstName
                                + "] is less than 3 characters";
                warnings.add(message);
                continue;
            }

            // email and phone numbers if specified should be valid

            // at least email or 1 contact number should be specified

            boolean emailOrPhoneNoValid = false;
            if (c.email != null) {
                if (Utils.isValidEmailAddress(c.email)) {
                    emailOrPhoneNoValid = true;
                } else {
                    String message =
                            "Error in line no [" + (i + 2) + "]: Email [" + c.email + "] is not valid";
                    errors.add(message);
                    continue;
                }
            }

            // country should be valid when a phone number is specified
            if ((c.homePhone != null) || (c.mobilePhone != null) || (c.officePhone != null)) {
                if (c.homeAddress.country == null) {
                    String message =
                            "Warning in line no [" + (i + 2)
                                    + "]: Country should be specified to validate phone no(s)";
                    warnings.add(message);
                    continue;
                }
            }

            if (c.homePhone != null) {
                String message = Utils.getPhoneNoErrMsgIfAnyElseNull(c.homePhone, c.homeAddress.country);

                if (message != null) {
                    warnings.add("Warning in line no [" + (i + 2) + "]: " + message);
                    continue;
                }

                emailOrPhoneNoValid = true;
            }

            if (c.mobilePhone != null) {
                String message = Utils.getPhoneNoErrMsgIfAnyElseNull(c.mobilePhone, c.homeAddress.country);

                if (message != null) {
                    warnings.add("Warning in line no [" + (i + 2) + "]: " + message);
                    continue;
                }

                emailOrPhoneNoValid = true;
            }

            if (c.officePhone != null) {
                String message = Utils.getPhoneNoErrMsgIfAnyElseNull(c.officePhone, c.homeAddress.country);

                if (message != null) {
                    warnings.add("Warning in line no [" + (i + 2) + "]: " + message);
                    continue;
                }

                emailOrPhoneNoValid = true;
            }

            if (!emailOrPhoneNoValid) {
                String message =
                        "Warning in line no [" + (i + 2) + "]: No email or contact number found for ["
                                + c.firstName + "]";
                warnings.add(message);
                continue;
            }

            String firstName3Char = c.firstName.substring(0, 3).toLowerCase();
            if (firstName3CharVsIndex.containsKey(firstName3Char)) {
                List<Integer> list = firstName3CharVsIndex.get(firstName3Char);

                for (Integer index : list) {
                    if (isMatching(c, contacts.get(index))) {
                        String message =
                                "Warning in line no ["
                                        + (i + 2)
                                        + "]: Line ["
                                        + (i + 2)
                                        + "] seems to be a duplicate of line ["
                                        + (index + 2)
                                        + "]. First 3 chars of first name and atleast one phone number or email are the same";
                        warnings.add(message);
                    }
                }

                list.add(i);
            } else {
                List<Integer> list = new ArrayList<>();
                list.add(i);
                firstName3CharVsIndex.put(firstName3Char, list);
            }
        }

        ValidationResultProp prop = new ValidationResultProp();
        prop.numEntries = contacts.size();
        prop.errors = errors;
        prop.warnings = warnings;

        return prop;
    }

    public static void ensureEmailOrPhoneNumberValid(ContactProp contact) {

        ensureNotNull(contact);

        if (contact.email != null)
            Utils.ensureValidEmail(contact.email);

        if (null != contact.homePhone)
            Utils.ensureValidPhoneNumber(contact.homePhone);

        if (null != contact.mobilePhone)
            Utils.ensureValidPhoneNumber(contact.mobilePhone);

        if (null != contact.officePhone)
            Utils.ensureValidPhoneNumber(contact.officePhone);

        // email or one of the phone numbers should be specified
        if ((contact.email == null) && (contact.mobilePhone == null) && (contact.homePhone == null)
                && (contact.officePhone == null)) {
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Either email or one of the phone numbers should be specified");
        }
    }

    public static void ensureFirstNameAndValidEmailSpecified(ContactProp contact) {

        if ((contact.firstName == null) || (contact.firstName.equals("")))
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "First name not specified");

        if ((contact.email == null) || (contact.email.equals("")))
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Email not specified");

        Utils.ensureValidEmail(contact.email);
    }

    public static List<ContactProp> getContactDetailsFromListOfMap(List<Map<String, String>> listOfMap) {

        List<ContactProp> contactDetailProps = new ArrayList<>();

        if (listOfMap == null)
            return contactDetailProps;

        boolean columnsValidated = false;
        for (Map<String, String> map : listOfMap) {
            if (!columnsValidated) {
                ensureValidColumns(map);
                columnsValidated = true;
            }

            ContactProp contactDetailProp = getContactDetailFromMap(map);
            contactDetailProps.add(contactDetailProp);
        }

        return contactDetailProps;
    }

    static void ensureValidColumns(Map<String, String> map) {

        Set<String> set = map.keySet();

        ensureElementInSet(set, EMAIL);
        ensureElementInSet(set, FIRSTNAME);
        ensureElementInSet(set, LASTNAME);
        ensureElementInSet(set, NICKNAME);
        ensureElementInSet(set, HOMEPHONE);
        ensureElementInSet(set, OFFICEPHONE);
        ensureElementInSet(set, MOBILEPHONE);
        ensureElementInSet(set, GENDER);
        ensureElementInSet(set, CITY);
        ensureElementInSet(set, COUNTRY);
        ensureElementInSet(set, HOMEADDRESS);
        ensureElementInSet(set, OFFICEADDRESS);
        ensureElementInSet(set, HOMEPOSTALCODE);
        ensureElementInSet(set, OFFICEPOSTALCODE);
    }

    private static void ensureElementInSet(Set<String> set, String element) {

        if (!set.contains(element))
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Column [" + element + "] (space and case insensitve) is missing");
    }

    static ContactProp getContactDetailFromMap(Map<String, String> map) {
        if (map == null)
            return null;

        ContactProp contact = new ContactProp();

        for (String key : map.keySet()) {
            key = key.toLowerCase();
            String value = map.get(key);

            if (key.equals("email") || key.equals("emailid")) {
                value = Utils.sanitizeEmail(value);
                contact.email = value;
            } else if (key.equals("firstname"))
                contact.firstName = value;
            else if (key.equals("lastname"))
                contact.lastName = value;
            else if (key.equals("nickname"))
                contact.nickName = value;
            else if (key.equals("homephone") || key.equals("homenumber") || key.equals("homeno")) {
                contact.homePhone = value;
            } else if (key.equals("officephone") || key.equals("officenumber") || key.equals("officeno")) {
                contact.officePhone = value;
            } else if (key.equals("mobilephone") || key.equals("mobilenumber") || key.equals("mobileno")) {
                contact.mobilePhone = value;
            } else if (key.equals("gender"))
                contact.gender = getGender(value);
            else if (key.equals("city"))
                contact.homeAddress.city = value;
            else if (key.equals("country"))
                contact.homeAddress.country = value;
            else if (key.equals("homeaddress") || key.equals("address"))
                contact.homeAddress.address = value;
            else if (key.equals("officeaddress"))
                contact.officeAddress.address = value;
            else if (key.equals("occupation"))
                contact.occupation = value;
            else if (key.equals("company"))
                contact.company = value;

            // sanitize phone numbers
            contact.homePhone = Utils.sanitizePhoneNo(contact.homePhone, contact.homeAddress.country);
            contact.mobilePhone = Utils.sanitizePhoneNo(contact.mobilePhone, contact.homeAddress.country);
            contact.officePhone = Utils.sanitizePhoneNo(contact.officePhone, contact.homeAddress.country);
        }

        return contact;
    }

    public static String getCSV(List<ContactProp> contactDetailProps) {
        if (contactDetailProps == null)
            return null;

        StringBuilder builder = new StringBuilder();
        builder.append("First Name,Last Name,Email,Gender,Mobile Phone,Home Phone,Office Phone,City,Country");
        builder.append("\n");

        for (ContactProp c : contactDetailProps) {
            builder.append(getCSV(c)).append("\n");
        }

        return builder.toString();
    }

    private static String getCSV(ContactProp c) {
        if (null == c)
            return null;

        String gender = null;
        if (c.gender != null)
            gender = c.gender.toString();

        StringBuilder builder = new StringBuilder();
        builder.append(Utils.csvEncode(c.firstName)).append(",").append(Utils.csvEncode(c.lastName))
                .append(",").append(Utils.csvEncode(c.email)).append(",").append(Utils.csvEncode(gender))
                .append(",").append(Utils.csvEncode(c.mobilePhone)).append(",")
                .append(Utils.csvEncode(c.homePhone)).append(",").append(Utils.csvEncode(c.officePhone))
                .append(",").append(Utils.csvEncode(c.homeAddress.city)).append(",")
                .append(Utils.csvEncode(c.homeAddress.country));

        return builder.toString();
    }

    public static boolean isMatching(ContactProp c1, ContactProp c2) {
        ensureNotNull(c1, "c1 cannot be null");
        ensureNotNull(c2, "c2 cannot be null");

        // if names are not close enough return false
        if (!Utils.closeEnough(c1.getName(), c2.getName()))
            return false;

        // if any phone number or email matches return true
        Set<String> s1 = c1.getEmailAndPhoneNosAsSet();
        Set<String> s2 = c2.getEmailAndPhoneNosAsSet();

        s1.retainAll(s2);
        if (!s1.isEmpty())
            return true;

        // if home address matches return true
        String c1HomeAddress = c1.homeAddress.toString();
        String c2HomeAddress = c2.homeAddress.toString();

        if (!c1HomeAddress.equals("") && !c2HomeAddress.equals("")) {
            if (c1HomeAddress.toLowerCase().equals(c2HomeAddress.toLowerCase()))
                return true;
        }

        // if office address matches return true
        String c1OfficeAddress = c1.officeAddress.toString();
        String c2OfficeAddress = c2.officeAddress.toString();

        if (!c1OfficeAddress.equals("") && !c2OfficeAddress.equals("")) {
            if (c1OfficeAddress.toLowerCase().equals(c2OfficeAddress.toLowerCase()))
                return true;
        }

        // None of email, phone, office address, home address match
        return false;
    }

    public enum Gender {
        MALE, FEMALE, NOT_SPECIFIED
    }
}
