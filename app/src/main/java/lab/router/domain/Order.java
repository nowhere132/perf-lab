package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 50)
    private Long id;
    @Column(name = "account_id")
    private Long accountId;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private Side side;
    private int qty;
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String venue;
    @Column(name = "venue_order_id")
    private String venueOrderId;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Fill> fills = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderEvent> events = new ArrayList<>();

    protected Order() {}

    public Order(Long accountId, String symbol, Side side, int qty, BigDecimal price, String venue) {
        this.accountId = accountId;
        this.symbol = symbol;
        this.side = side;
        this.qty = qty;
        this.price = price;
        this.venue = venue;
        this.status = OrderStatus.NEW;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void accepted(String venueOrderId) {
        this.status = OrderStatus.ACCEPTED;
        this.venueOrderId = venueOrderId;
        this.updatedAt = Instant.now();
    }

    public void rejected() {
        this.status = OrderStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public int getQty() { return qty; }
    public BigDecimal getPrice() { return price; }
    public OrderStatus getStatus() { return status; }
    public String getVenue() { return venue; }
    public String getVenueOrderId() { return venueOrderId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Fill> getFills() { return fills; }
    public List<OrderEvent> getEvents() { return events; }
}
