package com.loantrading.matching.extraction;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Extracts text from Microsoft Word documents (both .doc and .docx)
 */
public class WordDocumentExtractor {
    private static final Logger logger = LoggerFactory.getLogger(WordDocumentExtractor.class);
    
    /**
     * Extract text from modern Word format (.docx)
     */
    public String extractDocxText(byte[] wordContent) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(wordContent))) {
            StringBuilder text = new StringBuilder();
            
            // Extract from paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                    text.append(paragraphText).append("\n");
                }
            }
            
            // Extract from tables
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            text.append(cellText).append("\t");
                        }
                    }
                    text.append("\n");
                }
            }
            
            // Extract from headers
            for (XWPFHeader header : document.getHeaderList()) {
                String headerText = header.getText();
                if (headerText != null && !headerText.trim().isEmpty()) {
                    text.append(headerText).append("\n");
                }
            }
            
            // Extract from footers
            for (XWPFFooter footer : document.getFooterList()) {
                String footerText = footer.getText();
                if (footerText != null && !footerText.trim().isEmpty()) {
                    text.append(footerText).append("\n");
                }
            }
            
            logger.debug("Extracted {} characters from DOCX", text.length());
            return text.toString();
        }
    }
    
    /**
     * Extract text from legacy Word format (.doc)
     */
    public String extractDocText(byte[] wordContent) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(wordContent))) {
            WordExtractor extractor = new WordExtractor(document);
            String text = extractor.getText();
            
            logger.debug("Extracted {} characters from DOC", text.length());
            
            extractor.close();
            return text;
        }
    }
}