package com.loantrading.matching;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.orchestrator.EntityMatchingOrchestrator;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class EntityMatchingIntegrationTest {

    private static Connection connection;
    private static EntityMatchingOrchestrator orchestrator;

    @BeforeAll
    static void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:integrationtestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        createTestSchema(connection);
        insertTestData(connection);
        orchestrator = new EntityMatchingOrchestrator(connection);
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (connection != null) {
            connection.close();
        }
        if (orchestrator != null) {
            orchestrator.shutdown();
        }
    }

    private static void createTestSchema(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS entities (" +
                    "entity_id BIGSERIAL PRIMARY KEY, " +
                    "full_name VARCHAR(500) NOT NULL, " +
                    "short_name VARCHAR(200), " +
                    "ultimate_parent VARCHAR(500), " +
                    "mei VARCHAR(10), " +
                    "lei VARCHAR(20), " +
                    "ein VARCHAR(20), " +
                    "debt_domain_id VARCHAR(20), " +
                    "email_domain VARCHAR(100), " +
                    "country_code VARCHAR(2), " +
                    "legal_address TEXT, " +
                    "tax_address TEXT, " +
                    "last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            statement.execute("CREATE TABLE IF NOT EXISTS entity_locations (" +
                    "location_id BIGINT PRIMARY KEY, " +
                    "parent_customer_id BIGINT, " +
                    "mei VARCHAR(10), " +
                    "lei VARCHAR(20), " +
                    "ein VARCHAR(20), " +
                    "FOREIGN KEY (location_id) REFERENCES entities(entity_id), " +
                    "FOREIGN KEY (parent_customer_id) REFERENCES entities(entity_id))");
        }
    }

    private static void insertTestData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Perfect Match Entity
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, ultimate_parent, mei, lei, ein, debt_domain_id, email_domain, country_code, legal_address, tax_address) VALUES " +
                    "(101, 'Pine Street Fund V, L.P.', 'Pine Street V', 'Pine Street Capital', 'US87654321', '54930084UKL21D49S726', '12-3456789', 'PSF5DEBT', 'pinestreet.com', 'US', '123 Main St, Anytown, USA', '123 Main St, Anytown, USA')");
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name) VALUES (201, 'Pine Street Fund V Location', 'Pine St V Loc')");
            stmt.execute("INSERT INTO entity_locations (location_id, parent_customer_id, mei, lei, ein) VALUES (201, 101, 'US87654321', '54930084UKL21D49S726', '12-3456789')");

            // Discrepancy Match Entity
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, ultimate_parent, mei, lei, ein, debt_domain_id, email_domain, country_code, legal_address, tax_address) VALUES " +
                    "(102, 'Oak Avenue Associates, Inc.', 'Oak Ave Associates', 'Oak Avenue Holdings', 'CA11223344', '89450073URL32E58T412', '98-7654321', 'OAK1DEBT', 'oakave.com', 'US', '456 Oak Ave, Sometown, USA', '456 Oak Ave, Sometown, USA')");

            // No Match Entity (in DB, but not in docs)
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, ultimate_parent, mei, lei, ein) VALUES " +
                    "(103, 'Birch Tree Partners, LLC', 'Birch Tree', 'Birch Tree Capital', 'GB99887766', '11223344ABC556677889', '55-4433221')");
        }
    }

    @Test
    void testPerfectMatch() throws IOException {
        // 1. Define entity data for a perfect match from test data
        String legalName = "Pine Street Fund V, L.P.";
        String fundManager = "Pine Street Capital";
        String mei = "US87654321";
        String lei = "54930084UKL21D49S726";
        String ein = "12-3456789";
        String address = "123 Main St, Anytown, USA";

        // 2. Generate documents with all identifiers
        byte[] adfPdf = createPdf("ADF_PerfectMatch.pdf", "Legal Name: " + legalName, "Fund Manager: " + fundManager, "MEI: " + mei, "LEI: " + lei, "EIN: " + ein, "Address: " + address);
        byte[] taxFormDocx = createDocx("TaxForm_PerfectMatch.docx", "Legal Name: " + legalName, "Fund Manager: " + fundManager, "MEI: " + mei, "LEI: " + lei, "EIN: " + ein, "Address: " + address);

        // 3. Process documents
        ProcessingResult result = orchestrator.processDocuments(adfPdf, "ADF_PerfectMatch.pdf", taxFormDocx, "TaxForm_PerfectMatch.docx");

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("MATCH", result.getDecision(), "Decision should be MATCH for a perfect scenario");
        assertNotNull(result.getSelectedMatch(), "There should be a selected match");
        assertEquals(101L, result.getSelectedMatch().getMatchedEntity().getEntityId(), "Matched entity ID should be correct");
        assertEquals(legalName, result.getSelectedMatch().getMatchedEntity().getFullName(), "Matched entity name should be correct");
        assertTrue(result.getSelectedMatch().getScore() >= 90, "Score should be high for a perfect match");
    }

    @Test
    void testImagePdfMatch() throws IOException {
        // 1. Define entity data for a perfect match
        String legalName = "Pine Street Fund V, L.P.";
        String fundManager = "Pine Street Capital";
        String mei = "US87654321";
        String lei = "54930084UKL21D49S726";
        String ein = "12-3456789";
        String address = "123 Main St, Anytown, USA";

        // 2. Generate an image-based PDF and a standard DOCX
        byte[] adfImagePdf = createImagePdf("ADF_ImageMatch.pdf", "Legal Name: " + legalName, "Fund Manager: " + fundManager, "MEI: " + mei, "LEI: " + lei, "EIN: " + ein, "Address: " + address);
        byte[] taxFormDocx = createDocx("TaxForm_ImageMatch.docx", "Legal Name: " + legalName, "Fund Manager: " + fundManager, "MEI: " + mei, "LEI: " + lei, "EIN: " + ein, "Address: " + address);

        // 3. Process documents
        ProcessingResult result = orchestrator.processDocuments(adfImagePdf, "ADF_ImageMatch.pdf", taxFormDocx, "TaxForm_ImageMatch.docx");

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("MATCH", result.getDecision(), "Decision should be MATCH for an image-based PDF perfect scenario");
        assertNotNull(result.getSelectedMatch(), "There should be a selected match");
        assertEquals(101L, result.getSelectedMatch().getMatchedEntity().getEntityId(), "Matched entity ID should be correct");
        assertTrue(result.getSelectedMatch().getScore() >= 90, "Score should be high for a perfect image-based match");
    }

    @Test
    void testDiscrepancyMatch() throws IOException {
        // 1. Define entity data with discrepancies
        String legalNameInDoc = "Oak Ave Associates, Incorporated"; // DB has "Oak Avenue Associates, Inc."
        String einInDoc = "98-7654322"; // DB has "98-7654321"
        String mei = "CA11223344"; // This matches the DB

        // 2. Generate documents with discrepant data
        byte[] adfPdf = createPdf("ADF_Discrepancy.pdf", "Legal Name: " + legalNameInDoc, "EIN: " + einInDoc, "MEI: " + mei);
        byte[] taxFormDocx = createDocx("TaxForm_Discrepancy.docx", "Entity Name: " + legalNameInDoc, "Tax ID: " + einInDoc);

        // 3. Process documents
        ProcessingResult result = orchestrator.processDocuments(adfPdf, "ADF_Discrepancy.pdf", taxFormDocx, "TaxForm_Discrepancy.docx");

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("MANUAL_REVIEW", result.getDecision(), "Decision should be MANUAL_REVIEW for a discrepancy scenario");
        assertNotNull(result.getSelectedMatch(), "There should be a selected match");
        assertEquals(102L, result.getSelectedMatch().getMatchedEntity().getEntityId(), "Matched entity ID should be correct for the discrepancy");
        double score = result.getSelectedMatch().getScore();
        assertTrue(score >= 60 && score < 90, "Score should be in the medium confidence range (60-89). Actual: " + score);
        assertFalse(result.getSelectedMatch().getDiscrepancies().isEmpty(), "Discrepancies should be detected");
    }

    @Test
    void testNoMatch() throws IOException {
        // 1. Define entity data for a new entity not in the DB
        String legalName = "Maple Financial Group, LLC";
        String mei = "GB12345678";
        String ein = "11-2233445";

        // 2. Generate documents for the new entity
        byte[] adfPdf = createPdf("ADF_NoMatch.pdf", "Legal Name: " + legalName, "MEI: " + mei, "EIN: " + ein);
        byte[] taxFormDocx = createDocx("TaxForm_NoMatch.docx", "Entity Name: " + legalName, "Tax ID: " + ein);

        // 3. Process documents
        ProcessingResult result = orchestrator.processDocuments(adfPdf, "ADF_NoMatch.pdf", taxFormDocx, "TaxForm_NoMatch.docx");

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("NO_MATCH", result.getDecision(), "Decision should be NO_MATCH for a new entity");
        assertNull(result.getSelectedMatch(), "There should be no selected match for a new entity");
    }

    @Test
    void testEinMismatchHaltsProcessing() throws IOException {
        // 1. Define data with conflicting EINs
        String legalName = "Pine Street Fund V, L.P.";
        String adfEin = "12-3456789";
        String taxFormEin = "99-9999999"; // Different EIN

        // 2. Generate documents with conflicting EINs
        byte[] adfPdf = createPdf("ADF_Conflict.pdf", "Legal Name: " + legalName, "EIN: " + adfEin);
        byte[] taxFormDocx = createDocx("TaxForm_Conflict.docx", "Legal Name: " + legalName, "Tax ID: " + taxFormEin);

        // 3. Process documents
        ProcessingResult result = orchestrator.processDocuments(adfPdf, "ADF_Conflict.pdf", taxFormDocx, "TaxForm_Conflict.docx");

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("ERROR", result.getDecision(), "Decision should be ERROR for conflicting EINs");
        assertTrue(result.getAuditTrail().stream().anyMatch(s -> s.contains("Critical Error: EIN mismatch between documents")), "Audit trail should contain EIN mismatch error");
        assertNull(result.getSelectedMatch(), "There should be no selected match when processing is halted");
    }

    private byte[] createImagePdf(String filename, String... lines) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] imageBytes = createImageFromText(lines);

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Image image = new Image(ImageDataFactory.create(imageBytes));
        document.add(image);

        document.close();
        return baos.toByteArray();
    }

    private byte[] createImageFromText(String[] lines) throws IOException {
        int width = 800;
        int height = 600;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));

        int y = 20;
        for (String line : lines) {
            g2d.drawString(line, 20, y);
            y += 15;
        }

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createPdf(String filename, String... lines) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        for (String line : lines) {
            document.add(new Paragraph(line));
        }
        document.close();
        return baos.toByteArray();
    }

    private byte[] createDocx(String filename, String... lines) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            for (String line : lines) {
                addDocxLine(document, line);
            }
            document.write(baos);
        }
        return baos.toByteArray();
    }

    private void addDocxLine(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
    }
}