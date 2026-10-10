package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "positions")
@IdClass(PositionId.class)
@Getter
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

    public long sellable() { return qty - heldQty; }
    public void hold(long n) { heldQty += n; }
    public void release(long n) { heldQty -= n; }
}
