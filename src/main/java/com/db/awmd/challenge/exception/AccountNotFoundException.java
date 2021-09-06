package com.db.awmd.challenge.exception;

public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(String accountId) {
    super(String.format("Account id %s not found!", accountId));
  }
}
