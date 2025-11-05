package com.javaProgram.services;

/**
 * 前端的代码差异结果类
 * 用于接收后端返回的差异信息
 */
public class CodeDiffResult {
    private String originalCode;
    private String modifiedCode;
    private String instruction;
    private String fileName;
    private String status;
    private String error;

    public CodeDiffResult() {}

    public CodeDiffResult(String originalCode, String modifiedCode, String instruction, String fileName) {
        this.originalCode = originalCode;
        this.modifiedCode = modifiedCode;
        this.instruction = instruction;
        this.fileName = fileName;
        this.status = "success";
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

    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        return "error".equals(status) && error != null && !error.trim().isEmpty();
    }

    /**
     * 检查是否有实际差异
     */
    public boolean hasChanges() {
        return originalCode != null && modifiedCode != null && !originalCode.equals(modifiedCode);
    }
}