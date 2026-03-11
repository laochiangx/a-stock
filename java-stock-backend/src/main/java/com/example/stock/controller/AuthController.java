package com.example.stock.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stock.dao.SysUserDao;
import com.example.stock.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private SysUserDao sysUserDao;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();

        String username = body == null ? null : body.get("username");
        String password = body == null ? null : body.get("password");

        if (username == null || username.trim().isEmpty() || password == null) {
            result.put("success", false);
            result.put("message", "username/password required");
            return result;
        }

        SysUser user = sysUserDao.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            result.put("success", false);
            result.put("message", "user not found");
            return result;
        }

        if (Boolean.FALSE.equals(user.getIsEnabled())) {
            result.put("success", false);
            result.put("message", "user disabled");
            return result;
        }

        String passwordHash = sha256Hex(password);
        if (user.getPasswordHash() == null || !user.getPasswordHash().equalsIgnoreCase(passwordHash)) {
            result.put("success", false);
            result.put("message", "invalid password");
            return result;
        }

        try {
            SysUser update = new SysUser();
            update.setId(user.getId());
            update.setLastLoginAt(LocalDateTime.now());
            update.setUpdatedAt(LocalDateTime.now());
            sysUserDao.updateById(update);
        } catch (Exception ignored) {
        }

        result.put("success", true);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("displayName", user.getDisplayName());
        userInfo.put("isAdmin", user.getIsAdmin());
        result.put("user", userInfo);
        return result;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
