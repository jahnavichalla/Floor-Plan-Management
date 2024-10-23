-- -- Insert sample users
-- INSERT INTO users (username, password, role) VALUES
-- ('admin', 'adminpass', 'admin'),
-- ('user1', 'userpass', 'user');

-- -- Insert sample floors
-- INSERT INTO floors (floor_number, total_rooms) VALUES
-- (1, 3),
-- (2, 2),
-- (5, 1);

-- -- Insert sample rooms with room names
-- INSERT INTO rooms (floor_number, capacity, room_name) VALUES
-- (1, 10, 'Conference Room A'),
-- (1, 20, 'Conference Room B'),
-- (1, 5, 'Meeting Room 1'),
-- (2, 15, 'Workshop Room 1'),
-- (2, 30, 'Lecture Hall'),
-- (3, 8, 'Small Meeting Room');


-- Insert sample users
INSERT INTO users (username, password, role) VALUES
('admin', 'adminpass', 'admin'),
('user1', 'userpass', 'user');

-- Insert sample floors
INSERT INTO floors (floor_number, total_rooms) VALUES
(1, 3),
(2, 2),
(5, 1);

-- Now check the ids generated for floors
SELECT * FROM floors;

-- Assume the IDs are:
-- 1 for floor 1, 2 for floor 2, and 3 for floor 5 (based on the auto-increment).

-- Insert sample rooms with room names
INSERT INTO rooms (floor_id, capacity, room_name) VALUES
(1, 10, 'Conference Room A'),
(1, 20, 'Conference Room B'),
(1, 5, 'Meeting Room 1'),
(2, 15, 'Workshop Room 1'),
(2, 30, 'Lecture Hall');
