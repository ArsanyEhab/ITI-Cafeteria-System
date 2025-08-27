package services;

import contracts.INotificationService;
import contracts.IStudentManager;
import contracts.IUserRepository;
import domain.IStudent;
import domain.Student;

public class StudentManager implements IStudentManager {
    private IStudentManager studentRepository;
    private IUserRepository userRepo;

    public StudentManager(IStudentManager studentRepository, IUserRepository userRepo, INotificationService notifier) {
        this.studentRepository = studentRepository;
        this.userRepo = userRepo;
        // notifier can be used for future notifications if needed
    }

    @Override
    public IStudent registerStudent(String name, String studentID, String password) {
        IStudent student = studentRepository.registerStudent(name, studentID, password);
        if (student != null) {
            // Save to user repository as well for consistency
            userRepo.save((Student) student);
        }
        return student;
    }

    @Override
    public IStudent login(String studentID, String name, String password) {
        return studentRepository.login(studentID, name, password);
    }
}
