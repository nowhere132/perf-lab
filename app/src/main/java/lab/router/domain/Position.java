package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "positions")
@IdClass(PositionId.class)
public class Position {
    @Id
    @Column(name = "account_id")
    private Long accountId;
    @Id
    private String symbol;
    private long qty;
    @Column(name = "held_qty")
    private long heldQty;

    protected Position() {}

    public Long getAccountId() { return accountId; }
    public String getSymbol() { return symbol; }
    public long getQty() { return qty; }
    public long getHeldQty() { return heldQty; }
    public long sellable() { return qty - heldQty; }
    public void hold(long n) { heldQty += n; }
    public void release(long n) { heldQty -= n; }
}
