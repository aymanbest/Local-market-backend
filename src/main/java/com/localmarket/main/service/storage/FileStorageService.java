package com.localmarket.main.service.storage;

import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String DEFAULT_UPLOAD_DIR = "uploads/products";
    private final Path uploadPath;

    public FileStorageService(@Value("${app.upload.dir:#{null}}") String configuredUploadDir) {
        String uploadDir = (configuredUploadDir != null) ? configuredUploadDir : DEFAULT_UPLOAD_DIR;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException ex) {
            throw new ApiException(ErrorType.FILE_STORAGE_ERROR, 
                "Could not create upload directory: " + this.uploadPath);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            validateFile(file);
            String filename = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());
            Path targetLocation = this.uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation);
            return filename;
        } catch (IOException ex) {
            throw new ApiException(ErrorType.FILE_STORAGE_ERROR, "Could not store file");
        }
    }

    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new ApiException(ErrorType.INVALID_FILE, "File is empty");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5_000_000) {
            throw new ApiException(ErrorType.INVALID_FILE, "File size exceeds maximum limit of 5MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ApiException(ErrorType.INVALID_FILE, "Only image files are allowed");
        }
    }

    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }

    public Path getUploadPath() {
        return this.uploadPath;
    }
} 