package com.easypan.utils;

/**
 * 文件大小格式化工具类
 */
public class FileSizeUtils {

    /**
     * 字节转易读格式（B/KB/MB/GB）
     */
    public static String formatFileSize(Long size) {
        if (size == null || size < 0) {
            return "0B";
        }
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", size / (1024.0 * 1024 * 1024));
        }
    }
}