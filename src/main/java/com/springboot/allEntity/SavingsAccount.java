package com.springboot.allEntity;

public class SavingsAccount extends Account{

	public SavingsAccount(String accountNumber, int pin, double balance) {
        super(accountNumber, pin, balance);
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}
