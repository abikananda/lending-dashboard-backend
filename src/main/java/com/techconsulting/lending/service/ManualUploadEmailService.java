package com.techconsulting.lending.service;

import com.techconsulting.lending.dto.DashboardSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        if (!enabled) { log.info("Manual upload email notification is disabled for batch {}",event.batchId()); return; }
        try {
            SimpleMailMessage message=new SimpleMailMessage();
            if(from!=null&&!from.isBlank()) message.setFrom(from);
            message.setTo(event.recipient());
            message.setSubject("LenDenClub report uploaded: " + event.filename());
            message.setText(body(event));
            mailSender.send(message);
            log.info("Manual upload email sent for batch {}",event.batchId());
        } catch (RuntimeException ex) {
            log.error("Unable to send manual upload email for batch {}",event.batchId(),ex);
        }
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

    private void metric(StringBuilder b,String name,BigDecimal before,BigDecimal after) {
        b.append(String.format("%-28s %18s %18s%n",name,money(before),money(after)));
    }
    private void count(StringBuilder b,String name,long before,long after) {
        b.append(String.format("%-28s %18d %18d%n",name,before,after));
    }
    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(INDIA).format(value==null?BigDecimal.ZERO:value);
    }
    private String blank(String value,String fallback) { return value==null||value.isBlank()?fallback:value; }
}
