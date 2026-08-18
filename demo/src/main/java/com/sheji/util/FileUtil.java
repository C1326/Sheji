package com.sheji.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class FileUtil {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public String store(MultipartFile file) throws IOException {
        File root = new File(uploadDir);
        if (!root.exists()) {
            root.mkdirs();
        }
        String original = file.getOriginalFilename();
        String suffix = "";
        if (original != null && original.contains(".")) {
            suffix = original.substring(original.lastIndexOf("."));
        }
        String newName = UUID.randomUUID().toString().replace("-", "") + suffix;
        Path target = java.nio.file.Paths.get(root.getAbsolutePath(), newName);
        file.transferTo(target.toFile());
        // 返回相对访问路径
        return "/uploads/" + newName;
    }

    /**
     * 根据存储的相对路径（/uploads/xxx）解析本地文件，只取文件名，防止路径穿越。
     */
    public File resolve(String filePath) {
        String fileName = filePath == null ? "" : filePath.substring(filePath.lastIndexOf('/') + 1);
        return new File(uploadDir, fileName);
    }

    /**
     * 删除存储的相对路径对应的物理文件（不存在时静默忽略）。
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        File file = resolve(filePath);
        if (file.exists()) {
            file.delete();
        }
    }
}
