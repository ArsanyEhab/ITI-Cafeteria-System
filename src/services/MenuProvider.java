package services;

import contracts.IMenuProvider;
import domain.MenuItem;
import services.MenuManager;

import java.util.List;

public class MenuProvider implements IMenuProvider {
    private List<MenuItem> items;

    public MenuProvider(List<MenuItem> items) {
        this.items = items;
    }

    @Override
    public List<MenuItem> getMenu() {
        return items;
    }
}

