package lab.router.market;

public record BoardRow(String symbol, String ref, String ceiling, String floor,
                       String last, String high, String low, String vwap, long volume, String changePct) {
}
