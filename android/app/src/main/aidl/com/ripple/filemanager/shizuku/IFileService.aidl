package com.ripple.filemanager.shizuku;

interface IFileService {
    List<String> listFiles(String path);
    boolean deleteFile(String path);
    boolean copyFile(String src, String dest);
    boolean renameFile(String src, String dest);
}
