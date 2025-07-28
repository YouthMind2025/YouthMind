-- Sample Test Data for Performance Testing
-- Task 3: Query Optimization Testing

-- Insert sample users for testing
INSERT INTO users (id, email_hash, created_at) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'hash_user_001', NOW() - INTERVAL '30 days'),
('550e8400-e29b-41d4-a716-446655440002', 'hash_user_002', NOW() - INTERVAL '25 days'),
('550e8400-e29b-41d4-a716-446655440003', 'hash_user_003', NOW() - INTERVAL '20 days'),
('550e8400-e29b-41d4-a716-446655440004', 'hash_user_004', NOW() - INTERVAL '15 days'),
('550e8400-e29b-41d4-a716-446655440005', 'hash_user_005', NOW() - INTERVAL '10 days');

-- Insert sample posts for testing (multiple posts per user)
INSERT INTO posts (id, user_id, content, emotion_tag, created_at) VALUES
-- User 1 posts
('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'I am feeling really excited about this new project!', 'excited', NOW() - INTERVAL '5 days'),
('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'Had a calm and peaceful meditation session today.', 'calm', NOW() - INTERVAL '4 days'),
('650e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', 'Feeling happy after meeting with friends!', 'happy', NOW() - INTERVAL '3 days'),
('650e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440001', 'A bit anxious about the upcoming presentation.', 'anxious', NOW() - INTERVAL '2 days'),
('650e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440001', 'Very excited about the weekend plans!', 'excited', NOW() - INTERVAL '1 day'),

-- User 2 posts
('650e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440002', 'Feeling grateful for all the good things in life.', 'grateful', NOW() - INTERVAL '6 days'),
('650e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440002', 'Happy about completing my workout today!', 'happy', NOW() - INTERVAL '5 days'),
('650e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440002', 'Feeling a bit sad about saying goodbye to friends.', 'sad', NOW() - INTERVAL '4 days'),
('650e8400-e29b-41d4-a716-446655440009', '550e8400-e29b-41d4-a716-446655440002', 'Calm evening with a good book.', 'calm', NOW() - INTERVAL '3 days'),
('650e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440002', 'Excited about starting a new hobby!', 'excited', NOW() - INTERVAL '2 days'),

-- User 3 posts
('650e8400-e29b-41d4-a716-446655440011', '550e8400-e29b-41d4-a716-446655440003', 'Feeling motivated to tackle new challenges.', 'motivated', NOW() - INTERVAL '7 days'),
('650e8400-e29b-41d4-a716-446655440012', '550e8400-e29b-41d4-a716-446655440003', 'Happy moments with family today!', 'happy', NOW() - INTERVAL '6 days'),
('650e8400-e29b-41d4-a716-446655440013', '550e8400-e29b-41d4-a716-446655440003', 'Anxious about the job interview tomorrow.', 'anxious', NOW() - INTERVAL '5 days'),
('650e8400-e29b-41d4-a716-446655440014', '550e8400-e29b-41d4-a716-446655440003', 'Feeling peaceful after yoga session.', 'calm', NOW() - INTERVAL '4 days'),
('650e8400-e29b-41d4-a716-446655440015', '550e8400-e29b-41d4-a716-446655440003', 'So excited about the vacation next month!', 'excited', NOW() - INTERVAL '3 days'),

-- User 4 posts  
('650e8400-e29b-41d4-a716-446655440016', '550e8400-e29b-41d4-a716-446655440004', 'Happy to have learned something new today.', 'happy', NOW() - INTERVAL '8 days'),
('650e8400-e29b-41d4-a716-446655440017', '550e8400-e29b-41d4-a716-446655440004', 'Feeling grateful for my supportive friends.', 'grateful', NOW() - INTERVAL '7 days'),
('650e8400-e29b-41d4-a716-446655440018', '550e8400-e29b-41d4-a716-446655440004', 'A bit sad about the rainy weather.', 'sad', NOW() - INTERVAL '6 days'),
('650e8400-e29b-41d4-a716-446655440019', '550e8400-e29b-41d4-a716-446655440004', 'Excited about the new restaurant opening!', 'excited', NOW() - INTERVAL '5 days'),
('650e8400-e29b-41d4-a716-446655440020', '550e8400-e29b-41d4-a716-446655440004', 'Calm Sunday afternoon reading.', 'calm', NOW() - INTERVAL '4 days'),

-- User 5 posts
('650e8400-e29b-41d4-a716-446655440021', '550e8400-e29b-41d4-a716-446655440005', 'Happy about finishing the marathon!', 'happy', NOW() - INTERVAL '9 days'),
('650e8400-e29b-41d4-a716-446655440022', '550e8400-e29b-41d4-a716-446655440005', 'Feeling motivated after the inspiring conference.', 'motivated', NOW() - INTERVAL '8 days'),
('650e8400-e29b-41d4-a716-446655440023', '550e8400-e29b-41d4-a716-446655440005', 'Anxious about the big decision I need to make.', 'anxious', NOW() - INTERVAL '7 days'),
('650e8400-e29b-41d4-a716-446655440024', '550e8400-e29b-41d4-a716-446655440005', 'Excited about the new project at work!', 'excited', NOW() - INTERVAL '6 days'),
('650e8400-e29b-41d4-a716-446655440025', '550e8400-e29b-41d4-a716-446655440005', 'Grateful for all the opportunities I have.', 'grateful', NOW() - INTERVAL '5 days');

-- Verification queries to test indexes
-- Use these to verify data was inserted correctly:

-- SELECT COUNT(*) FROM users;  -- Should return 5
-- SELECT COUNT(*) FROM posts;  -- Should return 25
-- SELECT emotion_tag, COUNT(*) FROM posts GROUP BY emotion_tag ORDER BY COUNT(*) DESC;
-- SELECT u.email_hash, COUNT(p.id) as post_count FROM users u LEFT JOIN posts p ON u.id = p.user_id GROUP BY u.id, u.email_hash; 