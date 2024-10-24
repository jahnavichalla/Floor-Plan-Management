// STEP 1. Import required packages

import java.sql.*;
import java.util.*;
import java.io.*; // For file handling
import java.util.stream.Collectors;

public class floorManagement {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost/FloorManagementDb?useSSL=false";
    static final String USER = "Nikki"; // add your user 
    static final String PASS = "Nikki@2002"; // add password

    private static String loggedInAdminUsername = null;  // Store the logged-in admin's username
    private static int loggedInAdminPriority = -1;
    private static final String PLAN_DATA_FILE = "plan_data.txt";  // File to store plan details

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Connection conn = null;
        long startTime = System.currentTimeMillis();
        boolean connectionEstablished = false;

        // Step 2: Collect plan details while waiting for the connection
        System.out.println("Database connection not yet established. Collecting plan details...");
        collectPlanData(sc);

        System.out.println("Attempting to connect to the database...");

        // Try connecting to the database with a 10ms delay
        while (!connectionEstablished) {
            try {
                if (System.currentTimeMillis() - startTime >= 10) {
                    System.out.println("Connecting to database...");
                    conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    System.out.println("Connection successful!");
                    connectionEstablished = true;
                    break; // Exit loop on successful connection
                }
            } catch (SQLException e) {
                System.out.println("Connection failed. Retrying...");
                try {
                    Thread.sleep(10); // Wait 10ms before retrying
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        // Step 3: Process pending plan requests after successful connection
        processPendingPlanRequests(conn);

        boolean running = true;
        while (running) {
            System.out.println("+------------------------------+");
            System.out.println("|    Welcome to MoveInSync     |");
            System.out.println("| Floor Plan Management System |");
            System.out.println("+------------------------------+");
            System.out.println("|          1. Login            |");
            System.out.println("|          2. Signup           |");
            System.out.println("|          3. Exit             |");
            System.out.println("+------------------------------+");
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
                    System.out.println("XXXX---- Invalid choice. Try again. ----XXXX");
            }
        }

        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Collect plan details while waiting for connection
    public static void collectPlanData(Scanner sc) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PLAN_DATA_FILE, true))) {

            // Collect floor details
            System.out.print("Enter floor number: ");
            int floorNumber = sc.nextInt();
            sc.nextLine();  // Consume newline

            System.out.print("Enter total number of rooms: ");
            int totalRooms = sc.nextInt();
            sc.nextLine();  // Consume newline

            System.out.print("Enter admin name: ");
            String adminName = sc.nextLine();

            System.out.print("Enter admin priority: ");
            int priority = sc.nextInt();
            sc.nextLine();  // Consume newline

            // Save the collected data into the file
            StringBuilder data = new StringBuilder();
            data.append(floorNumber).append(",").append(totalRooms).append(",").append(adminName).append(",").append(priority);

            // Gather room details
            for (int i = 1; i <= totalRooms; i++) {
                System.out.println("Enter details for Room " + i);
                System.out.print("Room Name: ");
                String roomName = sc.nextLine();

                System.out.print("Capacity: ");
                int capacity = sc.nextInt();
                sc.nextLine();  // Consume newline

                // Append room details to the data string
                data.append(",").append(roomName).append(",").append(capacity);
            }

            writer.write(data.toString());
            writer.newLine();
            System.out.println("Plan details saved locally.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Process and sync plan details to the database once connection is available
public static void processPendingPlanRequests(Connection conn) {
    try (BufferedReader reader = new BufferedReader(new FileReader(PLAN_DATA_FILE))) {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] planData = line.split(",");

            // Extracting details from the stored string data
            int floorNumber = Integer.parseInt(planData[0]);
            int totalRooms = Integer.parseInt(planData[1]);
            String adminName = planData[2];
            int priority = Integer.parseInt(planData[3]);

            System.out.println("Syncing plan data for floor " + floorNumber + " to the database...");

            // Sync floor plan data to the database
            try {
                // Check if the floor already exists
                String checkFloorSql = "SELECT id FROM floors WHERE floor_number = ?";
                PreparedStatement checkFloorPstmt = conn.prepareStatement(checkFloorSql);
                checkFloorPstmt.setInt(1, floorNumber);
                ResultSet floorRs = checkFloorPstmt.executeQuery();

                if (floorRs.next()) {
                    System.out.println("Floor already exists. Skipping plan sync for floor: " + floorNumber);
                } else {
                    // Insert new floor plan
                    String insertFloorSql = "INSERT INTO floors (floor_number, total_rooms) VALUES (?, ?)";
                    PreparedStatement insertFloorPstmt = conn.prepareStatement(insertFloorSql, Statement.RETURN_GENERATED_KEYS);
                    insertFloorPstmt.setInt(1, floorNumber);
                    insertFloorPstmt.setInt(2, totalRooms);
                    int floorRowsAffected = insertFloorPstmt.executeUpdate();

                    if (floorRowsAffected == 0) {
                        System.out.println("Failed to create new floor.");
                        return; // Exit if the insert fails
                    }

                    // Get the new floor ID
                    ResultSet generatedKeys = insertFloorPstmt.getGeneratedKeys();
                    if (!generatedKeys.next()) {
                        System.out.println("Failed to retrieve the new floor ID.");
                        return; // Exit if the key retrieval fails
                    }
                    int floorId = generatedKeys.getInt(1); // Get the new floor ID
                    System.out.println("New floor created with ID: " + floorId);

                    // Insert into "floor_plans" table
                    String insertPlanSql = "INSERT INTO floor_plans (floor_id, plan_details, finalised_by, priority, is_finalised) VALUES (?, ?, ?, ?, true)";
                    PreparedStatement insertPlanPstmt = conn.prepareStatement(insertPlanSql, Statement.RETURN_GENERATED_KEYS);
                    insertPlanPstmt.setInt(1, floorId); // Use the newly created floor ID
                    insertPlanPstmt.setString(2, "Floor " + floorNumber + " plan finalized by " + adminName);
                    insertPlanPstmt.setString(3, adminName);
                    insertPlanPstmt.setInt(4, priority);
                    insertPlanPstmt.executeUpdate();

                    // Get the new plan ID from floor_plans
                    ResultSet planGeneratedKeys = insertPlanPstmt.getGeneratedKeys();
                    if (!planGeneratedKeys.next()) {
                        System.out.println("Failed to retrieve the new floor plan ID.");
                        return; // Exit if the key retrieval fails
                    }
                    int floorPlanId = planGeneratedKeys.getInt(1); // Get the new floor plan ID

                    // Now sync the room details
                    String insertRoomSql = "INSERT INTO rooms (floor_id, room_name, capacity) VALUES (?, ?, ?)";
                    PreparedStatement insertRoomPstmt = conn.prepareStatement(insertRoomSql);

                    for (int i = 0; i < totalRooms; i++) {
                        String roomName = planData[4 + i * 2]; // Room name at every 4 + 2*i
                        int capacity = Integer.parseInt(planData[5 + i * 2]); // Capacity at every 5 + 2*i

                        insertRoomPstmt.setInt(1, floorId); // Link room to the new floor
                        insertRoomPstmt.setString(2, roomName);
                        insertRoomPstmt.setInt(3, capacity);
                        insertRoomPstmt.executeUpdate();

                        System.out.println("Room " + roomName + " for floor " + floorNumber + " inserted successfully!");
                    }

                    // Prepare plan details for versions
                    String planDetails = "Floor " + floorNumber + " plan finalized by " + adminName;

                    // Insert into "floor_plan_versions" table
                    String insertVersionSql = "INSERT INTO floor_plan_versions (floor_plan_id, previous_plan_details, finalised_by, priority) VALUES (?, ?, ?, ?)";
                    PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
                    insertVersionPstmt.setInt(1, floorPlanId); // Use the newly created floor plan ID (not floor ID)
                    insertVersionPstmt.setString(2, planDetails);
                    insertVersionPstmt.setString(3, adminName);
                    insertVersionPstmt.setInt(4, priority);
                    insertVersionPstmt.executeUpdate();

                    System.out.println("Floor plan version added successfully for floor " + floorNumber + ".");
                }
            } catch (SQLException e) {
                System.out.println("Failed to sync plan data for floor " + floorNumber);
                e.printStackTrace();
            }
        }

        // Optionally, delete the file after processing
        File file = new File(PLAN_DATA_FILE);
        // if (file.exists()) {
        //     file.delete();
        // }

    } catch (IOException e) {
        e.printStackTrace();
    }
}

    // Method for signup
public static void signup(Connection conn, Scanner sc) {
    try {
        System.out.println("\n------------ Sign Up ------------");
        System.out.print("Enter Username: ");
        String username = sc.nextLine();

        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        System.out.print("Enter Role (user/admin): ");
        String role = sc.nextLine().toLowerCase();

        // Initialize priority as null
        String priority = null;

        // If role is admin, ask for priority
        if (role.equals("admin")) {
            System.out.print("Enter Priority: ");
            priority = sc.nextLine(); // Admins must enter a priority
        }

        System.out.println("---------------------------------");

        // Validate role input
        if (!role.equals("user") && !role.equals("admin")) {
            System.out.println("\n[ERROR] Invalid role. Please choose 'user' or 'admin'.\n");
            return;
        }

        // Prepare SQL statement including priority for both admin and users
        String sql = "INSERT INTO users (username, password, role, priority) VALUES (?, ?, ?, ?)";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, username);
        pstmt.setString(2, password);
        pstmt.setString(3, role);

        // Set priority value for admin, and NULL for user
        if (role.equals("admin")) {
            pstmt.setInt(4, Integer.parseInt(priority)); // Admins must have a priority
        } else {
            pstmt.setNull(4, java.sql.Types.INTEGER); // No priority for users, insert NULL
        }

        int rows = pstmt.executeUpdate();
        if (rows > 0) {
            System.out.println("\n[SUCCESS] Signup completed!! You may now login.\n");
        } else {
            System.out.println("\n[ERROR] Signup failed!!\n");
        }
    } catch (SQLIntegrityConstraintViolationException e) {
        System.out.println("\n[ERROR] Username already exists. Please try again!!\n");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    // Method for login
    public static void login(Connection conn, Scanner sc) {
        try {
            System.out.println("\n-------- Login as: --------");
            System.out.println("         1. User");
            System.out.println("         2. Admin");
            System.out.println("         3. Exit");
            System.out.println("---------------------------");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();  // Consume newline

            if (choice == 3) {
                return;
            }
            if(choice == 1) System.out.println("\n---------- User Login ----------");
            if(choice == 2) System.out.println("\n---------- Admin Login ---------");
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
            if(choice == 1 || choice == 2) System.out.println("--------------------------------");
            if(choice == 3) System.out.println();
            if (rs.next()) {
                System.out.println("\n[Success] Login successful! Welcome, " + username + " (" + role + ")\n");
                if (role.equals("user")) {
                    userMenu(conn, sc);
                } else {
                    loggedInAdminUsername = rs.getString("username");
                    loggedInAdminPriority = rs.getInt("priority");
                    System.out.println("Logged in as admin: " + loggedInAdminUsername + " with priority: " + loggedInAdminPriority);
                    adminMenu(conn, sc);
                }
            } else {
                System.out.println("\n[Error] Invalid credentials or role. Try again!!\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void adminMenu(Connection conn, Scanner sc) {
        boolean adminRunning = true;
        while (adminRunning) {
            System.out.println("------- Admin Menu -------");
            System.out.println("      1. Add Plan");
            System.out.println("      2. Update Plan");
            System.out.println("      3. Exit");
            System.out.println("--------------------------");
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
                    System.out.println("\n[SUCCESS] Exiting Admin Menu...\n");
                    break;
                default:
                    System.out.println("\n[ERROR] Invalid choice. Try again!!\n");
            }
        }
    }

    
public static void addPlan(Connection conn, Scanner sc) {
    try {
        System.out.println("\n------------------------- Addding a New Plan -------------------------");
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
            System.out.println("\n[ERROR] You can't add the plan as this floor already exists.\n");
            System.out.println("----------------------------------------------------------------------\n");
            return;  // Exit the method
        } else {
            // Floor does not exist, gather details for the new floor
            System.out.println("\nThe plan for floor number " + floorNumber + " has not been created previously.");
            System.out.println("Creating a new plan for floor: " + floorNumber + "\n");

            String adminName = loggedInAdminUsername;
            int priority = loggedInAdminPriority;

            // Gather floor details
            System.out.print("\nPlease enter the total number of rooms for this floor: ");
            int totalRooms = sc.nextInt();
            sc.nextLine();  // Consume newline

            // Insert the new floor into the database
            String insertFloorSql = "INSERT INTO floors (floor_number, total_rooms) VALUES (?, ?)";
            PreparedStatement insertFloorPstmt = conn.prepareStatement(insertFloorSql, Statement.RETURN_GENERATED_KEYS);
            insertFloorPstmt.setInt(1, floorNumber);
            insertFloorPstmt.setInt(2, totalRooms);  // Set total_rooms
            int floorRowsAffected = insertFloorPstmt.executeUpdate();

            if (floorRowsAffected == 0) {
                System.out.println("\n[ERROR] Failed to create new floor.\n");
                return; // Exit if the insert fails
            }

            ResultSet generatedKeys = insertFloorPstmt.getGeneratedKeys();
            if (!generatedKeys.next()) {
                System.out.println("\n[ERROR] Failed to retrieve the new floor ID.\n");
                return; // Exit if the key retrieval fails
            }
            int floorId = generatedKeys.getInt(1); // Get the new floor ID
            System.out.println("\n[SUCCESS] New floor created with ID: " + floorId);

            // Gather room details
            List<Room> rooms = new ArrayList<>();

            for (int i = 1; i <= totalRooms; i++) {
                System.out.println("\nEnter details for Room " + i + ":");
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
                System.out.println("\n[ERROR] Failed to insert floor plan. Please check your data.\n");
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
            
            System.out.println();
            for (Room room : rooms) {
                insertRoomPstmt.setInt(1, floorId); // Link room to the new floor
                insertRoomPstmt.setString(2, room.getRoomName());
                insertRoomPstmt.setInt(3, room.getCapacity());
                insertRoomPstmt.executeUpdate();
                System.out.println("[SUCCESS] Room " + room.getRoomName() + " added successfully.");
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

            System.out.println("\n[SUCCESS] Floor plan added successfully for floor " + floorNumber + " and finalized.\n");
        }
        System.out.println("----------------------------------------------------------------------\n");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    // Room class to hold room details
    static class Room {
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
        System.out.println("\n------------------------- Updating the Plan -------------------------");
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
            System.out.println("\n[ERROR] There is no floor associated with the given number.\n");
            System.out.println("-------------------------------------------------------------------\n");
            return;
        }

        // Floor exists, begin update process
        System.out.println("\nA floor with this number already exists.");
        System.out.println("We can update this floor plan. Preparing to update...");

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
            System.out.println("\n[ERROR] There is no finalized plan available for this floor.\n");
            System.out.println("-------------------------------------------------------------------\n");
            return;
        }

        // Plan exists, begin updating
        String currentFinaliser = rs.getString("finalised_by");
        int currentPriority = rs.getInt("priority");

        String adminName = loggedInAdminUsername;
        int adminPriority = loggedInAdminPriority;

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
        System.out.print("\nPlease enter the total number of rooms for this floor: ");
        int numberOfRooms = sc.nextInt();
        sc.nextLine(); // Consume newline

        // Collect details for each room
        System.out.println();
        List<String> roomDetails = new ArrayList<>();
        for (int i = 0; i < numberOfRooms; i++) {
            System.out.print("Enter details for room " + (i + 1) + " (name and capacity separated by a comma): ");
            String roomDetail = sc.nextLine();
            roomDetails.add(roomDetail);
        }

        // If rooms are occupied, save version but do not update
        if (occupiedCount > 0) {
            System.out.println("\n[ERROR] Some rooms are currently occupied.");
            System.out.println("Unable to update the plan, but the version will be saved.\n");
            
            // Insert the current plan into the version history
            String insertVersionSql = "INSERT INTO floor_plan_versions (floor_plan_id, previous_plan_details, finalised_by, priority) " +
                                       "VALUES (?, ?, ?, ?)";
            PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
            insertVersionPstmt.setInt(1, rs.getInt("id")); // Use the existing plan's ID
            insertVersionPstmt.setString(2, String.join(";", roomDetails)); // Store room details
            insertVersionPstmt.setString(3, adminName);
            insertVersionPstmt.setInt(4, adminPriority);
            insertVersionPstmt.executeUpdate();

            System.out.println("\n[SUCCESS] The version has been stored successfully.\n");
            return;
        }

        // Check if the new admin can update the plan
        if (adminPriority < currentPriority) {
            System.out.println("\n[ERROR] Unable to update the plan due to low priority, but the version will be saved.");
            
            // Store the version but do not update the final plan
            String insertVersionSql = "INSERT INTO floor_plan_versions (floor_plan_id, previous_plan_details, finalised_by, priority) " +
                                       "VALUES (?, ?, ?, ?)";
            PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
            insertVersionPstmt.setInt(1, rs.getInt("id")); // Use the existing plan's ID
            insertVersionPstmt.setString(2, String.join(";", roomDetails)); // Store room details
            insertVersionPstmt.setString(3, adminName);
            insertVersionPstmt.setInt(4, adminPriority);
            insertVersionPstmt.executeUpdate();

            System.out.println("\n[SUCCESS] Version stored successfully.\n");
        } else {
            // **Step to delete existing rooms for the floor**
            System.out.println("\nDeleting previous rooms for the floor...");

            String deleteRoomsSql = "DELETE FROM rooms WHERE floor_id = ?";
            PreparedStatement deleteRoomsPstmt = conn.prepareStatement(deleteRoomsSql);
            deleteRoomsPstmt.setInt(1, floorId);
            deleteRoomsPstmt.executeUpdate();

            System.out.println("\n[SUCCESS] Previous rooms deleted successfully.");

            // Now proceed with updating the plan since admin has a higher priority
            String updatePlanSql = "UPDATE floor_plans SET plan_details = ?, finalised_by = ?, priority = ?, last_updated = CURRENT_TIMESTAMP " +
                                   "WHERE id = ?";
            PreparedStatement updatePlanPstmt = conn.prepareStatement(updatePlanSql);
            updatePlanPstmt.setString(1, String.join(";", roomDetails)); // Update with new room details
            updatePlanPstmt.setString(2, adminName);
            updatePlanPstmt.setInt(3, adminPriority);
            updatePlanPstmt.setInt(4, rs.getInt("id")); // Use the existing plan's ID
            updatePlanPstmt.executeUpdate();

            System.out.println("\n[SUCCESS] Plan updated successfully.\n");

            // Now update the rooms table with new room details
            int totalRoomsUpdated = 0; // Counter for updated room count
            for (String roomDetail : roomDetails) {
                String[] details = roomDetail.split(",");
                String roomName = details[0].trim();
                int capacity = Integer.parseInt(details[1].trim());

                // Insert new room
                String insertRoomSql = "INSERT INTO rooms (floor_id, room_name, capacity) VALUES (?, ?, ?)";
                PreparedStatement insertRoomPstmt = conn.prepareStatement(insertRoomSql);
                insertRoomPstmt.setInt(1, floorId); // Use the existing floor ID
                insertRoomPstmt.setString(2, roomName);
                insertRoomPstmt.setInt(3, capacity);
                insertRoomPstmt.executeUpdate();
                System.out.println("[Success] Added new room: " + roomName + " with capacity: " + capacity);
                totalRoomsUpdated++; // Increment the counter for new rooms added
            }

            // Update total_rooms in floors table
            String updateTotalRoomsSql = "UPDATE floors SET total_rooms = ? WHERE id = ?";
            PreparedStatement updateTotalRoomsPstmt = conn.prepareStatement(updateTotalRoomsSql);
            updateTotalRoomsPstmt.setInt(1, numberOfRooms); // Update with the number of new rooms added
            updateTotalRoomsPstmt.setInt(2, floorId); // Use the existing floor ID
            updateTotalRoomsPstmt.executeUpdate();
            System.out.println("\n[SUCCESS] Successfully updated the total number of rooms for floor " + floorNumber + "\n");
        }
        System.out.println("-------------------------------------------------------------------\n");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}




    // User menu for additional functionalities
    public static void userMenu(Connection conn, Scanner sc) {
        boolean userRunning = true;
        while (userRunning) {
            System.out.println("---------- User Menu ----------");
            System.out.println("    1. View Available Rooms");
            System.out.println("    2. Recommend Room");
            System.out.println("    3. Logout");
            System.out.println("-------------------------------");
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
                    System.out.println("\n[SUCCESS] Logging out...\n");
                    break;
                default:
                    System.out.println("\n[Error] Invalid choice. Try again!!\n");
            }
        }
    }

   // Method to view available rooms based on user-specified time range
public static void viewAvailableRooms(Connection conn, Scanner sc) {
    try {
        System.out.print("\nStart Time (HH:MM): ");
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

        System.out.println("\n------------------ Available Rooms ------------------");
        boolean foundAvailableRoom = false;

        // Print table header
        System.out.println(String.format("%-10s %-20s %-10s %-10s", "Room ID", "Name", "Floor", "Capacity"));
        while (rs.next()) {
            foundAvailableRoom = true;
            // Print each room's details in a formatted way
            System.out.println(String.format("%-10d %-20s %-10d %-10d", 
                                     rs.getInt("id"), 
                                     rs.getString("room_name"), 
                                     rs.getInt("floor_number"), 
                                     rs.getInt("capacity")));
        }

        if (!foundAvailableRoom) {
            System.out.println("No rooms are available for the specified time range.");
        }
        System.out.println("-----------------------------------------------------\n");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


    public static void recommendRoom(Connection conn, Scanner sc) {
    System.out.println("\n------- Room Recommendation -------");
    System.out.print("Start Time (HH:MM): ");
    String startTime = sc.nextLine();
    System.out.print("End Time (HH:MM): ");
    String endTime = sc.nextLine();
    System.out.print("Minimum Capacity: ");
    int minCapacity = sc.nextInt();
    sc.nextLine();  // Consume newline
    System.out.print("Preferred Floor Number: ");
    int preferredFloorNumber = sc.nextInt();
    sc.nextLine();  // Consume newline
    System.out.println("-----------------------------------\n");

    try {
        // Step 1: Fetch the floor ID based on the floor number
        String floorIdSql = "SELECT id FROM floors WHERE floor_number = ?";
        PreparedStatement floorIdPstmt = conn.prepareStatement(floorIdSql);
        floorIdPstmt.setInt(1, preferredFloorNumber);  // Use the floor number entered by the user

        ResultSet floorIdRs = floorIdPstmt.executeQuery();

        // Check if the floor exists
        if (!floorIdRs.next()) {
            System.out.println("No floor found with the given floor number.");
            return;
        }

        // Get the floor ID from the result set
        int preferredFloorId = floorIdRs.getInt("id");
        // Step 1: Check for room availability in the specified time slot
        String availabilitySql = "SELECT * FROM rooms WHERE id NOT IN " +
                                 "(SELECT room_id FROM bookings WHERE NOT(start_time >= ? OR end_time <= ?)) " +
                                 "AND available = true";

        PreparedStatement availabilityPstmt = conn.prepareStatement(availabilitySql);
        availabilityPstmt.setString(1, endTime);
        availabilityPstmt.setString(2, startTime);

        ResultSet availabilityRs = availabilityPstmt.executeQuery();

        // Collect available room IDs
        System.out.println("------------------------- Recommended Room -------------------------");
        List<Integer> availableRoomIds = new ArrayList<>();
        while (availabilityRs.next()) {
            availableRoomIds.add(availabilityRs.getInt("id"));
        }

        // Step 2: Check if any rooms are available
        if (availableRoomIds.isEmpty()) {
            System.out.println("No rooms are available in the given time slots.");
            System.out.println("--------------------------------------------------------------------\n");
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
            String recommendSql = "SELECT * FROM rooms WHERE floor_id = ? AND capacity < ? AND id IN (" +
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
                // Ask the user if they want to book a recommended room
                System.out.print("Would you like to book one of these rooms? (yes/no): ");
                String choice = sc.nextLine();

                if (choice.equalsIgnoreCase("yes")) {
                    System.out.print("Please enter the number of the room you wish to book (or type 'exit' to exit): ");
                    String roomInput = sc.nextLine();

                    // Check if the user wants to exit
                    if (roomInput.equalsIgnoreCase("exit")) {
                        System.out.println("Exiting without booking.");
                        System.out.println("--------------------------------------------------------------------\n");
                        return; // Exit without booking
                    }

                    try {
                        int roomNumber = Integer.parseInt(roomInput);
                        if (roomNumber > 0 && roomNumber <= recommendedRoomIds.size()) {
                            int roomIdToBook = recommendedRoomIds.get(roomNumber - 1);
                            String roomNameToBook = recommendedRoomNames.get(roomNumber - 1);
                            // Mark room as booked
                            bookRoom(conn, roomIdToBook, roomNameToBook, startTime, endTime);
                            System.out.println("Room booked: " + roomNameToBook + " on floor " + preferredFloorNumber);
                        } else {
                            System.out.println("Invalid room number selected.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid room number.");
                    }
                } else {
                    System.out.println("Sorry!!!");  // User chose not to book
                }
                return; // Stop further processing
            } else {
                System.out.println("No rooms available on your preferred floor with the required capacity.");
            }
            System.out.println("--------------------------------------------------------------------\n");
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
            
            // Ask if the user wants to book this room
            System.out.print("Room recommended: " + roomName + " on floor " + preferredFloorNumber + ". Would you like to book this room? (yes/no): ");
            String bookChoice = sc.nextLine();
            if (bookChoice.equalsIgnoreCase("yes")) {
                // Mark room as booked
                bookRoom(conn, roomId, roomName, startTime, endTime);
                System.out.println("Room booked: " + roomName + " on floor " + preferredFloorNumber);
            } else {
                System.out.println("Sorry!!! We would like to host u the next time.");  // User chose not to book
            }
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
                    
                    // Ask if the user wants to book this room
                    System.out.print("Recommended room on a nearby floor: " + roomName + ". Would you like to book this room? (yes/no): ");
                    String nearbyBookChoice = sc.nextLine();
                    if (nearbyBookChoice.equalsIgnoreCase("yes")) {
                        // Mark room as booked
                        bookRoom(conn, roomId, roomName, startTime, endTime);
                        System.out.println("Room booked: " + roomName);
                    } else {
                        System.out.println("Sorry!!!");  // User chose not to book
                    }
                }
            } else {
                System.out.println("No rooms available on nearby floors.");
            }
        }
        System.out.println("--------------------------------------------------------------------\n");
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


