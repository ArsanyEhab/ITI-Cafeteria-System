package services;

import contracts.IMenuAdmin;
import contracts.IMenuProvider;
import domain.MenuItem;

import java.util.List;

public class MenuManager implements IMenuAdmin {
    private IMenuAdmin menuRepository;
    private IMenuProvider menuProvider;

    // Backward compatible constructor for existing app
    public MenuManager() {
        infrastructure.MenuOperationsRepository menuOpsRepo = new infrastructure.MenuOperationsRepository();
        this.menuRepository = menuOpsRepo;
        this.menuProvider = menuOpsRepo;
    }

    // Constructor for dependency injection
    public MenuManager(IMenuAdmin menuRepository, IMenuProvider menuProvider) {
        this.menuRepository = menuRepository;
        this.menuProvider = menuProvider;
    }

    // Staff methods - delegate to repository
    @Override
    public MenuItem addMenuItem(String name, String description, double price, String category) {
        return menuRepository.addMenuItem(name, description, price, category);
    }

    @Override
    public void editMenuItem(int id, String name, String description, double price, String category) {
        menuRepository.editMenuItem(id, name, description, price, category);
    }

    @Override
    public void removeMenuItem(int id) {
        menuRepository.removeMenuItem(id);
    }

    // Additional method for getting items
    public List<MenuItem> getItems() {
        return menuProvider.getMenu();
    }
}

