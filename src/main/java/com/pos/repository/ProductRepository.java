package com.pos.repository;

import com.pos.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Reads product stock off the local SQLite file. Simple, fast and
 * concurrency-light; SQLite's WAL mode is not required for a single
 * standing terminal that mostly reads.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Look up by barcode (the primary scanner lookup key).
     */
    Optional<Product> findByBarcode(String barcode);

    /**
     * Active products only, filtered by category when supplied.
     */
    List<Product> findByActiveTrueAndCategoryIgnoreCaseOrderByCategoryAscNameAsc(
            String category);

    /** All active products (used to pre-load the search dropdown). */
    @Query("""
            select p from Product p
            where p.active = true
            order by p.category asc, p.name asc
            """)
    List<Product> findActiveOrdered();

    /** Exact category + name prefix match (case-insensitive). */
    @Query("""
            select p from Product p
            where p.active = true
              and lower(p.category) = lower(:category)
              and lower(p.name) like lower(concat(:q, '%'))
            order by p.name asc
            """)
    List<Product> searchByCategoryAndNameLike(
            @Param("category") String category,
            @Param("q") String q);

    /** All categories currently in stock (case-preserving, de-duped). */
    @Query("""
            select distinct p.category from Product p
            where p.active = true
            order by p.category asc
            """)
    List<String> findDistinctCategories();

    /** Search across barcode + name + category (used by the POS search bar). */
    @Query("""
            select p from Product p
            where p.active = true
              and (lower(p.barcode) like lower(concat('%', :q, '%'))
              or lower(p.name) like lower(concat('%', :q, '%'))
              or lower(p.category) like lower(concat('%', :q, '%')))
            order by p.name asc
            """)
    List<Product> searchBy(@Param("q") String q);

    long countByActiveTrueAndStockLessThan(@Param("threshold") Integer threshold);

    /** Active products at or below their own low-stock threshold. */
    @Query("select count(p) from Product p where p.active = true and p.stock <= p.lowStockThreshold")
    long countLowStock();
}
