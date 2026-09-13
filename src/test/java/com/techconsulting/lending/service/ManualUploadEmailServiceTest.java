package com.techconsulting.lending.service;

import com.techconsulting.lending.dto.DashboardSummary;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ManualUploadEmailServiceTest {
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
    }

    private DashboardSummary summary(String lent,String interest,String npa,long npaCount) {
        BigDecimal zero=money("0");
        return new DashboardSummary(money("500000"),money(lent),zero,money(interest),zero,zero,zero,
                zero,zero,zero,zero,zero,0,0,npaCount,money(npa),zero,0,zero,zero,"Healthy");
    }
    private BigDecimal money(String value) { return new BigDecimal(value); }
}
