package uk.ac.ebi.ddi.annotation.utils;

/**
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @date 20/10/15
 */
class Utils {

    public static String encode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (isUnsafe(ch)) {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            } else {
                resultStr.append(ch);
            }
        }
        return resultStr.toString();
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private static boolean isUnsafe(char ch) {
        return ch > 128 || ch < 0 || " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
    }
}