package com.myCafeteria.foodMenu;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<FoodEntity, String>{

}
