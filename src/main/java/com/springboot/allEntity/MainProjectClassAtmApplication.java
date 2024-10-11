package com.springboot.allEntity;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class MainProjectClassAtmApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MainProjectClassAtmApplication.class, args);

        ATM atm = context.getBean(ATM.class);

        try {
            Connection connection = atm.getConnection();
            Account account = atm.authenticateUser(connection);
            if (account != null) {
                atm.showMenu(account, connection);
            } else {
                System.out.println("Authentication failed. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
