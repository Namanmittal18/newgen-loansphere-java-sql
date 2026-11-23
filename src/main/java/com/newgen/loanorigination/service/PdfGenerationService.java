package com.newgen.loanorigination.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.utility.CurrencyFormatter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] generateApplicationFormPdf(LoanApplication app, Customer customer) {
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
            PdfPTable appTable = new PdfPTable(4);
            appTable.setWidthPercentage(100);
            appTable.setSpacingBefore(5);
            appTable.setSpacingAfter(15);

            addCell(appTable, "Application No:", labelFont, true);
            addCell(appTable, app.getApplicationNumber(), valueFont, false);
            addCell(appTable, "Current Status:", labelFont, true);
            addCell(appTable, app.getStatus().name(), valueFont, false);

            addCell(appTable, "Product Type:", labelFont, true);
            addCell(appTable, app.getProductType().name(), valueFont, false);
            addCell(appTable, "Branch:", labelFont, true);
            addCell(appTable, app.getBranchName() + " (" + app.getBranchCode() + ")", valueFont, false);

            addCell(appTable, "Submission Date:", labelFont, true);
            addCell(appTable, app.getSubmittedAt() != null ? app.getSubmittedAt().format(DATE_TIME_FORMATTER) : "N/A", valueFont, false);
            addCell(appTable, "Approval Date:", labelFont, true);
            addCell(appTable, app.getApprovedAt() != null ? app.getApprovedAt().format(DATE_TIME_FORMATTER) : "N/A", valueFont, false);

            document.add(appTable);

            // Table 2: Customer Profile
            document.add(new Paragraph("2. Applicant Information", sectionFont));
            PdfPTable custTable = new PdfPTable(4);
            custTable.setWidthPercentage(100);
            custTable.setSpacingBefore(5);
            custTable.setSpacingAfter(15);

            addCell(custTable, "Customer Name:", labelFont, true);
            addCell(custTable, customer != null ? customer.getFullName() : "N/A", valueFont, false);
            addCell(custTable, "Customer Code:", labelFont, true);
            addCell(custTable, customer != null ? customer.getCustomerCode() : "N/A", valueFont, false);

            addCell(custTable, "Date of Birth:", labelFont, true);
            addCell(custTable, customer != null && customer.getDateOfBirth() != null ? customer.getDateOfBirth().format(DATE_FORMATTER) : "N/A", valueFont, false);
            addCell(custTable, "Mobile Number:", labelFont, true);
            addCell(custTable, customer != null ? customer.getMobileNumber() : "N/A", valueFont, false);

            addCell(custTable, "KYC Type & Ref:", labelFont, true);
            addCell(custTable, customer != null ? customer.getKycType() + ": " + customer.getKycReference() : "N/A", valueFont, false);
            addCell(custTable, "KYC Status:", labelFont, true);
            addCell(custTable, customer != null ? customer.getKycStatus() : "N/A", valueFont, false);

            addCell(custTable, "Residential Address:", labelFont, true);
            PdfPCell addrCell = new PdfPCell(new Phrase(customer != null ? customer.getFullAddress() : "N/A", valueFont));
            addrCell.setColspan(3);
            addrCell.setPadding(5);
            custTable.addCell(addrCell);

            document.add(custTable);

            // Table 3: Financial Terms & Affordability
            document.add(new Paragraph("3. Financial Evaluation & Terms", sectionFont));
            PdfPTable finTable = new PdfPTable(4);
            finTable.setWidthPercentage(100);
            finTable.setSpacingBefore(5);
            finTable.setSpacingAfter(15);

            addCell(finTable, "Requested Amount:", labelFont, true);
            addCell(finTable, CurrencyFormatter.formatInr(app.getRequestedAmount()), valueFont, false);
            addCell(finTable, "Sanctioned Amount:", labelFont, true);
            addCell(finTable, CurrencyFormatter.formatInr(app.getApprovedAmount() != null ? app.getApprovedAmount() : app.getRequestedAmount()), valueFont, false);

            addCell(finTable, "Tenure:", labelFont, true);
            addCell(finTable, app.getTenureMonths() + " Months", valueFont, false);
            addCell(finTable, "Interest Rate:", labelFont, true);
            addCell(finTable, app.getAnnualInterestRate() + "% p.a.", valueFont, false);

            addCell(finTable, "Monthly Net Income:", labelFont, true);
            addCell(finTable, CurrencyFormatter.formatInr(app.getMonthlyNetIncome()), valueFont, false);
            addCell(finTable, "Existing Obligations:", labelFont, true);
            addCell(finTable, CurrencyFormatter.formatInr(app.getExistingMonthlyObligations()), valueFont, false);

            addCell(finTable, "Calculated EMI:", labelFont, true);
            addCell(finTable, CurrencyFormatter.formatInr(app.getProposedEmi()), valueFont, false);
            addCell(finTable, "Calculated FOIR:", labelFont, true);
            addCell(finTable, app.getFoirPercentage() + "% (Limit: 50.00%)", valueFont, false);

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
            addCell(apprvTable, app.getDecisionReason() != null ? app.getDecisionReason() : "Approved per credit policy guidelines.", valueFont, false);

            document.add(apprvTable);

            // Signatures block
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setSpacingBefore(30);

            PdfPCell sigApplicant = new PdfPCell(new Paragraph("___________________________\nApplicant Signature\n(" + (customer != null ? customer.getFullName() : "Applicant") + ")", valueFont));
            sigApplicant.setBorder(Rectangle.NO_BORDER);
            sigApplicant.setHorizontalAlignment(Element.ALIGN_CENTER);
            sigTable.addCell(sigApplicant);

            PdfPCell sigOfficer = new PdfPCell(new Paragraph("___________________________\nCredit Officer / Sanction Authority\n(Code: " + (app.getReviewerCode() != null ? app.getReviewerCode() : "OFFICER") + ")", valueFont));
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

    private void addCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(new Color(245, 247, 250));
        }
        table.addCell(cell);
    }
}
