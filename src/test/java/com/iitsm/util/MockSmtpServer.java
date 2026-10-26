package com.iitsm.util;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;

/**
 * Lightweight in-memory SMTP server runner for local evaluation without Docker.
 * Listens on port 1025 (matching application.yml default) and prints received emails to console.
 */
public class MockSmtpServer {

    public static void main(String[] args) {
        ServerSetup setup = new ServerSetup(1025, null, ServerSetup.PROTOCOL_SMTP);
        GreenMail greenMail = new GreenMail(setup);
        greenMail.start();

        System.out.println("==================================================================");
        System.out.println(">>> Mock SMTP Server (GreenMail) is ACTIVE on port 1025");
        System.out.println(">>> Ready to receive real email dispatches from I-ITSM.");
        System.out.println(">>> Incoming emails will print below in real time.");
        System.out.println(">>> Press Ctrl+C in this terminal to stop the server.");
        System.out.println("==================================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopping Mock SMTP server...");
            greenMail.stop();
        }));

        while (true) {
            try {
                if (greenMail.waitForIncomingEmail(2000, 1)) {
                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    for (MimeMessage msg : messages) {
                        System.out.println("\n-------------------- [EMAIL NOTIFICATION RECEIVED] --------------------");
                        System.out.println("From:    " + Arrays.toString(msg.getFrom()));
                        System.out.println("To:      " + Arrays.toString(msg.getAllRecipients()));
                        System.out.println("Subject: " + msg.getSubject());
                        System.out.println("Date:    " + msg.getSentDate());
                        System.out.println("Body:");
                        System.out.println(msg.getContent());
                        System.out.println("------------------------------------------------------------------------\n");
                    }
                    greenMail.reset();
                }
            } catch (Exception e) {
                // Ignore interruption or parsing issues
            }
        }
    }
}

