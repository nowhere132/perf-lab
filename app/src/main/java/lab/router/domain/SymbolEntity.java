package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "symbols")
@Getter
public class SymbolEntity {
    @Id
    private String symbol;
    private String board;
    @Column(name = "ref_price")
    private BigDecimal refPrice;
    @Column(name = "band_pct")
    private BigDecimal bandPct;
    @Column(name = "tick_size")
    private BigDecimal tickSize;
    @Column(name = "lot_size")
    private int lotSize;

    protected SymbolEntity() {}
}
