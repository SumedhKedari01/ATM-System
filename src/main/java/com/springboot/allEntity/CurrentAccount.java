package com.springboot.allEntity;

public class CurrentAccount extends Account {

	  public CurrentAccount(String accountNumber, int pin, double balance) {
	        super(accountNumber, pin, balance);
	    }
	    @Override
	    public String getAccountType() {
	        return "Current";
	    }
}
