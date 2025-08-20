package com.loantrading.matching;

import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.orchestrator.DocumentPair;
import com.loantrading.matching.orchestrator.EntityMatchingOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for the Entity Matching System
 */
public class EntityMatchingApplication {
    private static final Logger logger = LoggerFactory.getLogger(EntityMatchingApplication.class);
    
    private final EntityMatchingOrchestrator orchestrator;
    private final HikariDataSource dataSource;
    private final ObjectMapper jsonMapper;
    
    /**
     * Initialize the application with database connection
     */
    public EntityMatchingApplication(String dataSourceClassName, String serverName) throws SQLException {
        logger.info("Initializing Entity Matching Application");
        
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName(dataSourceClassName);
        config.addDataSourceProperty("serverName", serverName);
        
        config.setAutoCommit(false);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("EntityMatchingPool");

        this.dataSource = new HikariDataSource(config);

        // The orchestrator and its components expect a single long-lived connection.
        // We'll get one connection from the pool and use it for the application's lifetime.
        // The LoanIQRepository will close this connection when the app shuts down.
        Connection dbConnection = dataSource.getConnection();
        
        // Initialize orchestrator
        this.orchestrator = new EntityMatchingOrchestrator(dbConnection);
        
        // Configure JSON mapper
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.registerModule(new JavaTimeModule());
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        logger.info("Application initialized successfully");
    }
    
    /**
     * Process Admin Details Form with Tax Form
     */
    public ProcessingResult processWithTaxForm(byte[] adfContent, String adfFilename,
                                               byte[] taxFormContent, String taxFormFilename) {
        logger.info("Processing ADF: {} with Tax Form: {}", adfFilename, taxFormFilename);
        return orchestrator.processDocuments(adfContent, adfFilename, taxFormContent, taxFormFilename);
    }

    /**
     * Process Admin Details Form without a Tax Form
     */
    public ProcessingResult processAdminDetailsForm(byte[] adfContent, String adfFilename) {
        logger.info("Processing ADF without tax form: {}", adfFilename);
        return processWithTaxForm(adfContent, adfFilename, null, null);
    }
    
    /**
     * Process a batch of documents
     */
    public List<ProcessingResult> processBatch(List<DocumentPair> documents) {
        logger.info("Processing batch of {} documents", documents.size());
        return orchestrator.processBatch(documents);
    }
    
    /**
     * Process files from filesystem
     */
    public ProcessingResult processFiles(String adfPath, String taxFormPath) throws Exception {
        byte[] adfContent = Files.readAllBytes(Paths.get(adfPath));
        byte[] taxFormContent = null;
        String taxFormFilename = null;
        
        if (taxFormPath != null) {
            taxFormContent = Files.readAllBytes(Paths.get(taxFormPath));
            taxFormFilename = Paths.get(taxFormPath).getFileName().toString();
        }
        
        String adfFilename = Paths.get(adfPath).getFileName().toString();
        
        if (taxFormContent != null) {
            return processWithTaxForm(adfContent, adfFilename, taxFormContent, taxFormFilename);
        } else {
            return processAdminDetailsForm(adfContent, adfFilename);
        }
    }
    
    /**
     * Process a directory of documents
     */
    public List<ProcessingResult> processDirectory(String directoryPath) throws Exception {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + directoryPath);
        }
        
        List<DocumentPair> pairs = new ArrayList<>();
        File[] files = directory.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".pdf") || 
            name.toLowerCase().endsWith(".docx") ||
            name.toLowerCase().endsWith(".doc")
        );
        
        if (files != null) {
            for (File file : files) {
                byte[] content = Files.readAllBytes(file.toPath());
                String filename = file.getName();
                String referenceId = filename.replaceFirst("[.][^.]+$", ""); // Remove extension
                
                // Simple heuristic: files with "tax" in name are tax forms
                if (filename.toLowerCase().contains("tax")) {
                    logger.info("Skipping tax form for batch processing: {}", filename);
                } else {
                    pairs.add(new DocumentPair(content, filename, referenceId));
                }
            }
        }
        
        return processBatch(pairs);
    }
    
    /**
     * Save processing result to JSON file
     */
    public void saveResultToJson(ProcessingResult result, String outputPath) throws Exception {
        String json = jsonMapper.writeValueAsString(result);
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(json.getBytes());
        }
        logger.info("Result saved to: {}", outputPath);
    }
    
    /**
     * Generate summary report for batch results
     */
    public void generateBatchReport(List<ProcessingResult> results, String reportPath) throws Exception {
        BatchReport report = new BatchReport();
        report.setProcessedAt(LocalDateTime.now());
        report.setTotalDocuments(results.size());
        
        int matches = 0;
        int noMatches = 0;
        int reviews = 0;
        int errors = 0;
        
        for (ProcessingResult result : results) {
            switch (result.getDecision()) {
                case "MATCH":
                    matches++;
                    break;
                case "NO_MATCH":
                    noMatches++;
                    break;
                case "MANUAL_REVIEW":
                    reviews++;
                    break;
                case "ERROR":
                    errors++;
                    break;
            }
        }
        
        report.setMatchCount(matches);
        report.setNoMatchCount(noMatches);
        report.setReviewCount(reviews);
        report.setErrorCount(errors);
        report.setSuccessRate((double)(matches + noMatches) / results.size() * 100);
        
        String json = jsonMapper.writeValueAsString(report);
        try (FileOutputStream fos = new FileOutputStream(reportPath)) {
            fos.write(json.getBytes());
        }
        
        logger.info("Batch report saved to: {}", reportPath);
        logger.info("Summary - Matches: {}, No Matches: {}, Reviews: {}, Errors: {}", 
            matches, noMatches, reviews, errors);
    }
    
    /**
     * Close application resources
     */
    public void close() {
        // The orchestrator shutdown will close the connection obtained from the pool.
        orchestrator.shutdown();

        // Close the datasource, which will close the connection pool.
        if (dataSource != null) {
            dataSource.close();
        }

        logger.info("Application closed successfully");
    }
    
    /**
     * Main method for command-line execution
     */
    public static void main(String[] args) {
        // Configure logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss");
        
        if (args.length < 3) {
            System.err.println("Usage: EntityMatchingApplication <dataSourceClassName> <serverName> <command> [options]");
            System.err.println("Commands:");
            System.err.println("  single <adf_file> [tax_form_file] - Process single document");
            System.err.println("  batch <directory> - Process all documents in directory");
            System.exit(1);
        }
        
        String dataSourceClassName = args[0];
        String serverName = args[1];
        String command = args[2];
        
        try {
            EntityMatchingApplication app = new EntityMatchingApplication(dataSourceClassName, serverName);
            
            switch (command.toLowerCase()) {
                case "single":
                    if (args.length < 5) {
                        System.err.println("Error: ADF file required for single command");
                        System.exit(1);
                    }
                    processSingleDocument(app, args);
                    break;
                    
                case "batch":
                    if (args.length < 5) {
                        System.err.println("Error: Directory required for batch command");
                        System.exit(1);
                    }
                    processBatchDocuments(app, args[4]);
                    break;
                    
                default:
                    System.err.println("Unknown command: " + command);
                    System.exit(1);
            }
            
            app.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void processSingleDocument(EntityMatchingApplication app, String[] args) throws Exception {
        String adfFile = args[4];
        String taxFormFile = args.length > 5 ? args[5] : null;
        
        ProcessingResult result = app.processFiles(adfFile, taxFormFile);
        
        // Generate output filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputFile = String.format("matching_result_%s.json", timestamp);
        
        app.saveResultToJson(result, outputFile);
        
        // Print summary to console
        System.out.println("\n=== MATCHING RESULT ===");
        System.out.println("Decision: " + result.getDecision());
        System.out.println("Entity Type: " + result.getEntityType());
        System.out.println("Processing Time: " + result.getProcessingTimeMs() + " ms");
        
        if (result.getSelectedMatch() != null) {
            System.out.println("Best Match: " + result.getSelectedMatch().getMatchedEntity().getFullName());
            System.out.println("Score: " + result.getSelectedMatch().getScore());
            System.out.println("Confidence: " + result.getSelectedMatch().getConfidence());
        }
        
        System.out.println("\nFull results saved to: " + outputFile);
    }
    
    private static void processBatchDocuments(EntityMatchingApplication app, String directory) throws Exception {
        List<ProcessingResult> results = app.processDirectory(directory);
        
        // Generate report
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportFile = String.format("batch_report_%s.json", timestamp);
        
        app.generateBatchReport(results, reportFile);
        
        // Save individual results
        for (int i = 0; i < results.size(); i++) {
            ProcessingResult result = results.get(i);
            String outputFile = String.format("result_%d_%s.json", i + 1, timestamp);
            app.saveResultToJson(result, outputFile);
        }
        
        System.out.println("\n=== BATCH PROCESSING COMPLETE ===");
        System.out.println("Processed " + results.size() + " documents");
        System.out.println("Report saved to: " + reportFile);
    }
    
    
    /**
     * Inner class for batch reporting
     */
    static class BatchReport {
        private LocalDateTime processedAt;
        private int totalDocuments;
        private int matchCount;
        private int noMatchCount;
        private int reviewCount;
        private int errorCount;
        private double successRate;
        
        // Getters and setters
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
        public int getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }
        public int getMatchCount() { return matchCount; }
        public void setMatchCount(int matchCount) { this.matchCount = matchCount; }
        public int getNoMatchCount() { return noMatchCount; }
        public void setNoMatchCount(int noMatchCount) { this.noMatchCount = noMatchCount; }
        public int getReviewCount() { return reviewCount; }
        public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }
}