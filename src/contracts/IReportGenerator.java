package contracts;

import java.util.Date;

public interface IReportGenerator {
    double viewDailySale(Date date);
    double viewWeeklySales(Date weekStart);
     int viewLoyaltyRedemptions();
}
