-- YouthMind Database Setup Script
-- Run this to create the database and user

-- Create database
CREATE DATABASE youthmind_db;

-- Create user
CREATE USER youthmind_user WITH PASSWORD 'password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE youthmind_db TO youthmind_user;

-- Connect to the new database
\c youthmind_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO youthmind_user;

