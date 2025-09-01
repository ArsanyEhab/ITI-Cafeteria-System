package services;

import domain.MenuItem;
import domain.Order;
import infrastructure.DatabaseOrderRepository;
import infrastructure.DatabaseRepository;

import java.sql.*;
import java.util.*;

public class RecommendationSystem {
    private Map<Integer, Double> totalRewards = new HashMap<>();
    private Map<Integer, Integer> playCounts = new HashMap<>();
    private int totalPlays = 0;
    private final DatabaseOrderRepository orderRepo;

    public RecommendationSystem() {
        this.orderRepo = new DatabaseOrderRepository();
        loadHistoricalData();
    }

    // Load historical order data to initialize the recommendation system
    private void loadHistoricalData() {
        try {
            List<Order> allOrders = orderRepo.getAllOrders();
            for (Order order : allOrders) {
                if (order.getItems() != null) {
                    for (MenuItem item : order.getItems()) {
                        update(item);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to load historical data for recommendations: " + e.getMessage());
        }
    }

    // Update reward of an item after being chosen
    public void update(MenuItem item) {
        int id = item.getId();
        totalRewards.put(id, totalRewards.getOrDefault(id, 0.0) + 1.0);
        playCounts.put(id, playCounts.getOrDefault(id, 0) + 1);
        totalPlays++;
    }

    // Update based on order completion (higher reward for completed orders)
    public void updateFromOrder(Order order) {
        if (order.getItems() != null) {
            // Give higher reward for completed orders
            double rewardMultiplier = "completed".equalsIgnoreCase(order.getStatus()) ? 2.0 : 1.0;
            
            for (MenuItem item : order.getItems()) {
                int id = item.getId();
                totalRewards.put(id, totalRewards.getOrDefault(id, 0.0) + rewardMultiplier);
                playCounts.put(id, playCounts.getOrDefault(id, 0) + 1);
                totalPlays++;
            }
        }
    }

    // Recommend item based on UCB
    public MenuItem recommend(List<MenuItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        MenuItem bestItem = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (MenuItem item : items) {
            int id = item.getId();
            int count = playCounts.getOrDefault(id, 0);

            double ucbValue;
            if (count == 0) {
                ucbValue = Double.POSITIVE_INFINITY; // force exploration
            } else {
                double avgReward = totalRewards.get(id) / count;
                ucbValue = avgReward + Math.sqrt(2 * Math.log(totalPlays) / count);
            }

            if (ucbValue > bestValue) {
                bestValue = ucbValue;
                bestItem = item;
            }
        }
        return bestItem != null ? bestItem : items.get(0);
    }

    // Get top N recommendations
    public List<MenuItem> getTopRecommendations(List<MenuItem> allItems, int topN) {
        if (allItems == null || allItems.isEmpty()) {
            return new ArrayList<>();
        }

        List<MenuItem> recommendations = new ArrayList<>();
        List<MenuItem> availableItems = new ArrayList<>(allItems);

        for (int i = 0; i < Math.min(topN, allItems.size()); i++) {
            MenuItem recommended = recommend(availableItems);
            if (recommended != null) {
                recommendations.add(recommended);
                availableItems.remove(recommended);
            }
        }

        return recommendations;
    }

    // Get personalized recommendations based on student history
    public List<MenuItem> getPersonalizedRecommendations(String studentId, List<MenuItem> allItems, int topN) {
        List<MenuItem> personalizedItems = new ArrayList<>();
        
        try {
            // Get student's order history
            List<Order> studentOrders = orderRepo.getStudentOrders(studentId);
            Map<Integer, Integer> studentPreferences = new HashMap<>();
            
            // Analyze student's preferences
            for (Order order : studentOrders) {
                if (order.getItems() != null) {
                    for (MenuItem item : order.getItems()) {
                        int itemId = item.getId();
                        studentPreferences.put(itemId, studentPreferences.getOrDefault(itemId, 0) + 1);
                    }
                }
            }
            
            // Sort items by student preference and UCB score
            List<MenuItem> sortedItems = new ArrayList<>(allItems);
            sortedItems.sort((item1, item2) -> {
                int pref1 = studentPreferences.getOrDefault(item1.getId(), 0);
                int pref2 = studentPreferences.getOrDefault(item2.getId(), 0);
                
                if (pref1 != pref2) {
                    return Integer.compare(pref2, pref1); // Higher preference first
                }
                
                // If same preference, use UCB score
                double ucb1 = getUCBValue(item1);
                double ucb2 = getUCBValue(item2);
                return Double.compare(ucb2, ucb1);
            });
            
            // Return top N items
            for (int i = 0; i < Math.min(topN, sortedItems.size()); i++) {
                personalizedItems.add(sortedItems.get(i));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to get personalized recommendations: " + e.getMessage());
            // Fallback to general recommendations
            return getTopRecommendations(allItems, topN);
        }
        
        return personalizedItems;
    }

    // Get UCB value for a specific item
    private double getUCBValue(MenuItem item) {
        int id = item.getId();
        int count = playCounts.getOrDefault(id, 0);
        
        if (count == 0) {
            return Double.POSITIVE_INFINITY;
        } else {
            double avgReward = totalRewards.get(id) / count;
            return avgReward + Math.sqrt(2 * Math.log(totalPlays) / count);
        }
    }

    // Get recommendation statistics
    public Map<String, Object> getRecommendationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_plays", totalPlays);
        stats.put("unique_items", playCounts.size());
        stats.put("total_rewards", totalRewards.values().stream().mapToDouble(Double::doubleValue).sum());
        
        // Calculate average reward per item
        if (!playCounts.isEmpty()) {
            double avgReward = totalRewards.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            stats.put("average_reward_per_item", avgReward);
        }
        
        return stats;
    }

    // Get top performing items based on UCB scores
    public List<Map<String, Object>> getTopPerformingItems(List<MenuItem> allItems, int topN) {
        List<Map<String, Object>> topItems = new ArrayList<>();
        
        if (allItems == null || allItems.isEmpty()) {
            return topItems;
        }
        
        // Calculate UCB values for all items
        List<Map<String, Object>> itemsWithUCB = new ArrayList<>();
        for (MenuItem item : allItems) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("item", item);
            itemData.put("ucb_value", getUCBValue(item));
            itemData.put("play_count", playCounts.getOrDefault(item.getId(), 0));
            itemData.put("total_reward", totalRewards.getOrDefault(item.getId(), 0.0));
            itemsWithUCB.add(itemData);
        }
        
        // Sort by UCB value
        itemsWithUCB.sort((a, b) -> Double.compare(
            (Double) b.get("ucb_value"), 
            (Double) a.get("ucb_value")
        ));
        
        // Return top N
        for (int i = 0; i < Math.min(topN, itemsWithUCB.size()); i++) {
            topItems.add(itemsWithUCB.get(i));
        }
        
        return topItems;
    }

    // Reset recommendation system (for testing or fresh start)
    public void reset() {
        totalRewards.clear();
        playCounts.clear();
        totalPlays = 0;
        System.out.println("🔄 Recommendation system reset");
    }
}
