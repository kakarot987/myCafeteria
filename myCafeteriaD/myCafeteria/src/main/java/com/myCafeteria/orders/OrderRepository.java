package com.myCafeteria.orders;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface OrderRepository extends JpaRepository<CafeteriaOrder,Long> {
 Optional<CafeteriaOrder> findByUserIdAndIdempotencyKey(Long userId,String key);
 List<CafeteriaOrder> findByUserIdOrderByIdDesc(Long userId);
 List<CafeteriaOrder> findAllByOrderByIdDesc();

 /** Include orders placed before the period but completed within it. */
 @Query("select distinct o from CafeteriaOrder o left join fetch o.items " +
        "where o.createdAt >= :start or o.completedAt >= :start")
 List<CafeteriaOrder> findAnalyticsSince(@Param("start") java.time.LocalDateTime start);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select o from CafeteriaOrder o where o.id=:id")
 Optional<CafeteriaOrder> findForUpdate(@Param("id") Long id);
}
