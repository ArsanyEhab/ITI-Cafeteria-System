package services;

import contracts.IOrderRepository;
import contracts.IReportGenerator;
import domain.*;
import java.util.*;
public class ReportGeneratorImpl implements IReportGenerator {
    private IOrderRepository orderRepo;
    private LoyaltyProgram loyaltyProgram;


    public ReportGeneratorImpl(IOrderRepository orderRepo,LoyaltyProgram loyaltyProgram) {
        this.orderRepo = orderRepo;
        this.loyaltyProgram=loyaltyProgram;

    }

    // FR6.1 Daily sales
    public double viewDailySale(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        return orderRepo.getAllOrders().stream()
                .filter(o -> {
                    Calendar c = Calendar.getInstance();
                    c.setTime(o.getDate());
                    return c.get(Calendar.YEAR) == year &&
                            c.get(Calendar.DAY_OF_YEAR) == dayOfYear;
                })
                .mapToDouble(Order::getTotalCost)
                .sum();
    }

    // FR6.1 Weekly sales
    public double viewWeeklySales(Date weekStart) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(weekStart);

        Date start = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date end = cal.getTime();

        return orderRepo.getAllOrders().stream()
                .filter(o -> !o.getDate().before(start) && o.getDate().before(end))
                .mapToDouble(Order::getTotalCost)
                .sum();
    }

    // FR6.1 Loyalty Redemptions
    public int viewLoyaltyRedemptions() {

        return loyaltyProgram.getTotalRedeemedPoints();
    }
}
