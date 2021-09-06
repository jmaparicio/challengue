package com.db.awmd.challenge.exception;

public class SameAccountException extends RuntimeException {

  public SameAccountException(String message) {
    super(message);
  }
}
