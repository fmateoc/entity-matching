package com.loantrading.matching.extraction;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.util.EncodingDetectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main document extractor that handles multiple formats.
 * It now intelligently delegates parsing to a specialized parser based on the document type (digital vs. scanned/OCR).
 */
public class MultiFormatDocumentExtractor {
    private static final Logger logger = LoggerFactory.getLogger(MultiFormatDocumentExtractor.class);
    
    private final PDFExtractor pdfExtractor;
    private final WordDocumentExtractor wordExtractor;
    private final FieldParser fieldParser;
    private final OcrFieldParser ocrFieldParser;
    private final OCRProcessor ocrProcessor;

    public MultiFormatDocumentExtractor() {
        this.pdfExtractor = new PDFExtractor();
        this.wordExtractor = new WordDocumentExtractor();
        this.fieldParser = new FieldParser();
        this.ocrFieldParser = new OcrFieldParser();
        this.ocrProcessor = new OCRProcessor();
    }
    
    public ExtractedEntity extract(byte[] documentContent, String filename) throws ExtractionException {
        String text;
        boolean isOcr = false;
        double ocrConfidence = 1.0;
        
        try {
            // Step 1: Determine file type and extract raw text.
            if (filename.toLowerCase().endsWith(".pdf")) {
                text = pdfExtractor.extractText(documentContent);
                
                // If the PDF has no selectable text, it's scanned. Use OCR.
                if (text.trim().isEmpty() || text.length() < 100) {
                    logger.info("PDF appears to be scanned, performing OCR for: {}", filename);
                    isOcr = true;
                    OCRResult ocrResult = ocrProcessor.performOCR(documentContent);
                    text = ocrResult.getText();
                    ocrConfidence = ocrResult.getConfidence();
                }
            } else if (filename.toLowerCase().endsWith(".docx")) {
                text = wordExtractor.extractDocxText(documentContent);
            } else if (filename.toLowerCase().endsWith(".doc")) {
                text = wordExtractor.extractDocText(documentContent);
            } else if (filename.toLowerCase().endsWith(".txt")) {
                java.nio.charset.Charset charset = EncodingDetectorUtil.detectCharset(documentContent);
                text = new String(documentContent, charset);
            } else {
                throw new ExtractionException("Unsupported file format: " + filename);
            }
            
            // Step 2: Delegate to the appropriate parser.
            ExtractedEntity entity;
            if (isOcr) {
                logger.debug("Delegating to OcrFieldParser.");
                entity = ocrFieldParser.parse(text);
            } else {
                logger.debug("Delegating to standard FieldParser.");
                entity = fieldParser.parseFields(text);
            }
            
            // Step 3: Adjust final confidence based on OCR quality.
            entity.setExtractionConfidence(entity.getExtractionConfidence() * ocrConfidence);
            
            logger.info("Successfully extracted entity from {} with confidence {}", 
                filename, entity.getExtractionConfidence());
            
            return entity;
            
        } catch (Exception e) {
            logger.error("Failed to extract from document: {}", filename, e);
            throw new ExtractionException("Failed to extract from document: " + filename, e);
        }
    }
}