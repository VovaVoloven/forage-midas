package com.jpmc.midascore;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.component.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KafkaListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaListener.class);

    private final TransactionService transactionService;

    public KafkaListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @org.springframework.kafka.annotation.KafkaListener(topics = "${general.kafka-topic}")
    public void onMessage(Transaction tx){
        log.info("Received Transaction: {}", tx);
        transactionService.processIncoming(tx);
    }
}
