package com.newgen.loanorigination;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.newgen.loanorigination.controller.CreditApprovalController;
import com.newgen.loanorigination.exception.ApplicationNotFoundException;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.service.LoanApplicationService;
import com.newgen.loanorigination.service.PdfGenerationService;
import com.newgen.loanorigination.service.ReviewerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ApplicationFormPdfTest {

    private PdfGenerationService pdfGenerationService;
    private LoanApplicationService loanApplicationService;
    private ReviewerService reviewerService;
    private CreditApprovalController creditApprovalController;

    @BeforeEach
    void setUp() {
        pdfGenerationService = new PdfGenerationService();
        loanApplicationService = Mockito.mock(LoanApplicationService.class);
        reviewerService = Mockito.mock(ReviewerService.class);
        creditApprovalController = new CreditApprovalController(reviewerService, loanApplicationService, pdfGenerationService);
    }

    private Customer createSampleCustomer(String name, String mobile, LocalDate dob, String address) {
        Customer c = new Customer();
        c.setCustomerId(101L);
        c.setCustomerCode("CUST-101");
        c.setFullName(name);
        c.setMobileNumber(mobile);
        c.setDateOfBirth(dob);
        c.setAddressLine(address);
        c.setCity("Mumbai");
        c.setState("Maharashtra");
        c.setPostalCode("400001");
        c.setKycType("PAN");
        c.setKycReference("ABCDE1234F");
        c.setKycStatus("VERIFIED");
        return c;
    }

    private LoanApplication createSampleApplication(String appNo, ProductType product, BigDecimal amount,
                                                    Integer tenure, BigDecimal rate, BigDecimal income,
                                                    BigDecimal obligations, BigDecimal emi, BigDecimal foir,
                                                    ApplicationStatus status) {
        LoanApplication app = new LoanApplication();
        app.setApplicationId(1L);
        app.setApplicationNumber(appNo);
        app.setCustomerId(101L);
        app.setProductType(product);
        app.setBranchCode("BR001");
        app.setBranchName("Bandra Kurla Complex");
        app.setRequestedAmount(amount);
        app.setApprovedAmount(amount);
        app.setTenureMonths(tenure);
        app.setAnnualInterestRate(rate);
        app.setMonthlyNetIncome(income);
        app.setExistingMonthlyObligations(obligations);
        app.setProposedEmi(emi);
        app.setFoirPercentage(foir);
        app.setEligible(true);
        app.setStatus(status);
        app.setCreatedAt(LocalDateTime.of(2026, 11, 20, 10, 0));
        app.setSubmittedAt(LocalDateTime.of(2026, 11, 20, 10, 30));
        if (status == ApplicationStatus.CREDIT_APPROVAL || status == ApplicationStatus.DISBURSED || status == ApplicationStatus.EXIT) {
            app.setApprovedAt(LocalDateTime.of(2026, 11, 21, 15, 0));
            app.setReviewerCode("REV-007");
            app.setDecisionReason("Applicant fulfills credit criteria with comfortable FOIR.");
        }
        return app;
    }

    private String extractAllText(byte[] pdfBytes) throws IOException {
        PdfReader reader = new PdfReader(pdfBytes);
        int pages = reader.getNumberOfPages();
        StringBuilder sb = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int i = 1; i <= pages; i++) {
            sb.append(extractor.getTextFromPage(i)).append("\n");
        }
        reader.close();
        return sb.toString().replaceAll("\\s+", " ");
    }

    @Test
    @DisplayName("Scenario 1: Valid Personal Loan Application PDF Content & Formatting")
    void testValidPersonalLoanPdfContent() throws Exception {
        Customer customer = createSampleCustomer("Aarav Mehta", "9876543210",
                LocalDate.of(1995, 6, 15), "Flat 402, Sea Green Apartments, Bandra West");
        LoanApplication app = createSampleApplication("APP-2026-00101", ProductType.PERSONAL,
                new BigDecimal("250000.00"), 36, new BigDecimal("12.00"),
                new BigDecimal("60000.00"), new BigDecimal("5000.00"),
                new BigDecimal("8303.56"), new BigDecimal("22.17"),
                ApplicationStatus.CREDIT_APPROVAL);

        byte[] pdfBytes = pdfGenerationService.generateApplicationFormPdf(app, customer);

        // PDF Integrity assertions
        assertThat(pdfBytes).isNotNull().isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        // Parse and inspect extracted text
        String text = extractAllText(pdfBytes);

        // Required application fields
        assertThat(text).contains("LoanSphere | Loan Origination System");
        assertThat(text).contains("APP-2026-00101");
        assertThat(text).contains("CREDIT_APPROVAL");
        assertThat(text).contains("PERSONAL");
        assertThat(text).contains("Bandra Kurla Complex (BR001)");
        assertThat(text).contains("20-Nov-2026 10:30");
        assertThat(text).contains("21-Nov-2026 15:00");

        // Required customer fields
        assertThat(text).contains("Aarav Mehta");
        assertThat(text).contains("CUST-101");
        assertThat(text).contains("15-Jun-1995 (Age: 31 years)");
        assertThat(text).contains("9876543210");
        assertThat(text).contains("PAN: ABCDE1234F");
        assertThat(text).contains("VERIFIED");
        assertThat(text).contains("Flat 402, Sea Green Apartments, Bandra West");

        // Required financial fields & currency formatting
        assertThat(text).contains("INR 2,50,000.00"); // Requested Amount
        assertThat(text).contains("36 Months"); // Tenure
        assertThat(text).contains("12.00% p.a."); // Interest rate
        assertThat(text).contains("INR 60,000.00"); // Monthly income
        assertThat(text).contains("INR 5,000.00"); // Existing obligations
        assertThat(text).contains("INR 8,303.56"); // Calculated EMI
        assertThat(text).contains("22.17% (Limit: 50.00%)"); // FOIR

        // Sanction Authority & Signatures
        assertThat(text).contains("REV-007");
        assertThat(text).contains("Applicant fulfills credit criteria");
        assertThat(text).contains("Applicant Signature");
        assertThat(text).contains("Credit Officer / Sanction Authority");
    }

    @Test
    @DisplayName("Scenario 2: Valid Education Loan Application PDF Content & Formatting")
    void testValidEducationLoanPdfContent() throws Exception {
        Customer customer = createSampleCustomer("Ananya Iyer", "9123456780",
                LocalDate.of(2002, 3, 25), "House No. 18, 4th Cross, Malleshwaram");
        LoanApplication app = createSampleApplication("APP-2026-00202", ProductType.EDUCATION,
                new BigDecimal("800000.00"), 60, new BigDecimal("9.50"),
                new BigDecimal("85000.00"), new BigDecimal("10000.00"),
                new BigDecimal("16801.32"), new BigDecimal("31.53"),
                ApplicationStatus.DISBURSED);

        byte[] pdfBytes = pdfGenerationService.generateApplicationFormPdf(app, customer);

        assertThat(pdfBytes).isNotNull().isNotEmpty();
        String text = extractAllText(pdfBytes);

        assertThat(text).contains("APP-2026-00202");
        assertThat(text).contains("EDUCATION");
        assertThat(text).contains("Ananya Iyer");
        assertThat(text).contains("25-Mar-2002 (Age: 24 years)");
        assertThat(text).contains("9123456780");
        assertThat(text).contains("DISBURSED");
        assertThat(text).contains("INR 8,00,000.00");
        assertThat(text).contains("60 Months");
        assertThat(text).contains("9.50% p.a.");
        assertThat(text).contains("INR 16,801.32");
        assertThat(text).contains("31.53%");
    }

    @Test
    @DisplayName("Scenario 3: Long Customer Name and Lengthy Address Non-Truncation")
    void testLongCustomerNameAndAddress() throws Exception {
        String longName = "Dr. Padmanabhaswamy Venkatanarasimha Raju Ramachandran Thekkedath";
        String longAddress = "Villa No. 42B, Block 7, Prestige Golfshire Gated Community, Nandi Hills International Airport Corridor, Devanahalli Rural District, Bengaluru North, Karnataka - 562110";

        Customer customer = createSampleCustomer(longName, "9998887776",
                LocalDate.of(1980, 8, 10), longAddress);
        LoanApplication app = createSampleApplication("APP-2026-00303", ProductType.PERSONAL,
                new BigDecimal("500000.00"), 48, new BigDecimal("12.00"),
                new BigDecimal("150000.00"), new BigDecimal("20000.00"),
                new BigDecimal("13166.89"), new BigDecimal("22.11"),
                ApplicationStatus.CREDIT_APPROVAL);

        byte[] pdfBytes = pdfGenerationService.generateApplicationFormPdf(app, customer);
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        PdfReader reader = new PdfReader(pdfBytes);
        assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        reader.close();

        String text = extractAllText(pdfBytes);
        assertThat(text).contains(longName);
        assertThat(text).contains("Prestige Golfshire Gated Community");
        assertThat(text).contains("562110");
    }

    @Test
    @DisplayName("Scenario 4: Different Workflow Statuses (DRAFT, UNDER_REVIEW, REJECTED, EXIT)")
    void testDifferentApplicationStatuses() throws Exception {
        Customer customer = createSampleCustomer("Rohan Sen", "9811223344",
                LocalDate.of(1992, 1, 1), "Park Street, Kolkata");

        // 4a: DRAFT - No approved amount yet, shows N/A (Pending Sanction)
        LoanApplication draftApp = createSampleApplication("APP-DRAFT-01", ProductType.PERSONAL,
                new BigDecimal("100000.00"), 12, new BigDecimal("12.00"),
                new BigDecimal("40000.00"), BigDecimal.ZERO,
                new BigDecimal("8884.88"), new BigDecimal("22.21"),
                ApplicationStatus.DRAFT);
        draftApp.setApprovedAmount(null);
        draftApp.setApprovedAt(null);
        draftApp.setSubmittedAt(null); // unsubmitted
        byte[] draftPdf = pdfGenerationService.generateApplicationFormPdf(draftApp, customer);
        String draftText = extractAllText(draftPdf);
        assertThat(draftText).contains("DRAFT");
        assertThat(draftText).contains("N/A (Pending Sanction)");

        // 4b: UNDER_REVIEW
        LoanApplication underReviewApp = createSampleApplication("APP-REVIEW-02", ProductType.PERSONAL,
                new BigDecimal("150000.00"), 24, new BigDecimal("12.00"),
                new BigDecimal("45000.00"), BigDecimal.ZERO,
                new BigDecimal("7061.02"), new BigDecimal("15.69"),
                ApplicationStatus.UNDER_REVIEW);
        underReviewApp.setApprovedAmount(null);
        byte[] reviewPdf = pdfGenerationService.generateApplicationFormPdf(underReviewApp, customer);
        String reviewText = extractAllText(reviewPdf);
        assertThat(reviewText).contains("UNDER_REVIEW");
        assertThat(reviewText).contains("N/A (Pending Sanction)");

        // 4c: REJECTED - Rejection remarks must appear
        LoanApplication rejectedApp = createSampleApplication("APP-REJECT-03", ProductType.PERSONAL,
                new BigDecimal("400000.00"), 12, new BigDecimal("12.00"),
                new BigDecimal("30000.00"), new BigDecimal("10000.00"),
                new BigDecimal("35539.52"), new BigDecimal("151.80"),
                ApplicationStatus.REJECTED);
        rejectedApp.setApprovedAmount(null);
        rejectedApp.setReviewerCode("REV-009");
        rejectedApp.setDecisionReason("FOIR exceeds 50.00% benchmark and negative obligation history.");
        byte[] rejectedPdf = pdfGenerationService.generateApplicationFormPdf(rejectedApp, customer);
        String rejectedText = extractAllText(rejectedPdf);
        assertThat(rejectedText).contains("REJECTED");
        assertThat(rejectedText).contains("FOIR exceeds 50.00% benchmark");

        // 4d: EXIT
        LoanApplication exitApp = createSampleApplication("APP-EXIT-04", ProductType.PERSONAL,
                new BigDecimal("200000.00"), 12, new BigDecimal("12.00"),
                new BigDecimal("50000.00"), BigDecimal.ZERO,
                new BigDecimal("17769.76"), new BigDecimal("35.54"),
                ApplicationStatus.EXIT);
        byte[] exitPdf = pdfGenerationService.generateApplicationFormPdf(exitApp, customer);
        String exitText = extractAllText(exitPdf);
        assertThat(exitText).contains("EXIT");
        assertThat(exitText).contains("INR 2,00,000.00");
    }

    @Test
    @DisplayName("Scenario 5: Nonexistent Application ID handled via Service & Controller")
    void testNonexistentApplicationId() {
        Long nonExistentId = 999999L;
        when(loanApplicationService.getApplicationById(nonExistentId))
                .thenThrow(new ApplicationNotFoundException("Loan application not found with ID: " + nonExistentId));

        assertThatThrownBy(() -> creditApprovalController.downloadApplicationFormPdf(nonExistentId))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining("Loan application not found with ID: 999999");
    }

    @Test
    @DisplayName("Scenario 6: Null and Incomplete Application / Customer Data Handling")
    void testIncompleteAndNullDataGracefulHandling() throws Exception {
        // 6a: Null application throws IllegalArgumentException
        assertThatThrownBy(() -> pdfGenerationService.generateApplicationFormPdf(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loan application cannot be null");

        // 6b: Application with completely null customer generates valid PDF with N/A placeholders
        LoanApplication app = createSampleApplication("APP-NOCUST-01", ProductType.PERSONAL,
                new BigDecimal("100000.00"), 12, new BigDecimal("12.00"),
                new BigDecimal("30000.00"), BigDecimal.ZERO,
                new BigDecimal("8884.88"), new BigDecimal("29.62"),
                ApplicationStatus.DRAFT);
        app.setCustomer(null);

        byte[] pdfBytes = pdfGenerationService.generateApplicationFormPdf(app, null);
        assertThat(pdfBytes).isNotNull().isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        String text = extractAllText(pdfBytes);
        assertThat(text).contains("APP-NOCUST-01");
        assertThat(text).contains("N/A"); // Fallback for customer name, address, DOB
    }

    @Test
    @DisplayName("Scenario 7: File System Write and Integrity Verification in Temp Directory")
    void testPhysicalFileWriteAndReadability(@TempDir Path tempDir) throws Exception {
        Customer customer = createSampleCustomer("Vikram Singhania", "9822001122",
                LocalDate.of(1988, 11, 5), "Nariman Point, Marine Drive, Mumbai");
        LoanApplication app = createSampleApplication("APP-TEMP-TEST", ProductType.PERSONAL,
                new BigDecimal("300000.00"), 24, new BigDecimal("12.00"),
                new BigDecimal("80000.00"), new BigDecimal("5000.00"),
                new BigDecimal("14122.04"), new BigDecimal("23.90"),
                ApplicationStatus.CREDIT_APPROVAL);

        byte[] pdfBytes = pdfGenerationService.generateApplicationFormPdf(app, customer);

        Path pdfFilePath = tempDir.resolve("LoanSphere_Application_APP-TEMP-TEST.pdf");
        Files.write(pdfFilePath, pdfBytes);

        // Also save a copy to target/ for manual evaluator review
        Path targetSample = Path.of("target", "sample_application_form.pdf");
        if (Files.exists(Path.of("target"))) {
            Files.write(targetSample, pdfBytes);
        }

        // Verify file on disk
        assertThat(Files.exists(pdfFilePath)).isTrue();
        assertThat(Files.size(pdfFilePath)).isGreaterThan(1000);

        // Re-read file with PdfReader from disk
        PdfReader diskReader = new PdfReader(pdfFilePath.toString());
        assertThat(diskReader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        assertThat(diskReader.getPdfVersion()).isIn('4', '5'); // PDF-1.4 or 1.5 standard
        diskReader.close();
    }

    @Test
    @DisplayName("Scenario 8: CreditApprovalController End-to-End PDF Download Endpoint")
    void testCreditApprovalControllerDownloadEndpoint() {
        Customer customer = createSampleCustomer("Sneha Kapoor", "9777666555",
                LocalDate.of(1996, 4, 12), "Koramangala 5th Block, Bengaluru");
        LoanApplication app = createSampleApplication("APP-2026-END2END", ProductType.PERSONAL,
                new BigDecimal("200000.00"), 24, new BigDecimal("12.00"),
                new BigDecimal("55000.00"), BigDecimal.ZERO,
                new BigDecimal("9414.69"), new BigDecimal("17.12"),
                ApplicationStatus.CREDIT_APPROVAL);
        app.setCustomer(customer);

        when(loanApplicationService.getApplicationById(101L)).thenReturn(app);

        ResponseEntity<byte[]> response = creditApprovalController.downloadApplicationFormPdf(101L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"ApplicationForm_APP-2026-END2END.pdf\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(new String(response.getBody(), 0, 5)).isEqualTo("%PDF-");
    }
}
