package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.AccountTransfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.MoneyTransferException;
import com.db.awmd.challenge.exception.NotEnoughBalanceException;
import com.db.awmd.challenge.exception.SameAccountException;
import com.db.awmd.challenge.repository.AccountsRepositoryInMemory;
import com.db.awmd.challenge.service.AccountsServiceImpl;
import com.db.awmd.challenge.service.EmailNotificationService;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @InjectMocks
  private AccountsServiceImpl accountsService;

  @Spy
  private EmailNotificationService notificationService;

  @Spy
  private AccountsRepositoryInMemory accountsRepository;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }

  @Test
  public void transferMoney_ok() throws InterruptedException {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(BigDecimal.valueOf(50))
        .build();

    when(this.accountsService.getAccount("ac1")).thenReturn(accountFrom);
    when(this.accountsService.getAccount("ac2")).thenReturn(accountTo);
    doNothing().when(this.accountsRepository).updateAccount(any());
    doNothing().when(this.notificationService).notifyAboutTransfer(any(), any());

    AccountTransfer accountTransfer = AccountTransfer.builder()
        .accountFromId("ac1")
        .accountToId("ac2")
        .amount(BigDecimal.TEN)
        .build();

    assertTrue(this.accountsService.transferMoney(accountTransfer));
    verify(this.accountsRepository, times(2)).updateAccount(any());
    verify(this.notificationService, times(2)).notifyAboutTransfer(any(), any());
  }

  @Test(expected = SameAccountException.class)
  public void transferMoney_sameAccount() throws InterruptedException {
    AccountTransfer accountTransfer = AccountTransfer.builder()
        .accountFromId("ac1")
        .accountToId("ac1")
        .amount(BigDecimal.TEN)
        .build();

    this.accountsService.transferMoney(accountTransfer);
  }

  @Test(expected = AccountNotFoundException.class)
  public void transferMoney_accountNotFound1() throws InterruptedException {

    when(this.accountsService.getAccount("ac1")).thenReturn(null);

    AccountTransfer accountTransfer = AccountTransfer.builder()
        .accountFromId("ac1")
        .accountToId("ac2")
        .amount(BigDecimal.TEN)
        .build();

    this.accountsService.transferMoney(accountTransfer);
    verify(this.accountsRepository, times(2)).getAccount("ac1");
  }

  @Test(expected = AccountNotFoundException.class)
  public void transferMoney_accountNotFound2() throws InterruptedException {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();

    when(this.accountsService.getAccount("ac1")).thenReturn(accountFrom);
    when(this.accountsService.getAccount("ac2")).thenReturn(null);

    AccountTransfer accountTransfer = AccountTransfer.builder()
        .accountFromId("ac1")
        .accountToId("ac2")
        .amount(BigDecimal.TEN)
        .build();

    this.accountsService.transferMoney(accountTransfer);
    verify(this.accountsRepository, times(2)).getAccount("ac2");
  }

  @Test(expected = NotEnoughBalanceException.class)
  public void transferMoney_notEnoughBalance() throws InterruptedException {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(BigDecimal.valueOf(50))
        .build();

    when(this.accountsService.getAccount("ac1")).thenReturn(accountFrom);
    when(this.accountsService.getAccount("ac2")).thenReturn(accountTo);

    AccountTransfer accountTransfer = AccountTransfer.builder()
        .accountFromId("ac1")
        .accountToId("ac2")
        .amount(BigDecimal.valueOf(50))
        .build();

    this.accountsService.transferMoney(accountTransfer);
  }

  @Test
  public void transferMoney_locked() throws InterruptedException {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();
    accountFrom.getLock().writeLock().lock();
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(BigDecimal.valueOf(50))
        .build();

    when(this.accountsService.getAccount("ac1")).thenReturn(accountFrom);
    when(this.accountsService.getAccount("ac2")).thenReturn(accountTo);
    doNothing().when(this.accountsRepository).updateAccount(any());
    doNothing().when(this.notificationService).notifyAboutTransfer(any(), any());

    AccountTransfer accountTransfer = AccountTransfer.builder()
        .accountFromId("ac1")
        .accountToId("ac2")
        .amount(BigDecimal.TEN)
        .build();

    assertFalse(this.accountsService.transferMoney(accountTransfer));
    verify(this.accountsRepository, never()).updateAccount(any());
    verify(this.notificationService, never()).notifyAboutTransfer(any(), any());
  }

  @Test(expected = MoneyTransferException.class)
  public void transferMoney_withdrawAndDepositIssue() throws InterruptedException {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(null)
        .build();

    when(this.accountsService.getAccount("ac1")).thenReturn(accountFrom);
    when(this.accountsService.getAccount("ac2")).thenReturn(accountTo);

    AccountTransfer accountTransfer = AccountTransfer.builder()
        .accountFromId("ac1")
        .accountToId("ac2")
        .amount(BigDecimal.TEN)
        .build();

    this.accountsService.transferMoney(accountTransfer);
  }
}
