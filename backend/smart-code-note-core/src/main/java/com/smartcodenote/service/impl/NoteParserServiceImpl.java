package com.smartcodenote.service.impl;

import com.smartcodenote.exception.BusinessException;
import com.smartcodenote.service.NoteParserService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NoteParserServiceImpl implements NoteParserService {

    @Override
    public ParsedNote parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("file is required");
        }
        try {
            return parse(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("failed to read uploaded file");
        }
    }

    @Override
    public ParsedNote parseContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("content is required");
        }
        String cleanContent = clean(content);
        if (!StringUtils.hasText(cleanContent)) {
            throw new BusinessException("note content is empty after cleaning");
        }
        return new ParsedNote("txt", content, cleanContent);
    }

    @Override
    public ParsedNote parse(String fileName, byte[] bytes) {
        String fileType = resolveFileType(fileName);
        if (!"txt".equals(fileType) && !"md".equals(fileType)) {
            throw new BusinessException("only .txt and .md files are supported");
        }
        String originalContent = new String(bytes, StandardCharsets.UTF_8);
        String cleanContent = clean(originalContent);
        if (!StringUtils.hasText(cleanContent)) {
            throw new BusinessException("note content is empty");
        }
        return new ParsedNote(fileType, originalContent, cleanContent);
    }

    private String resolveFileType(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new BusinessException("file extension is required");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String clean(String content) {
        return content.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("(?m)^ +", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
