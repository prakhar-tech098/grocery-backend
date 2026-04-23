package com.grocery.repository;

import com.grocery.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // ─── Daily sales totals (last N days) ─────────────────────────────────────
    @Query("""
        SELECT CAST(o.createdAt AS date) AS saleDate,
               SUM(oi.price * oi.quantity) AS revenue,
               SUM(oi.quantity) AS unitsSold
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status = 'COMPLETED'
          AND o.createdAt >= :since
        GROUP BY CAST(o.createdAt AS date)
        ORDER BY CAST(o.createdAt AS date) ASC
    """)
    List<Object[]> findDailySales(@Param("since") LocalDateTime since);

    // ─── Top products by revenue ───────────────────────────────────────────────
    @Query("""
        SELECT oi.product.id,
               oi.product.name,
               SUM(oi.price * oi.quantity) AS revenue,
               SUM(oi.quantity)            AS unitsSold
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status = 'COMPLETED'
          AND o.createdAt >= :since
        GROUP BY oi.product.id, oi.product.name
        ORDER BY revenue DESC
    """)
    List<Object[]> findTopProductsByRevenue(@Param("since") LocalDateTime since);

    // ─── Sales by category ────────────────────────────────────────────────────
    @Query("""
        SELECT oi.product.category,
               SUM(oi.price * oi.quantity) AS revenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status = 'COMPLETED'
          AND o.createdAt >= :since
          AND oi.product.category IS NOT NULL
        GROUP BY oi.product.category
        ORDER BY revenue DESC
    """)
    List<Object[]> findSalesByCategory(@Param("since") LocalDateTime since);

    // ─── Units sold per product in last N days (for demand prediction) ────────
    @Query("""
        SELECT oi.product.id,
               SUM(oi.quantity) AS totalSold
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status = 'COMPLETED'
          AND o.createdAt >= :since
        GROUP BY oi.product.id
    """)
    List<Object[]> findUnitsSoldPerProduct(@Param("since") LocalDateTime since);

    // ─── Market basket: co-purchase counts ────────────────────────────────────
    // Find all pairs of products that appear in the same order
    @Query("""
        SELECT oi1.product.id,
               oi2.product.id,
               COUNT(oi1.order.id) AS coCount
        FROM OrderItem oi1
        JOIN OrderItem oi2
          ON oi1.order.id = oi2.order.id
         AND oi1.product.id < oi2.product.id
        JOIN oi1.order o
        WHERE o.status = 'COMPLETED'
        GROUP BY oi1.product.id, oi2.product.id
        ORDER BY coCount DESC
    """)
    List<Object[]> findCoPurchasePairs();

    // ─── Co-purchases for a specific product ──────────────────────────────────
    @Query("""
        SELECT oi2.product.id,
               oi2.product.name,
               oi2.product.price,
               COUNT(oi1.order.id) AS coCount
        FROM OrderItem oi1
        JOIN OrderItem oi2
          ON oi1.order.id = oi2.order.id
         AND oi1.product.id <> oi2.product.id
        JOIN oi1.order o
        WHERE o.status = 'COMPLETED'
          AND oi1.product.id = :productId
        GROUP BY oi2.product.id, oi2.product.name, oi2.product.price
        ORDER BY coCount DESC
    """)
    List<Object[]> findFrequentlyBoughtWith(@Param("productId") Long productId);
}
