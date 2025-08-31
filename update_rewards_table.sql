-- Migration script to update rewards table structure
-- Run this script to fix the rewards table schema

-- Step 1: Add the new column
ALTER TABLE rewards ADD COLUMN rewarded_item_id INT AFTER points_required;

-- Step 2: Update existing data to convert rewarded_item (VARCHAR) to rewarded_item_id (INT)
-- This assumes the existing rewarded_item values are valid menu_item_id numbers
UPDATE rewards SET rewarded_item_id = CAST(rewarded_item AS UNSIGNED) WHERE rewarded_item REGEXP '^[0-9]+$';

-- Step 3: Remove the old column
ALTER TABLE rewards DROP COLUMN rewarded_item;

-- Step 4: Make the new column NOT NULL
ALTER TABLE rewards MODIFY COLUMN rewarded_item_id INT NOT NULL;

-- Step 5: Add foreign key constraint (optional but recommended)
-- ALTER TABLE rewards ADD CONSTRAINT fk_rewards_menu_item FOREIGN KEY (rewarded_item_id) REFERENCES menu_items(menu_item_id);

-- Verify the changes
SELECT * FROM rewards;
