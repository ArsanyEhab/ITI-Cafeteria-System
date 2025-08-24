package contracts;
import domain.Student;

public interface IUserRepository {
    Student findById(String studentId);
    void save(Student student);


}
