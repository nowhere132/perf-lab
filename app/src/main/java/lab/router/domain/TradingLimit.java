package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "trading_limits")
@Getter
public class TradingLimit {
    @Id
    private String symbol;
    @Column(name = "max_order_qty")
    private int maxOrderQty;
    @Column(name = "foreign_room")
    private long foreignRoom;

    protected TradingLimit() {}
}
