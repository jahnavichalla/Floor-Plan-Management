
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
    plan_details TEXT NOT NULL,  
    finalised_by VARCHAR(50) NOT NULL,  
    priority INT NOT NULL,  
    is_finalised BOOLEAN DEFAULT TRUE,  -- Marks if this is the finalized version
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (floor_id) REFERENCES floors(id) ON DELETE CASCADE
);

-- Create floor plan versions table
CREATE TABLE floor_plan_versions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_plan_id INT NOT NULL,  -- Foreign key to floor plans
    previous_plan_details TEXT NOT NULL,  
    finalised_by VARCHAR(50) NOT NULL,  
    priority INT NOT NULL, 
    version_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (floor_plan_id) REFERENCES floor_plans(id) ON DELETE CASCADE
);
