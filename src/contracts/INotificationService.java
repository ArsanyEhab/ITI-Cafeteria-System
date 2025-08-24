package contracts;

import java.util.List;

public interface INotificationService {
    void   sendNotification(String studentId  ,String message) ;
    List<String>getNotificationsFor(String studentId);
  void  displayNotifications(String userId);


}
