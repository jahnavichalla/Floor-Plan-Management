// STEP 1. Import required packages

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
// import javax.json.Json;
// import javax.json.JsonArrayBuilder;
// import javax.json.JsonObject;
// import javax.json.JsonObjectBuilder;
// import javax.json.JsonWriter;
// import java.io.StringWriter;
// Other imports remain unchanged...


public class floorManagement {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost/db1?useSSL=false";
    static final String USER = "chakri"; // add your user 
    static final String PASS = "Chakri@2003"; // add password

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Connection conn = null;
        try {
            // Register JDBC driver
            Class.forName(JDBC_DRIVER);

            // Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            boolean running = true;
            while (running) {
                System.out.println("\nWelcome to the Login System!");
                System.out.println("1. Login");
                System.out.println("2. Signup");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");
                int choice = sc.nextInt();
                sc.nextLine();  // Consume newline

                switch (choice) {
                    case 1:
                        login(conn, sc);
                        break;
                    case 2:
                        signup(conn, sc);
                        break;
                    case 3:
                        running = false;
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Method for signup
    public static void signup(Connection conn, Scanner sc) {
        try {
            System.out.print("Enter username: ");
            String username = sc.nextLine();

            System.out.print("Enter password: ");
            String password = sc.nextLine();

            System.out.print("Enter role (user/admin): ");
            String role = sc.nextLine().toLowerCase();

            System.out.print("Enter priority: ");
            String priority = sc.nextLine();

            if (!role.equals("user") && !role.equals("admin")) {
                System.out.println("Invalid role. Please choose 'user' or 'admin'.");
                return;
            }

            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Signup successful! You can now login.");
            } else {
                System.out.println("Signup failed.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Username already exists. Try again.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method for login
    public static void login(Connection conn, Scanner sc) {
        try {
            System.out.println("\nLogin as:");
            System.out.println("1. User");
            System.out.println("2. Admin");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();  // Consume newline

            if (choice == 3) {
                return;
            }

            System.out.print("Enter username: ");
            String username = sc.nextLine();

            System.out.print("Enter password: ");
            String password = sc.nextLine();

            String role = (choice == 1) ? "user" : "admin";

            String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Login successful! Welcome, " + username + " (" + role + ")");
                if (role.equals("user")) {
                    userMenu(conn, sc);
                } else {
                    adminMenu(conn, sc);
                }
            } else {
                System.out.println("Invalid credentials or role. Try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void adminMenu(Connection conn, Scanner sc) {
        boolean adminRunning = true;
        while (adminRunning) {
            System.out.println("\nAdmin Menu:");
            System.out.println("1. Add Plan");
            System.out.println("2. Update Plan");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    addPlan(conn,sc);
                    break;
                case 2:
                    updatePlan(conn, sc);
                    break;
                case 3:
                    adminRunning = false;
                    System.out.println("Exiting Admin Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    
public static void addPlan(Connection conn, Scanner sc) {
    try {
        System.out.print("Enter floor number: ");
        int floorNumber = sc.nextInt();
        sc.nextLine();  // Consume newline

        // Check if the floor exists
        String checkFloorSql = "SELECT id FROM floors WHERE floor_number = ?";
        PreparedStatement checkFloorPstmt = conn.prepareStatement(checkFloorSql);
        checkFloorPstmt.setInt(1, floorNumber);
        ResultSet floorRs = checkFloorPstmt.executeQuery();

        if (floorRs.next()) {
            // Floor exists, cannot add plan
            System.out.println("You can't add the plan as this floor already exists.");
            return;  // Exit the method
        } else {
            // Floor does not exist, gather details for the new floor
            System.out.println("Floor number " + floorNumber + " does not exist. Proceeding to create a new floor and plan.");

            // Gather floor details
            System.out.print("Enter total number of rooms for this floor: ");
            int totalRooms = sc.nextInt();
            sc.nextLine();  // Consume newline

            System.out.print("Enter admin name: ");
            String adminName = sc.nextLine();

            System.out.print("Enter admin priority: ");
            int priority = sc.nextInt();
            sc.nextLine();  // Consume newline

            // Insert the new floor into the database
            String insertFloorSql = "INSERT INTO floors (floor_number, total_rooms) VALUES (?, ?)";
            PreparedStatement insertFloorPstmt = conn.prepareStatement(insertFloorSql, Statement.RETURN_GENERATED_KEYS);
            insertFloorPstmt.setInt(1, floorNumber);
            insertFloorPstmt.setInt(2, totalRooms);  // Set total_rooms
            int floorRowsAffected = insertFloorPstmt.executeUpdate();

            if (floorRowsAffected == 0) {
                System.out.println("Failed to create new floor.");
                return; // Exit if the insert fails
            }

            ResultSet generatedKeys = insertFloorPstmt.getGeneratedKeys();
            if (!generatedKeys.next()) {
                System.out.println("Failed to retrieve the new floor ID.");
                return; // Exit if the key retrieval fails
            }
            int floorId = generatedKeys.getInt(1); // Get the new floor ID
            System.out.println("New floor created with ID: " + floorId);

            // Gather room details
            List<Room> rooms = new ArrayList<>();

            for (int i = 1; i <= totalRooms; i++) {
                System.out.println("Details for Room " + i + ":");
                System.out.print("Room Name: ");
                String roomName = sc.nextLine();
                System.out.print("Capacity: ");
                int capacity = sc.nextInt();
                sc.nextLine();  // Consume newline
                
                // Create room object
                rooms.add(new Room(roomName, capacity));
            }

            // Insert into "floor_plans" table
            String insertPlanSql = "INSERT INTO floor_plans (floor_id, plan_details, finalised_by, priority, is_finalised) VALUES (?, ?, ?, ?, true)";
            PreparedStatement insertPlanPstmt = conn.prepareStatement(insertPlanSql, Statement.RETURN_GENERATED_KEYS);
            insertPlanPstmt.setInt(1, floorId); // Use the newly created floor ID
            insertPlanPstmt.setString(2, "Floor " + floorNumber + " plan finalized by " + adminName);
            insertPlanPstmt.setString(3, adminName);
            insertPlanPstmt.setInt(4, priority);
            
            int planRowsAffected = insertPlanPstmt.executeUpdate();
            if (planRowsAffected == 0) {
                System.out.println("Failed to insert floor plan. Please check your data.");
                return; // Exit if the insert fails
            }

            // Get the generated floor plan ID
            ResultSet generatedPlanKeys = insertPlanPstmt.getGeneratedKeys();
            int floorPlanId = 0;
            if (generatedPlanKeys.next()) {
                floorPlanId = generatedPlanKeys.getInt(1);
            }

            // Insert room details into "rooms" table
            String insertRoomSql = "INSERT INTO rooms (floor_id, room_name, capacity, available) VALUES (?, ?, ?, true)";
            PreparedStatement insertRoomPstmt = conn.prepareStatement(insertRoomSql);
            
            for (Room room : rooms) {
                insertRoomPstmt.setInt(1, floorId); // Link room to the new floor
                insertRoomPstmt.setString(2, room.getRoomName());
                insertRoomPstmt.setInt(3, room.getCapacity());
                insertRoomPstmt.executeUpdate();
                System.out.println("Room " + room.getRoomName() + " added successfully.");
            }

            // Prepare plan details for versions
            String planDetails = "Floor " + floorNumber + " plan finalized by " + adminName;

            // Insert into "floor_plan_versions" table
            String insertVersionSql = "INSERT INTO floor_plan_versions (floor_plan_id, previous_plan_details, finalised_by, priority) VALUES (?, ?, ?, ?)";
            PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
            insertVersionPstmt.setInt(1, floorPlanId);
            insertVersionPstmt.setString(2, planDetails);
            insertVersionPstmt.setString(3, adminName);
            insertVersionPstmt.setInt(4, priority);
            insertVersionPstmt.executeUpdate();

            System.out.println("Floor plan added successfully for floor " + floorNumber + " and finalized.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}



// Room class to hold room details
public static class Room {
    private String roomName;
    private int capacity;

    public Room(String roomName, int capacity) {
        this.roomName = roomName;
        this.capacity = capacity;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getCapacity() {
        return capacity;
    }
}


    public static void updatePlan(Connection conn, Scanner sc) {
    try {
        System.out.print("Enter floor number: ");
        int floorNumber = sc.nextInt();
        sc.nextLine(); // Consume newline

        // Check if the floor exists
        String checkFloorSql = "SELECT id FROM floors WHERE floor_number = ?";
        PreparedStatement checkFloorPstmt = conn.prepareStatement(checkFloorSql);
        checkFloorPstmt.setInt(1, floorNumber);
        ResultSet floorRs = checkFloorPstmt.executeQuery();

        if (!floorRs.next()) {
            // Floor does not exist, call addPlan
            System.out.println("No floor exists with the given number. Adding a new plan...");
            addPlan(conn, sc);
            return;
        }

        // Floor exists, begin update process
        System.out.println("A floor exists with this number. Considering update...");

        // Get the existing floor ID
        int floorId = floorRs.getInt("id");

        // Get existing plan's details if any
        String checkPlanSql = "SELECT fp.id, fp.finalised_by, fp.priority " +
                               "FROM floor_plans fp " +
                               "WHERE fp.floor_id = ? AND fp.is_finalised = TRUE";
        PreparedStatement checkPlanPstmt = conn.prepareStatement(checkPlanSql);
        checkPlanPstmt.setInt(1, floorId);
        ResultSet rs = checkPlanPstmt.executeQuery();

        // Check for existing finalized plan
        if (!rs.next()) {
            // No finalized plan exists, can proceed with adding new plan
            System.out.println("No finalized plan exists for this floor. Adding a new plan...");
            addPlan(conn, sc);
            return;
        }

        // Plan exists, begin updating
        String currentFinaliser = rs.getString("finalised_by");
        int currentPriority = rs.getInt("priority");

        System.out.print("Enter admin name for this update: ");
        String adminName = sc.nextLine();

        System.out.print("Enter admin priority: ");
        int adminPriority = sc.nextInt();
        sc.nextLine(); // Consume newline

        // Check if there are any rooms occupied on this floor
        String checkOccupiedSql = "SELECT COUNT(*) FROM rooms r " +
                                   "JOIN bookings b ON r.id = b.room_id " +
                                   "WHERE r.floor_id = ?";
        PreparedStatement checkOccupiedPstmt = conn.prepareStatement(checkOccupiedSql);
        checkOccupiedPstmt.setInt(1, floorId);
        ResultSet occupiedRs = checkOccupiedPstmt.executeQuery();
        occupiedRs.next();

        int occupiedCount = occupiedRs.getInt(1);

        // Prompt for new floor plan details
        System.out.print("Enter the number of rooms for this floor: ");
        int numberOfRooms = sc.nextInt();
        sc.nextLine(); // Consume newline

        // Collect details for each room
        List<String> roomDetails = new ArrayList<>();
        for (int i = 0; i < numberOfRooms; i++) {
            System.out.print("Enter details for room " + (i + 1) + " (name and capacity separated by a comma): ");
            String roomDetail = sc.nextLine();
            roomDetails.add(roomDetail);
        }

        // If rooms are occupied, save version but do not update
        if (occupiedCount > 0) {
            System.out.println("Some rooms are occupied. Cannot update the plan but will store the version.");
            
            // Insert the current plan into the version history
            String insertVersionSql = "INSERT INTO floor_plan_versions (floor_plan_id, previous_plan_details, finalised_by, priority) " +
                                       "VALUES (?, ?, ?, ?)";
            PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
            insertVersionPstmt.setInt(1, rs.getInt("id")); // Use the existing plan's ID
            insertVersionPstmt.setString(2, String.join(";", roomDetails)); // Store room details
            insertVersionPstmt.setString(3, adminName);
            insertVersionPstmt.setInt(4, adminPriority);
            insertVersionPstmt.executeUpdate();

            System.out.println("Version stored successfully.");
            return;
        }

        // Check if the new admin can update the plan
        if (adminPriority < currentPriority) {
            System.out.println("You cannot update the plan because your priority is too low.");
            
            // Store the version but do not update the final plan
            String insertVersionSql = "INSERT INTO floor_plan_versions (floor_plan_id, previous_plan_details, finalised_by, priority) " +
                                       "VALUES (?, ?, ?, ?)";
            PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
            insertVersionPstmt.setInt(1, rs.getInt("id")); // Use the existing plan's ID
            insertVersionPstmt.setString(2, String.join(";", roomDetails)); // Store room details
            insertVersionPstmt.setString(3, adminName);
            insertVersionPstmt.setInt(4, adminPriority);
            insertVersionPstmt.executeUpdate();

            System.out.println("Version stored successfully.");
        } else {
            // Update the plan since admin has a higher priority
            String updatePlanSql = "UPDATE floor_plans SET plan_details = ?, finalised_by = ?, priority = ?, last_updated = CURRENT_TIMESTAMP " +
                                   "WHERE id = ?";
            PreparedStatement updatePlanPstmt = conn.prepareStatement(updatePlanSql);
            updatePlanPstmt.setString(1, String.join(";", roomDetails)); // Update with new room details
            updatePlanPstmt.setString(2, adminName);
            updatePlanPstmt.setInt(3, adminPriority);
            updatePlanPstmt.setInt(4, rs.getInt("id")); // Use the existing plan's ID
            updatePlanPstmt.executeUpdate();

            System.out.println("Plan updated successfully.");

            // Now update the rooms table with new room details
            int totalRoomsUpdated = 0; // Counter for updated room count
            for (String roomDetail : roomDetails) {
                String[] details = roomDetail.split(",");
                String roomName = details[0].trim();
                int capacity = Integer.parseInt(details[1].trim());

                // Check if the room already exists
                String checkRoomSql = "SELECT id FROM rooms WHERE room_name = ? AND floor_id = ?";
                PreparedStatement checkRoomPstmt = conn.prepareStatement(checkRoomSql);
                checkRoomPstmt.setString(1, roomName);
                checkRoomPstmt.setInt(2, floorId);
                ResultSet roomRs = checkRoomPstmt.executeQuery();

                if (roomRs.next()) {
                    // Update existing room
                    String updateRoomSql = "UPDATE rooms SET capacity = ? WHERE id = ?";
                    PreparedStatement updateRoomPstmt = conn.prepareStatement(updateRoomSql);
                    updateRoomPstmt.setInt(1, capacity);
                    updateRoomPstmt.setInt(2, roomRs.getInt("id"));
                    updateRoomPstmt.executeUpdate();
                    System.out.println("Updated room: " + roomName);
                } else {
                    // Insert new room
                    String insertRoomSql = "INSERT INTO rooms (floor_id, room_name, capacity) VALUES (?, ?, ?)";
                    PreparedStatement insertRoomPstmt = conn.prepareStatement(insertRoomSql);
                    insertRoomPstmt.setInt(1, floorId); // Use the existing floor ID
                    insertRoomPstmt.setString(2, roomName);
                    insertRoomPstmt.setInt(3, capacity);
                    insertRoomPstmt.executeUpdate();
                    System.out.println("Added new room: " + roomName);
                    totalRoomsUpdated++; // Increment the counter for new rooms added
                }
            }

            // Update total_rooms in floors table
            String updateTotalRoomsSql = "UPDATE floors SET total_rooms = total_rooms + ? WHERE id = ?";
            PreparedStatement updateTotalRoomsPstmt = conn.prepareStatement(updateTotalRoomsSql);
            updateTotalRoomsPstmt.setInt(1, totalRoomsUpdated); // Update with the number of new rooms added
            updateTotalRoomsPstmt.setInt(2, floorId); // Use the existing floor ID
            updateTotalRoomsPstmt.executeUpdate();
            System.out.println("Updated total rooms for floor " + floorNumber + " successfully.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}





    // User menu for additional functionalities
    public static void userMenu(Connection conn, Scanner sc) {
        boolean userRunning = true;
        while (userRunning) {
            System.out.println("\nUser Menu:");
            System.out.println("1. View Available Rooms");
            System.out.println("2. Recommend Room");
            System.out.println("3. Logout");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    viewAvailableRooms(conn, sc);
                    break;
                case 2:
                    recommendRoom(conn, sc);
                    break;
                case 3:
                    userRunning = false;
                    System.out.println("Logging out...");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

   // Method to view available rooms based on user-specified time range
public static void viewAvailableRooms(Connection conn, Scanner sc) {
    try {
        System.out.print("Start Time (HH:MM): ");
        String startTime = sc.nextLine();
        System.out.print("End Time (HH:MM): ");
        String endTime = sc.nextLine();

        // Query to find available rooms
        String sql = "SELECT r.id, r.room_name, r.capacity, f.floor_number " +
                     "FROM rooms r " +
                     "JOIN floors f ON r.floor_id = f.id " +
                     "WHERE r.available = true " +
                     "AND r.id NOT IN (SELECT room_id FROM bookings WHERE (start_time < ? AND end_time > ?))";
        
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, endTime);
        pstmt.setString(2, startTime);

        ResultSet rs = pstmt.executeQuery();

        System.out.println("\nAvailable Rooms:");
        boolean foundAvailableRoom = false;
        while (rs.next()) {
            foundAvailableRoom = true;
            System.out.println("Room ID: " + rs.getInt("id") +
                               ", Name: " + rs.getString("room_name") +
                               ", Floor: " + rs.getInt("floor_number") +
                               ", Capacity: " + rs.getInt("capacity"));
        }

        if (!foundAvailableRoom) {
            System.out.println("No rooms are available for the specified time range.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


    public static void recommendRoom(Connection conn, Scanner sc) {
    System.out.print("Start Time (HH:MM): ");
    String startTime = sc.nextLine();
    System.out.print("End Time (HH:MM): ");
    String endTime = sc.nextLine();
    System.out.print("Minimum Capacity: ");
    int minCapacity = sc.nextInt();
    sc.nextLine();  // Consume newline
    System.out.print("Preferred Floor ID: "); // Changed to preferred floor ID
    int preferredFloorId = sc.nextInt();
    sc.nextLine();  // Consume newline

    try {
        // Step 1: Check for room availability in the specified time slot
        String availabilitySql = "SELECT * FROM rooms WHERE id NOT IN " +
                                 "(SELECT room_id FROM bookings WHERE (start_time < ? AND end_time > ?)) " +
                                 "AND available = true";

        PreparedStatement availabilityPstmt = conn.prepareStatement(availabilitySql);
        availabilityPstmt.setString(1, endTime);
        availabilityPstmt.setString(2, startTime);

        ResultSet availabilityRs = availabilityPstmt.executeQuery();

        // Collect available room IDs
        List<Integer> availableRoomIds = new ArrayList<>();
        while (availabilityRs.next()) {
            availableRoomIds.add(availabilityRs.getInt("id"));
        }

        // Step 2: Check if any rooms are available
        if (availableRoomIds.isEmpty()) {
            System.out.println("No rooms are available in the given time slots.");
            return; // Stop further processing
        }

        // Step 3: Filter by capacity
        String capacitySql = "SELECT * FROM rooms WHERE id IN (" + 
                             availableRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") " +
                             "AND capacity >= ?";

        PreparedStatement capacityPstmt = conn.prepareStatement(capacitySql);
        capacityPstmt.setInt(1, minCapacity);

        ResultSet capacityRs = capacityPstmt.executeQuery();

        // Collect rooms that meet capacity requirements
        List<Integer> capacityRoomIds = new ArrayList<>();
        while (capacityRs.next()) {
            capacityRoomIds.add(capacityRs.getInt("id"));
        }

        // Step 4: Check if any rooms meet the capacity requirement
        if (capacityRoomIds.isEmpty()) {
            System.out.println("No rooms available with capacity greater than required.");
            // Step 5: Recommend rooms on the preferred floor with a capacity less than or equal to the required capacity
            String recommendSql = "SELECT * FROM rooms WHERE floor_id = ? AND capacity <= ? AND id IN (" +
                                  availableRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
            PreparedStatement recommendPstmt = conn.prepareStatement(recommendSql);
            recommendPstmt.setInt(1, preferredFloorId); // Use floor_id
            recommendPstmt.setInt(2, minCapacity);

            ResultSet recommendRs = recommendPstmt.executeQuery();

            List<Integer> recommendedRoomIds = new ArrayList<>();
            List<String> recommendedRoomNames = new ArrayList<>();
            int count = 1;

            while (recommendRs.next()) {
                int roomId = recommendRs.getInt("id");
                String roomName = recommendRs.getString("room_name");
                recommendedRoomIds.add(roomId);
                recommendedRoomNames.add(roomName);
                System.out.println(count + ". " + roomName + " (Capacity: " + recommendRs.getInt("capacity") + ")");
                count++;
            }

            if (!recommendedRoomIds.isEmpty()) {
                System.out.print("Would you like to book one of these rooms? (yes/no): ");
                String choice = sc.nextLine();

                if (choice.equalsIgnoreCase("yes")) {
                    System.out.print("Please enter the number of the room you wish to book (or type 'exit' to exit): ");
                    String roomInput = sc.nextLine();

                    // Check if the user wants to exit
                    if (roomInput.equalsIgnoreCase("exit")) {
                        System.out.println("Exiting without booking.");
                        return; // Exit without booking
                    }

                    try {
                        int roomNumber = Integer.parseInt(roomInput);
                        if (roomNumber > 0 && roomNumber <= recommendedRoomIds.size()) {
                            int roomIdToBook = recommendedRoomIds.get(roomNumber - 1);
                            String roomNameToBook = recommendedRoomNames.get(roomNumber - 1);
                            // Mark room as booked
                            bookRoom(conn, roomIdToBook, roomNameToBook, startTime, endTime);
                            System.out.println("Room booked: " + roomNameToBook + " on floor ID " + preferredFloorId);
                        } else {
                            System.out.println("Invalid room number selected.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid room number.");
                    }
                }
            } else {
                System.out.println("No rooms available on your preferred floor with the required capacity.");
            }
            return; // Stop further processing
        }

        // Step 5: Filter by preferred floor
        String preferredFloorSql = "SELECT * FROM rooms WHERE id IN (" +
                                    capacityRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") " +
                                    "AND floor_id = ?";

        PreparedStatement preferredFloorPstmt = conn.prepareStatement(preferredFloorSql);
        preferredFloorPstmt.setInt(1, preferredFloorId); // Use floor_id

        ResultSet preferredFloorRs = preferredFloorPstmt.executeQuery();

        // Step 6: Recommend a room or inform the user
        if (preferredFloorRs.next()) {
            int roomId = preferredFloorRs.getInt("id");
            String roomName = preferredFloorRs.getString("room_name");
            // Mark room as booked
            bookRoom(conn, roomId, roomName, startTime, endTime);
            System.out.println("Room recommended: " + roomName + " on floor ID " + preferredFloorId);
        } else {
            System.out.println("No rooms available on your preferred floor.");

            // Step 7: Recommend nearby floors
            List<Integer> nearbyFloorRoomIds = new ArrayList<>();

            // Check for rooms on lower floor
            String lowerFloorSql = "SELECT * FROM rooms WHERE id IN (" +
                                   capacityRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") " +
                                   "AND floor_id = ?";
            PreparedStatement lowerFloorPstmt = conn.prepareStatement(lowerFloorSql);
            lowerFloorPstmt.setInt(1, preferredFloorId - 1); // Use floor_id
            ResultSet lowerFloorRs = lowerFloorPstmt.executeQuery();
            while (lowerFloorRs.next()) {
                nearbyFloorRoomIds.add(lowerFloorRs.getInt("id"));
            }

            // Check for rooms on upper floor
            String upperFloorSql = "SELECT * FROM rooms WHERE id IN (" +
                                   capacityRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") " +
                                   "AND floor_id = ?";
            PreparedStatement upperFloorPstmt = conn.prepareStatement(upperFloorSql);
            upperFloorPstmt.setInt(1, preferredFloorId + 1); // Use floor_id
            ResultSet upperFloorRs = upperFloorPstmt.executeQuery();
            while (upperFloorRs.next()) {
                nearbyFloorRoomIds.add(upperFloorRs.getInt("id"));
            }

            // Step 8: Check if any rooms are available on nearby floors
            if (!nearbyFloorRoomIds.isEmpty()) {
                String nearbyRoomsSql = "SELECT * FROM rooms WHERE id IN (" +
                                         nearbyFloorRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
                PreparedStatement nearbyRoomsPstmt = conn.prepareStatement(nearbyRoomsSql);
                ResultSet nearbyRoomsRs = nearbyRoomsPstmt.executeQuery();

                if (nearbyRoomsRs.next()) {
                    int roomId = nearbyRoomsRs.getInt("id");
                    String roomName = nearbyRoomsRs.getString("room_name");
                    // Mark room as booked
                    bookRoom(conn, roomId, roomName, startTime, endTime);
                    System.out.println("Recommended room on a nearby floor: " + roomName);
                }
            } else {
                System.out.println("No rooms available on nearby floors.");
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


    public static void bookRoom(Connection conn, int roomId, String roomName, String startTime, String endTime) {
        try {
            // Insert booking into the bookings table
            String bookingSql = "INSERT INTO bookings (room_id, start_time, end_time) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(bookingSql);
            pstmt.setInt(1, roomId);
            pstmt.setString(2, startTime);
            pstmt.setString(3, endTime);
            pstmt.executeUpdate();

            // Mark the room as unavailable
            String updateSql = "UPDATE rooms SET available = false WHERE id = ?";
            PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
            updatePstmt.setInt(1, roomId);
            updatePstmt.executeUpdate();

            System.out.println("Successfully booked room: " + roomName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
