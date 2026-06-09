package com.itheima.smartcodenote.service;

import org.springframework.web.multipart.MultipartFile;

public interface NoteParserService {

    ParsedNote parse(MultipartFile file);

    ParsedNote parse(String fileName, byte[] bytes);

    ParsedNote parseContent(String content);

    record ParsedNote(String fileType, String originalContent, String cleanContent) {
    }
}
