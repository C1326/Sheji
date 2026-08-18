package com.sheji.service;

import com.sheji.common.BusinessException;
import com.sheji.common.UserContext;
import com.sheji.controller.vo.UploadVO;
import com.sheji.entity.Diary;
import com.sheji.entity.DiaryMedia;
import com.sheji.mapper.DiaryMapper;
import com.sheji.mapper.DiaryMediaMapper;
import com.sheji.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class MediaService {

    private static final long MAX_SIZE = 50L * 1024 * 1024;

    private final FileUtil fileUtil;
    private final DiaryMapper diaryMapper;
    private final DiaryMediaMapper diaryMediaMapper;

    public MediaService(FileUtil fileUtil, DiaryMapper diaryMapper, DiaryMediaMapper diaryMediaMapper) {
        this.fileUtil = fileUtil;
        this.diaryMapper = diaryMapper;
        this.diaryMediaMapper = diaryMediaMapper;
    }

    /**
     * 上传文件：服务端校验类型和大小，仅保存物理文件并返回访问路径。
     * diary_media 记录在保存日记时统一创建，避免上传阶段 diary_id 为空触发 NOT NULL 约束。
     */
    public UploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "文件大小不能超过 50MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = name.toLowerCase();
        boolean isImage = lower.matches(".*\\.(jpg|jpeg|png|gif|webp)$");
        boolean isVideo = lower.matches(".*\\.(mp4|mov|avi|webm)$");
        if (!isImage && !isVideo) {
            throw new BusinessException(400, "仅支持图片(jpg/png/gif/webp)或视频(mp4/mov/avi/webm)");
        }
        try {
            String url = fileUtil.store(file);
            UploadVO vo = new UploadVO();
            vo.setFilePath(url);
            vo.setFileName(name);
            vo.setFileSize(file.getSize());
            vo.setMediaType(isVideo ? 2 : 1);
            return vo;
        } catch (Exception e) {
            throw new BusinessException(500, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 加载媒体文件（带权限校验），供在线预览 /view 使用。
     */
    public File load(Long mediaId) {
        DiaryMedia media = diaryMediaMapper.selectById(mediaId);
        if (media == null) {
            throw new BusinessException(400, "媒体不存在");
        }
        checkPermission(media);
        return resolveFile(media);
    }

    /**
     * 下载媒体文件（带权限校验）。
     */
    public File download(Long mediaId) {
        return load(mediaId);
    }

    /**
     * 权限校验：公开日记任何人可访问；私有日记仅本人可访问。
     */
    private void checkPermission(DiaryMedia media) {
        if (media.getDiaryId() != null) {
            Diary diary = diaryMapper.selectById(media.getDiaryId());
            if (diary != null) {
                Long current = UserContext.getUserId();
                boolean isOwner = current != null && current.equals(diary.getUserId());
                if (diary.getPermission() == 0 && !isOwner) {
                    throw new BusinessException(403, "无权访问该文件");
                }
            }
        }
    }

    private File resolveFile(DiaryMedia media) {
        String path = media.getFilePath();
        if (path == null) {
            throw new BusinessException(400, "文件路径为空");
        }
        File file = fileUtil.resolve(path);
        if (!file.exists()) {
            throw new BusinessException(400, "文件不存在");
        }
        return file;
    }
}
