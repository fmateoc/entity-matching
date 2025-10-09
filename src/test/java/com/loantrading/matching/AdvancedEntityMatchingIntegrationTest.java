package com.loantrading.matching;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdvancedEntityMatchingIntegrationTest {

    private static Connection connection;
    private static EntityMatchingOrchestrator orchestrator;

    @BeforeAll
    static void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:advancedtestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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
            InputStream is = AdvancedEntityMatchingIntegrationTest.class.getClassLoader().getResourceAsStream("schema.sql");
            if (is == null) {
                throw new RuntimeException("Cannot find schema.sql in classpath");
            }
            String schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            statement.execute(schema);
        } catch (IOException e) {
            throw new SQLException("Failed to read schema.sql", e);
        }
    }

    private static void insertTestData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 1. Composite Identity Match ("Legal Entity" + "Fund Manager")
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, ultimate_parent, mei, country_code) VALUES " +
                    "(301, 'Global Infrastructure Fund III', 'Global Infra III', 'Mega Asset Management', 'US12345678', 'US')");

            // 2. DBA (Doing Business As) Match
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, mei, country_code) VALUES " +
                    "(302, 'Regional Holdings LLC DBA Speedy Logistics', 'Speedy Logistics', 'US23456789', 'US')");

            // 3. Source of Truth Hierarchy Match
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, ein, country_code) VALUES " +
                    "(303, 'Vertex Innovations Inc.', 'Vertex', '33-4455667', 'US')");

            // 4. OCR Error Match
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, mei, country_code) VALUES " +
                    "(304, 'Maritime Shipping Co', 'Maritime Ship', 'GB87654321', 'GB')");

            // 5. Location-Level Match
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, country_code) VALUES " +
                    "(401, 'Continental Enterprises', 'Continental', 'DE')");
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name) VALUES " +
                    "(501, 'Continental Enterprises Berlin Office', 'Continental Berlin')");
            stmt.execute("INSERT INTO entity_locations (location_id, parent_customer_id, mei, lei) VALUES " +
                    "(501, 401, 'DE11223344', '549300BF8C62F247C542')");
        }
    }

    // --- Document Creation Helpers ---

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
                XWPFParagraph p = document.createParagraph();
                XWPFRun run = p.createRun();
                run.setText(line);
            }
            document.write(baos);
        }
        return baos.toByteArray();
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

    @Test
    void testCompositeIdentityMatch() throws IOException {
        // 1. Define entity data for a composite match
        String legalName = "Global Infrastructure Fund III";
        String fundManager = "Mega Asset Management";
        String mei = "US12345678";

        // 2. Generate documents
        byte[] adfPdf = createPdf("ADF_Composite.pdf", "Legal Name: " + legalName, "Fund Manager: " + fundManager, "MEI: " + mei);
        byte[] taxFormDocx = createDocx("TaxForm_Composite.docx", "Entity Name: " + legalName, "MEI: " + mei);

        // 3. Process documents
        com.loantrading.matching.entity.ProcessingResult result = orchestrator.processDocuments(adfPdf, "ADF_Composite.pdf", taxFormDocx, "TaxForm_Composite.docx");

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("MATCH", result.getDecision(), "Decision should be MATCH for a composite identity");
        assertNotNull(result.getSelectedMatch(), "There should be a selected match");
        assertEquals(301L, result.getSelectedMatch().getMatchedEntity().getEntityId(), "Matched entity ID should be correct");
        assertEquals(fundManager, result.getSelectedMatch().getMatchedEntity().getUltimateParent(), "Fund manager should be correctly matched");
        assertTrue(result.getSelectedMatch().getScore() >= 90, "Score should be high for a composite match");
    }

    @Test
    void testDbaNameMatch() throws IOException {
        // 1. Define entity data for a DBA match
        String tradeName = "Speedy Logistics";
        String mei = "US23456789";

        // 2. Generate documents using the trade name
        byte[] adfPdf = createPdf("ADF_DBA.pdf", "Entity Name: " + tradeName, "MEI: " + mei);
        byte[] taxFormDocx = createDocx("TaxForm_DBA.docx", "Name: " + tradeName, "MEI: " + mei);

        // 3. Process documents
        com.loantrading.matching.entity.ProcessingResult result = orchestrator.processDocuments(adfPdf, "ADF_DBA.pdf", taxFormDocx, "TaxForm_DBA.docx");

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("MATCH", result.getDecision(), "Decision should be MATCH for a DBA name");
        assertNotNull(result.getSelectedMatch(), "There should be a selected match");
        assertEquals(302L, result.getSelectedMatch().getMatchedEntity().getEntityId(), "Matched entity ID should be correct for DBA match");
        assertTrue(result.getSelectedMatch().getMatchedEntity().getFullName().contains("DBA"), "Matched entity name should contain 'DBA'");
        assertTrue(result.getSelectedMatch().getScore() >= 90, "Score should be high for a DBA match");
    }

    @Test
    void testSourceOfTruthHierarchy() throws IOException {
        // 1. Define entity data with conflicting names
        String nameOnAdf = "Vertex Innovations, LLC"; // Incorrect name
        String nameOnTaxForm = "Vertex Innovations Inc."; // Correct name, matches DB
        String ein = "33-4455667"; // Matching identifier

        // 2. Generate documents with conflicting data
        byte[] adfPdf = createPdf("ADF_SourceOfTruth.pdf", "Legal Name: " + nameOnAdf, "EIN: " + ein);
        byte[] taxFormDocx = createDocx("TaxForm_SourceOfTruth.docx", "Official Name: " + nameOnTaxForm, "Tax ID: " + ein);

        // 3. Process documents
        com.loantrading.matching.entity.ProcessingResult result = orchestrator.processDocuments(adfPdf, "ADF_SourceOfTruth.pdf", taxFormDocx, "TaxForm_SourceOfTruth.docx");

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("MANUAL_REVIEW", result.getDecision(), "Decision should be MANUAL_REVIEW due to name discrepancy");
        assertNotNull(result.getSelectedMatch(), "There should be a selected match");
        assertEquals(303L, result.getSelectedMatch().getMatchedEntity().getEntityId(), "Matched entity ID should be correct");
        assertFalse(result.getSelectedMatch().getDiscrepancies().isEmpty(), "Discrepancies should be detected");
        assertTrue(result.getSelectedMatch().getDiscrepancies().stream().anyMatch(d -> "corporateForm".equals(d.getType())), "A discrepancy for the corporate form should be flagged");
    }

    @Test
    void testOcrErrorMatch() throws IOException {
        // 1. Define entity data with common OCR errors ("rn" for "m", "I" for "l")
        String ocrLegalName = "Maritirne Shippinq Co"; // DB has "Maritime Shipping Co"
        String mei = "GB87654321";

        // 2. Generate an image-based PDF to simulate a scanned document
        byte[] adfImagePdf = createImagePdf("ADF_OCR_Error.pdf", "Legal Name: " + ocrLegalName, "MEI: " + mei);

        // 3. Process the document
        com.loantrading.matching.entity.ProcessingResult result = orchestrator.processDocuments(adfImagePdf, "ADF_OCR_Error.pdf", null, null);

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("MATCH", result.getDecision(), "Decision should be MATCH despite OCR errors");
        assertNotNull(result.getSelectedMatch(), "There should be a selected match");
        assertEquals(304L, result.getSelectedMatch().getMatchedEntity().getEntityId(), "Matched entity ID should be correct for OCR error match");
        assertTrue(result.getSelectedMatch().getScore() > 80, "Score should be high for a match with correctable OCR errors");
    }

    @Test
    void testLocationLevelMatch() throws IOException {
        // 1. Define entity data that matches a location, not a primary entity
        String legalName = "Continental Enterprises Berlin Office";
        String mei = "DE11223344"; // This MEI is on the location, not the parent

        // 2. Generate documents
        byte[] adfPdf = createPdf("ADF_Location.pdf", "Legal Name: " + legalName, "MEI: " + mei);

        // 3. Process the document
        com.loantrading.matching.entity.ProcessingResult result = orchestrator.processDocuments(adfPdf, "ADF_Location.pdf", null, null);

        // 4. Assert results
        assertNotNull(result, "Processing result should not be null");
        assertEquals("MATCH", result.getDecision(), "Decision should be MATCH for a location-level entity");
        assertNotNull(result.getSelectedMatch(), "There should be a selected match");
        assertEquals(501L, result.getSelectedMatch().getMatchedEntity().getEntityId(), "Matched entity ID should be the location's ID");
        assertTrue(result.getSelectedMatch().getScore() >= 90, "Score should be high for a location-level identifier match");
    }

}