-- Create rewards table for loyalty program rewards
CREATE TABLE IF NOT EXISTS rewards (
    reward_id INT AUTO_INCREMENT PRIMARY KEY,
    reward_name VARCHAR(100) NOT NULL,
    points_required INT NOT NULL,
    rewarded_item_id INT NOT NULL
);

-- Insert sample rewards data
-- Note: rewarded_item_id should contain the menu_item_id from the menu_items table
INSERT INTO rewards (reward_name, points_required, rewarded_item_id) VALUES 
('Free Coffee', 100, 7),  -- Coffee (menu_item_id: 7)
('Free Tea', 50, 8),      -- Tea (menu_item_id: 8)
('Free Burger', 150, 5);  -- Single Burger (menu_item_id: 5)

-- Note: The rewarded_item_id field should contain the menu_item_id from the menu_items table
-- This allows the admin to control which specific items can be redeemed

-- Create exchange_points table for controlling points-to-EGP exchange rate
CREATE TABLE IF NOT EXISTS exchange_points (
    id INT AUTO_INCREMENT PRIMARY KEY,
    points_per_egp INT NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default exchange rate (e.g., 50 points = 1 EGP)
INSERT INTO exchange_points (points_per_egp) VALUES (50);


