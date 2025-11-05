package com.example.aicodehelper.dto;

import java.util.List;
import java.util.ArrayList;

/**
 * 代码差异比较结果
 * 用于前后端传输代码修改的差异信息
 */
public class CodeDiffResult {
    private String originalCode;           // 原始代码
    private String modifiedCode;           // 修改后代码
    private List<DiffHunk> hunks;          // 差异块列表
    private String status;                 // 状态: success, error
    private String error;                  // 错误信息
    private String instruction;            // 用户的修改指令
    private String fileName;               // 文件名

    public CodeDiffResult() {
        this.hunks = new ArrayList<>();
        this.status = "success";
    }

    public CodeDiffResult(String originalCode, String modifiedCode, String instruction, String fileName) {
        this();
        this.originalCode = originalCode;
        this.modifiedCode = modifiedCode;
        this.instruction = instruction;
        this.fileName = fileName;
    }

    // Getters and Setters
    public String getOriginalCode() {
        return originalCode;
    }

    public void setOriginalCode(String originalCode) {
        this.originalCode = originalCode;
    }

    public String getModifiedCode() {
        return modifiedCode;
    }

    public void setModifiedCode(String modifiedCode) {
        this.modifiedCode = modifiedCode;
    }

    public List<DiffHunk> getHunks() {
        return hunks;
    }

    public void setHunks(List<DiffHunk> hunks) {
        this.hunks = hunks;
    }

    public void addHunk(DiffHunk hunk) {
        this.hunks.add(hunk);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
        this.status = "error";
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 差异块类
     * 表示代码中的一块差异
     */
    public static class DiffHunk {
        private int originalStart;          // 原始代码起始行号（从0开始）
        private int originalLines;          // 原始代码行数
        private int modifiedStart;          // 修改后代码起始行号（从0开始）
        private int modifiedLines;          // 修改后代码行数
        private List<String> originalLinesList;  // 原始代码行列表
        private List<String> modifiedLinesList;  // 修改后代码行列表
        private DiffType type;              // 差异类型
        private String description;         // 差异描述

        public DiffHunk() {
            this.originalLinesList = new ArrayList<>();
            this.modifiedLinesList = new ArrayList<>();
        }

        public DiffHunk(int originalStart, int originalLines, int modifiedStart, int modifiedLines, DiffType type) {
            this();
            this.originalStart = originalStart;
            this.originalLines = originalLines;
            this.modifiedStart = modifiedStart;
            this.modifiedLines = modifiedLines;
            this.type = type;
        }

        // Getters and Setters
        public int getOriginalStart() {
            return originalStart;
        }

        public void setOriginalStart(int originalStart) {
            this.originalStart = originalStart;
        }

        public int getOriginalLines() {
            return originalLines;
        }

        public void setOriginalLines(int originalLines) {
            this.originalLines = originalLines;
        }

        public int getModifiedStart() {
            return modifiedStart;
        }

        public void setModifiedStart(int modifiedStart) {
            this.modifiedStart = modifiedStart;
        }

        public int getModifiedLines() {
            return modifiedLines;
        }

        public void setModifiedLines(int modifiedLines) {
            this.modifiedLines = modifiedLines;
        }

        public List<String> getOriginalLinesList() {
            return originalLinesList;
        }

        public void setOriginalLinesList(List<String> originalLinesList) {
            this.originalLinesList = originalLinesList;
        }

        public List<String> getModifiedLinesList() {
            return modifiedLinesList;
        }

        public void setModifiedLinesList(List<String> modifiedLinesList) {
            this.modifiedLinesList = modifiedLinesList;
        }

        public DiffType getType() {
            return type;
        }

        public void setType(DiffType type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * 添加原始代码行
         */
        public void addOriginalLine(String line) {
            this.originalLinesList.add(line);
        }

        /**
         * 添加修改后代码行
         */
        public void addModifiedLine(String line) {
            this.modifiedLinesList.add(line);
        }
    }

    /**
     * 差异类型枚举
     */
    public enum DiffType {
        ADDED("新增"),      // 新增的行
        REMOVED("删除"),    // 删除的行
        MODIFIED("修改"),   // 修改的行
        UNCHANGED("未变");  // 未变更的行

        private final String description;

        DiffType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}