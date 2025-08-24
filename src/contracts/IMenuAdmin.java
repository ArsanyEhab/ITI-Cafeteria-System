package contracts;

import domain.MenuItem;

public interface IMenuAdmin {
    MenuItem addMenuItem(String name,String  description,double price,String category );
    void editMenuItem(int id, String name,String description,double price , String category );
    void removeMenuItem(int id);
}
