package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
public class Account {

  @NotNull
  @NotEmpty
  private final String accountId;

  @NotNull
  @Min(value = 0, message = "Initial balance must be positive.")
  private BigDecimal balance;

  @JsonIgnore
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public Account(String accountId) {
    this.accountId = accountId;
    this.balance = BigDecimal.ZERO;
  }

  @JsonCreator
  public Account(@JsonProperty("accountId") String accountId,
                 @JsonProperty("balance") BigDecimal balance) {
    this.accountId = accountId;
    this.balance = balance;
  }

  public BigDecimal getBalance() {
    lock.readLock().lock();
    BigDecimal balanceGot;
    try {
      balanceGot = this.balance;
    } finally {
      lock.readLock().unlock();
    }
    return balanceGot;
  }

  public boolean hasBalance(BigDecimal amount) {
    return balance.compareTo(amount) >= 0;
  }
}
