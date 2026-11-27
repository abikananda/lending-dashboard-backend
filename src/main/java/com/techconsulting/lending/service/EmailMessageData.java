package com.techconsulting.lending.service;

import java.time.Instant;

public record EmailMessageData(String messageId, String sender, String subject,
                               Instant receivedAt, String body) { }
