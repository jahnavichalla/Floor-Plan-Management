import java.sql.*;
import java.util.*;
import java.io.*; // For file handling
import java.util.stream.Collectors;

public class FloorPlanService {

    private Connection conn;
    private static final String PLAN_DATA_FILE = "plan_data.txt";  // File to store plan details

    public FloorPlanService(Connection conn) {
        this.conn = conn;
    }

    // Collect plan data offline (for delayed connection)
    public void collectPlanData(Scanner sc) {
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

    // Sync pending plans after connection is established
    public void processPendingPlanRequests() {
        
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
}
