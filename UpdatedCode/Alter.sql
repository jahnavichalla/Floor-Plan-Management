ALTER TABLE bookings ADD COLUMN user_id INT NOT NULL,
ADD FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE floor_plan_versions
ADD COLUMN number_of_rooms INT,
ADD COLUMN room_details TEXT;

ALTER TABLE floor_plan_versions DROP COLUMN previous_plan_details;