package com.techconsulting.lending.service;

import com.techconsulting.lending.dto.DashboardSummary;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManualUploadEmailServiceTest {
    @Test
    void sendsPlainTextAndHtmlAsMultipartEmail() throws Exception {
        JavaMailSender mailSender=mock(JavaMailSender.class);
        MimeMessage message=new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        ManualUploadEmailService service=new ManualUploadEmailService(mailSender,true,"sender@example.com");
        DashboardSummary summary=summary("1000","100","0",0);

        service.send(new ManualUploadNotification(14L,"report.xlsx","recipient@example.com",
                summary,summary,List.of()));

        verify(mailSender).send(message);
        message.saveChanges();
        assertThat(message.getContentType()).startsWith("multipart/mixed");
        assertThat(message.getContent()).isInstanceOf(Multipart.class);
    }

    @Test
    void includesBeforeAfterDashboardStatsAndOnlyNewNpaDetails() {
        ManualUploadEmailService service=new ManualUploadEmailService(mock(JavaMailSender.class),false,"");
        DashboardSummary before=summary("1000","100","0",0);
        DashboardSummary after=summary("1200","130","80",1);
        var npa=new ManualUploadNotification.NewNpaBorrower("Borrower A","L-1","S-1",
                money("500"),money("420"),money("80"),"REPORTED_AS_NPA");

        String body=service.body(new ManualUploadNotification(12L,"report.xlsx","user@example.com",
                before,after,List.of(npa)));

        assertThat(body).contains("Dashboard statistics","Before","After","₹1,000.00","₹1,200.00");
        assertThat(body).contains("New borrowers moved to NPA: 1","Borrower A","L-1","S-1",
                "₹80.00","REPORTED_AS_NPA");

        String html=service.htmlBody(new ManualUploadNotification(12L,"report.xlsx","user@example.com",
                before,after,List.of(npa)));
        assertThat(html).contains("<html>","Report upload completed","Dashboard statistics",
                "<table","Metric","Before","After","Change");
        assertThat(html).contains("New borrowers moved to NPA (1)","Borrower A","L-1",
                "₹500.00","₹420.00","₹80.00","REPORTED_AS_NPA");
        assertThat(html).contains("background:#16a34a","Healthy");
    }

    @Test
    void htmlEmailShowsPositiveMessageWhenNoBorrowerMovesToNpaAndEscapesFilename() {
        ManualUploadEmailService service=new ManualUploadEmailService(mock(JavaMailSender.class),false,"");
        DashboardSummary summary=summary("1000","100","0",0);

        String html=service.htmlBody(new ManualUploadNotification(13L,"report<script>.xlsx",
                "user@example.com",summary,summary,List.of()));

        assertThat(html).contains("No new borrowers moved to NPA in this upload.");
        assertThat(html).contains("report&lt;script&gt;.xlsx").doesNotContain("report<script>.xlsx");
    }

    private DashboardSummary summary(String lent,String interest,String npa,long npaCount) {
        BigDecimal zero=money("0");
        return new DashboardSummary(money("500000"),money(lent),zero,money(interest),zero,zero,zero,
                zero,zero,zero,zero,zero,0,0,npaCount,money(npa),zero,0,zero,zero,"Healthy");
    }
    private BigDecimal money(String value) { return new BigDecimal(value); }
}
