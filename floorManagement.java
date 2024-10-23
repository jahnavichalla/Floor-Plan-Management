// STEP 1. Import required packages

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

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
                    System.out.println("Added Plan");
                    break;
                case 2:
                    System.out.println("1. Updated Plan");
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
                                  availableRoomIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") LIMIT 2";
            PreparedStatement recommendPstmt = conn.prepareStatement(recommendSql);
            recommendPstmt.setInt(1, preferredFloor);
            recommendPstmt.setInt(2, minCapacity);

            ResultSet recommendRs = recommendPstmt.executeQuery();

            List<String> recommendedRooms = new ArrayList<>();
            while (recommendRs.next()) {
                int roomId = recommendRs.getInt("id");
                String roomName = recommendRs.getString("room_name");
                recommendedRooms.add(roomName);
            }

            if (!recommendedRooms.isEmpty()) {
                System.out.println("Recommended rooms with capacity less than or equal to the required capacity on your preferred floor:");
                for (String roomName : recommendedRooms) {
                    System.out.println("- " + roomName);
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
