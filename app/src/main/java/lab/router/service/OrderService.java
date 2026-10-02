package lab.router.service;

import lab.router.domain.Account;
import lab.router.domain.Order;
import lab.router.domain.OrderEvent;
import lab.router.domain.Position;
import lab.router.domain.Side;
import lab.router.gateway.RiskClient;
import lab.router.gateway.VenueGateway;
import lab.router.market.ReferenceData;
import lab.router.market.SymbolRef;
import lab.router.repo.AccountRepository;
import lab.router.repo.OrderEventRepository;
import lab.router.repo.OrderRepository;
import lab.router.repo.PositionRepository;
import lab.router.risk.ExposureBook;
import lab.router.risk.RoutingStats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {
    private static final BigDecimal EXPOSURE_LIMIT_PER_TIER = new BigDecimal("50000000000");

    private final ReferenceData referenceData;
    private final OrderValidator validator;
    private final AccountRepository accounts;
    private final PositionRepository positions;
    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final RiskClient riskClient;
    private final VenueGateway venueGateway;
    private final ExposureBook exposureBook;
    private final RoutingStats routingStats;

    public OrderService(ReferenceData referenceData, OrderValidator validator, AccountRepository accounts,
                        PositionRepository positions, OrderRepository orders, OrderEventRepository events,
                        RiskClient riskClient, VenueGateway venueGateway, ExposureBook exposureBook,
                        RoutingStats routingStats) {
        this.referenceData = referenceData;
        this.validator = validator;
        this.accounts = accounts;
        this.positions = positions;
        this.orders = orders;
        this.events = events;
        this.riskClient = riskClient;
        this.venueGateway = venueGateway;
        this.exposureBook = exposureBook;
        this.routingStats = routingStats;
    }

    /**
     * Validate, reserve cash (buy) or stock (sell), clear pre-trade risk, send
     * to the venue and record the outcome. Any failure rolls the whole thing
     * back, so a reservation can never outlive an order that did not go out.
     */
    @Transactional
    public Order place(PlaceOrderCommand cmd) {
        SymbolRef symbol = referenceData.find(cmd.symbol());
        if (symbol == null) throw new OrderRejectedException("unknown symbol " + cmd.symbol());
        validator.validate(cmd, symbol);

        BigDecimal notional = cmd.price().multiply(BigDecimal.valueOf(cmd.qty()));
        Account account = accounts.lockById(cmd.accountId())
                .orElseThrow(() -> new OrderRejectedException("unknown account " + cmd.accountId()));
        if (!account.isActive()) throw new OrderRejectedException("account is not active");

        if (cmd.side() == Side.BUY) {
            if (account.buyingPower().compareTo(notional) < 0)
                throw new OrderRejectedException("insufficient buying power");
            account.hold(notional);
        } else {
            Position position = positions.lock(cmd.accountId(), cmd.symbol())
                    .orElseThrow(() -> new OrderRejectedException("no position in " + cmd.symbol()));
            if (position.sellable() < cmd.qty()) throw new OrderRejectedException("insufficient sellable quantity");
            position.hold(cmd.qty());
        }

        BigDecimal limit = EXPOSURE_LIMIT_PER_TIER.multiply(BigDecimal.valueOf(account.getTier() + 1));
        if (!exposureBook.tryAdd(account.getId(), notional, limit))
            throw new OrderRejectedException("intraday exposure limit reached");
        if (!riskClient.approve(cmd.accountId(), cmd.symbol(), cmd.side().name(), cmd.qty(), cmd.price()))
            throw new OrderRejectedException("rejected by pre-trade risk");

        Order order = orders.save(new Order(cmd.accountId(), cmd.symbol(), cmd.side(), cmd.qty(), cmd.price(), symbol.board()));
        String venueOrderId = venueGateway.send(order);
        order.accepted(venueOrderId);
        events.save(new OrderEvent(order, "ACCEPTED", "venue=" + order.getVenue() + " id=" + venueOrderId));

        routingStats.record(RoutingStats.boardIndex(symbol.board()), notional);
        return order;
    }
}
