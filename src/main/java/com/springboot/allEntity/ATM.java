package com.springboot.allEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class ATM {
    private Scanner scanner = new Scanner(System.in);

    @Autowired
    private JavaMailSender jm; 

    public Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/ATMSystem";
        String username = "root";
        String password = "SumedhKedari@123";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(url, username, password);
    }

    public Account authenticateUser(Connection connection) throws SQLException {
        System.out.print("Enter your account number: ");
        String accountNumber = scanner.nextLine();
        System.out.print("Enter your PIN: ");
        int inputPin = scanner.nextInt();
        scanner.nextLine();

        String query = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, accountNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int pin = rs.getInt("pin");
                    double balance = rs.getDouble("balance");
                    String accountType = rs.getString("account_type");

                    if (pin == inputPin) {
                        if (accountType.equals("Savings")) {
                            return new SavingsAccount(accountNumber, pin, balance);
                        } else if (accountType.equals("Current")) {
                            return new CurrentAccount(accountNumber, pin, balance);
                        }
                    }
                }
            }
        }
        return null;
    }

    public void showMenu(Account account, Connection connection) {
        int choice;
        do {
            System.out.println("\n=== ATM Menu ===");
            System.out.println("1. Check Balance");
            System.out.println("2. Withdraw");
            System.out.println("3. Deposit");
            System.out.println("4. Change Pin");
            System.out.println("5. Mini Statment");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    checkBalance(account);
                    break;
                case 2:
                    withdraw(account, connection);
                    break;
                case 3:
                    deposit(account, connection);
                    break;
                case 4:
                	changePin(account, connection);
                    break;
                case 5:
                    viewMiniStatement(account, connection);
                    break;
                case 6:
                	System.out.println("Thank you for using the ATM. Goodbye!");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 4);
    }

    private void checkBalance(Account account) {
        System.out.println("Your current balance is: " + account.getBalance());
    }

    @SuppressWarnings("unused")
	private void changePin(Account account, Connection connection) {
        System.out.print("Enter your current PIN: ");
        int currentPin = scanner.nextInt();

        if (currentPin != account.getPin()) {
            System.out.println("Current PIN is incorrect.");
            return;
        }
        System.out.print("Enter your new PIN: ");
        int newPin = scanner.nextInt();

        if (!isValidPin(newPin)) {
            System.out.println("New PIN must be a 4-digit number.");
            return;
        }
        updatePinInDatabase(account, newPin, connection);
        
        account.setPin(newPin); 
        System.out.println("PIN changed successfully.");
    }
    private boolean isValidPin(int pin) {
        
        return pin >= 1000 && pin <= 9999;
    }
    private void updatePinInDatabase(Account account, int newPin, Connection connection) {
        String updateQuery = "UPDATE accounts SET pin = ? WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
            stmt.setInt(1, newPin);
            stmt.setString(2, account.getAccountNumber());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void withdraw(Account account, Connection connection) {
    	
        System.out.print("Enter the amount to withdraw: ");
        double amount = scanner.nextDouble();

        if (account.withdraw(amount)) {
            updateBalanceInDatabase(account, connection,amount,"withdraw");
        } else {
            System.out.println("Insufficient funds.");
        }
    }

    private void deposit(Account account, Connection connection) {
        System.out.print("Enter the amount to deposit: ");
        double amount = scanner.nextDouble();

        account.deposit(amount);
        updateBalanceInDatabase(account, connection,amount,"deposit");
    }

    private void updateBalanceInDatabase(Account account, Connection connection,double amount,String operation) {
    	 String updateQuery = "UPDATE accounts SET balance = ? WHERE account_number = ?";
         String updateQuery2 = "SELECT eamil FROM accounts WHERE account_number = ?";
         String insertMiniStatement = "INSERT INTO mini_statements (account_id, transaction_type, amount) VALUES ((SELECT id FROM accounts WHERE account_number = ?), ?, ?)";
         
         try (PreparedStatement stmt = connection.prepareStatement(updateQuery);
              PreparedStatement stmt2 = connection.prepareStatement(updateQuery2);
              PreparedStatement stmt3 = connection.prepareStatement(insertMiniStatement)) {

             stmt.setDouble(1, account.getBalance());
             stmt.setString(2, account.getAccountNumber());
             stmt.executeUpdate();

             stmt2.setString(1, account.getAccountNumber());
             ResultSet email = stmt2.executeQuery();

             stmt3.setString(1, account.getAccountNumber());
             stmt3.setString(2, operation);
             stmt3.setDouble(3, amount);
             stmt3.executeUpdate();

             if (email.next()) {
                 String userEmail = email.getString("eamil");
                 System.out.println("Transaction successful. Your updated balance is: " + account.getBalance());
                 sendMail(userEmail, "Transaction Alert", "The amount " + operation + " is: " + amount + ". Your updated balance is: " + account.getBalance());
             } else {
                 System.out.println("No email found for this account.");
             }

         } catch (SQLException e) {
             e.printStackTrace();
         }
     }

    
    private void viewMiniStatement(Account account, Connection connection) {
        String query = "SELECT transaction_type, amount, timestamp FROM mini_statements WHERE account_id = (SELECT id FROM accounts WHERE account_number = ?) ORDER BY timestamp DESC LIMIT 5";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, account.getAccountNumber());
            ResultSet rs = stmt.executeQuery();

            System.out.println("\nMini-Statement:");
            while (rs.next()) {
                String transactionType = rs.getString("transaction_type");
                double amount = rs.getDouble("amount");
                String timestamp = rs.getString("timestamp");

                System.out.println(transactionType + ": " + amount + " at " + timestamp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void sendMail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("mcaprojectatmsystem@gmail.com");
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        jm.send(message);  
    }
}
