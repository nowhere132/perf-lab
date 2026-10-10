package lab.router.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "order_events")
@Getter
public class OrderEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_events_seq")
    @SequenceGenerator(name = "order_events_seq", sequenceName = "order_events_seq", allocationSize = 50)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    private String type;
    private String detail;
    @Column(name = "created_at")
    private Instant createdAt;

    protected OrderEvent() {}

    public OrderEvent(Order order, String type, String detail) {
        this.order = order;
        this.type = type;
        this.detail = detail;
        this.createdAt = Instant.now();
    }
}
