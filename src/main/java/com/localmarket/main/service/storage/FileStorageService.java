package com.localmarket.main.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String DEFAULT_UPLOAD_DIR = "uploads/products";
    private final Path uploadPath;
    private final Cloudinary cloudinary;

    public FileStorageService(
            @Value("${app.upload.dir:#{null}}") String configuredUploadDir,
            Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
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
            
            // Try to upload to Cloudinary first
            try {
                Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
                // Return Cloudinary URL directly without prefix
                return uploadResult.get("url").toString();
            } catch (Exception e) {
                // If Cloudinary upload fails, fall back to local storage
                String filename = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());
                Path targetLocation = this.uploadPath.resolve(filename);
                Files.copy(file.getInputStream(), targetLocation);
                // Add prefix only for local storage
                return "/api/products/images/" + filename;
            }
        } catch (IOException ex) {
            throw new ApiException(ErrorType.FILE_STORAGE_ERROR, "Could not store file");
        }
    }

    public Resource loadFileAsResource(String filename) {
        try {
            // If it's a full URL (Cloudinary), return it directly
            if (filename.startsWith("http")) {
                return new UrlResource(filename);
            }
            
            // For local files, resolve the path
            Path filePath = this.uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            }
            
            throw new ApiException(ErrorType.FILE_NOT_FOUND, "File not found");
        } catch (IOException ex) {
            throw new ApiException(ErrorType.FILE_NOT_FOUND, "File not found");
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