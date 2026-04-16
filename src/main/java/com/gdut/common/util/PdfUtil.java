package com.gdut.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * PDF处理工具类
 */
@Slf4j
public class PdfUtil {

    /**
     * 从PDF文件中提取文本内容
     *
     * @param inputStream PDF文件输入流
     * @return 提取的文本内容
     */
    public static String extractText(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("PDF文本提取失败", e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage());
        }
    }

    /**
     * 从PDF文件中提取文本内容
     *
     * @param file PDF文件
     * @return 提取的文本内容
     */
    public static String extractText(File file) {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("PDF文本提取失败", e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage());
        }
    }
}
