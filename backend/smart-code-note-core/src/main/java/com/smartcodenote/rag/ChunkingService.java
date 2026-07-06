package com.smartcodenote.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Splits Chinese text into overlapping chunks for embedding.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Split by double-newline (paragraph boundary)</li>
 *   <li>Merge short paragraphs into adjacent ones</li>
 *   <li>Split long paragraphs at sentence boundaries (。；！？)</li>
 *   <li>Add overlap between adjacent chunks to avoid cutting key info</li>
 * </ol>
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[。；！？;!?])\\s*");
    private static final int MIN_PARAGRAPH_LENGTH = 80;

    private final RagProperties properties;

    public ChunkingService(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * Split text into chunks suitable for embedding.
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int chunkSize = properties.getChunkSize();
        int overlap = properties.getChunkOverlap();

        // Step 1: split by paragraphs
        String[] paragraphs = text.split("\\n\\s*\\n");
        List<String> merged = mergeShortParagraphs(paragraphs);

        // Step 2: split long paragraphs into sentence-level chunks
        List<String> chunks = new ArrayList<>();
        for (String para : merged) {
            if (para.length() <= chunkSize) {
                chunks.add(para.trim());
            } else {
                chunks.addAll(splitLongParagraph(para, chunkSize, overlap));
            }
        }

        // Step 3: add overlap between chunks
        if (overlap > 0 && chunks.size() > 1) {
            chunks = addOverlap(chunks, overlap);
        }

        log.debug("Chunked text ({} chars) into {} chunks", text.length(), chunks.size());
        return chunks;
    }

    /**
     * Merge paragraphs that are too short into adjacent ones.
     */
    private List<String> mergeShortParagraphs(String[] paragraphs) {
        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (buffer.length() > 0 && buffer.length() + trimmed.length() < MIN_PARAGRAPH_LENGTH) {
                // Merge with previous
                buffer.append('\n').append(trimmed);
            } else if (trimmed.length() < MIN_PARAGRAPH_LENGTH && result.isEmpty()) {
                // First paragraph is short, buffer it
                buffer.append(trimmed);
            } else if (buffer.length() > 0) {
                // Flush buffer before starting new paragraph
                result.add(buffer.toString().trim());
                buffer.setLength(0);
                buffer.append(trimmed);
            } else {
                result.add(trimmed);
            }
        }

        if (buffer.length() > 0) {
            result.add(buffer.toString().trim());
        }

        return result;
    }

    /**
     * Split a long paragraph at sentence boundaries, keeping chunks under maxLength.
     */
    private List<String> splitLongParagraph(String paragraph, int maxLength, int overlap) {
        List<String> result = new ArrayList<>();
        String[] sentences = SENTENCE_BOUNDARY.split(paragraph);
        StringBuilder chunk = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            if (chunk.length() + trimmed.length() > maxLength && chunk.length() > 0) {
                result.add(chunk.toString().trim());
                // Start new chunk with overlap from previous
                String prevText = chunk.toString();
                if (prevText.length() > overlap) {
                    chunk = new StringBuilder(prevText.substring(prevText.length() - overlap));
                } else {
                    chunk = new StringBuilder();
                }
            }
            chunk.append(trimmed);
        }

        if (chunk.length() > 0) {
            result.add(chunk.toString().trim());
        }

        return result;
    }

    /**
     * Add overlapping text between consecutive chunks.
     */
    private List<String> addOverlap(List<String> chunks, int overlap) {
        List<String> result = new ArrayList<>();
        result.add(chunks.get(0));

        for (int i = 1; i < chunks.size(); i++) {
            String prev = chunks.get(i - 1);
            String current = chunks.get(i);

            if (prev.length() > overlap) {
                String overlapText = prev.substring(prev.length() - overlap);
                result.add(overlapText + "\n" + current);
            } else {
                result.add(current);
            }
        }

        return result;
    }
}
