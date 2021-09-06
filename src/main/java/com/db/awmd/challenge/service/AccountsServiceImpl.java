package com.db.awmd.challenge.service;

import static java.lang.String.format;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.AccountTransfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.MoneyTransferException;
import com.db.awmd.challenge.exception.NotEnoughBalanceException;
import com.db.awmd.challenge.exception.SameAccountException;
import com.db.awmd.challenge.repository.AccountsRepository;
import java.math.BigDecimal;
import java.util.Random;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class AccountsServiceImpl implements AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  @Getter
  private final NotificationService notificationService;

  private final Random random;

  @Autowired
  public AccountsServiceImpl(AccountsRepository accountsRepository,
                             NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
    this.random = new Random();
  }

  @Override
  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  @Override
  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  private void deposit(Account account, BigDecimal amount) {
    account.getLock().writeLock().lock();
    try {
      account.setBalance(account.getBalance().add(amount));
    } catch (Exception e) {
      throw new MoneyTransferException(e.getMessage());
    } finally {
      account.getLock().writeLock().unlock();
    }
  }

  private void withdraw(Account account, BigDecimal amount) {
    deposit(account, amount.negate());
  }

  @Override
  public boolean transferMoney(AccountTransfer accountTransfer)
      throws SameAccountException, AccountNotFoundException, NotEnoughBalanceException,
      InterruptedException {
    if (accountTransfer.getAccountFromId().equals(accountTransfer.getAccountToId())) {
      throw new SameAccountException("You can transfer money to the same account");
    }

    Account accountFrom = this.accountsRepository.getAccount(accountTransfer.getAccountFromId());
    if (accountFrom == null) {
      throw new AccountNotFoundException(
          format("Account with id %s not found", accountTransfer.getAccountFromId()));
    }
    Account accountTo = this.accountsRepository.getAccount(accountTransfer.getAccountToId());
    if (accountTo == null) {
      throw new AccountNotFoundException(
          format("Account with id %s not found", accountTransfer.getAccountToId()));
    }

    // The transfer is only allowed if the first account has enough amount in balance
    if (accountFrom.hasBalance(accountTransfer.getAmount())) {
      for (int lockRetries = 0; lockRetries < 10; lockRetries++) {
        if (!accountFrom.getLock().isWriteLocked() && !accountTo.getLock().isWriteLocked()) {
          withdraw(accountFrom, accountTransfer.getAmount());
          deposit(accountTo, accountTransfer.getAmount());

          this.accountsRepository.updateAccount(accountFrom);
          this.notificationService.notifyAboutTransfer(accountFrom,
              format("Your account have transferred %s to account id %s",
                  accountTransfer.getAmount().toString(), accountTo.getAccountId()));

          this.accountsRepository.updateAccount(accountTo);
          this.notificationService.notifyAboutTransfer(accountTo,
              format("Your account have received %s from account id %s",
                  accountTransfer.getAmount().toString(), accountFrom.getAccountId()));

          return true;
        } else {
          log.info("Waiting for both accounts to be unlock: {}, {} (Retry {}/10)",
              accountFrom.getAccountId(), accountTo.getAccountId(), lockRetries);
          Thread.sleep(random.nextInt(1001));
        }
      }
      log.info("Operation cancelled due to lock time, accounts: {}, {}",
          accountFrom.getAccountId(), accountTo.getAccountId());
      return false;
    } else {
      throw new NotEnoughBalanceException(
          format("Account with id %s doesn't have the desired amount to transfer",
              accountFrom.getAccountId()));
    }
  }
}
