package lab.router.api;

import lab.router.market.BoardRow;
import lab.router.market.PriceBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {
    private final PriceBoardService priceBoard;

    @GetMapping("/board/{board}")
    public List<BoardRow> board(@PathVariable String board,
                                @RequestParam(defaultValue = "0") int offset,
                                @RequestParam(defaultValue = "30") int limit) {
        return priceBoard.board(board, offset, Math.min(limit, 100));
    }
}
