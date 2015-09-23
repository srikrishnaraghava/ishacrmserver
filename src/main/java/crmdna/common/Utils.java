package crmdna.common;

import com.google.appengine.api.users.User;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.RequestInfo;
import crmdna.common.config.ConfigCRMDNA;
import crmdna.email.EmailProp;
import crmdna.email.GAEEmail;
import crmdna.refdata.CountryProp;
import crmdna.refdata.RefData;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static crmdna.common.AssertUtils.*;

public class Utils {

    private static final String[] HEX_LOOKUP_TABLE = new String[]{"00", "01", "02", "03", "04",
            "05", "06", "07", "08", "09", "0a", "0b", "0c", "0d", "0e", "0f", "10", "11", "12", "13",
            "14", "15", "16", "17", "18", "19", "1a", "1b", "1c", "1d", "1e", "1f", "20", "21", "22",
            "23", "24", "25", "26", "27", "28", "29", "2a", "2b", "2c", "2d", "2e", "2f", "30", "31",
            "32", "33", "34", "35", "36", "37", "38", "39", "3a", "3b", "3c", "3d", "3e", "3f", "40",
            "41", "42", "43", "44", "45", "46", "47", "48", "49", "4a", "4b", "4c", "4d", "4e", "4f",
            "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "5a", "5b", "5c", "5d", "5e",
            "5f", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "6a", "6b", "6c", "6d",
            "6e", "6f", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "7a", "7b", "7c",
            "7d", "7e", "7f", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "8a", "8b",
            "8c", "8d", "8e", "8f", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "9a",
            "9b", "9c", "9d", "9e", "9f", "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9",
            "aa", "ab", "ac", "ad", "ae", "af", "b0", "b1", "b2", "b3", "b4", "b5", "b6", "b7", "b8",
            "b9", "ba", "bb", "bc", "bd", "be", "bf", "c0", "c1", "c2", "c3", "c4", "c5", "c6", "c7",
            "c8", "c9", "ca", "cb", "cc", "cd", "ce", "cf", "d0", "d1", "d2", "d3", "d4", "d5", "d6",
            "d7", "d8", "d9", "da", "db", "dc", "dd", "de", "df", "e0", "e1", "e2", "e3", "e4", "e5",
            "e6", "e7", "e8", "e9", "ea", "eb", "ec", "ed", "ee", "ef", "f0", "f1", "f2", "f3", "f4",
            "f5", "f6", "f7", "f8", "f9", "fa", "fb", "fc", "fd", "fe", "ff"};

    public enum SingleChar {
        a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z
    }

    public static Set<String> getQSTags_old(String... fields) {
        // creates all combinations of 3 consecutive character as a set
        // eg: if invoked with getQSTags("sathya", "thilakan") the set
        // will be populated as: sat, ath, thy, hya, thi, hil, ila, aka, kan

        Set<String> set = new TreeSet<>();

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null)
                continue;

            fields[i] = fields[i].toLowerCase();
            if (fields[i].length() < 3)
                continue;

            for (int beginIndex = 0; beginIndex < fields[i].length() - 2; beginIndex++) {
                set.add(fields[i].substring(beginIndex, beginIndex + 3));
            }
        }

        return set;
    }

    public static String toHexStr(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(HEX_LOOKUP_TABLE[((int) bytes[i]) & 0xff]);
        }

        return sb.toString();
    }

    public static byte[] toByteArray(String hexStr) {
        if (hexStr == null) {
            return null;
        }

        if ((hexStr.length() % 2) != 0) {
            throw new APIException().status(Status.ERROR_INVALID_INPUT).message(
                    "Expected length of the input to be even.");
        }

        hexStr = hexStr.toLowerCase();
        int numBytes = hexStr.length() / 2;
        byte[] bytes = new byte[numBytes];

        for (int i = 0, j = 0; i < numBytes; i++, j += 2) {
            int c1 = hexStr.charAt(j) - '0';
            if (c1 > 9) {
                c1 = c1 + '0' - 'a' + 10;
            }
            int c2 = hexStr.charAt(j + 1) - '0';
            if (c2 > 9) {
                c2 = c2 + '0' - 'a' + 10;
            }

            bytes[i] = (byte) (((c1 << 4) + c2) & 0xff);
        }

        return bytes;
    }

    public static Set<String> getQSTags(String... fields) {
        // creates all combinations of 3 consecutive character as a set
        // eg: if invoked with getQSTags("sathya", "thilakan") the set
        // will be populated as: sat, ath, thy, hya, thi, hil, ila, aka, kan

        Set<String> set = new TreeSet<>();

        // if any of the field has a space then split it.
        List<String> fieldsAfterSplitting = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null)
                continue;

            String[] split = fields[i].split("\\s+");
            ensureNotNull(split, "array after splitting by \\s+ is null");

            for (int j = 0; j < split.length; j++) {
                fieldsAfterSplitting.add(split[j]);
            }
        }

        for (String field : fieldsAfterSplitting) {
            if (fields == null)
                continue;

            field = field.toLowerCase();
            if (field.length() < 3)
                continue;

            for (int beginIndex = 0; beginIndex < field.length() - 2; beginIndex++) {
                set.add(field.substring(beginIndex, beginIndex + 3));
            }
        }

        return set;
    }

    public static boolean closeEnough(String s1, String s2) {

        if ((s1 == null) || (s2 == null))
            return false;

        s1 = s1.toLowerCase().replaceAll(" ", "");
        s2 = s2.toLowerCase().replaceAll(" ", "");

        // if less than or equal to 3 char, entire string should match
        if (s1.length() <= 3)
            return s1.equals(s2);

        // if first 3 chars match return true
        if (s1.substring(0, 3).equals(s2.substring(0, 3)))
            return true;

        Set<String> s1QSTags = getQSTags(s1);
        Set<String> s2QSTags = getQSTags(s2);

        s1QSTags.retainAll(s2QSTags);

        return s1QSTags.size() > Math.min(s1.length(), s1.length()) / 3;
    }

    public static boolean isValidEmailAddress(String email) {
        if (null == email)
            return false;

        boolean stricterFilter = true;
        String stricterFilterString = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
        String laxString = ".+@.+\\.[A-Za-z]{2}[A-Za-z]*";
        String emailRegex = stricterFilter ? stricterFilterString : laxString;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(emailRegex);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public static boolean isPresentInListCaseInsensitive(List<String> list, String s) {

        if ((null == list) || (null == s))
            return false;

        for (String element : list) {
            if (element.equalsIgnoreCase(s))
                return true;
        }

        return false;
    }

    public static String getLoginEmail(User user) {
        if (null == user)
            return null;

        return user.getEmail();
    }

    public static void throwIncorrectSpecException(String message) {
        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(message);
    }

    public static void throwNotFoundException(String message) {
        throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(message);
    }

    public static void throwAlreadyExistsException(String message) {
        throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(message);
    }

    public static void ensureValidPhoneNumber(String phoneNumber) {
        // TODO: remove code duplication in isValidPhoneNumber function
        final String phoneNumberRegex =
                "\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|"
                        + "2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|"
                        + "4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$";

        if ((phoneNumber == null) || (phoneNumber.equals("")))
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Phone number cannot be null or empty");

        if (phoneNumber.charAt(0) != '+')
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid phone no [" + phoneNumber + "]. First character should be +");

        if (!phoneNumber.matches(phoneNumberRegex))
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Phone number [" + phoneNumber + "] is invalid");
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {

        // Make this function also take in country as input and do a stricter
        // validation

        final String phoneNumberRegex =
                "\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|"
                        + "2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|"
                        + "4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$";

        if ((phoneNumber == null) || phoneNumber.equals(""))
            return false;

        if (phoneNumber.charAt(0) != '+')
            return false;

        if (!phoneNumber.matches(phoneNumberRegex))
            return false;

        return true;
    }

    public static String getPhoneNoErrMsgIfAnyElseNull(String phoneNumber, String country,
                                                       PhoneNoType phoneNoType) {

        // Make this function also take in country as input and do a stricter
        // validation

        CountryProp countryProp = RefData.getCountryProp(country);
        if (countryProp == null)
            return "["
                    + country
                    + "] is not a valid country or not yet added to IshaCRM. To add a country to IshaCRM please email ["
                    + "sathya.t@ishafoundation.org]";

        if (phoneNoType == PhoneNoType.LANDLINE) {
            if (!phoneNumber.matches(countryProp.landlineRegex))
                return countryProp.messageIfError;
        } else if (phoneNoType == PhoneNoType.MOBILE) {
            if (!phoneNumber.matches(countryProp.mobileRegex))
                return countryProp.messageIfError;
        } else {
            // should never come here
            throw new APIException().status(Status.ERROR_INTERNAL).message(
                    "Internal error when validating phone no");
        }

        return null;
    }

    public static String getPhoneNoErrMsgIfAnyElseNull(String phoneNo, String country) {

        // Make this function also take in country as input and do a stricter
        // validation

        CountryProp countryProp = RefData.getCountryProp(country);
        if (countryProp == null)
            return "["
                    + country
                    + "] is not a valid country or not yet added to IshaCRM. To add a country to IshaCRM please email ["
                    + "sathya.t@ishafoundation.org]";

        if (!phoneNo.matches(countryProp.landlineRegex) && !phoneNo.matches(countryProp.mobileRegex)) {
            return countryProp.messageIfError;
        }

        return null;
    }

    public static void ensureValidEmail(String email) {
        if ((email == null) || (email.equals("")))
            throw new APIException("Email cannot be null or empty")
                    .status(Status.ERROR_RESOURCE_INCORRECT);

        boolean valid = Utils.isValidEmailAddress(email);

        if (valid == false)
            throw new APIException("Email [" + email + "] is invalid")
                    .status(Status.ERROR_RESOURCE_INCORRECT);
    }

    public static void ensureValidUrl(String url) {

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            Utils.throwIncorrectSpecException("URL [" + url + "] is invalid");
        }
    }

    public static long safeParseAsLong(String s) {
        try {
            ensureNotNull(s, "s is null");
            long l = Long.parseLong(s);
            return l;
        } catch (NumberFormatException e) {
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Cannot parse [" + s + "] as long");
        }
    }

    public static boolean canParseAsLong(String s) {
        try {
            safeParseAsLong(s);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static int safeParseAsInt(String s) {
        try {
            int i = Integer.parseInt(s);
            return i;
        } catch (NumberFormatException e) {
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Cannot parse [" + s + "] as integer");
        }
    }

    public static double safeParseAsDouble(String s) {
        try {
            double d = Double.parseDouble(s);
            return d;
        } catch (NumberFormatException e) {
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Cannot parse [" + s + "] as double");
        }
    }

    @SafeVarargs
    public static <T> List<T> getList(T... elements) {
        List<T> list = new ArrayList<>();

        for (T element : elements) {
            list.add(element);
        }

        return list;
    }

    @SafeVarargs
    public static <T> Set<T> getSet(T... elements) {
        Set<T> set = new HashSet<>();

        for (T element : elements) {
            set.add(element);
        }

        return set;
    }

    public static String getUrl(String baseUrl, Map<String, Object> queryParams) {
        Utils.ensureValidUrl(baseUrl);

        if (queryParams == null)
            return baseUrl;

        // sort query params by key
        Map<String, Object> treeMap = new TreeMap<>();
        treeMap.putAll(queryParams);

        queryParams = treeMap;

        StringBuilder builder = new StringBuilder(baseUrl);
        if (!baseUrl.contains("?"))
            builder.append("?");
        else
            builder.append("&");

        for (String key : queryParams.keySet()) {
            try {
                final String ENCODING_SCHEME = "UTF-8";
                String encodedKey = URLEncoder.encode(key, ENCODING_SCHEME);
                String encodedValue = URLEncoder.encode(queryParams.get(key).toString(), ENCODING_SCHEME);

                if (encodedValue.length() > 250)
                    encodedValue = encodedValue.substring(0, 250);

                builder.append(encodedKey + "=" + encodedValue + "&");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();

                throw new RuntimeException("Unsupported Encoding Exception. Message: " + e.getMessage());
            }
        }

        String url = builder.toString();

        // space should be encoded as %20, java encoder encodes it as a +
        url = url.replaceAll(Pattern.quote("+"), "%20");

        // should be valid - but just in case
        Utils.ensureValidUrl(url);

        return url;
    }

    public static void ensureNonNegative(double n) {
        if (n < 0)
            throwIncorrectSpecException("Specified number [" + n + "] is negative");
    }

    public static int getNumDays(Date former, Date later) {
        final int MILLI_SECONDS_IN_A_DAY = 3600 * 24 * 1000;

        int numDays = (int) (former.getTime() - later.getTime()) / MILLI_SECONDS_IN_A_DAY;

        return numDays;
    }

    public static String getFullName(String firstName, String lastName) {
        if ((firstName == null) && (lastName == null))
            return null;

        if (firstName == null)
            return lastName;

        if (lastName == null)
            return firstName;

        return firstName + " " + lastName;
    }

    public static String asCurrencyString(double d) {
        DecimalFormat df = new DecimalFormat("#.00");
        return df.format(d);
    }

    public static String urlEncode(String url) {
        try {
            url = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new APIException().status(Status.ERROR_OPERATION_NOT_ALLOWED).message(
                    "Exception thrown by URLEncoder.encode for url [" + url + "]");
        }

        url.replaceAll(Pattern.quote("+"), "%20");
        return url;
    }

    public static void ensureNotNullOrEmpty(String value, String errMessage) {
        if ((value == null) || value.equals(""))
            Utils.throwIncorrectSpecException(errMessage);
    }

    public static void ensureNonZero(long value, String errMessage) {
        if (value == 0)
            Utils.throwIncorrectSpecException(errMessage);
    }

    public static String csvEncode(String s) {
        if (s == null)
            return "";

        s = s.replaceAll(Pattern.quote("\""), "");

        if (s.contains(",") || s.contains("\r\n"))
            return "\"" + s + "\"";

        return s;
    }

    public static String stackTraceToString_old(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        return sw.toString();
    }

    public static String stackTraceToString(Throwable e) {

        if (e == null)
            return null;

        StringBuilder builder = new StringBuilder();

        builder.append(e.getMessage() + "<br>");

        StackTraceElement[] stElements = e.getStackTrace();

        for (int i = 0; i < stElements.length; i++) {
            builder.append(stElements[i].toString());
            builder.append("<br>");
        }

        return builder.toString();
    }

    // TODO: move this to incidents package later
    public static void sendAlertEmailToDevTeam(String client, Exception ex, HttpServletRequest req,
                                               String login) {

        try {
            ensureNotNull(ex, "Exception is null");

            // to address
            EmailProp emailProp = new EmailProp();
            emailProp.toEmailAddresses = ConfigCRMDNA.get().toProp().devTeamEmails;

            if (emailProp.toEmailAddresses.size() == 0)
                emailProp.toEmailAddresses.add(crmdna.user.User.SUPER_USER);

            if (client == null)
                client = "Not Available";

            // subject
            emailProp.subject = "Unhandled exception for [" + client + "]: " + ex.getMessage();

            // body
            StringBuilder builder = new StringBuilder();
            builder.append("<br><i>Logged in user:</i> " + login + "<br><br>");
            builder.append("<i>Timestamp:</i> " + new Date() + "<br><br>");

            if (req != null) {
                builder.append("<i>Request:</i> " + req.getRequestURI() + "<br><br>");

                @SuppressWarnings("unchecked")
                Enumeration<String> parameterNames = req.getParameterNames();

                if (parameterNames.hasMoreElements()) {
                    builder.append("<i>Query parameters: </i><br>");
                    while (parameterNames.hasMoreElements()) {
                        String key = (String) parameterNames.nextElement();
                        String value = req.getParameter(key);
                        builder.append(key + ": " + value + "<br>");
                    }
                }
                builder.append("<br>");

                @SuppressWarnings("unchecked")
                Enumeration<String> headerNames = req.getParameterNames();
                if (headerNames.hasMoreElements()) {
                    builder.append("<i>Headers: </i><br>");
                    while (headerNames.hasMoreElements()) {
                        String key = (String) parameterNames.nextElement();
                        String value = req.getParameter(key);
                        builder.append(key + ": " + value + "<br>");
                    }
                }
            }

            builder.append("<br>");
            builder.append("<i>Exception message:</i> " + ex.getMessage());
            builder.append("<br><br>");
            builder.append("<i>Stack trace:</i><br>");
            builder.append(stackTraceToString(ex));

            emailProp.bodyHtml = builder.toString();

            GAEEmail.send(emailProp);
        } catch (Exception exception) {
            // This is usually called in response to a unhandled exception.
            // just log the exception and swallow it
            Logger logger = Logger.getLogger(Utils.class.getName());
            logger.severe(stackTraceToString(exception));
        }
    }

    // TODO: move this to incidents package later
    public static void sendAlertEmailToDevTeam(Exception ex, RequestInfo requestInfo) {

        try {
            if (ex == null)
                return;

            // requestInfo can be null

            // to address
            EmailProp emailProp = new EmailProp();
            emailProp.toEmailAddresses = ConfigCRMDNA.get().toProp().devTeamEmails;

            if (emailProp.toEmailAddresses.size() == 0)
                emailProp.toEmailAddresses.add(crmdna.user.User.SUPER_USER);

            String client = "Not Available";
            if ((requestInfo != null) && (requestInfo.getClient() != null))
                client = requestInfo.getClient();

            // subject
            emailProp.subject = "Unhandled exception for [" + client + "]: " + ex.getMessage();

            // body
            String login = "Not Available";
            if ((requestInfo != null) && (requestInfo.getLogin() != null))
                login = requestInfo.getLogin();
            StringBuilder builder = new StringBuilder();
            builder.append("<br><i>Logged in user:</i> " + login + "<br><br>");
            builder.append("<i>Timestamp:</i> " + new Date() + "<br><br>");

            String requestURI = "Not Available";
            if ((requestInfo != null) && (requestInfo.getReq() != null)) {
                requestURI = requestInfo.getReq().getRequestURI();

                builder.append("<i>Request:</i> " + requestURI + "<br><br>");

                @SuppressWarnings("unchecked")
                Enumeration<String> parameterNames = requestInfo.getReq().getParameterNames();

                if (parameterNames.hasMoreElements()) {
                    builder.append("<i>Query parameters: </i><br>");
                    while (parameterNames.hasMoreElements()) {
                        String key = (String) parameterNames.nextElement();
                        String value = requestInfo.getReq().getParameter(key);
                        builder.append(key + ": " + value + "<br>");
                    }
                }

                builder.append("<br>");

                @SuppressWarnings("unchecked")
                Enumeration<String> headerNames = requestInfo.getReq().getParameterNames();
                if (headerNames.hasMoreElements()) {
                    builder.append("<i>Headers: </i><br>");
                    while (headerNames.hasMoreElements()) {
                        String key = (String) headerNames.nextElement();
                        String value = requestInfo.getReq().getParameter(key);
                        builder.append(key + ": " + value + "<br>");
                    }
                }
            }

            builder.append("<br>");
            builder.append("<i>Exception message:</i> " + ex.getMessage());
            builder.append("<br><br>");
            builder.append("<i>Stack trace:</i><br>");
            builder.append(stackTraceToString(ex));

            emailProp.bodyHtml = builder.toString();

            GAEEmail.send(emailProp);
        } catch (Exception exception) {
            // This is usually called in response to an unhandled exception.
            // just log the exception and swallow it
            Logger logger = Logger.getLogger(Utils.class.getName());
            logger.severe(stackTraceToString(exception));
        }
    }

    public static String sanitizePhoneNo_do_not_use(String phoneNo) {
        if (phoneNo == null)
            return null;

        // remove - and empty spaces
        phoneNo = phoneNo.replaceAll(Pattern.quote("-"), "");
        phoneNo = phoneNo.replaceAll(Pattern.quote(" "), "");

        // replace leading 00 with +
        phoneNo = phoneNo.replace("(^[0][0])", "+");

        // remove leading zeros
        phoneNo = phoneNo.replaceAll("^0+", "");

        if (phoneNo.equals(""))
            return null;

        // if it does not contain + add it
        if ((phoneNo.length() > 1) && (phoneNo.charAt(0) != '+'))
            phoneNo = "+" + phoneNo;

        return phoneNo;
    }

    public static String removeSpaceUnderscoreBracketAndHyphen(String s) {
        if (s == null)
            return null;

        s = s.replaceAll(Pattern.quote("_"), "");
        s = s.replaceAll(Pattern.quote(" "), "");
        s = s.replaceAll(Pattern.quote("-"), "");
        s = s.replaceAll(Pattern.quote("("), "");
        s = s.replaceAll(Pattern.quote(")"), "");

        return s;
    }

    public static String sanitizePhoneNo(String phoneNo, String country) {
        if (phoneNo == null)
            return null;

        // remove - and empty spaces
        phoneNo = phoneNo.replaceAll(Pattern.quote("-"), "");
        phoneNo = phoneNo.replaceAll(Pattern.quote(" "), "");

        // remove ( and )
        phoneNo = phoneNo.replaceAll(Pattern.quote("("), "");
        phoneNo = phoneNo.replaceAll(Pattern.quote(")"), "");

        // add isd code if not specified
        // for eg: if phone no is 93232152 and country is singapore make it
        // +6593232152
        if (country != null) {
            CountryProp countryProp = RefData.getCountryProp(country);
            if (countryProp != null) {
                if (countryProp.numDigitsWOCountryCode != null) {
                    if ((phoneNo.length() == countryProp.numDigitsWOCountryCode) && !phoneNo.contains("+")) {
                        phoneNo = countryProp.isdCode + phoneNo;
                    }
                }
            }
        }

        // replace leading 00 with ""
        phoneNo = phoneNo.replaceFirst("^(0*)", "");

        if (phoneNo.equals(""))
            return null;

        // if it does not contain + add it
        if ((phoneNo.length() > 1) && (phoneNo.charAt(0) != '+'))
            phoneNo = "+" + phoneNo;

        return phoneNo;
    }

    public static String sanitizeEmail(String email) {
        if ((email == null) || email.equals(""))
            return null;

        return email;
    }

    // TODO: check if this method is really required. probably can be removed
    public static <T> String toUserFriendlyString(Iterable<T> iterable) {
        if (iterable == null)
            return null;

        StringBuilder builder = new StringBuilder();

        int lineNo = 0;
        for (T t : iterable) {
            lineNo++;

            builder.append("<br>" + lineNo + ") - " + t);
        }

        return builder.toString();
    }

    public static boolean isDifferentCaseInsensitive(String s1, String s2) {
        if ((s1 == null) || (s2 == null))
            return true;

        return !s1.toLowerCase().equals(s2.toLowerCase());
    }

    public static double getWeightedAvg(List<Double> elements, List<Double> weights) {

        ensure(!elements.isEmpty(), "No elements specified");

        ensureEqual(elements.size(), weights.size(), "Num elements [" + elements.size()
                + "] does not match num weights [" + weights.size() + "]");

        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < weights.size(); i++) {
            denominator += weights.get(i);
            numerator += elements.get(i) * weights.get(i);
        }

        ensure(denominator != 0, "Sum of weights is [0]");

        return numerator / denominator;
    }

    public static String getFirstNChar(String s, int n) {
        ensureNotNull(s, "s is null");

        ensure(n >= 0, "invalid n [" + n + "]. should be greater than or equal to 0");

        if (s.length() <= n)
            return s;

        return s.substring(0, n);
    }

    public static String readDataFromURL(String urlAddress) throws IOException {

        URL url = new URL(urlAddress);

        StringBuilder builder = new StringBuilder();

        try (InputStreamReader inputStreamReader = new InputStreamReader(url.openStream())) {
            try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    builder.append(line);
                }
            }
        }

        return builder.toString();
    }

    public static Set<String> getHrefs(String html) {

        ensureNotNull(html, "html is null");

        final String HREF_PATTERN = "href=\"([^\"]*)\"";
        Matcher matcher = Pattern.compile(HREF_PATTERN).matcher(html);

        Set<String> hrefs = new HashSet<>();
        while (matcher.find()) {
            String s = matcher.group(1);
            hrefs.add(s);
        }

        return hrefs;
    }

    public static String getRandomAlphaNumericString(int numChars) {
        ensure(numChars > 0, "numChars [" + numChars + "] is not positive");

        String all = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        ensureEqual(62, all.length());

        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numChars; i++) {
            char c = all.charAt(random.nextInt(all.length()));
            builder.append(c);
        }

        return builder.toString();
    }

    public static <T> Iterable<T> safe(Iterable<T> iterable) {
        return iterable == null ? Collections.<T>emptyList() : iterable;
    }

    public enum Currency {
        SGD, USD, INR, MYR, AUD, GBP
    }

    public enum PaypalErrorType {
        PAYPAL_SET_EXPRESS_CHECKOUT_FAILURE, PAYPAL_GET_EXPRESS_CHECKOUT_FAILURE, PAYPAL_DO_EXPRESS_CHECKOUT_FAILURE
    }

    public enum PhoneNoType {
        LANDLINE, MOBILE
    }
}
