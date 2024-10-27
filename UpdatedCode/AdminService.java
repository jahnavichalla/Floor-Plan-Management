import java.sql.*;
import java.util.*;
import java.io.*; // For file handling
import java.util.stream.Collectors;

public class AdminService {

    private Connection conn;
    private String loggedInAdminUsername;  
    private int loggedInAdminPriority;

    public AdminService(Connection conn, String loggedInAdminUsername, int loggedInAdminPriority) {
        this.conn = conn;
        this.loggedInAdminUsername = loggedInAdminUsername;
        this.loggedInAdminPriority = loggedInAdminPriority;
    }

    // Admin menu
    public void adminMenu(Scanner sc) {
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
                    addPlan(sc);
                    break;
                case 2:
                    updatePlan(sc);
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

    // Add Plan method
    public void addPlan(Scanner sc) {
        try {
            System.out.println("\n------------------------- Adding a New Plan -------------------------");
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

    public void updatePlan(Scanner sc) {
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
                // Floor does not exist, show error
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
                // No finalized plan exists
                System.out.println("\n[ERROR] There is no finalized plan available for this floor.\n");
                System.out.println("-------------------------------------------------------------------\n");
                return;
            }

            // Plan exists, begin updating
            String currentFinaliser = rs.getString("finalised_by");
            int currentPriority = rs.getInt("priority");

            // Check if there are any rooms occupied on this floor
            String checkOccupiedSql = "SELECT COUNT(*) FROM bookings AS b " +
                                  "JOIN rooms AS r ON r.id = b.room_id " +
                                  "WHERE r.floor_id = ? " +
                                  "AND b.end_time > NOW()";
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
                insertVersionPstmt.setString(3, loggedInAdminUsername);
                insertVersionPstmt.setInt(4, loggedInAdminPriority);
                insertVersionPstmt.executeUpdate();

                System.out.println("\n[SUCCESS] The version has been stored successfully.\n");
                return;
            }

            // Check if the new admin can update the plan
            if (loggedInAdminPriority < currentPriority) {
                System.out.println("\n[ERROR] Unable to update the plan due to low priority, but the version will be saved.");

                // Store the version but do not update the final plan
                String insertVersionSql = "INSERT INTO floor_plan_versions (floor_plan_id, previous_plan_details, finalised_by, priority) " +
                        "VALUES (?, ?, ?, ?)";
                PreparedStatement insertVersionPstmt = conn.prepareStatement(insertVersionSql);
                insertVersionPstmt.setInt(1, rs.getInt("id")); // Use the existing plan's ID
                insertVersionPstmt.setString(2, String.join(";", roomDetails)); // Store room details
                insertVersionPstmt.setString(3, loggedInAdminUsername);
                insertVersionPstmt.setInt(4, loggedInAdminPriority);
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
                updatePlanPstmt.setString(2, loggedInAdminUsername);
                updatePlanPstmt.setInt(3, loggedInAdminPriority);
                updatePlanPstmt.setInt(4, rs.getInt("id")); // Use the existing plan's ID
                updatePlanPstmt.executeUpdate();

                System.out.println("\n[SUCCESS] Plan updated successfully.\n");

                // Now update the rooms table with new room details
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

}
