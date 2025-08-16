package com.loantrading.matching.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts text and images from PDF documents
 */
public class PDFExtractor {
    private static final Logger logger = LoggerFactory.getLogger(PDFExtractor.class);
    private static final int MAX_PAGES = 10;
    private static final int DPI_FOR_OCR = 300;
    
    /**
     * Extract text from PDF using PDFBox
     */
    public String extractText(byte[] pdfContent) throws IOException {
        try (PDDocument document = PDDocument.load(pdfContent)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(MAX_PAGES, document.getNumberOfPages()));
            
            String text = stripper.getText(document);
            logger.debug("Extracted {} characters from PDF", text.length());
            
            return text;
        }
    }
    
    /**
     * Extract images from PDF for OCR processing
     */
    public List<BufferedImage> extractImages(byte[] pdfContent, int maxPages) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(pdfContent)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(maxPages, document.getNumberOfPages());
            
            logger.debug("Extracting {} pages as images for OCR", pages);
            
            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI_FOR_OCR);
                images.add(image);
            }
        }
        
        return images;
    }
    
    /**
     * Check if PDF contains extractable text
     */
    public boolean hasText(byte[] pdfContent) {
        try {
            String text = extractText(pdfContent);
            return text != null && text.trim().length() > 50;
        } catch (IOException e) {
            logger.warn("Error checking PDF for text", e);
            return false;
        }
    }
}