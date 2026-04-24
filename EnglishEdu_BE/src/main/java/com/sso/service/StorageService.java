package com.sso.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg",
            ".pdf", ".doc", ".docx", ".mp3", ".mp4", ".wav");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp");

    public String uploadFile(MultipartFile file, String folder) throws Exception {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (10 MB)");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed: " + extension);
        }

        String objectName = folder + "/" + UUID.randomUUID() + extension;

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .method(Method.GET)
                .expiry(7, TimeUnit.DAYS)
                .build());
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * Upload a course cover image to MinIO. Returns the MinIO object path
     * (e.g. "course-images/uuid.jpg") — NOT a presigned URL.
     * The caller is responsible for storing this path and serving it via the proxy endpoint.
     */
    public String uploadCourseImage(MultipartFile file) throws Exception {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (10 MB)");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed for course image: " + extension);
        }
        String objectName = "course-images/" + UUID.randomUUID() + extension;
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        return objectName;
    }

    /**
     * Fetch raw bytes for a given MinIO object path (e.g. "course-images/uuid.jpg").
     */
    public byte[] getFileBytes(String objectName) throws Exception {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectName).build())) {
            return stream.readAllBytes();
        }
    }
}
