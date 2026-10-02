package lab.router.domain;

import java.io.Serializable;

public record PositionId(Long accountId, String symbol) implements Serializable {}
