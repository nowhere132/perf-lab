package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trading_limits")
public class TradingLimit {
    @Id
    private String symbol;
    @Column(name = "max_order_qty")
    private int maxOrderQty;
    @Column(name = "foreign_room")
    private long foreignRoom;

    protected TradingLimit() {}

    public String getSymbol() { return symbol; }
    public int getMaxOrderQty() { return maxOrderQty; }
    public long getForeignRoom() { return foreignRoom; }
}
