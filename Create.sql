-- -- Create users table
-- CREATE TABLE users (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     username VARCHAR(50) NOT NULL UNIQUE,
--     password VARCHAR(50) NOT NULL,
--     priority INT,
--     role ENUM('user', 'admin') NOT NULL
-- );

-- -- Create floors table
-- CREATE TABLE floors (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     floor_number INT NOT NULL,
--     total_rooms INT NOT NULL
-- );

-- -- Create rooms table
-- CREATE TABLE rooms (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     floor_number INT NOT NULL,
--     capacity INT NOT NULL,
--     room_name VARCHAR(50) NOT NULL UNIQUE,
--     available BOOLEAN DEFAULT true
-- );

-- -- Create bookings table
-- CREATE TABLE bookings (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     room_id INT NOT NULL,
--     start_time TIME NOT NULL,
--     end_time TIME NOT NULL
-- );

-- CREATE TABLE floor_plans (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     floor_number INT NOT NULL, 
--     plan_details TEXT NOT NULL,  -- Details about the floor plan
--     finalised_by VARCHAR(50) NOT NULL,  -- Admin who finalized the plan
--     priority INT NOT NULL,  -- Priority level of the finalizer
--     is_finalised BOOLEAN DEFAULT TRUE,  -- Marks if this is the finalised version
--     last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Timestamp for the last update
-- );


-- CREATE TABLE floor_plan_versions (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     floor_number INT NOT NULL,  -- Floor number for which the version was created
--     plan_details TEXT NOT NULL,  -- Previous plan details
--     finalised_by VARCHAR(50) NOT NULL,  -- Admin who finalized the previous plan
--     priority INT NOT NULL,  -- Priority level of the finalizer for the previous plan
--     version_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Timestamp for the version
-- );



-- Create users table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    priority INT,
    role ENUM('user', 'admin') NOT NULL
);

-- Create floors table
CREATE TABLE floors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_number INT NOT NULL UNIQUE,  -- Ensure floor numbers are unique
    total_rooms INT NOT NULL
);

-- Create rooms table
CREATE TABLE rooms (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_id INT NOT NULL,  -- Foreign key to floors
    room_name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    available BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (floor_id) REFERENCES floors(id) ON DELETE CASCADE
);

-- Create bookings table
CREATE TABLE bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

-- Create floor plans table
CREATE TABLE floor_plans (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_id INT NOT NULL,  -- Foreign key to floors
    plan_details TEXT NOT NULL,  -- Details about the floor plan
    finalised_by VARCHAR(50) NOT NULL,  -- Admin who finalized the plan
    priority INT NOT NULL,  -- Priority level of the finalizer
    is_finalised BOOLEAN DEFAULT TRUE,  -- Marks if this is the finalized version
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (floor_id) REFERENCES floors(id) ON DELETE CASCADE
);

-- Create floor plan versions table
CREATE TABLE floor_plan_versions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_plan_id INT NOT NULL,  -- Foreign key to floor plans
    previous_plan_details TEXT NOT NULL,  -- Previous plan details
    finalised_by VARCHAR(50) NOT NULL,  -- Admin who finalized the previous plan
    priority INT NOT NULL,  -- Priority level of the finalizer for the previous plan
    version_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (floor_plan_id) REFERENCES floor_plans(id) ON DELETE CASCADE
);
