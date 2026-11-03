-- =============================================================================
-- GestureTalk AI — Database Schema
-- Database Name: gesturetalk_db
-- Target: MySQL 8.0+ / 8.4 LTS (Compatible with H2 MySQL Mode)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `gesturetalk_db`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `gesturetalk_db`;

-- Core Audit & Recognition Log Table
CREATE TABLE IF NOT EXISTS `gesture_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `gesture_code` VARCHAR(50) NOT NULL,
  `mapped_message` VARCHAR(255) NOT NULL,
  `explanation` TEXT NULL,
  `confidence` DECIMAL(4,3) NOT NULL DEFAULT 0.000,
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'WEBCAM',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

-- Indexes for performance optimization on queries and filtering
CREATE INDEX `idx_gesture_code` ON `gesture_history` (`gesture_code`);
CREATE INDEX `idx_created_at` ON `gesture_history` (`created_at`);

