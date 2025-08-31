-- SQL script to add is_read column to notifications table
-- This is optional - the application will work without it

-- Check if column already exists
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'notifications' 
    AND COLUMN_NAME = 'is_read'
);

-- Add column if it doesn't exist
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE notifications ADD COLUMN is_read BOOLEAN DEFAULT FALSE',
    'SELECT "Column is_read already exists" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Update existing notifications to mark them as read (optional)
-- UPDATE notifications SET is_read = TRUE WHERE created_at < NOW() - INTERVAL 1 DAY;

-- Show current table structure
DESCRIBE notifications;
