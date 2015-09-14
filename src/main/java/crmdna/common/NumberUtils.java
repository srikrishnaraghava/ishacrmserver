package crmdna.common;

import java.text.DecimalFormat;

public class NumberUtils {

    private static final String[] tens =
        {"", " ten", " twenty", " thirty", " forty", " fifty", " sixty", " seventy", " eighty",
            " ninety"};

    private static final String[] nums =
        {"", " one", " two", " three", " four", " five", " six", " seven", " eight", " nine",
            " ten", " eleven", " twelve", " thirteen", " fourteen", " fifteen", " sixteen",
            " seventeen", " eighteen", " nineteen"};

    private static String convertLessThanOneThousand(int number) {
        String soFar;

        if (number % 100 < 20) {
            soFar = nums[number % 100];
            number /= 100;
        } else {
            soFar = nums[number % 10];
            number /= 10;

            soFar = tens[number % 10] + soFar;
            number /= 10;
        }
        if (number == 0)
            return soFar;

        if (soFar.equals("")) {
            return nums[number] + " hundred";
        } else {
            return nums[number] + " hundred and" + soFar;
        }
    }

    private static void convertAndAppend(String part, String unit, StringBuilder sb) {
        int partInt = Integer.parseInt(part);
        if (partInt > 0) {
            sb.append(convertLessThanOneThousand(partInt)).append(" ").append(unit).append(" ");
        }
    }

    private static String toWords(long number) {
        // 0 to 999 999 999 999
        if (number == 0) {
            return "zero";
        }

        // pad with "0"
        String mask = "000000000000";
        DecimalFormat df = new DecimalFormat(mask);
        String snumber = df.format(number);
        StringBuilder sb = new StringBuilder();
        int i = 0;

        // XXXnnnnnnnnn
        convertAndAppend(snumber.substring(i, i + 3), "billion", sb); i += 3;

        // nnnXXXnnnnnn
        convertAndAppend(snumber.substring(i, i + 3), "million", sb); i += 3;

        // nnnnnnXXXnnn
        convertAndAppend(snumber.substring(i, i + 3), "thousand", sb); i += 3;

        // nnnnnnnnnXXX
        convertAndAppend(snumber.substring(i, i + 3), "", sb);

        // remove extra spaces!
        return sb.toString().replaceAll("^\\s+", "").replaceAll("\\s+$", "").replaceAll("\\b\\s{2,}\\b", " ");
    }

    public static DecimalNumberString toWords(double number) {

        int decimal = (int) number;
        int mantissa = (int) ((number - decimal) * 100);

        DecimalNumberString result = new DecimalNumberString();
        result.whole = toWords(decimal);
        if (mantissa > 0) {
            result.fraction = toWords(mantissa);
        }

        return result;
    }

    public static class DecimalNumberString {
        public String whole;
        public String fraction;
    }
}
