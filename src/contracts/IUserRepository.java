package contracts;
import domain.Student;

public interface IUserRepository {
    Student findById(String studentId);
    void save(Student student);
    boolean verifyPassword(String studentId, String password);
}
