package com.example.cloud_service.controller;

import com.example.cloud_service.model.FileDTO;
import com.example.cloud_service.model.EditFileRequest;
import com.example.cloud_service.security.ApplicationUser;
import com.example.cloud_service.service.FileServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/file")
@AllArgsConstructor
public class FileController {

    private final FileServiceImpl fileService;

    @PostMapping("/upload")
    public void uploadFile(
            @RequestParam("hash") String hash,
            @RequestParam("file") MultipartFile file,
            @RequestParam("filename") String fileName,
            @AuthenticationPrincipal ApplicationUser user) {
        fileService.saveFile(hash, file, fileName, user);
    }

    @DeleteMapping("/delete")
    public void deleteFile(@RequestParam("filename") String fileName,
                           @AuthenticationPrincipal ApplicationUser user) throws FileNotFoundException {
        fileService.deleteFile(fileName, user);
    }

    @GetMapping("/download")
    public Resource downloadFile(@RequestParam("filename") String fileName,
                                 @AuthenticationPrincipal ApplicationUser user) throws IOException {
        return fileService.downloadFile(fileName, user);
    }

    @PutMapping("/edit")
    public EditFileRequest editFileName(@RequestParam("filename") String fileName,
                                        @RequestBody EditFileRequest fileRequest,
                                        @AuthenticationPrincipal ApplicationUser user) throws IOException {
        return fileService.editFileName(fileName, fileRequest, user);

    }

    @GetMapping("/list")
    public ResponseEntity<List<FileDTO>> getAllFiles(@RequestParam(defaultValue = "5") Integer limit,
                                                     @AuthenticationPrincipal ApplicationUser user) {
        List<FileDTO> files = fileService.getAllFiles(limit, user);
        return ResponseEntity.ok(files);

    }


}
