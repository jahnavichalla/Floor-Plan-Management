// STEP 1. Import required packages

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.StringWriter;
// Other imports remain unchanged...


public class floorManagement {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost/db1?useSSL=false";
    static final String USER = "Nikki"; // add your user 
    static final String PASS = "Nikki@2002"; // add password

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

        // Check if a plan already exists for this floor in "floor_plans"
        String checkFloorSql = "SELECT COUNT(*) FROM floor_plans WHERE floor_number = ?";
        PreparedStatement checkFloorPstmt = conn.prepareStatement(checkFloorSql);
        checkFloorPstmt.setInt(1, floorNumber);
        ResultSet rs = checkFloorPstmt.executeQuery();
        rs.next();

        if (rs.getInt(1) > 0) {
            // Floor plan already exists, don't add it again
            System.out.println("Floor number " + floorNumber + " already exists in the finalized plans.");
            return;
        }

        System.out.print("Enter admin name: ");
        String adminName = sc.nextLine();

        System.out.print("Enter admin priority: ");
        int priority = sc.nextInt();
        sc.nextLine();  // Consume newline

        System.out.print("Enter the number of rooms on this floor: ");
        int numberOfRooms = sc.nextInt();
        sc.nextLine();  // Consume newline

        // Create JSON array to hold room details
        JsonArrayBuilder roomsArrayBuilder = Json.createArrayBuilder();

        for (int i = 1; i <= numberOfRooms; i++) {
            System.out.println("Details for Room " + i + ":");

            System.out.print("Capacity: ");
            int capacity = sc.nextInt();
            sc.nextLine();  // Consume newline

            System.out.print("Room Name: ");
            String roomName = sc.nextLine();

            // Insert into "rooms" table
            String insertRoomSql = "INSERT INTO rooms (floor_number, capacity, room_name, available) VALUES (?, ?, ?, true)";
            PreparedStatement insertRoomPstmt = conn.prepareStatement(insertRoomSql);
            insertRoomPstmt.setInt(1, floorNumber);
            insertRoomPstmt.setInt(2, capacity);
            insertRoomPstmt.setString(3, roomName);
            insertRoomPstmt.executeUpdate();

            // Create a JSON object for this room
            JsonObject roomDetails = Json.createObjectBuilder()
                    .add("room_name", roomName)
                    .add("capacity", capacity)
                    .build();

            // Add the room details to the rooms array
            roomsArrayBuilder.add(roomDetails);

            System.out.println("Room " + roomName + " added successfully on floor " + floorNumber + ".");
        }

        // Convert the rooms array to a JSON object
        JsonObject planDetailsJson = Json.createObjectBuilder()
                .add("rooms", roomsArrayBuilder)
                .build();

        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.write(planDetailsJson);
        }
        String planDetails = stringWriter.toString();  // Convert the JSON object to a string

        // Insert into the "floor_plans" table (finalized plans)
        String insertPlanSql = "INSERT INTO floor_plans (floor_number, plan_details, finalised_by, priority, is_finalised) VALUES (?, ?, ?, ?, true)";
        PreparedStatement insertPlanPstmt = conn.prepareStatement(insertPlanSql);
        insertPlanPstmt.setInt(1, floorNumber);
        insertPlanPstmt.setString(2, planDetails);
        insertPlanPstmt.setString(3, adminName);
        insertPlanPstmt.setInt(4, priority);
        insertPlanPstmt.executeUpdate();

        // Insert into the "floor_plan_versions" table (for version history)
        String insertVersionSql = "INSERT INTO floor_plan_versions (floor_number, plan_details, finalised_by, priority, version_timestamp) VALUES (?, ?, ?, ?, NOW())";
        PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
        insertVersionPstmt.setInt(1, floorNumber);
        insertVersionPstmt.setString(2, planDetails);
        insertVersionPstmt.setString(3, adminName);
        insertVersionPstmt.setInt(4, priority);
        insertVersionPstmt.executeUpdate();

        System.out.println("Floor plan added successfully for floor " + floorNumber + " and finalized.");

    } catch (SQLException e) {
        e.printStackTrace();
    }
}



    public static void updatePlan(Connection conn, Scanner sc) {
    try {
        System.out.print("Enter floor number: ");
        int floorNumber = sc.nextInt();
        sc.nextLine(); // Consume newline

        // Check if a floor plan already exists for this floor
        String checkPlanSql = "SELECT id, finalised_by, priority FROM floor_plans WHERE floor_number = ? AND is_finalised = TRUE";
        PreparedStatement checkPlanPstmt = conn.prepareStatement(checkPlanSql);
        checkPlanPstmt.setInt(1, floorNumber);
        ResultSet rs = checkPlanPstmt.executeQuery();

        if (!rs.next()) {
            // No finalized plan exists, call addPlan
            System.out.println("No finalized plan exists for this floor. Adding a new plan...");
            addPlan(conn, sc);
            return;
        }

        // Plan exists, begin update process
        System.out.println("A plan already exists for this floor. Considering update...");

        // Get existing plan's finalised admin and priority
        String currentFinaliser = rs.getString("finalised_by");
        int currentPriority = rs.getInt("priority");

        System.out.print("Enter admin name for this update: ");
        String adminName = sc.nextLine();

        System.out.print("Enter admin priority: ");
        int adminPriority = sc.nextInt();
        sc.nextLine(); // Consume newline

        System.out.println("Please enter the new floor plan details: ");
        String newPlanDetails = sc.nextLine();

        // Check if there are any rooms occupied on this floor
        String checkOccupiedSql = "SELECT COUNT(*) FROM rooms r JOIN bookings b ON r.id = b.room_id WHERE r.floor_number = ?";
        PreparedStatement checkOccupiedPstmt = conn.prepareStatement(checkOccupiedSql);
        checkOccupiedPstmt.setInt(1, floorNumber);
        ResultSet occupiedRs = checkOccupiedPstmt.executeQuery();
        occupiedRs.next();

        int occupiedCount = occupiedRs.getInt(1);

        if (occupiedCount > 0) {
            // If rooms are occupied, save version but do not update
            System.out.println("Some rooms are occupied. Cannot update the plan but will store the version.");
            
            // Insert the current plan into the version history
            String insertVersionSql = "INSERT INTO floor_plan_versions (floor_number, plan_details, finalised_by, priority) "
                    + "VALUES (?, ?, ?, ?)";
            PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
            insertVersionPstmt.setInt(1, floorNumber);
            insertVersionPstmt.setString(2, newPlanDetails);
            insertVersionPstmt.setString(3, adminName);
            insertVersionPstmt.setInt(4, adminPriority);
            insertVersionPstmt.executeUpdate();

            System.out.println("Version stored successfully.");
            return;
        }

        // Check if the new admin can update the plan
        if (adminPriority >= currentPriority) {
            System.out.println("You cannot update the plan because your priority is too low.");
            
            // Store the version but do not update the final plan
            String insertVersionSql = "INSERT INTO floor_plan_versions (floor_number, plan_details, finalised_by, priority) "
                    + "VALUES (?, ?, ?, ?)";
            PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
            insertVersionPstmt.setInt(1, floorNumber);
            insertVersionPstmt.setString(2, newPlanDetails);
            insertVersionPstmt.setString(3, adminName);
            insertVersionPstmt.setInt(4, adminPriority);
            insertVersionPstmt.executeUpdate();

            System.out.println("Version stored successfully.");
        } else {
            // Update the plan since admin has a higher priority
            String updatePlanSql = "UPDATE floor_plans SET plan_details = ?, finalised_by = ?, priority = ?, last_updated = CURRENT_TIMESTAMP "
                    + "WHERE floor_number = ?";
            PreparedStatement updatePlanPstmt = conn.prepareStatement(updatePlanSql);
            updatePlanPstmt.setString(1, newPlanDetails);
            updatePlanPstmt.setString(2, adminName);
            updatePlanPstmt.setInt(3, adminPriority);
            updatePlanPstmt.setInt(4, floorNumber);
            updatePlanPstmt.executeUpdate();

            System.out.println("Plan updated successfully.");
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
        String sql = "SELECT * FROM rooms WHERE available = true " +
                     "AND id NOT IN (SELECT room_id FROM bookings WHERE (start_time < ? AND end_time > ?))";
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


// Method to recommend a room based on user preferences
public static void recommendRoom(Connection conn, Scanner sc) {
    System.out.print("Start Time (HH:MM): ");
    String startTime = sc.nextLine();
    System.out.print("End Time (HH:MM): ");
    String endTime = sc.nextLine();
    System.out.print("Minimum Capacity: ");
    int minCapacity = sc.nextInt();
    sc.nextLine();  // Consume newline
    System.out.print("Preferred Floor: ");
    int preferredFloor = sc.nextInt();
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
            String recommendSql = "SELECT * FROM rooms WHERE floor_number = ? AND capacity <= ? AND id IN (" +
                                  availableRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
            PreparedStatement recommendPstmt = conn.prepareStatement(recommendSql);
            recommendPstmt.setInt(1, preferredFloor);
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
                            System.out.println("Room booked: " + roomNameToBook + " on floor " + preferredFloor);
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
                                    "AND floor_number = ?";
        
        PreparedStatement preferredFloorPstmt = conn.prepareStatement(preferredFloorSql);
        preferredFloorPstmt.setInt(1, preferredFloor);

        ResultSet preferredFloorRs = preferredFloorPstmt.executeQuery();

        // Step 6: Recommend a room or inform the user
        if (preferredFloorRs.next()) {
            int roomId = preferredFloorRs.getInt("id");
            String roomName = preferredFloorRs.getString("room_name");
            // Mark room as booked
            bookRoom(conn, roomId, roomName, startTime, endTime);
            System.out.println("Room recommended: " + roomName + " on floor " + preferredFloor);
        } else {
            System.out.println("No rooms available on your preferred floor.");

            // Step 7: Recommend nearby floors
            List<Integer> nearbyFloorRoomIds = new ArrayList<>();

            // Check for rooms on lower floor
            String lowerFloorSql = "SELECT * FROM rooms WHERE id IN (" +
                                   capacityRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") " +
                                   "AND floor_number = ?";
            PreparedStatement lowerFloorPstmt = conn.prepareStatement(lowerFloorSql);
            lowerFloorPstmt.setInt(1, preferredFloor - 1);
            ResultSet lowerFloorRs = lowerFloorPstmt.executeQuery();
            while (lowerFloorRs.next()) {
                nearbyFloorRoomIds.add(lowerFloorRs.getInt("id"));
            }

            // Check for rooms on upper floor
            String upperFloorSql = "SELECT * FROM rooms WHERE id IN (" +
                                   capacityRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") " +
                                   "AND floor_number = ?";
            PreparedStatement upperFloorPstmt = conn.prepareStatement(upperFloorSql);
            upperFloorPstmt.setInt(1, preferredFloor + 1);
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





    // Method to book a room
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

            System.out.println("Room '" + roomName + "' booked from " + startTime + " to " + endTime + ".");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
