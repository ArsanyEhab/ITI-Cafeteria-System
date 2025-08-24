package services;

import CrossCutting.IdGenerator;
import contracts.IMenuAdmin;
import contracts.IMenuProvider;
import domain.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MenuManager implements IMenuAdmin {
    private List<MenuItem> items = new ArrayList<>();

    // Staff methods
    public MenuItem addMenuItem(String name, String description, double price, String category) {
        int id = IdGenerator.getInstance().generateNewMenuItemId(); // Generate unique ID
        MenuItem newItem = new MenuItem(id, name, description, price, category);
        items.add(newItem);   // add to list
        return newItem;
    }

    public void editMenuItem(int id, String name, String description, double price, String category) {
        for (MenuItem item : items) {
            if (item.getId() == id) {
                items.remove(item);
                items.add(new MenuItem(id, name, description, price, category));
                break;
            }
        }
    }

    public void removeMenuItem(int id) {
        items.removeIf(item -> item.getId() == id);
    }
    public List<MenuItem> getItems() {
        return items;
    }

}

