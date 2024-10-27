import java.sql.*;
import java.util.*;
import java.io.*; // For file handling
import java.util.stream.Collectors;

public class Main {

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost/FloorManagementDb?useSSL=false";
    static final String USER = "Nikki"; // Add your user 
    static final String PASS = "Nikki@2002"; // Add password
    static String loggedInAdminUsername = null;  
    static int loggedInAdminPriority = -1;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Connection conn = null;
        long startTime = System.currentTimeMillis();
        boolean connectionEstablished = false;

        System.out.println("Database connection not yet established.");
        // FloorPlanService floorPlanService1 = new FloorPlanService(conn);
        // floorPlanService1.collectPlanData(sc);

        System.out.println("Attempting to connect to the database...");
        while (!connectionEstablished) {
            try {
                if (System.currentTimeMillis() - startTime >=10) {
                    System.out.println("Connecting to database...");
                    conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    System.out.println("Connection successful!");
                    connectionEstablished = true;
                }
            } catch (SQLException e) {
                System.out.println("Connection failed. Retrying...");
                try {
                    Thread.sleep(1000); // Wait 10ms before retrying
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        
        // FloorPlanService floorPlanService2 = new FloorPlanService(conn);
        // floorPlanService2.processPendingPlanRequests();

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
            if (choice == 1) System.out.println("\n---------- User Login ----------");
            if (choice == 2) System.out.println("\n---------- Admin Login ---------");
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
            if (choice == 1 || choice == 2) System.out.println("--------------------------------");
            if (choice == 3) System.out.println();
            if (rs.next()) {
                System.out.println("\n[Success] Login successful! Welcome, " + username + " (" + role + ")\n");
                if (role.equals("user")) {
                    UserService userService = new UserService(conn);
                    userService.userMenu(sc);  // Call user menu
                } else {
                    loggedInAdminUsername = rs.getString("username");
                    loggedInAdminPriority = rs.getInt("priority");
                    System.out.println("Logged in as admin: " + loggedInAdminUsername + " with priority: " + loggedInAdminPriority);

                    AdminService adminService = new AdminService(conn, loggedInAdminUsername, loggedInAdminPriority);
                    adminService.adminMenu(sc);  // Call admin menu
                }
            } else {
                System.out.println("\n[Error] Invalid credentials or role. Try again!!\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
