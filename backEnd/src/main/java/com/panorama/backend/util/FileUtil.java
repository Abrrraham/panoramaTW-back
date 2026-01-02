package com.panorama.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-24 14:45:17
 * @version: 1.0
 */
@Slf4j
public class FileUtil {

    /**
     * 确保目录存在，如果不存在则创建
     * @param dirPath 目录路径
     * @throws IOException 如果创建失败
     */
    public static synchronized void ensureDirectoryExists(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            // 双重检查，防止并发情况下重复创建
            synchronized (FileUtil.class) {
                if (!dir.exists()) {
                    try {
                        boolean created = dir.mkdirs();
                        if (!created && !dir.exists()) {
                            throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
                        }
                        if (created) {
                            log.info("Created directory: {}", dir.getAbsolutePath());
                        }
                    } catch (SecurityException e) {
                        throw new IOException("Permission denied when creating directory: " + dir.getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    public static String sanitizeFileName(String originalFilename) {
        if (originalFilename == null) {
            return "upload.bin";
        }
        String name = originalFilename.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank()) {
            return "upload.bin";
        }
        return name;
    }

    public static String sanitizeTableName(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        if (cleaned.isBlank()) {
            return "";
        }
        return cleaned;
    }

    public static boolean hasAllowedExtension(String filename, String[] allowed) {
        if (filename == null || allowed == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        for (String ext : allowed) {
            if (lower.endsWith("." + ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static synchronized File convertMultipartFileToFile(MultipartFile multipartFile, String temp) throws IOException {

        String originalFilename = multipartFile.getOriginalFilename();
        String safeFileName = sanitizeFileName(originalFilename);
        
        // 首先确保基础temp目录存在
        ensureDirectoryExists(temp);
        
        // 生成安全的目录名，避免中文字符问题和并发冲突
        String baseName;
        int dotIndex = safeFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = safeFileName.substring(0, dotIndex);
        } else {
            baseName = safeFileName;
        }
        // 使用时间戳+UUID+线程ID确保绝对唯一性
        String uniqueId = System.currentTimeMillis() + "_" + 
                         Thread.currentThread().getId() + "_" + 
                         java.util.UUID.randomUUID().toString().replace("-", "");
        String safeDirName = baseName + "_" + uniqueId;
        
        String path = String.join(
                File.separator,
                temp,
                safeDirName
        );

        log.info("Creating directory for file: {} -> {}", originalFilename, path);
        
        // 确保子目录存在，多次重试机制
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ensureDirectoryExists(path);
                break;
            } catch (IOException e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                log.warn("Directory creation failed, retrying... attempt {}/{}", i + 1, maxRetries);
                try {
                    Thread.sleep(10); // 短暂等待后重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while creating directory", ie);
                }
            }
        }

        File file = new File(path + File.separator + safeFileName);

        // 将 multipartFile 的内容转存到 file 中
        try {
            multipartFile.transferTo(file);
            log.info("Successfully created file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to transfer file to: {}", file.getAbsolutePath(), e);
            // 清理失败的文件和目录
            try {
                if (file.exists()) {
                    file.delete();
                }
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory() && dir.list().length == 0) {
                    dir.delete();
                }
            } catch (Exception cleanupException) {
                log.warn("Failed to cleanup after error: {}", cleanupException.getMessage());
            }
            throw e;
        }

        return file;
    }

    public static List<String> unZipFiles(File srcFile, String destDirPath) throws RuntimeException {
        List<String> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        if (!srcFile.exists()) {
            throw new RuntimeException(srcFile.getPath() + " file not found");
        }
        ZipFile zipFile = null;
        try {
            Path destDir = Paths.get(destDirPath).toAbsolutePath().normalize();
            zipFile = new ZipFile(srcFile, Charset.forName("GBK"));
            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                Path targetPath = destDir.resolve(entry.getName()).normalize();
                if (!targetPath.startsWith(destDir)) {
                    throw new RuntimeException("zip slip detected: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                    continue;
                }
                if (targetPath.getParent() != null) {
                    Files.createDirectories(targetPath.getParent());
                }
                try (InputStream is = zipFile.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                }
                list.add(targetPath.toString());
            }
            long end = System.currentTimeMillis();
            log.info("unzip complete, cost: {} ms", end - start);
        } catch (Exception e) {
            throw new RuntimeException("unzip error", e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    log.warn("zip close failed: {}", e.getMessage());
                }
            }
        }
        return list;
    }

    public static String findFileWithExtension(List<String> list, String extension){
        String fileName = "";
        for (String file : list) {
            if (file.endsWith(extension)) {
                fileName = file;
                break;
            }
        }
        return fileName;
    }

    public static String findFileWithExtensionIgnoreCase(List<String> list, String... extensions) {
        if (list == null || extensions == null) {
            return "";
        }
        for (String file : list) {
            String lower = file.toLowerCase();
            for (String ext : extensions) {
                if (lower.endsWith(ext.toLowerCase())) {
                    return file;
                }
            }
        }
        return "";
    }

    public static String findFileWithExtension(String directoryPath, String extension) {
        Path dirPath = Paths.get(directoryPath);

        try {
            // 使用 Files.walk() 遍历目录中的所有文件
            return Files.walk(dirPath, 1)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(extension))
                    .map(Path::toString) // 将 Path 转换为 String
                    .findFirst() // 找到第一个匹配的文件
                    .orElse(null); // 如果没有找到，则返回 null
        } catch (IOException e) {
            System.err.println("访问目录时出错: " + e.getMessage());
            return null;
        }
    }

    public static String findFileWithExtensionIgnoreCase(String directoryPath, String... extensions) {
        Path dirPath = Paths.get(directoryPath);
        try {
            return Files.walk(dirPath, 1)
                    .filter(path -> Files.isRegularFile(path))
                    .map(Path::toString)
                    .filter(path -> {
                        String lower = path.toLowerCase();
                        for (String ext : extensions) {
                            if (lower.endsWith(ext.toLowerCase())) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            System.err.println("Failed to access directory: " + e.getMessage());
            return null;
        }
    }

    public static void deleteFilesInDirectory(String directoryPath) throws IOException {
        Path directory = Paths.get(directoryPath);

        // 检查目录是否存在
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file); // 删除文件
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE; // 保留目录本身
                }
            });
        } else {
            throw new IllegalArgumentException("目录不存在或路径不是目录：" + directoryPath);
        }
    }

    public static void deleteDirectory(Path path) {
        try {
            // 检查路径是否是目录
            if (Files.isDirectory(path)) {
                // 遍历目录中的所有文件和子目录
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path entry : stream) {
                        deleteDirectory(entry); // 递归删除
                    }
                }
            }
            // 删除空目录或文件
            Files.delete(path);
            log.info("删除成功: {}", path);
        } catch (IOException e) {
            log.info("删除失败: {} - {}", path, e.getMessage());
        }
    }

    public static void moveFile(String sourcePath, String destinationPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path destination = Paths.get(destinationPath);
            // 如果目标目录不存在，创建目录
            if (Files.notExists(destination.getParent())) {
                Files.createDirectories(destination.getParent());
            }
            // 移动文件（即“剪切”）
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("File moved from {} to {}", sourcePath, destinationPath);
        } catch (Exception e) {
            log.error("failed to move file from {} to {}, {}", sourcePath, destinationPath, e.getMessage());
        }
    }

    public static long countFilesWithPrefix(String directoryPath, String prefix) throws IOException {
        Path dir = Paths.get(directoryPath);

        // 遍历文件并统计匹配的文件名
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .count();
        }
    }

}
