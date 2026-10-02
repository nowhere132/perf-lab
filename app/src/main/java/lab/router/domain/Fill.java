package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fills")
public class Fill {
    @Id
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    private int qty;
    private BigDecimal price;
    @Column(name = "executed_at")
    private Instant executedAt;

    protected Fill() {}

    public Long getId() { return id; }
    public int getQty() { return qty; }
    public BigDecimal getPrice() { return price; }
    public Instant getExecutedAt() { return executedAt; }
}
