import java.sql.*;
import java.util.*;
import java.io.*; // For file handling
import java.util.stream.Collectors;

public class UserService {

    private Connection conn;
    private String loggedInUserUsername;

    public UserService(Connection conn,String loggedInUserUsername) {
        this.conn = conn;
        this.loggedInUserUsername=loggedInUserUsername;
    }

    // User menu
    public void userMenu(Scanner sc) {
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
                    viewAvailableRooms(sc);
                    break;
                case 2:
                    recommendRoom(sc);
                    break;
                case 3:
                    userRunning = false;
                    System.out.println("\n[SUCCESS] Logging out...\n");
                    break;
                default:
                    System.out.println("\n[ERROR] Invalid choice. Try again!!\n");
            }
        }
    }

    public void viewAvailableRooms(Scanner sc) {
        try {
            System.out.print("\nStart Time (HH:MM): ");
            String startTime = sc.nextLine();
            System.out.print("End Time (HH:MM): ");
            String endTime = sc.nextLine();

            // Query to find available rooms
            String sql = "SELECT r.id, r.room_name, r.capacity, f.floor_number " +
                     "FROM rooms r " +
                     "JOIN floors f ON r.floor_id = f.id " +
                     "WHERE r.id NOT IN (SELECT room_id FROM bookings WHERE (start_time < ? AND end_time > ?))";
            
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

    // Recommend room method
    public void recommendRoom(Scanner sc) {
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
                            bookRoom(roomIdToBook, roomNameToBook, startTime, endTime,loggedInUserUsername);
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
                bookRoom(roomId, roomName, startTime, endTime,loggedInUserUsername);
                System.out.println("Room booked: " + roomName + " on floor " + preferredFloorNumber);
            } else {
                System.out.println("Sorry!!! We would like to host u the next time.");  // User chose not to book
            }
        } else {
            System.out.println("No rooms available on your preferred floor.");

        String availableRoomsSql = "SELECT r.id, r.room_name, r.capacity, f.floor_number " +
                                   "FROM rooms r " +
                                   "JOIN floors f ON r.floor_id = f.id " +
                                   "WHERE r.id NOT IN (SELECT room_id FROM bookings WHERE (start_time < ? AND end_time > ?)) " +
                                   "AND r.capacity >= ?";
        
        PreparedStatement availableRoomsPstmt = conn.prepareStatement(availableRoomsSql);
        availableRoomsPstmt.setString(1, endTime);
        availableRoomsPstmt.setString(2, startTime);
        availableRoomsPstmt.setInt(3, minCapacity);

        ResultSet availableRoomsRs = availableRoomsPstmt.executeQuery();

        // Step 2: Organize available rooms by floor distance from the preferred floor
        TreeMap<Integer, List<Room>> floorDistanceMap = new TreeMap<>();

        while (availableRoomsRs.next()) {
            int roomId = availableRoomsRs.getInt("id");
            String roomName = availableRoomsRs.getString("room_name");
            int roomCapacity = availableRoomsRs.getInt("capacity");
            int floorNumber = availableRoomsRs.getInt("floor_number");

            // Calculate the distance from the preferred floor
            int distance = Math.abs(preferredFloorNumber - floorNumber);

            // Add the room to the map based on its floor distance
            Room room = new Room(roomId, roomName, roomCapacity, floorNumber);
            floorDistanceMap.computeIfAbsent(distance, k -> new ArrayList<>()).add(room);
        }

        // Step 3: Recommend rooms from the nearest floors
        boolean roomRecommended = false;
        for (Map.Entry<Integer, List<Room>> entry : floorDistanceMap.entrySet()) {
            List<Room> roomsOnFloor = entry.getValue();
            for (Room room : roomsOnFloor) {
                System.out.println("Recommended room: " + room.getRoomName() + " on floor " + room.getFloorNumber() +
                                   " (Capacity: " + room.getCapacity() + ")");
                
                // Ask if the user wants to book this room
                System.out.print("Would you like to book this room? (yes/no): ");
                String choice = sc.nextLine();

                if (choice.equalsIgnoreCase("yes")) {
                    // Book the room
                    bookRoom(room.getId(), room.getRoomName(), startTime, endTime, loggedInUserUsername);
                    System.out.println("Room booked: " + room.getRoomName() + " on floor " + room.getFloorNumber());
                    roomRecommended = true;
                    break;
                }
            }
            if (roomRecommended) break;
        }

        // Step 4: If no rooms were booked, notify the user
        if (!roomRecommended) {
            System.out.println("No rooms booked. All nearby options were declined.");
        }
        }
        System.out.println("--------------------------------------------------------------------\n");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    }

    // Book room method
    public void bookRoom(int roomId, String roomName, String startTime, String endTime,String user_name) {
        try {
            //get userid from username
            String sql="SELECT id FROM users WHERE username=?";
            PreparedStatement stmt=conn.prepareStatement(sql);
            stmt.setString(1,user_name);
            ResultSet rs=stmt.executeQuery();
            if(!rs.next())
            {
                System.out.println("No user found with given name");
                return;
            }
            int userid=rs.getInt("id");
            
            // Insert booking into the bookings table
            String bookingSql = "INSERT INTO bookings (room_id, start_time, end_time,user_id) VALUES (?, ?, ?,?)";
            PreparedStatement pstmt = conn.prepareStatement(bookingSql);
            pstmt.setInt(1, roomId);
            pstmt.setString(2, startTime);
            pstmt.setString(3, endTime);
            pstmt.setInt(4,userid);
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

    static class Room {
    private int id;
    private String roomName;
    private int capacity;
    private int floorNumber;

    public Room(int id, String roomName, int capacity, int floorNumber) {
        this.id = id;
        this.roomName = roomName;
        this.capacity = capacity;
        this.floorNumber = floorNumber;
    }

    public int getId() { return id; }
    public String getRoomName() { return roomName; }
    public int getCapacity() { return capacity; }
    public int getFloorNumber() { return floorNumber; }
}
}