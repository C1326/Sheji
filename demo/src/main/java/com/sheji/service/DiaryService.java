package com.sheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sheji.common.BusinessException;
import com.sheji.common.UserContext;
import com.sheji.controller.dto.DiaryDTO;
import com.sheji.controller.vo.DiaryDetailVO;
import com.sheji.controller.vo.DiaryListItemVO;
import com.sheji.controller.vo.MediaVO;
import com.sheji.controller.vo.PageResultVO;
import com.sheji.entity.Diary;
import com.sheji.entity.DiaryMedia;
import com.sheji.entity.User;
import com.sheji.mapper.DiaryMapper;
import com.sheji.mapper.DiaryMediaMapper;
import com.sheji.mapper.UserMapper;
import com.sheji.util.FileUtil;
import com.sheji.util.HtmlSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DiaryService {

    private final DiaryMapper diaryMapper;
    private final DiaryMediaMapper diaryMediaMapper;
    private final UserMapper userMapper;
    private final FileUtil fileUtil;

    public DiaryService(DiaryMapper diaryMapper, DiaryMediaMapper diaryMediaMapper, UserMapper userMapper, FileUtil fileUtil) {
        this.diaryMapper = diaryMapper;
        this.diaryMediaMapper = diaryMediaMapper;
        this.userMapper = userMapper;
        this.fileUtil = fileUtil;
    }

    @Transactional
    public Long saveDiary(DiaryDTO dto) {
        Long userId = UserContext.getUserId();
        boolean isUpdate = dto.getDiaryId() != null;
        Diary diary;
        if (isUpdate) {
            diary = diaryMapper.selectById(dto.getDiaryId());
            if (diary == null) {
                throw new BusinessException(400, "日记不存在");
            }
            if (!diary.getUserId().equals(userId)) {
                throw new BusinessException(403, "无权修改该日记");
            }
        } else {
            diary = new Diary();
        }

        diary.setUserId(userId);
        diary.setTitle(dto.getTitle());
        diary.setContent(HtmlSanitizer.sanitize(dto.getContent()));
        diary.setPermission(dto.getPermission());
        diary.setUpdateTime(LocalDateTime.now());

        // 媒体路径去空、去重
        List<String> newUrls = dto.getMediaUrls() == null ? new ArrayList<>()
                : dto.getMediaUrls().stream().filter(StringUtils::hasText).distinct().collect(Collectors.toList());

        if (!isUpdate) {
            diary.setCreateTime(LocalDateTime.now());
            diaryMapper.insert(diary);
        } else {
            // 记录旧媒体路径，用于清理本次编辑后不再使用的物理文件
            List<String> oldUrls = diaryMediaMapper.selectList(
                            new LambdaQueryWrapper<DiaryMedia>().eq(DiaryMedia::getDiaryId, diary.getDiaryId()))
                    .stream().map(DiaryMedia::getFilePath).collect(Collectors.toList());
            diaryMapper.updateById(diary);
            diaryMediaMapper.delete(new LambdaQueryWrapper<DiaryMedia>().eq(DiaryMedia::getDiaryId, diary.getDiaryId()));
            for (String oldUrl : oldUrls) {
                if (!newUrls.contains(oldUrl)) {
                    fileUtil.deleteFile(oldUrl);
                }
            }
        }

        // 保存媒体记录（上传阶段只存了物理文件，记录在此时统一创建）
        for (String url : newUrls) {
            DiaryMedia media = new DiaryMedia();
            media.setDiaryId(diary.getDiaryId());
            media.setFilePath(url);
            media.setFileName(url.substring(url.lastIndexOf('/') + 1));
            media.setMediaType(url.toLowerCase().matches(".*\\.(mp4|mov|avi|webm)$") ? 2 : 1);
            media.setCreateTime(LocalDateTime.now());
            diaryMediaMapper.insert(media);
        }
        return diary.getDiaryId();
    }

    @Transactional
    public void deleteDiary(Long diaryId) {
        Long userId = UserContext.getUserId();
        Diary diary = diaryMapper.selectById(diaryId);
        if (diary == null) {
            throw new BusinessException(400, "日记不存在");
        }
        if (!diary.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该日记");
        }
        List<DiaryMedia> medias = diaryMediaMapper.selectList(
                new LambdaQueryWrapper<DiaryMedia>().eq(DiaryMedia::getDiaryId, diaryId));
        diaryMapper.deleteById(diaryId);
        diaryMediaMapper.delete(new LambdaQueryWrapper<DiaryMedia>().eq(DiaryMedia::getDiaryId, diaryId));
        // 同步清理物理文件，避免垃圾文件堆积
        for (DiaryMedia m : medias) {
            fileUtil.deleteFile(m.getFilePath());
        }
    }

    public PageResultVO<DiaryListItemVO> publicList(Integer page, Integer pageSize) {
        page = page == null || page < 1 ? 1 : page;
        pageSize = pageSize == null || pageSize < 1 ? 12 : pageSize;
        IPage<Diary> iPage = diaryMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Diary>().eq(Diary::getPermission, 1)
                        .orderByDesc(Diary::getCreateTime));
        return toPageVO(iPage, page, pageSize);
    }

    public PageResultVO<DiaryListItemVO> myList(Integer page, Integer pageSize) {
        Long userId = UserContext.getUserId();
        page = page == null || page < 1 ? 1 : page;
        pageSize = pageSize == null || pageSize < 1 ? 12 : pageSize;
        IPage<Diary> iPage = diaryMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Diary>().eq(Diary::getUserId, userId)
                        .orderByDesc(Diary::getCreateTime));
        return toPageVO(iPage, page, pageSize);
    }

    public DiaryDetailVO detail(Long diaryId) {
        Diary diary = diaryMapper.selectById(diaryId);
        if (diary == null) {
            throw new BusinessException(400, "日记不存在");
        }
        Long current = UserContext.getUserId();
        boolean isOwner = current != null && current.equals(diary.getUserId());
        if (diary.getPermission() == 0 && !isOwner) {
            throw new BusinessException(403, "该日记为私有，无权查看");
        }
        User user = userMapper.selectById(diary.getUserId());
        DiaryDetailVO vo = new DiaryDetailVO();
        vo.setDiaryId(diary.getDiaryId());
        vo.setUserId(diary.getUserId());
        vo.setTitle(diary.getTitle());
        vo.setContent(diary.getContent());
        vo.setPermission(diary.getPermission());
        vo.setCreateTime(diary.getCreateTime());
        vo.setUpdateTime(diary.getUpdateTime());
        vo.setNickname(user == null ? "" : user.getNickname());
        vo.setAvatar(user == null ? "" : user.getAvatar());
        vo.setMediaList(getMedia(diaryId));
        vo.setCanDownload(isOwner || diary.getPermission() == 1);
        return vo;
    }

    private List<MediaVO> getMedia(Long diaryId) {
        List<DiaryMedia> medias = diaryMediaMapper.selectList(
                new LambdaQueryWrapper<DiaryMedia>().eq(DiaryMedia::getDiaryId, diaryId));
        return toMediaVO(medias);
    }

    private PageResultVO<DiaryListItemVO> toPageVO(IPage<Diary> iPage, Integer page, Integer pageSize) {
        List<Diary> records = iPage.getRecords();
        List<Long> userIds = records.stream().map(Diary::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getUserId, u -> u));

        // 批量查询本页所有日记的媒体，避免逐条查询的 N+1 问题
        List<Long> diaryIds = records.stream().map(Diary::getDiaryId).collect(Collectors.toList());
        Map<Long, List<DiaryMedia>> mediaMap = diaryIds.isEmpty() ? Collections.emptyMap()
                : diaryMediaMapper.selectList(
                                new LambdaQueryWrapper<DiaryMedia>().in(DiaryMedia::getDiaryId, diaryIds))
                        .stream().collect(Collectors.groupingBy(DiaryMedia::getDiaryId));

        List<DiaryListItemVO> list = new ArrayList<>();
        for (Diary d : records) {
            DiaryListItemVO vo = new DiaryListItemVO();
            vo.setDiaryId(d.getDiaryId());
            vo.setUserId(d.getUserId());
            vo.setTitle(d.getTitle());
            vo.setContent(d.getContent());
            vo.setPermission(d.getPermission());
            vo.setCreateTime(d.getCreateTime());
            User u = userMap.get(d.getUserId());
            vo.setNickname(u == null ? "" : u.getNickname());
            vo.setAvatar(u == null ? "" : u.getAvatar());
            vo.setMediaList(toMediaVO(mediaMap.getOrDefault(d.getDiaryId(), Collections.emptyList())));
            list.add(vo);
        }
        PageResultVO<DiaryListItemVO> pageVO = new PageResultVO<>();
        pageVO.setList(list);
        pageVO.setTotal(iPage.getTotal());
        pageVO.setPage(page);
        pageVO.setPageSize(pageSize);
        return pageVO;
    }

    private List<MediaVO> toMediaVO(List<DiaryMedia> medias) {
        return medias.stream().map(m -> {
            MediaVO vo = new MediaVO();
            vo.setMediaId(m.getMediaId());
            vo.setMediaType(m.getMediaType());
            vo.setUrl(m.getFilePath());
            vo.setFileName(m.getFileName());
            vo.setFileSize(m.getFileSize());
            return vo;
        }).collect(Collectors.toList());
    }
}
