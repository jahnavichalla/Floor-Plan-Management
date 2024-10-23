import java.sql.*;
import java.util.*;

public class floorManagement {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost/db1?useSSL=false"; // Ensure 'db' exists
    static final String USER = "Nikki"; // replace with your user
    static final String PASS = "Nikki@2002"; // replace with your password

    // List to hold pending signup requests
    private static List<SignupRequest> pendingRequests = new ArrayList<>();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Connection conn = null;

        // Loop to keep trying to connect until user decides to exit
        long startTime = System.currentTimeMillis();
        while (true) {
            try {
                // Attempt to establish the connection after 10 seconds
                if (System.currentTimeMillis() - startTime >= 10000) {
                    System.out.println("Connecting to database...");
                    conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    System.out.println("Connection successful!");
                    break; // Exit the loop if connection is successful
                }
                
                // Connection attempt failed
                System.out.println("Connection failed. Any sign-up requests will be stored locally.");
                
                // Collect signup info even if the connection is not established
                signupOffline(sc);

                // Exit if the user does not want to retry
                System.out.print("Would you like to retry connecting? (yes/no): ");
                String choice = sc.nextLine().trim().toLowerCase();
                if (choice.equals("no")) {
                    System.out.println("Exiting the application.");
                    return; // Exit the application
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Handle connection failure
            }
        }

        // Process any pending requests after successful connection
        processPendingRequests(conn);

        boolean running = true;
        while (running) {
            System.out.println("\nWelcome to the Login System!");
            System.out.println("1. Login");
            System.out.println("2. Exit");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    login(conn, sc);
                    break;
                case 2:
                    running = false;
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }

        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method for collecting signup info offline
    public static void signupOffline(Scanner sc) {
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

        // Store the signup request locally
        pendingRequests.add(new SignupRequest(username, password, role));
        System.out.println("Signup request stored locally. It will be processed when the connection is available.");
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

            if (conn != null) {
                // Connection is available, proceed to sign up
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
            } else {
                // Connection is not available, store the request locally
                pendingRequests.add(new SignupRequest(username, password, role));
                System.out.println("Signup request stored locally. It will be processed when the connection is available.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Username already exists. Try again.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Process pending signup requests when the connection is available
    private static void processPendingRequests(Connection conn) {
        for (SignupRequest request : pendingRequests) {
            try {
                String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, request.getUsername());
                pstmt.setString(2, request.getPassword());
                pstmt.setString(3, request.getRole());

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("Pending signup successful for user: " + request.getUsername());
                } else {
                    System.out.println("Pending signup failed for user: " + request.getUsername());
                }
            } catch (SQLException e) {
                System.out.println("Could not process pending signup for user: " + request.getUsername());
                e.printStackTrace();
            }
        }
        // Clear the pending requests after processing
        pendingRequests.clear();
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
                    // userMenu(conn, sc);
                } else {
                    // adminMenu(conn, sc);
                }
            } else {
                System.out.println("Invalid credentials or role. Try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ... (Rest of the userMenu and adminMenu methods, along with SignupRequest class)
}

// SignupRequest class to hold signup request data
class SignupRequest {
    private String username;
    private String password;
    private String role;

    public SignupRequest(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
}
