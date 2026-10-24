package com.pos.repository;

import com.pos.model.Order;
import com.pos.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * General order access plus the aggregated report queries.
 * <p>
 * All aggregates are computed in SQL against the local SQLite file, so the
 * daily sales report stays fast with no in-memory accumulation by Spring.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findByCustomerNameContainingIgnoreCaseOrderByCreatedAtDesc(
            String customerName);

    /** Orders completed in the requested ISO-8601 day. */
    List<Order> findByCreatedAtBetween(Instant start, Instant end);

    /** All completed orders from the last N days (for the daily summary). */
    @Query("""
            select o from Order o
            where o.status = com.pos.model.OrderStatus.COMPLETED
              and o.createdAt >= :start
            order by o.createdAt desc
            """)
    List<Order> findByCompletedSince(@Param("start") Instant start);

    /** Sum of totals for completed orders in a day. */
    @Query("""
            select coalesce(sum(o.total), 0) from Order o
            where o.status = com.pos.model.OrderStatus.COMPLETED
              and o.createdAt >= :start
              and o.createdAt < :end
            """)
    BigDecimal sumTotalByDay(@Param("start") Instant start, @Param("end") Instant end);

    /** Count of completed orders in a day. */
    @Query("""
            select count(o) from Order o
            where o.status = com.pos.model.OrderStatus.COMPLETED
              and o.createdAt >= :start
              and o.createdAt < :end
            """)
    Long countCompletedByDay(@Param("start") Instant start, @Param("end") Instant end);

    /** Cash collected in a day (cash sales only). */
    @Query("""
            select coalesce(sum(o.cashTendered), 0) from Order o
            where o.status = com.pos.model.OrderStatus.COMPLETED
              and o.paymentMethod = com.pos.model.PaymentMethod.CASH
              and o.createdAt >= :start
              and o.createdAt < :end
            """)
    BigDecimal sumCashByDay(@Param("start") Instant start, @Param("end") Instant end);

    /** Card sales in a day. */
    @Query("""
            select coalesce(sum(o.total), 0) from Order o
            where o.status = com.pos.model.OrderStatus.COMPLETED
              and o.paymentMethod = com.pos.model.PaymentMethod.CARD
              and o.createdAt >= :start
              and o.createdAt < :end
            """)
    BigDecimal sumCardByDay(@Param("start") Instant start, @Param("end") Instant end);

    /** Count of cash vs card transactions for the day. */
    @Query("""
            select coalesce(sum(case when o.paymentMethod = com.pos.model.PaymentMethod
.CASH then 1 else 0 end), 0)
            from Order o
            where o.status = com.pos.model.OrderStatus.COMPLETED
              and o.createdAt >= :start
              and o.createdAt < :end
            """)
    Long countCashByDay(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            select count(o) from Order o
            where o.status = com.pos.model.OrderStatus.COMPLETED
              and o.paymentMethod = com.pos.model.PaymentMethod.CARD
              and o.createdAt >= :start
              and o.createdAt < :end
            """)
    Long countCardByDay(@Param("start") Instant start, @Param("end") Instant end);

    /** Live count of completed orders for the summary header. */
    @Query("""
            select count(o) from Order o
            where o.status = com.pos.model.OrderStatus.COMPLETED
            """)
    long countCompleted();
}
