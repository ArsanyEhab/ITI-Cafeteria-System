package infrastructure;

import contracts.IUserRepository;
import domain.Student;

/**
 * Backward compatibility class that delegates to the infrastructure DatabaseUserRepository
 * This is placed in services package to be picked up by the services.* import in CafeteriaApp
 */
public class InMemoryUserRepository implements IUserRepository {
    private infrastructure.DatabaseUserRepository databaseUserRepository;

    public InMemoryUserRepository() {
        this.databaseUserRepository = new infrastructure.DatabaseUserRepository();
    }

    @Override
    public Student findById(String studentId) {
        return databaseUserRepository.findById(studentId);
    }

    @Override
    public void save(Student student) {
        databaseUserRepository.save(student);
    }
    
    @Override
    public boolean verifyPassword(String studentId, String password) {
        return databaseUserRepository.verifyPassword(studentId, password);
    }
}
