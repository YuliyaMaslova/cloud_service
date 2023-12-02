package com.example.cloud_service.repository;

import com.example.cloud_service.entity.FileEntity;
import com.example.cloud_service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Integer> {

    Optional<FileEntity> findByFileName(String fileName);

    Page<FileEntity> findByUser(UserEntity user, Pageable pageable);
}
