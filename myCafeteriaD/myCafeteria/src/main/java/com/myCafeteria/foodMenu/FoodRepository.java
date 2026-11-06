package com.myCafeteria.foodMenu;

import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodRepository extends JpaRepository<FoodEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FoodEntity f where f.id = :id")
    Optional<FoodEntity> findForUpdate(@Param("id") String id);
}
