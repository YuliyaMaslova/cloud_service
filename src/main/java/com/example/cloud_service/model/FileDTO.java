package com.example.cloud_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileDTO {
    private Long id;
    private String fileName;
    private Long size;
    private LocalDateTime uploadedAt;

}
