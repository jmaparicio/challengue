package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.AccountTransfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.NotEnoughBalanceException;
import com.db.awmd.challenge.exception.SameAccountException;

/**
 * Account service.
 */
public interface AccountsService {

  /**
   * Create an account and store it in database.
   *
   * @param account the account
   */
  void createAccount(final Account account);

  /**
   * Get an account from database.
   *
   * @param accountId the account id
   * @return the desired account
   */
  Account getAccount(final String accountId);

  /**
   * Transfer money amount between accounts a save it in database.
   *
   * @param accountTransfer account transfer info
   * @return true if the transfer was completed
   * @throws SameAccountException      you tried to transfer money to the same account
   * @throws AccountNotFoundException  one of the accounts is not found
   * @throws NotEnoughBalanceException account does not have enough money to transfer that amount
   */
  boolean transferMoney(final AccountTransfer accountTransfer)
      throws SameAccountException, AccountNotFoundException, NotEnoughBalanceException,
      InterruptedException;
}
