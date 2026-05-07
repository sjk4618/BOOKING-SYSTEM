package booking.server.domain.stock.component;

import booking.server.domain.stock.domain.entity.StockHistoryEntity;
import booking.server.domain.stock.repository.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockHistorySaver {

	private final StockHistoryRepository stockHistoryRepository;

	public StockHistoryEntity save(final StockHistoryEntity stockHistory) {
		return stockHistoryRepository.save(stockHistory);
	}
}
