package com.loantrading.matching.extraction;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.util.EncodingDetectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main document extractor that handles multiple formats
 */
public class MultiFormatDocumentExtractor {
    private static final Logger logger = LoggerFactory.getLogger(MultiFormatDocumentExtractor.class);
    
    private final PDFExtractor pdfExtractor;
    private final WordDocumentExtractor wordExtractor;
    private final FieldParser fieldParser;
    private final OCRProcessor ocrProcessor;
    private final CharacterNormalizer characterNormalizer;
    
    public MultiFormatDocumentExtractor() {
        this.pdfExtractor = new PDFExtractor();
        this.wordExtractor = new WordDocumentExtractor();
        this.fieldParser = new FieldParser();
        this.ocrProcessor = new OCRProcessor();
        this.characterNormalizer = new CharacterNormalizer();
    }
    
    public ExtractedEntity extract(byte[] documentContent, String filename) throws ExtractionException {
        String text;
        double ocrConfidence = 1.0;
        
        try {
            // Determine file type and extract text
            if (filename.toLowerCase().endsWith(".pdf")) {
                text = pdfExtractor.extractText(documentContent);
                
                // Check if PDF is scanned (no text or very little text)
                if (text.trim().isEmpty() || text.length() < 100) {
                    logger.info("PDF appears to be scanned, performing OCR for: {}", filename);
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
            
            // Normalize characters to handle encoding issues
            text = characterNormalizer.normalize(text);
            
            // Parse fields from the extracted text
            ExtractedEntity entity = fieldParser.parseFields(text);
            
            // Adjust extraction confidence based on OCR confidence
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