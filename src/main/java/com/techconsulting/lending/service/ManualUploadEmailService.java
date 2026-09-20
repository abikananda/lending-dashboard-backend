package com.techconsulting.lending.service;

import com.techconsulting.lending.dto.DashboardSummary;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class ManualUploadEmailService {
    private static final Logger log = LoggerFactory.getLogger(ManualUploadEmailService.class);
    private static final Locale INDIA = Locale.of("en", "IN");
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public ManualUploadEmailService(JavaMailSender mailSender,
            @Value("${app.upload-notification.enabled:false}") boolean enabled,
            @Value("${app.upload-notification.from:${spring.mail.username:}}") String from) {
        this.mailSender=mailSender; this.enabled=enabled; this.from=from;
    }

    @Async("uploadEmailExecutor")
    public void send(ManualUploadNotification event) {
        if (!enabled) {
            log.info("Manual upload email notification is disabled for batch {}",event.batchId());
            return;
        }
        try {
            MimeMessage message=mailSender.createMimeMessage();
            MimeMessageHelper helper=new MimeMessageHelper(message,true, StandardCharsets.UTF_8.name());
            if(from!=null&&!from.isBlank()) helper.setFrom(from);
            helper.setTo(event.recipient());
            helper.setSubject("LenDenClub report uploaded: " + event.filename());
            helper.setText(body(event),htmlBody(event));
            mailSender.send(message);
            log.info("Manual upload email sent for batch {}",event.batchId());
        } catch (Exception ex) {
            log.error("Unable to send manual upload email for batch {}",event.batchId(),ex);
        }
    }

    String htmlBody(ManualUploadNotification event) {
        StringBuilder rows=new StringBuilder();
        metricRow(rows,"Investment principal",event.before().investmentPrincipal(),event.after().investmentPrincipal());
        metricRow(rows,"Total amount lent",event.before().totalAmountLent(),event.after().totalAmountLent());
        metricRow(rows,"Total amount received",event.before().totalAmountReceived(),event.after().totalAmountReceived());
        metricRow(rows,"Interest earned",event.before().interestEarned(),event.after().interestEarned());
        metricRow(rows,"Outstanding principal",event.before().outstandingPrincipal(),event.after().outstandingPrincipal());
        metricRow(rows,"Available to invest",event.before().amountAvailableToInvest(),event.after().amountAvailableToInvest());
        metricRow(rows,"NPA amount",event.before().npaAmount(),event.after().npaAmount());
        metricRow(rows,"Probable NPA amount",event.before().probableNpaAmount(),event.after().probableNpaAmount());
        metricRow(rows,"Principal loss",event.before().principalLoss(),event.after().principalLoss());
        countRow(rows,"Active loans",event.before().activeLoans(),event.after().activeLoans());
        countRow(rows,"Closed loans",event.before().closedLoans(),event.after().closedLoans());
        countRow(rows,"NPA count",event.before().npaLoans(),event.after().npaLoans());
        countRow(rows,"Probable NPA count",event.before().probableNpaLoans(),event.after().probableNpaLoans());

        String health=blank(event.after().portfolioHealth(),"Unknown");
        String healthColor=healthColor(health);
        String npaSection=npaSection(event);

        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f1f5f9;padding:24px 8px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                             style="max-width:760px;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 8px 28px rgba(15,23,42,.10);">
                        <tr>
                          <td style="background:#2563eb;padding:28px 32px;color:#ffffff;">
                            <div style="font-size:12px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;opacity:.85;">LenDenClub Portfolio</div>
                            <div style="font-size:26px;font-weight:700;margin-top:8px;">Report upload completed</div>
                            <div style="font-size:14px;margin-top:8px;opacity:.9;">Your dashboard has been refreshed successfully.</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:26px 32px 10px;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                                   style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:12px;">
                              <tr>
                                <td style="padding:16px 18px;">
                                  <div style="font-size:12px;color:#64748b;text-transform:uppercase;font-weight:700;">Uploaded file</div>
                                  <div style="font-size:15px;font-weight:700;color:#1e3a8a;margin-top:5px;word-break:break-word;">%s</div>
                                </td>
                                <td width="120" style="padding:16px 18px;border-left:1px solid #bfdbfe;">
                                  <div style="font-size:12px;color:#64748b;text-transform:uppercase;font-weight:700;">Batch</div>
                                  <div style="font-size:20px;font-weight:700;color:#1e3a8a;margin-top:5px;">#%s</div>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:18px 32px 8px;">
                            <div style="font-size:19px;font-weight:700;">Dashboard statistics</div>
                            <div style="font-size:13px;color:#64748b;margin-top:4px;">Values before and after this upload</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:8px 32px 20px;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                                   style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;border-collapse:separate;border-spacing:0;">
                              <thead>
                                <tr style="background:#0f172a;color:#ffffff;">
                                  <th align="left" style="padding:12px 14px;font-size:12px;">Metric</th>
                                  <th align="right" style="padding:12px 14px;font-size:12px;">Before</th>
                                  <th align="right" style="padding:12px 14px;font-size:12px;">After</th>
                                  <th align="right" style="padding:12px 14px;font-size:12px;">Change</th>
                                </tr>
                              </thead>
                              <tbody>%s</tbody>
                            </table>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:0 32px 22px;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                              <tr>
                                <td style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px 18px;">
                                  <span style="font-size:13px;color:#64748b;">Portfolio health</span>
                                  <span style="display:inline-block;margin-left:10px;padding:6px 11px;border-radius:999px;background:%s;color:#ffffff;font-size:13px;font-weight:700;">%s</span>
                                  <span style="font-size:12px;color:#64748b;margin-left:8px;">Previously: %s</span>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        %s
                        <tr>
                          <td style="padding:20px 32px;background:#f8fafc;color:#64748b;font-size:12px;text-align:center;">
                            Automated portfolio notification · Batch #%s
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escape(event.filename()),escape(String.valueOf(event.batchId())),rows,
                healthColor,escape(health),escape(blank(event.before().portfolioHealth(),"Unknown")),
                npaSection,escape(String.valueOf(event.batchId())));
    }

    String body(ManualUploadNotification event) {
        StringBuilder value=new StringBuilder("Manual lending report upload completed.\n\n")
                .append("File: ").append(event.filename()).append("\nBatch: ").append(event.batchId()).append("\n\n")
                .append("Dashboard statistics\n")
                .append(String.format("%-28s %18s %18s%n","Metric","Before","After"));
        metric(value,"Investment principal",event.before().investmentPrincipal(),event.after().investmentPrincipal());
        metric(value,"Total amount lent",event.before().totalAmountLent(),event.after().totalAmountLent());
        metric(value,"Total amount received",event.before().totalAmountReceived(),event.after().totalAmountReceived());
        metric(value,"Interest earned",event.before().interestEarned(),event.after().interestEarned());
        metric(value,"Outstanding principal",event.before().outstandingPrincipal(),event.after().outstandingPrincipal());
        metric(value,"Available to invest",event.before().amountAvailableToInvest(),event.after().amountAvailableToInvest());
        metric(value,"NPA amount",event.before().npaAmount(),event.after().npaAmount());
        metric(value,"Probable NPA amount",event.before().probableNpaAmount(),event.after().probableNpaAmount());
        metric(value,"Principal loss",event.before().principalLoss(),event.after().principalLoss());
        count(value,"Active loans",event.before().activeLoans(),event.after().activeLoans());
        count(value,"Closed loans",event.before().closedLoans(),event.after().closedLoans());
        count(value,"NPA count",event.before().npaLoans(),event.after().npaLoans());
        count(value,"Probable NPA count",event.before().probableNpaLoans(),event.after().probableNpaLoans());
        value.append("\nPortfolio health: ").append(event.before().portfolioHealth()).append(" -> ")
                .append(event.after().portfolioHealth()).append("\n\nNew borrowers moved to NPA: ")
                .append(event.newNpaBorrowers().size()).append("\n");
        for(var npa:event.newNpaBorrowers()) value.append("\n- ").append(blank(npa.borrowerName(),"Unknown borrower"))
                .append(" | Loan: ").append(blank(npa.loanId(),"-")).append(" | Scheme: ")
                .append(blank(npa.schemeId(),"-")).append("\n  Invested: ").append(money(npa.investedAmount()))
                .append(" | Principal received: ").append(money(npa.principalReceived()))
                .append(" | NPA amount: ").append(money(npa.npaAmount()))
                .append(" | Reason: ").append(blank(npa.reason(),"-")).append("\n");
        return value.toString();
    }

    private String npaSection(ManualUploadNotification event) {
        if(event.newNpaBorrowers().isEmpty()) {
            return """
                    <tr><td style="padding:0 32px 28px;">
                      <div style="background:#ecfdf5;border:1px solid #a7f3d0;color:#166534;border-radius:12px;padding:15px 18px;font-size:14px;font-weight:700;">
                        No new borrowers moved to NPA in this upload.
                      </div>
                    </td></tr>
                    """;
        }
        StringBuilder rows=new StringBuilder();
        for(var npa:event.newNpaBorrowers()) {
            rows.append("<tr>")
                    .append(cell(blank(npa.borrowerName(),"Unknown borrower"),false))
                    .append(cell(blank(npa.loanId(),"-"),false))
                    .append(cell(money(npa.investedAmount()),true))
                    .append(cell(money(npa.principalReceived()),true))
                    .append(cell(money(npa.npaAmount()),true))
                    .append(cell(blank(npa.reason(),"-"),false))
                    .append("</tr>");
        }
        return """
                <tr><td style="padding:0 32px 28px;">
                  <div style="font-size:19px;font-weight:700;color:#be123c;margin-bottom:10px;">New borrowers moved to NPA (%d)</div>
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                         style="border:1px solid #fecdd3;border-radius:12px;overflow:hidden;border-collapse:separate;border-spacing:0;">
                    <thead><tr style="background:#be123c;color:#ffffff;">
                      <th align="left" style="padding:10px;font-size:11px;">Borrower</th>
                      <th align="left" style="padding:10px;font-size:11px;">Loan</th>
                      <th align="right" style="padding:10px;font-size:11px;">Invested</th>
                      <th align="right" style="padding:10px;font-size:11px;">Principal received</th>
                      <th align="right" style="padding:10px;font-size:11px;">NPA amount</th>
                      <th align="left" style="padding:10px;font-size:11px;">Reason</th>
                    </tr></thead>
                    <tbody>%s</tbody>
                  </table>
                </td></tr>
                """.formatted(event.newNpaBorrowers().size(),rows);
    }

    private void metricRow(StringBuilder b,String name,BigDecimal before,BigDecimal after) {
        BigDecimal safeBefore=zero(before),safeAfter=zero(after);
        tableRow(b,name,money(safeBefore),money(safeAfter),signedMoney(safeAfter.subtract(safeBefore)));
    }

    private void countRow(StringBuilder b,String name,long before,long after) {
        long delta=after-before;
        tableRow(b,name,NumberFormat.getIntegerInstance(INDIA).format(before),
                NumberFormat.getIntegerInstance(INDIA).format(after),(delta>0?"+":"")+delta);
    }

    private void tableRow(StringBuilder b,String name,String before,String after,String change) {
        boolean changed=!change.equals("₹0.00")&&!change.equals("0");
        b.append("<tr style=\"background:#ffffff;\">")
                .append("<td style=\"padding:11px 14px;border-bottom:1px solid #e2e8f0;font-size:13px;font-weight:600;\">")
                .append(escape(name)).append("</td>")
                .append(numberCell(before,false)).append(numberCell(after,true))
                .append("<td align=\"right\" style=\"padding:11px 14px;border-bottom:1px solid #e2e8f0;font-size:12px;font-weight:700;color:")
                .append(changed?"#2563eb":"#64748b").append(";\">").append(escape(change)).append("</td></tr>");
    }

    private String numberCell(String value,boolean emphasized) {
        return "<td align=\"right\" style=\"padding:11px 14px;border-bottom:1px solid #e2e8f0;font-size:13px;"
                +(emphasized?"font-weight:700;color:#0f172a;":"color:#475569;")+"\">"+escape(value)+"</td>";
    }

    private String cell(String value,boolean right) {
        return "<td align=\""+(right?"right":"left")+"\" style=\"padding:10px;border-bottom:1px solid #ffe4e6;font-size:12px;color:#334155;\">"
                +escape(value)+"</td>";
    }

    private void metric(StringBuilder b,String name,BigDecimal before,BigDecimal after) {
        b.append(String.format("%-28s %18s %18s%n",name,money(before),money(after)));
    }
    private void count(StringBuilder b,String name,long before,long after) {
        b.append(String.format("%-28s %18d %18d%n",name,before,after));
    }
    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(INDIA).format(zero(value));
    }
    private String signedMoney(BigDecimal value) {
        if(value.signum()==0) return money(value);
        return (value.signum()>0?"+":"-")+money(value.abs());
    }
    private BigDecimal zero(BigDecimal value) { return value==null?BigDecimal.ZERO:value; }
    private String blank(String value,String fallback) { return value==null||value.isBlank()?fallback:value; }
    private String escape(String value) { return HtmlUtils.htmlEscape(value==null?"":value); }
    private String healthColor(String health) {
        if("Critical".equalsIgnoreCase(health)) return "#e11d48";
        if("Needs attention".equalsIgnoreCase(health)) return "#d97706";
        if("Healthy".equalsIgnoreCase(health)) return "#16a34a";
        return "#64748b";
    }
}
