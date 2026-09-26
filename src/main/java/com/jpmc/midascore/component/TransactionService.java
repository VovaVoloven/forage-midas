package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Component
public class TransactionService {
    private final UserRepository userRepo;
    private final TransactionRepository transRepo;
    private final RestTemplate restTemplate;
    private final String incentiveUrl;
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(UserRepository userRepo, TransactionRepository transRepo, RestTemplate restTemplate, @Value("${incentive.url}") String incentiveUrl) {
        this.userRepo = userRepo;
        this.transRepo = transRepo;
        this.restTemplate = restTemplate;
        this.incentiveUrl = incentiveUrl;
    }

    @Transactional
    public void processIncoming(Transaction tx){
        Optional<UserRecord> senderOpt = userRepo.findById(tx.getSenderId());
        Optional<UserRecord> recipientOpt = userRepo.findById(tx.getRecipientId());
        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            log.warn("One or Both of sender and recipient are empty!");
            return;
        }

        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        float amount = tx.getAmount();
        if (amount <= 0) {
            log.warn("Amount cannot be less than zero!");
            return;
        }
        if (sender.getBalance() < amount) {
            log.warn("Sender doesn't have enough credits to complete the transaction");
            return;
        }

        float incentive = 0f;
        Incentive resp = restTemplate.postForObject(incentiveUrl, tx, Incentive.class);
        if (resp != null) {
            incentive = Math.max(0f, resp.getAmount());
        }
        else log.warn("No response!");

        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + incentive);
        userRepo.save(sender);
        userRepo.save(recipient);

        TransactionRecord record = new TransactionRecord();
        record.setSender(sender);
        record.setRecipient(recipient);
        record.setAmount(amount);
        record.setIncentive(incentive);
        transRepo.save(record);
    }
}
