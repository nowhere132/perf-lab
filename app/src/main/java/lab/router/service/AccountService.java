package lab.router.service;

import lab.router.domain.Account;
import lab.router.domain.Position;
import lab.router.repo.AccountRepository;
import lab.router.repo.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    public record PositionView(String symbol, long qty, long heldQty) {}

    public record AccountView(long id, String accountNo, BigDecimal cash, BigDecimal held, BigDecimal buyingPower,
                              List<PositionView> positions) {}

    private final AccountRepository accounts;
    private final PositionRepository positions;

    @Transactional(readOnly = true)
    public AccountView summary(long accountId) {
        Account a = accounts.findById(accountId)
                .orElseThrow(() -> new OrderRejectedException("unknown account " + accountId));
        List<PositionView> held = positions.findByAccountId(accountId).stream()
                .map(p -> new PositionView(p.getSymbol(), p.getQty(), p.getHeldQty()))
                .toList();
        return new AccountView(a.getId(), a.getAccountNo(), a.getCash(), a.getHeld(), a.buyingPower(), held);
    }
}
