package com.yanglizi.diary.controller;

import com.yanglizi.common.anno.CurrentUser;
import com.yanglizi.common.pojo.vo.Result;
import com.yanglizi.diary.pojo.dto.DiaryDto;
import com.yanglizi.diary.pojo.dto.QueryDiaryDto;
import com.yanglizi.diary.service.DiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 日记
 */
@RestController
@RequestMapping("/diary")
public class DiaryController {

    @Autowired
    private DiaryService diaryService;

    /**
     * 生成日记
     */
    @PostMapping("/{sessionId}")
    public Result genDiary(@PathVariable String sessionId, Integer userId) {
        return Result.success(diaryService.genDiary(sessionId, userId));
    }

    /**
     * 根据日记id获取日记详情
     */
    @GetMapping("/{id}")
    public Result getDiary(@PathVariable Integer id, Integer userId) {
        return Result.success(diaryService.getDiaryById(id, userId));
    }

    /**
     * 根据日记版本组获取日记列表
     */
    @GetMapping("/list/{versionGroup}")
    public Result getDiaryList(@PathVariable String versionGroup, Integer userId) {
        return Result.success(diaryService.getDiaryListBySessionId(userId, versionGroup));
    }

    /**
     * 获取所有有效日记列表
     */
    @GetMapping
    public Result getAllDiaryList(QueryDiaryDto dto, Integer userId) {
        return Result.success(diaryService.getDiaryList(dto, userId));
    }

    /**
     * 查看日记生成队列
     */
    @GetMapping("/queue")
    public Result getDiaryQueue(QueryDiaryDto dto, Integer userId) {
        return Result.success(diaryService.getDiaryQueue(dto, userId));
    }

    /**
     * 确定 ai生成的日记为可用版本
     */
    @PutMapping("/confirm")
    public Result confirmDiary(@RequestBody DiaryDto dto, Integer userId) {
        // confirm_at 为null，表示未确认, 可以修改完再确认, status等于3生成完成的
        int i = diaryService.confirmDiary(dto, userId);
        if (i > 0) {
            return Result.success();
        }
        return Result.fail("确认失败");
    }

    /**
     * 手动提交新版本日记
     */
    @PostMapping
    public Result submitNewVersionDiary(@RequestBody DiaryDto dto, Integer userId) {
        int i = diaryService.submitNewVersionDiary(dto, userId);
        if (i > 0) {
            return Result.success();
        }
        return Result.fail("提交失败");
    }

//    /**
//     * 保存草稿
//     */
//    @PostMapping("/{id}/draft")
//    public Result saveDraft(@PathVariable Integer id, @RequestBody DiaryDto dto) {
////        String key = "draft:diary:" + diaryId;
////        redisTemplate.opsForValue().set(key,
////                JSON.toJSONString(draft),
////                Duration.ofDays(7));
//        return Result.success();
//    }
//
//    /**
//     * 获取草稿
//     */
//    @GetMapping("/{id}/draft")
//    public Result getDraft(@PathVariable Integer id) {
////        String key = "draft:diary:" + diaryId;
////        String json = redisTemplate.opsForValue().get(key);
//        return Result.success();
//    }

}
