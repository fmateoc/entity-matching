package com.loantrading.matching.extraction;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Performs OCR on scanned documents using Tesseract
 */
public class OCRProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OCRProcessor.class);
    private static final int MAX_PAGES_FOR_OCR = 5;
    
    private final Tesseract tesseract;
    private final PDFExtractor pdfExtractor;
    
    public OCRProcessor() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        this.tesseract.setLanguage("eng");
        this.tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
        this.tesseract.setOcrEngineMode(1); // Neural nets LSTM engine
        this.pdfExtractor = new PDFExtractor();
    }
    
    /**
     * Perform OCR on a document
     */
    public OCRResult performOCR(byte[] documentContent) throws Exception {
        try {
            // Convert PDF to images
            List<BufferedImage> images = pdfExtractor.extractImages(documentContent, MAX_PAGES_FOR_OCR);
            
            if (images.isEmpty()) {
                logger.warn("No images extracted for OCR");
                return new OCRResult("", 0.0);
            }
            
            StringBuilder fullText = new StringBuilder();
            double totalConfidence = 0;
            int processedPages = 0;
            
            for (BufferedImage image : images) {
                // Preprocess image for better OCR
                BufferedImage processed = preprocessImage(image);
                
                // Perform OCR
                String pageText = tesseract.doOCR(processed);
                fullText.append(pageText).append("\n");
                
                // Estimate confidence
                double pageConfidence = estimateConfidence(pageText);
                totalConfidence += pageConfidence;
                processedPages++;
                
                logger.debug("OCR page {} - confidence: {}", processedPages, pageConfidence);
            }
            
            double avgConfidence = processedPages > 0 ? totalConfidence / processedPages : 0.5;
            
            logger.info("OCR completed: {} pages processed, avg confidence: {}", 
                processedPages, avgConfidence);
            
            return new OCRResult(fullText.toString(), avgConfidence);
            
        } catch (TesseractException e) {
            logger.error("OCR processing failed", e);
            throw new Exception("OCR processing failed", e);
        }
    }
    
    /**
     * Preprocess image to improve OCR accuracy
     */
    private BufferedImage preprocessImage(BufferedImage image) {
        // In a production system, this would include:
        // - Deskewing
        // - Denoising
        // - Contrast adjustment
        // - Binarization
        // For now, return as-is
        return image;
    }
    
    /**
     * Estimate OCR confidence based on text characteristics
     */
    private double estimateConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        // Check for common OCR artifacts
        long artifactCount = text.chars()
            .filter(ch -> ch == '�' || ch == '□' || ch == '○' || ch == '■')
            .count();
        
        // Check for reasonable word patterns
        String[] words = text.split("\\s+");
        long validWords = Arrays.stream(words)
            .filter(w -> w.matches("[a-zA-Z0-9]+"))
            .count();
        
        // Calculate ratios
        double artifactRatio = (double) artifactCount / text.length();
        double validWordRatio = words.length > 0 ? (double) validWords / words.length : 0;
        
        // Calculate confidence score
        double confidence = 0.5; // Base confidence
        confidence += validWordRatio * 0.4; // Up to 0.4 for valid words
        confidence -= artifactRatio * 10; // Penalty for artifacts
        
        // Check for common patterns that indicate good OCR
        if (text.contains("@") && text.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
            confidence += 0.1; // Email patterns suggest good OCR
        }
        
        return Math.max(0.1, Math.min(1.0, confidence));
    }
}