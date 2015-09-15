package crmdna.common;

import org.junit.Test;

import static org.junit.Assert.*;

public class NumberUtilsTest {

    private String convertToDollars(NumberUtils.DecimalNumberString number) {
        return "Dollars " + number.whole + ((number.fraction != null) ? (" and cents " + number.fraction) : "") ;
    }
    @Test
    public void testNumbersToWords() {
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(0)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(1)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(16.12)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(100)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(118)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(200)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(219)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(800)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(801.23)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(1316)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(1000000)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(2000000)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(3000200)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(700000)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(9000000)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(9001000)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(123456789)));
        System.out.println("*** " + convertToDollars(NumberUtils.toWords(2147483647)));
    }
}
