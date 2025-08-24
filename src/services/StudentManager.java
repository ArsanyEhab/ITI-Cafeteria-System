package services;

import contracts.INotificationService;
import contracts.IStudentManager;
import contracts.IUserRepository;
import domain.IStudent;
import domain.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentManager implements IStudentManager {
    private List<IStudent> students = new ArrayList<>();
    IUserRepository userRepo;
    INotificationService notifier;

    @Override
    public IStudent registerStudent(String name, String studentID,String password) {
        for (IStudent s : students) {
            if (s.getStudentID().equals(studentID)&&s.getPassword().equals(password)) {
                System.out.println(" Student ID already exists");
                return null;
            }
        }

        IStudent student = new Student(name, studentID,password);
        students.add(student);
        System.out.println(" Student registered: " + name);
        return student;
    }
    @Override
    public IStudent login(String studentID, String name,String password) {
        for (IStudent s : students) {
            if (s.getStudentID().equals(studentID) && s.getName().equals(name)) {
                System.out.println(" Login successful for: " + s.getName());
                return s;
            }
        }
        System.out.println("Invalid studentID or password!");
        return null;
    }
    public  StudentManager(IUserRepository userRepo, INotificationService notifier){
        this.userRepo=userRepo;
        this.notifier=notifier;

    }
}
