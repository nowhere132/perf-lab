package lab.router.api;

import lab.router.domain.Order;
import lab.router.domain.Side;
import lab.router.service.OrderService;
import lab.router.service.PlaceOrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    public record PlaceOrderRequest(long accountId, String symbol, Side side, int qty, BigDecimal price) {}

    public record PlaceOrderResponse(long orderId, String status, String venue, String venueOrderId) {}

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceOrderResponse place(@RequestBody PlaceOrderRequest request) {
        Order order = orderService.place(new PlaceOrderCommand(
                request.accountId(), request.symbol(), request.side(), request.qty(), request.price()));
        return new PlaceOrderResponse(order.getId(), order.getStatus().name(), order.getVenue(), order.getVenueOrderId());
    }
}
