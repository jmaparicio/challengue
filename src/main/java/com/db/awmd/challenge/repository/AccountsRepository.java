package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

public interface AccountsRepository {

  void createAccount(Account account) throws DuplicateAccountIdException;

  /**
   * Update an existing account.
   *
   * @param account the account
   * @throws AccountNotFoundException if the account is not found
   */
  void updateAccount(Account account) throws AccountNotFoundException;

  Account getAccount(String accountId);

  void clearAccounts();
}
