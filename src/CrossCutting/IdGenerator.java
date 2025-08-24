package CrossCutting;

public class IdGenerator {
    private static int lastOrderId=0;
    private static int lastMenuItemId=0;
    private static IdGenerator instance;
    private IdGenerator() {}
    public static IdGenerator getInstance(){
        if (instance == null) instance = new IdGenerator();
        return instance;
    }
    public int generateNewOrderId() {
        return ++lastOrderId; }
    public int generateNewMenuItemId() {
        return ++lastMenuItemId; }

}
