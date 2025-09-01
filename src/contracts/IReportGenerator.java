package contracts;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IReportGenerator {
    // Basic sales reports
    double viewDailySale(Date date);
    double viewWeeklySales(Date weekStart);
    double viewMonthlySales(Date monthStart);
    int viewLoyaltyRedemptions();
    
    // Advanced analytics
    Map<String, Object> getSalesAnalytics(Date startDate, Date endDate);
    List<Map<String, Object>> getTopSellingItems(int limit);
    Map<String, Double> getCategorySales(Date startDate, Date endDate);
    Map<String, Integer> getOrderStatusDistribution();
    Map<String, Object> getCustomerAnalytics();
    Map<String, Object> getRevenueTrends(Date startDate, Date endDate);
    
    // Detailed reports
    List<Map<String, Object>> getDailySalesBreakdown(Date date);
    List<Map<String, Object>> getHourlySalesPattern(Date date);
    Map<String, Object> getLoyaltyProgramAnalytics();
}
