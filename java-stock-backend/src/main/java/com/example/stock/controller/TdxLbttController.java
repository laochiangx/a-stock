package com.example.stock.controller;

import com.example.stock.service.TdxLbttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tdx-ladder")
@CrossOrigin(origins = "*")
public class TdxLbttController {

    @Autowired
    private TdxLbttService tdxLbttService;

    @GetMapping("/sync")
    public Map<String, Object> sync(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Boolean force) {
        return tdxLbttService.sync(start, end, force);
    }

    @GetMapping("/query")
    public Map<String, Object> query(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Integer days) {
        return tdxLbttService.query(start, end, days);
    }
}
