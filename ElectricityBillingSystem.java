package electricity_billing_system;
import java.sql.*;
import java.util.Scanner;

public class ElectricityBillingSystem {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/electricity_billing"; // Replace
    private static final String DB_USER = "root";                                  // Replace
    private static final String DB_PASSWORD = "root";                              // Replace

    private static int loggedInUserId = -1; // To track the logged-in user

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ElectricityBillingSystem billingSystem = new ElectricityBillingSystem();

        while (loggedInUserId == -1) {
            System.out.println("\nElectricity Billing System - Authentication");
            System.out.println("1. Sign Up");
            System.out.println("2. Log In");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            int authChoice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (authChoice) {
                case 1:
                    billingSystem.signUp(scanner);
                    break;
                case 2:
                    billingSystem.logIn(scanner);
                    break;
                case 3:
                    System.out.println("Exiting system.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        // Main application menu after successful login
        while (loggedInUserId != -1) {
            System.out.println("\nElectricity Billing System - Console (Logged in as User ID: " + loggedInUserId + ")");
            System.out.println("1. Add New Customer");
            System.out.println("2. Record Meter Reading");
            System.out.println("3. Generate Bill for Period");
            System.out.println("4. View Billing History for Customer");
            System.out.println("5. Mark Bill as Paid"); // New Feature
            System.out.println("6. Delete Customer");   // New Feature
            System.out.println("7. Log Out");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    billingSystem.addCustomer(scanner);
                    break;
                case 2:
                    billingSystem.recordMeterReading(scanner);
                    break;
                case 3:
                    billingSystem.generateBillForPeriod(scanner);
                    break;
                case 4:
                    billingSystem.viewBillingHistory(scanner);
                    break;
                case 5: // Mark Bill as Paid
                    billingSystem.markBillAsPaid(scanner);
                    break;
                case 6: // Delete Customer
                    billingSystem.deleteCustomer(scanner);
                    break;
                case 7:
                    loggedInUserId = -1;
                    System.out.println("Logged out successfully.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
        scanner.close();
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Replace with your driver
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found!", e);
        }
    }

    public void signUp(Scanner scanner) {
        System.out.print("Enter new username: ");
        String username = scanner.nextLine();
        System.out.print("Enter new password: ");
        String password = scanner.nextLine(); // In a real app, hash this!

        String sql = "INSERT INTO Users (username, password) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // Never store plain passwords!
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Sign up successful. You can now log in.");
            } else {
                System.out.println("Sign up failed.");
            }
        } catch (SQLException e) {
            System.err.println("Error signing up: " + e.getMessage());
        }
    }

    public void logIn(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine(); 
        String sql = "SELECT user_id, password FROM Users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password"); 
                int userId = rs.getInt("user_id");
                if (password.equals(storedPassword)) { 
                    loggedInUserId = userId;
                    System.out.println("Login successful. Welcome User ID: " + userId);
                } else {
                    System.out.println("Login failed. Incorrect password.");
                }
            } else {
                System.out.println("Login failed. User not found.");
            }
        } catch (SQLException e) {
            System.err.println("Error logging in: " + e.getMessage());
        }
    }

    public void addCustomer(Scanner scanner) {
        System.out.print("Enter customer name: ");
        String name = scanner.nextLine();
        System.out.print("Enter customer address: ");
        String address = scanner.nextLine();
        System.out.print("Enter customer phone number: ");
        String phone = scanner.nextLine();
        System.out.print("Enter meter number: ");
        String meter = scanner.nextLine();

        String sql = "INSERT INTO Customers (name, address, phone_number, meter_number) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, address);
            pstmt.setString(3, phone);
            pstmt.setString(4, meter);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Customer added successfully.");
            } else {
                System.out.println("Failed to add customer.");
            }
        } catch (SQLException e) {
            System.err.println("Error adding customer: " + e.getMessage());
        }
    }

    public void recordMeterReading(Scanner scanner) {
        System.out.print("Enter customer meter number: ");
        String meterNumber = scanner.nextLine();
        System.out.print("Enter reading date (YYYY-MM-DD): ");
        String readingDate = scanner.nextLine();
        System.out.print("Enter units consumed: ");
        int units = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        int customerId = getCustomerIdByMeterNumber(meterNumber);
        if (customerId == -1) {
            System.out.println("Customer with meter number " + meterNumber + " not found.");
            return;
        }

        String sql = "INSERT INTO BillingData (customer_id, reading_date, units_consumed) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setDate(2, Date.valueOf(readingDate));
            pstmt.setInt(3, units);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Meter reading recorded successfully.");
            } else {
                System.out.println("Failed to record meter reading.");
            }
        } catch (SQLException e) {
            System.err.println("Error recording meter reading: " + e.getMessage());
        }
    }

    public void generateBillForPeriod(Scanner scanner) {
        System.out.print("Enter customer meter number to generate bill: ");
        String meterNumber = scanner.nextLine();
        System.out.print("Enter billing period start date (YYYY-MM-DD): ");
        String startDate = scanner.nextLine();
        System.out.print("Enter billing period end date (YYYY-MM-DD): ");
        String endDate = scanner.nextLine();

        int customerId = getCustomerIdByMeterNumber(meterNumber);
        if (customerId == -1) {
            System.out.println("Customer with meter number " + meterNumber + " not found.");
            return;
        }

      
        int totalUnits = getTotalUnitsConsumed(customerId, startDate, endDate);
        if (totalUnits == -1) {
            System.out.println("No meter readings found for the specified period.");
            return;
        }

       
        double totalAmount = calculateBillAmount(totalUnits);

        
        String sql = "INSERT INTO BillingData (customer_id, reading_date, billing_period_start, billing_period_end, units_consumed, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setDate(2, Date.valueOf(endDate)); 
            pstmt.setDate(3, Date.valueOf(startDate));
            pstmt.setDate(4, Date.valueOf(endDate));
            pstmt.setInt(5, totalUnits);
            pstmt.setDouble(6, totalAmount);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("\n--- Electricity Bill ---");
                getCustomerDetails(customerId);
                System.out.println("Billing Period: " + startDate + " to " + endDate);
                System.out.println("Total Units Consumed: " + totalUnits);
                System.out.printf("Total Amount Due: %.2f INR\n", totalAmount);
                System.out.println("----------------------");
            } else {
                System.out.println("Failed to generate bill.");
            }
        } catch (SQLException e) {
            System.err.println("Error generating bill: " + e.getMessage());
        }
    }

    public void viewBillingHistory(Scanner scanner) {
        System.out.print("Enter customer meter number to view billing history: ");
        String meterNumber = scanner.nextLine();

        int customerId = getCustomerIdByMeterNumber(meterNumber);
        if (customerId == -1) {
            System.out.println("Customer with meter number " + meterNumber + " not found.");
            return;
        }

       String sql = "SELECT billing_period_start, billing_period_end, units_consumed, total_amount, billing_date, bill_paid FROM BillingData WHERE customer_id = ? AND total_amount IS NOT NULL ORDER BY billing_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            System.out.println("\n--- Billing History for Customer ---");
            getCustomerDetails(customerId);
            boolean found = false;
            while (rs.next()) {
                System.out.println("Billing Period: " + rs.getDate("billing_period_start") + " to " + rs.getDate("billing_period_end"));
                System.out.println("Units Consumed: " + rs.getInt("units_consumed"));
                System.out.printf("Total Amount: %.2f INR\n", rs.getDouble("total_amount"));
                System.out.println("Billing Date: " + rs.getTimestamp("billing_date"));
                System.out.println("Bill Paid: " + (rs.getBoolean("bill_paid") ? "Yes" : "No")); 
                System.out.println("----------------------------------");
                found = true;
            }
            if (!found) {
                System.out.println("No billing history found for this customer.");
            }
        } catch (SQLException e) {
            System.err.println("Error viewing billing history: " + e.getMessage());
        }
    }

    public void markBillAsPaid(Scanner scanner) {
        System.out.print("Enter customer meter number: ");
        String meterNumber = scanner.nextLine();
        System.out.print("Enter billing period end date (YYYY-MM-DD) for the bill you want to mark as paid: ");
        String endDate = scanner.nextLine();

        int customerId = getCustomerIdByMeterNumber(meterNumber);
        if (customerId == -1) {
            System.out.println("Customer with meter number " + meterNumber + " not found.");
            return;
        }

        String sql = "UPDATE BillingData SET bill_paid = TRUE WHERE customer_id = ? AND billing_period_end = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setDate(2, Date.valueOf(endDate));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Bill marked as paid.");
            } else {
                System.out.println("No matching bill found to mark as paid.");
            }
        } catch (SQLException e) {
            System.err.println("Error marking bill as paid: " + e.getMessage());
        }
    }

    public void deleteCustomer(Scanner scanner) {
         System.out.print("Enter customer meter number to delete: ");
        String meterNumber = scanner.nextLine();

        int customerId = getCustomerIdByMeterNumber(meterNumber);
        if (customerId == -1) {
            System.out.println("Customer with meter number " + meterNumber + " not found.");
            return;
        }

        System.out.print("Are you sure you want to delete customer " + meterNumber + "? (y/n): ");
        String confirmation = scanner.nextLine();
        if (!confirmation.equalsIgnoreCase("y")) {
            System.out.println("Customer deletion cancelled.");
            return;
        }

        String sql = "DELETE FROM Customers WHERE customer_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Customer deleted successfully.");
            } else {
                System.out.println("Failed to delete customer."); // Should not happen if customerId is valid
            }
        } catch (SQLException e) {
            System.err.println("Error deleting customer: " + e.getMessage());
        }
    }


    private int getCustomerIdByMeterNumber(String meterNumber) {
        String sql = "SELECT customer_id FROM Customers WHERE meter_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, meterNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("customer_id");
            }
        } catch (SQLException e) {
            System.err.println("Error getting customer ID: " + e.getMessage());
        }
        return -1;
    }

    private int getTotalUnitsConsumed(int customerId, String startDate, String endDate) {
        String sql = "SELECT SUM(units_consumed) AS total_units FROM BillingData WHERE customer_id = ? AND reading_date >= ? AND reading_date <= ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_units");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total units consumed: " + e.getMessage());
        }
        return -1;
    }

    private double calculateBillAmount(int totalUnits) {
        double totalAmount = 0.0;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT slab_start, slab_end, rate FROM TariffRates ORDER BY slab_start")) {
            int remainingUnits = totalUnits;
            while (rs.next() && remainingUnits > 0) {
                int slabStart = rs.getInt("slab_start");
                Integer slabEnd = (Integer) rs.getObject("slab_end");
                double rate = rs.getDouble("rate");

                int unitsInSlab;
                if (slabEnd == null) {
                    unitsInSlab = remainingUnits;
                } else {
                    unitsInSlab = Math.min(remainingUnits, slabEnd - slabStart + 1);
                }
                totalAmount += unitsInSlab * rate;
                remainingUnits -= unitsInSlab;
            }
        } catch (SQLException e) {
            System.err.println("Error calculating bill amount: " + e.getMessage());
        }
        return totalAmount;
    }

    private void getCustomerDetails(int customerId) {
        String sql = "SELECT name, address, meter_number FROM Customers WHERE customer_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Customer Name: " + rs.getString("name"));
                System.out.println("Address: " + rs.getString("address"));
                System.out.println("Meter Number: " + rs.getString("meter_number"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting customer details: " + e.getMessage());
        }
    }
}