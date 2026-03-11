package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserDao extends BaseMapper<SysUser> {
}
