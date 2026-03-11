package com.example.stock.controller;

import com.example.stock.service.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 更新控制器
 */
@RestController
@RequestMapping("/api")
public class UpdateController {

    @Autowired
    private UpdateService updateService;

    /**
     * 检查更新
     *
     * @param params 参数
     * @return 更新信息
     */
    @GetMapping("/update/check")
    public Map<String, Object> checkUpdate(@RequestParam Map<String, String> params) {
        return updateService.checkUpdate(params);
    }
}