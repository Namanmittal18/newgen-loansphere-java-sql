package com.newgen.loanorigination.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.utility.CurrencyFormatter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] generateApplicationFormPdf(LoanApplication app, Customer customer) {
        if (app == null) {
            throw new IllegalArgumentException("Loan application cannot be null");
        }
        if (customer == null && app.getCustomer() != null) {
            customer = app.getCustomer();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 32, 67));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 32, 67));
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            // Header Banner
            Paragraph title = new Paragraph("LoanSphere | Loan Origination System", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph sub = new Paragraph("OFFICIAL LOAN APPLICATION & CREDIT SANCTION FORM\nNewgen Java & SQL Campus Pre-Onboarding Project", subTitleFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15);
            document.add(sub);

            // Table 1: Application Overview
            document.add(new Paragraph("1. Application Summary", sectionFont));
            PdfPTable appTable = new PdfPTable(new float[]{20f, 30f, 20f, 30f});
            appTable.setWidthPercentage(100);
            appTable.setSpacingBefore(5);
            appTable.setSpacingAfter(15);

            addCell(appTable, "Application No:", labelFont, true);
            addCell(appTable, app.getApplicationNumber() != null ? app.getApplicationNumber() : "N/A", valueFont, false);
            addCell(appTable, "Current Status:", labelFont, true);
            addCell(appTable, app.getStatus() != null ? app.getStatus().name() : "N/A", valueFont, false);

            addCell(appTable, "Product Type:", labelFont, true);
            addCell(appTable, app.getProductType() != null ? app.getProductType().name() : "N/A", valueFont, false);
            addCell(appTable, "Branch:", labelFont, true);
            String branch = (app.getBranchName() != null ? app.getBranchName() : "Main Branch") 
                    + " (" + (app.getBranchCode() != null ? app.getBranchCode() : "BR001") + ")";
            addCell(appTable, branch, valueFont, false);

            addCell(appTable, "Application Date:", labelFont, true);
            String appDate = app.getSubmittedAt() != null 
                    ? app.getSubmittedAt().format(DATE_TIME_FORMATTER) 
                    : (app.getCreatedAt() != null ? app.getCreatedAt().format(DATE_TIME_FORMATTER) : "N/A");
            addCell(appTable, appDate, valueFont, false);

            addCell(appTable, "Approval Date:", labelFont, true);
            addCell(appTable, app.getApprovedAt() != null ? app.getApprovedAt().format(DATE_TIME_FORMATTER) : "N/A", valueFont, false);

            document.add(appTable);

            // Table 2: Customer Profile
            document.add(new Paragraph("2. Applicant Information", sectionFont));
            PdfPTable custTable = new PdfPTable(new float[]{20f, 30f, 20f, 30f});
            custTable.setWidthPercentage(100);
            custTable.setSpacingBefore(5);
            custTable.setSpacingAfter(15);

            addCell(custTable, "Customer Name:", labelFont, true);
            addCell(custTable, customer != null && customer.getFullName() != null ? customer.getFullName() : "N/A", valueFont, false);
            addCell(custTable, "Customer Code:", labelFont, true);
            addCell(custTable, customer != null && customer.getCustomerCode() != null ? customer.getCustomerCode() : "N/A", valueFont, false);

            addCell(custTable, "Date of Birth & Age:", labelFont, true);
            String dobAndAge = "N/A";
            if (customer != null && customer.getDateOfBirth() != null) {
                LocalDate refDate = (app.getSubmittedAt() != null)
                        ? app.getSubmittedAt().toLocalDate()
                        : (app.getCreatedAt() != null ? app.getCreatedAt().toLocalDate() : LocalDate.now());
                int age = Period.between(customer.getDateOfBirth(), refDate).getYears();
                dobAndAge = customer.getDateOfBirth().format(DATE_FORMATTER) + " (Age: " + age + " years)";
            }
            addCell(custTable, dobAndAge, valueFont, false);

            addCell(custTable, "Mobile Number:", labelFont, true);
            addCell(custTable, customer != null && customer.getMobileNumber() != null ? customer.getMobileNumber() : "N/A", valueFont, false);

            addCell(custTable, "KYC Type & Ref:", labelFont, true);
            String kycInfo = (customer != null && customer.getKycType() != null ? customer.getKycType() : "PAN")
                    + ": " + (customer != null && customer.getKycReference() != null ? customer.getKycReference() : "N/A");
            addCell(custTable, kycInfo, valueFont, false);

            addCell(custTable, "KYC Status:", labelFont, true);
            addCell(custTable, customer != null && customer.getKycStatus() != null ? customer.getKycStatus() : "VERIFIED", valueFont, false);

            addCell(custTable, "Residential Address:", labelFont, true);
            String addressText = customer != null && customer.getFullAddress() != null && !customer.getFullAddress().isBlank()
                    ? customer.getFullAddress()
                    : "N/A";
            PdfPCell addrCell = new PdfPCell(new Phrase(addressText, valueFont));
            addrCell.setColspan(3);
            addrCell.setPadding(5);
            custTable.addCell(addrCell);

            document.add(custTable);

            // Table 3: Financial Terms & Affordability
            document.add(new Paragraph("3. Financial Evaluation & Terms", sectionFont));
            PdfPTable finTable = new PdfPTable(new float[]{22f, 28f, 22f, 28f});
            finTable.setWidthPercentage(100);
            finTable.setSpacingBefore(5);
            finTable.setSpacingAfter(15);

            addCell(finTable, "Requested Amount:", labelFont, true);
            addCell(finTable, formatCurrency(app.getRequestedAmount()), valueFont, false);

            addCell(finTable, "Approved Amount:", labelFont, true);
            String approvedAmtStr;
            if (app.getApprovedAmount() != null) {
                approvedAmtStr = formatCurrency(app.getApprovedAmount());
            } else if (app.getStatus() == ApplicationStatus.CREDIT_APPROVAL
                    || app.getStatus() == ApplicationStatus.DISBURSED
                    || app.getStatus() == ApplicationStatus.EXIT) {
                approvedAmtStr = formatCurrency(app.getRequestedAmount());
            } else {
                approvedAmtStr = "N/A (Pending Sanction)";
            }
            addCell(finTable, approvedAmtStr, valueFont, false);

            addCell(finTable, "Loan Tenure:", labelFont, true);
            addCell(finTable, (app.getTenureMonths() != null ? app.getTenureMonths() + " Months" : "N/A"), valueFont, false);
            addCell(finTable, "Interest Rate:", labelFont, true);
            addCell(finTable, (app.getAnnualInterestRate() != null ? app.getAnnualInterestRate() + "% p.a." : "N/A"), valueFont, false);

            addCell(finTable, "Monthly Net Income:", labelFont, true);
            addCell(finTable, formatCurrency(app.getMonthlyNetIncome()), valueFont, false);
            addCell(finTable, "Existing Obligations:", labelFont, true);
            addCell(finTable, formatCurrency(app.getExistingMonthlyObligations()), valueFont, false);

            addCell(finTable, "Calculated EMI:", labelFont, true);
            addCell(finTable, formatCurrency(app.getProposedEmi()), valueFont, false);
            addCell(finTable, "FOIR Percentage:", labelFont, true);
            String foirStr = app.getFoirPercentage() != null ? app.getFoirPercentage() + "% (Limit: 50.00%)" : "N/A";
            addCell(finTable, foirStr, valueFont, false);

            document.add(finTable);

            // Table 4: Approval & Signatures
            document.add(new Paragraph("4. Reviewer Approval & Sanction Authority", sectionFont));
            PdfPTable apprvTable = new PdfPTable(2);
            apprvTable.setWidthPercentage(100);
            apprvTable.setSpacingBefore(5);
            apprvTable.setSpacingAfter(20);

            addCell(apprvTable, "Reviewer Code:", labelFont, true);
            addCell(apprvTable, app.getReviewerCode() != null ? app.getReviewerCode() : "N/A", valueFont, false);

            addCell(apprvTable, "Sanction Remarks:", labelFont, true);
            String remarks = app.getDecisionReason() != null 
                    ? app.getDecisionReason() 
                    : (app.getStatus() == ApplicationStatus.REJECTED 
                            ? "Application rejected per underwriting policy." 
                            : "Approved per credit policy guidelines.");
            addCell(apprvTable, remarks, valueFont, false);

            document.add(apprvTable);

            // Signatures block
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setSpacingBefore(30);

            String applicantName = (customer != null && customer.getFullName() != null) ? customer.getFullName() : "Applicant";
            PdfPCell sigApplicant = new PdfPCell(new Paragraph("___________________________\nApplicant Signature\n(" + applicantName + ")", valueFont));
            sigApplicant.setBorder(Rectangle.NO_BORDER);
            sigApplicant.setHorizontalAlignment(Element.ALIGN_CENTER);
            sigTable.addCell(sigApplicant);

            String officerCode = (app.getReviewerCode() != null) ? app.getReviewerCode() : "CREDIT_OFFICER";
            PdfPCell sigOfficer = new PdfPCell(new Paragraph("___________________________\nCredit Officer / Sanction Authority\n(Code: " + officerCode + ")", valueFont));
            sigOfficer.setBorder(Rectangle.NO_BORDER);
            sigOfficer.setHorizontalAlignment(Element.ALIGN_CENTER);
            sigTable.addCell(sigOfficer);

            document.add(sigTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF document: " + e.getMessage(), e);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "INR 0.00";
        }
        return "INR " + CurrencyFormatter.formatIndianNumberOnly(amount);
    }

    private void addCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(new Color(245, 247, 250));
        }
        table.addCell(cell);
    }
}
