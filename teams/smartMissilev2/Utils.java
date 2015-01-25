package smartMissilev2;

public class Utils {
    
    public static <T> String printArray(T[] array) {
        StringBuilder printout = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            printout.append(i + ": " + array[i] + " ");
        }
        return printout.toString();
    }

}
