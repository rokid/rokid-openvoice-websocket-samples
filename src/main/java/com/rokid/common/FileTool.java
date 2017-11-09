package com.rokid.common;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 作者: mashuangwei
 * 日期: 2017/9/25
 */
@Slf4j
public class FileTool {

    public static long getFileSize(String filename){
        Path path= Paths.get(filename);
        BasicFileAttributeView basicview = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        BasicFileAttributes basicfile = null;
        try {
            basicfile = basicview.readAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("文件大小: {}", basicfile.size());
        return basicfile.size();
    }

    public static boolean deleteFile(String filename) {
        File file = new File(filename);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                log.info("删除单个文件 " + filename + " 成功");
                return true;
            } else {
                log.info("删除单个文件 " + filename + " 失败");
                return false;
            }
        } else {
            log.info("删除单个文件失败： " + filename + " 不存在");
            return false;
        }
    }

}
