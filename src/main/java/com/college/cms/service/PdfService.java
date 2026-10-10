package com.college.cms.service;

import com.college.cms.model.Payment;
import com.college.cms.model.Student;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] feeReceipt(Payment p) {
        try {
            Student s = p.getStudent();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font sub = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph h = new Paragraph("CampusCore College", title);
            h.setAlignment(Element.ALIGN_CENTER);
            doc.add(h);
            Paragraph h2 = new Paragraph("FEE RECEIPT", sub);
            h2.setAlignment(Element.ALIGN_CENTER);
            h2.setSpacingAfter(16);
            doc.add(h2);

            PdfPTable t = new PdfPTable(new float[]{1.2f, 2f});
            t.setWidthPercentage(100);
            row(t, "Receipt No", p.getReceiptNo(), bold, normal);
            row(t, "Date", p.getPaidOn().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), bold, normal);
            row(t, "Student", s.getUser().getFullName(), bold, normal);
            row(t, "ID Card No", s.getIdCardNo(), bold, normal);
            row(t, "Branch / Year", s.getBranch() + " / Year " + s.getYear(), bold, normal);
            row(t, "Payment Mode", p.getPayMode() == null ? "-" : p.getPayMode(), bold, normal);
            row(t, "Amount Paid", "Rs. " + p.getAmount().toPlainString(), bold, bold);
            row(t, "Total Fees", "Rs. " + s.getTotalFees().toPlainString(), bold, normal);
            row(t, "Pending Fees (Current)", "Rs. " + s.getPendingFees().toPlainString(), bold, normal);
            doc.add(t);

            Paragraph foot = new Paragraph("This is a computer generated receipt.", sub);
            foot.setSpacingBefore(24);
            foot.setAlignment(Element.ALIGN_CENTER);
            doc.add(foot);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF banane me error", e);
        }
    }

    private void row(PdfPTable t, String k, String v, Font fk, Font fv) {
        PdfPCell a = new PdfPCell(new Phrase(k, fk));
        PdfPCell b = new PdfPCell(new Phrase(v, fv));
        a.setPadding(7);
        b.setPadding(7);
        t.addCell(a);
        t.addCell(b);
    }
}