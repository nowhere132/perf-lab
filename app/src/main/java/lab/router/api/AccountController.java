package lab.router.api;

import lab.router.service.AccountService;
import lab.router.service.OrderHistoryService;
import lab.router.service.OrderView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final OrderHistoryService history;

    @GetMapping("/{id}")
    public AccountService.AccountView account(@PathVariable long id) {
        return accountService.summary(id);
    }

    @GetMapping("/{id}/orders")
    public List<OrderView> orders(@PathVariable long id, @RequestParam(defaultValue = "50") int limit) {
        return history.recent(id, Math.min(limit, 200));
    }
}
