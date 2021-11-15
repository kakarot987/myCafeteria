package com.myCafeteria.user_pic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PicRepository extends JpaRepository<PicEntity, Long>{

}

