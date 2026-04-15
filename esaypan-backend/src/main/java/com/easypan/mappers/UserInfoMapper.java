package com.easypan.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper; // MP 核心接口
import com.easypan.entity.po.UserInfo;
import org.apache.ibatis.annotations.Param;

/**
 * 用户信息 数据库操作接口
 */
public interface UserInfoMapper extends BaseMapper<UserInfo> {

}