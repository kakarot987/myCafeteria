package com.myCafeteria.File.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.myCafeteria.File.Entity.FileDB;

@Repository
public interface FileDBRepository extends JpaRepository<FileDB, String> {

}

