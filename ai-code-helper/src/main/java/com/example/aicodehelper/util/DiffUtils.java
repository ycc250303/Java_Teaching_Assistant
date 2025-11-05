package com.example.aicodehelper.util;

import com.example.aicodehelper.dto.CodeDiffResult;
import com.example.aicodehelper.dto.CodeDiffResult.DiffHunk;
import com.example.aicodehelper.dto.CodeDiffResult.DiffType;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * 差异比较工具类
 * 用于比较原始代码和修改后代码，生成差异信息
 */
public class DiffUtils {

    /**
     * 比较两段代码，生成差异结果
     *
     * @param originalCode 原始代码
     * @param modifiedCode 修改后代码
     * @return 差异比较结果
     */
    public static CodeDiffResult compareCode(String originalCode, String modifiedCode) {
        return compareCode(originalCode, modifiedCode, "", "");
    }

    /**
     * 比较两段代码，生成差异结果
     *
     * @param originalCode 原始代码
     * @param modifiedCode 修改后代码
     * @param instruction 修改指令
     * @param fileName 文件名
     * @return 差异比较结果
     */
    public static CodeDiffResult compareCode(String originalCode, String modifiedCode,
                                           String instruction, String fileName) {
        if (originalCode == null) originalCode = "";
        if (modifiedCode == null) modifiedCode = "";

        CodeDiffResult result = new CodeDiffResult(originalCode, modifiedCode, instruction, fileName);

        // 分割成行
        String[] originalLines = originalCode.split("\n");
        String[] modifiedLines = modifiedCode.split("\n");

        // 使用Myers算法计算差异
        List<DiffHunk> hunks = calculateDiff(originalLines, modifiedLines);
        result.setHunks(hunks);

        return result;
    }

    /**
     * 使用简化的Myers算法计算文本差异
     *
     * @param originalLines 原始文本行数组
     * @param modifiedLines 修改后文本行数组
     * @return 差异块列表
     */
    private static List<DiffHunk> calculateDiff(String[] originalLines, String[] modifiedLines) {
        List<DiffHunk> hunks = new ArrayList<>();

        // 使用简单的逐行比较算法
        int originalIndex = 0;
        int modifiedIndex = 0;

        while (originalIndex < originalLines.length || modifiedIndex < modifiedLines.length) {
            if (originalIndex >= originalLines.length) {
                // 原始代码已结束，剩余的都是新增行
                DiffHunk hunk = new DiffHunk(
                    originalIndex, 0,
                    modifiedIndex, modifiedLines.length - modifiedIndex,
                    DiffType.ADDED
                );
                for (int i = modifiedIndex; i < modifiedLines.length; i++) {
                    hunk.addModifiedLine(modifiedLines[i]);
                }
                hunks.add(hunk);
                break;
            }

            if (modifiedIndex >= modifiedLines.length) {
                // 修改后代码已结束，剩余的都是删除行
                DiffHunk hunk = new DiffHunk(
                    originalIndex, originalLines.length - originalIndex,
                    modifiedIndex, 0,
                    DiffType.REMOVED
                );
                for (int i = originalIndex; i < originalLines.length; i++) {
                    hunk.addOriginalLine(originalLines[i]);
                }
                hunks.add(hunk);
                break;
            }

            String originalLine = originalLines[originalIndex];
            String modifiedLine = modifiedLines[modifiedIndex];

            if (originalLine.equals(modifiedLine)) {
                // 行相同，继续下一行
                originalIndex++;
                modifiedIndex++;
            } else {
                // 行不同，需要找出差异块
                DiffHunk hunk = findDifferenceBlock(originalLines, modifiedLines, originalIndex, modifiedIndex);
                hunks.add(hunk);
                originalIndex += hunk.getOriginalLines();
                modifiedIndex += hunk.getModifiedLines();
            }
        }

        return hunks;
    }

    /**
     * 找出差异块
     */
    private static DiffHunk findDifferenceBlock(String[] originalLines, String[] modifiedLines,
                                              int originalStart, int modifiedStart) {
        DiffHunk hunk = new DiffHunk();
        hunk.setOriginalStart(originalStart);
        hunk.setModifiedStart(modifiedStart);

        int originalIndex = originalStart;
        int modifiedIndex = modifiedStart;
        int originalEnd = originalStart;
        int modifiedEnd = modifiedStart;

        // 找出连续的不同的行
        while (originalIndex < originalLines.length && modifiedIndex < modifiedLines.length) {
            String originalLine = originalLines[originalIndex];
            String modifiedLine = modifiedLines[modifiedIndex];

            if (originalLine.equals(modifiedLine)) {
                // 找到相同的行，可能是差异块的结束
                break;
            }

            hunk.addOriginalLine(originalLine);
            hunk.addModifiedLine(modifiedLine);
            originalIndex++;
            modifiedIndex++;
            originalEnd++;
            modifiedEnd++;
        }

        // 处理末尾的情况
        while (originalIndex < originalLines.length) {
            hunk.addOriginalLine(originalLines[originalIndex]);
            originalIndex++;
            originalEnd++;
        }

        while (modifiedIndex < modifiedLines.length) {
            hunk.addModifiedLine(modifiedLines[modifiedIndex]);
            modifiedIndex++;
            modifiedEnd++;
        }

        hunk.setOriginalLines(originalEnd - originalStart);
        hunk.setModifiedLines(modifiedEnd - modifiedStart);

        // 确定差异类型
        if (hunk.getOriginalLinesList().isEmpty()) {
            hunk.setType(DiffType.ADDED);
        } else if (hunk.getModifiedLinesList().isEmpty()) {
            hunk.setType(DiffType.REMOVED);
        } else {
            hunk.setType(DiffType.MODIFIED);
        }

        // 生成描述
        hunk.setDescription(generateHunkDescription(hunk));

        return hunk;
    }

    /**
     * 生成差异块的描述
     */
    private static String generateHunkDescription(DiffHunk hunk) {
        switch (hunk.getType()) {
            case ADDED:
                return String.format("新增 %d 行", hunk.getModifiedLines());
            case REMOVED:
                return String.format("删除 %d 行", hunk.getOriginalLines());
            case MODIFIED:
                return String.format("修改 %d 行为 %d 行", hunk.getOriginalLines(), hunk.getModifiedLines());
            default:
                return "未知变更";
        }
    }

    /**
     * 清理代码，去除可能的markdown代码块标记
     */
    public static String cleanCode(String code) {
        if (code == null) {
            return "";
        }

        String cleaned = code.trim();

        // 去除开头的```java、```或类似标记
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            } else {
                // 如果没有换行，可能是```code```的形式
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*", "");
            }
        }

        // 去除结尾的```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * 判断是否有实际差异
     */
    public static boolean hasRealChanges(CodeDiffResult diffResult) {
        if (diffResult == null || diffResult.getHunks() == null) {
            return false;
        }

        return diffResult.getHunks().stream()
                .anyMatch(hunk -> hunk.getType() != DiffType.UNCHANGED &&
                                  (hunk.getOriginalLines() > 0 || hunk.getModifiedLines() > 0));
    }
}