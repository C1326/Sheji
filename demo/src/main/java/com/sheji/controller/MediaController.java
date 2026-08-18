package com.sheji.controller;

import com.sheji.common.Result;
import com.sheji.controller.vo.UploadVO;
import com.sheji.service.MediaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * 上传图片/视频（登录用户），返回 filePath 等信息，供提交日记时放入 mediaUrls。
     */
    @PostMapping("/upload")
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(mediaService.upload(file));
    }

    /**
     * 在线预览：公开日记媒体任何人可看，私有日记仅本人可看。
     */
    @GetMapping("/view/{mediaId}")
    public void view(@PathVariable Long mediaId, HttpServletResponse response) throws IOException {
        File file = mediaService.load(mediaId);
        response.setContentType(contentType(file.getName()));
        copy(file, response);
    }

    /**
     * 下载：权限校验与 view 一致（公开 or 本人）。
     */
    @GetMapping("/download/{mediaId}")
    public void download(@PathVariable Long mediaId, HttpServletResponse response) throws IOException {
        File file = mediaService.download(mediaId);
        response.setContentType("application/octet-stream");
        String name = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
        copy(file, response);
    }

    private void copy(File file, HttpServletResponse response) throws IOException {
        try (java.io.FileInputStream in = new java.io.FileInputStream(file);
             java.io.OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    private String contentType(String fileName) {
        String n = fileName.toLowerCase();
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (n.endsWith(".png")) {
            return "image/png";
        }
        if (n.endsWith(".gif")) {
            return "image/gif";
        }
        if (n.endsWith(".webp")) {
            return "image/webp";
        }
        if (n.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (n.endsWith(".mov")) {
            return "video/quicktime";
        }
        if (n.endsWith(".webm")) {
            return "video/webm";
        }
        if (n.endsWith(".avi")) {
            return "video/x-msvideo";
        }
        return "application/octet-stream";
    }
}
