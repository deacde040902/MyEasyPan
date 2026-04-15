package com.easypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.entity.dto.SysSettingDto;
import com.easypan.entity.po.SysConfig;
import com.easypan.mappers.SysConfigMapper;
import com.easypan.service.SysConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置表 服务实现类
 * @author licheng
 * @since 2026-03-24
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    /**
     * 查询所有系统配置，返回键值对Map
     */
    @Override
    public Map<String, String> getSysConfigMap() {
        // 查询所有配置
        List<SysConfig> configList = this.list();
        Map<String, String> configMap = new HashMap<>(configList.size());

        // 转换为键值对格式
        for (SysConfig config : configList) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return configMap;
    }

    /**
     * 保存系统配置（存在则更新，不存在则新增）
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 修正：添加事务控制，确保数据一致性
    public void saveSysConfig(Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String configKey = entry.getKey();
            String configValue = entry.getValue();

            // 查询配置是否已存在
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfig::getConfigKey, configKey);
            SysConfig existConfig = this.getOne(wrapper);

            if (existConfig != null) {
                // 存在则更新值
                existConfig.setConfigValue(configValue);
                this.updateById(existConfig);
            } else {
                // 不存在则新增
                SysConfig newConfig = new SysConfig();
                newConfig.setConfigKey(configKey);
                newConfig.setConfigValue(configValue);
                newConfig.setRemark("系统配置-" + configKey); // 补充默认备注
                this.save(newConfig);
            }
        }
    }

    @Override
    public Map<String, String> getSystemSettings() {
        Map<String, String> settings = new HashMap<>();
        List<SysConfig> configs = this.list();
        for (SysConfig config : configs) {
            settings.put(config.getConfigKey(), config.getConfigValue());
        }
        // 确保默认值
        if (!settings.containsKey("systemName")) {
            settings.put("systemName", "Easy云盘");
        }
        if (!settings.containsKey("defaultSpace")) {
            settings.put("defaultSpace", "5");
        }
        if (!settings.containsKey("vipSpace")) {
            settings.put("vipSpace", "100");
        }
        return settings;
    }

    @Override
    public void saveSystemSettings(SysSettingDto settings) {
        // 保存系统名称
        saveConfig("systemName", settings.getSystemName());
        // 保存默认存储空间
        saveConfig("defaultSpace", settings.getDefaultSpace());
        // 保存VIP存储空间
        saveConfig("vipSpace", settings.getVipSpace());
    }

    private void saveConfig(String key, String value) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig config = this.getOne(wrapper);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            this.save(config);
        } else {
            config.setConfigValue(value);
            this.updateById(config);
        }
    }

}