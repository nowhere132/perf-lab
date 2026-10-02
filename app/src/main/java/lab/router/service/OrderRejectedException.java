package lab.router.service;

public class OrderRejectedException extends RuntimeException {
    public OrderRejectedException(String reason) { super(reason); }
}
