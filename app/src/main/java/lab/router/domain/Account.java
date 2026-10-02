package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private Long id;
    @Column(name = "account_no")
    private String accountNo;
    private String status;
    private short tier;
    private BigDecimal cash;
    private BigDecimal held;
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Account() {}

    public Long getId() { return id; }
    public String getAccountNo() { return accountNo; }
    public short getTier() { return tier; }
    public BigDecimal getCash() { return cash; }
    public BigDecimal getHeld() { return held; }
    public boolean isActive() { return "ACTIVE".equals(status); }
    public BigDecimal buyingPower() { return cash.subtract(held); }

    public void hold(BigDecimal amount) {
        held = held.add(amount);
        updatedAt = Instant.now();
    }

    public void release(BigDecimal amount) {
        held = held.subtract(amount);
        updatedAt = Instant.now();
    }
}
