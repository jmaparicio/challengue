package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.setRemoveAssertJRelatedElementsFromStackTrace;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.AccountTransfer;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.AccountsServiceImpl;
import com.db.awmd.challenge.web.AccountsController;
import java.math.BigDecimal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  AccountsController accountsController;

  @SpyBean
  private AccountsServiceImpl accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void transferMoneyBetweenAccountsOk() throws Exception {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(BigDecimal.valueOf(50))
        .build();
    accountsService.createAccount(accountFrom);
    accountsService.createAccount(accountTo);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountFromId\":\"ac1\",\"accountToId\":\"ac2\",\"amount\":30}"))
        .andExpect(status().isOk());
  }

  @Test
  public void transferMoneyBetweenAccountsNotEnoughBalance() throws Exception {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(BigDecimal.valueOf(50))
        .build();
    accountsService.createAccount(accountFrom);
    accountsService.createAccount(accountTo);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"ac1\",\"accountToId\":\"ac2\",\"amount\":31}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void transferMoneyBetweenAccountsAccountFromNotFound() throws Exception {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();
    accountsService.createAccount(accountFrom);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"ac1\",\"accountToId\":\"ac2\",\"amount\":30}"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void transferMoneyBetweenAccountsAccountToNotFound() throws Exception {
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(BigDecimal.valueOf(50))
        .build();
    accountsService.createAccount(accountTo);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"ac1\",\"accountToId\":\"ac2\",\"amount\":30}"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void transferMoneyBetweenAccountsSameAccount() throws Exception {
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(BigDecimal.valueOf(50))
        .build();
    accountsService.createAccount(accountTo);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"ac2\",\"accountToId\":\"ac2\",\"amount\":30}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void transferMoneyBetweenAccountsThreadInterrupt() throws Exception {
    Account accountFrom = Account.builder()
        .accountId("ac1")
        .balance(BigDecimal.valueOf(30))
        .build();
    Account accountTo = Account.builder()
        .accountId("ac2")
        .balance(BigDecimal.valueOf(50))
        .build();
    accountsService.createAccount(accountFrom);
    accountsService.createAccount(accountTo);

    AccountTransfer accountTransfer = AccountTransfer.builder()
        .accountFromId("ac1")
        .accountToId("ac2")
        .amount(BigDecimal.TEN)
        .build();

    doThrow(new InterruptedException()).when(accountsService).transferMoney(any());

    ResponseEntity<Object> response =
        this.accountsController.transferMoneyBetweenAccounts(accountTransfer);
    assertEquals(HttpStatus.LOCKED, response.getStatusCode());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }
}
