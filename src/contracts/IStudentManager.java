package contracts;

import domain.IStudent;

public interface IStudentManager {
    IStudent registerStudent(String name, String studentId ,String password);
    IStudent login(String studentId,String name,String password);
}
