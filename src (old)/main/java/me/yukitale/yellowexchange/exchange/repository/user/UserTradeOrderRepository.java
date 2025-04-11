package me.yukitale.yellowexchange.exchange.repository.user;

import me.yukitale.yellowexchange.exchange.model.user.UserTradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTradeOrderRepository extends JpaRepository<UserTradeOrder, Long> {

    long countByUserIdAndClosedAndTradeType(long userId, boolean closed, UserTradeOrder.TradeType tradeType);

    Optional<UserTradeOrder> findByIdAndUserId(long id, long userId);

    List<UserTradeOrder> findByClosedAndTradeType(boolean closed, UserTradeOrder.TradeType tradeType);

    List<UserTradeOrder> findByUserIdAndClosedAndTradeTypeOrderByCreatedDesc(long userId, boolean closed, UserTradeOrder.TradeType tradeType);

    List<UserTradeOrder> findByUserIdAndClosedOrderByCreatedDesc(long userId, boolean closed);

    boolean existsByUserIdAndClosedAndTradeType(long userId, boolean closed, UserTradeOrder.TradeType tradeType);
}
