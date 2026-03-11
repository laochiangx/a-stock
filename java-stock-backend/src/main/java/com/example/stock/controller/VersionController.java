package com.example.stock.controller;

import com.example.stock.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 版本控制器
 */
@RestController
@RequestMapping("/api")
public class VersionController {

    @Autowired
    private VersionService versionService;

    /**
     * 获取版本信息
     *
     * @return 版本信息
     */
    @GetMapping("/version")
    public Map<String, Object> getVersionInfo() {
        return versionService.getVersionInfo();
    }
}