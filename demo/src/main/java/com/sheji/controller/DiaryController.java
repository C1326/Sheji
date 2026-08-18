package com.sheji.controller;

import com.sheji.common.Result;
import com.sheji.controller.dto.DiaryDTO;
import com.sheji.controller.vo.DiaryDetailVO;
import com.sheji.controller.vo.PageResultVO;
import com.sheji.controller.vo.DiaryListItemVO;
import com.sheji.service.DiaryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diary")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @PostMapping("/add")
    public Result<Long> add(@Valid @RequestBody DiaryDTO dto) {
        return Result.success(diaryService.saveDiary(dto));
    }

    @PutMapping("/update")
    public Result<Long> update(@Valid @RequestBody DiaryDTO dto) {
        return Result.success(diaryService.saveDiary(dto));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        diaryService.deleteDiary(id);
        return Result.success();
    }

    @GetMapping("/public/list")
    public Result<PageResultVO<DiaryListItemVO>> publicList(@RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(diaryService.publicList(page, pageSize));
    }

    @GetMapping("/my/list")
    public Result<PageResultVO<DiaryListItemVO>> myList(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(diaryService.myList(page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<DiaryDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(diaryService.detail(id));
    }
}
