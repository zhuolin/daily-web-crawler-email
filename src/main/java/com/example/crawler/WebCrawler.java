package com.example.crawler;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class WebCrawler {
    private static final int TIMEOUT_MS = 30000;
    private static final int CONTEXT_CHARS = 180;

    public static void main(String[] args) {
        String url = getParameter(args, "url", "CRAWL_URL");
        String namesValue = getParameter(args, "names", "SEARCH_NAMES");

        require(url, "CRAWL_URL / --url");
        require(namesValue, "SEARCH_NAMES / --names");
        validateUrl(url);

        List<String> names = Arrays.stream(namesValue.split("\\|"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        try {
            LocalDateTime checkedAt = LocalDateTime.now(ZoneOffset.UTC);
            System.out.println("Daily Web Crawler");
            System.out.println("UTC: " + checkedAt);
            System.out.println("URL: " + url);

            Document document = Jsoup.connect(url)
                    //.userAgent("DailyWebCrawler/1.0 (+GitHub Actions)")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-AU,en-GB;q=0.9,en;q=0.8")
                    //.header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .referrer("https://www.google.com/")
                    //.method(Connection.Method.GET)
                    .timeout(TIMEOUT_MS)
                    .get();

            String pageText = document.body() == null ? document.text() : document.body().text();
            String normalizedPage = normalize(pageText);
            List<Match> matches = new ArrayList<>();

            for (String name : names) {
                String normalizedName = normalize(name);
                int position = normalizedPage.indexOf(normalizedName);
                if (position >= 0) {
                    String context = normalizedPage.substring(
                            Math.max(0, position - CONTEXT_CHARS),
                            Math.min(normalizedPage.length(),
                                    position + normalizedName.length() + CONTEXT_CHARS));
                    matches.add(new Match(name, context.trim()));
                    System.out.println("FOUND: " + name);
                    System.out.println("CONTEXT: " + context.trim());
                } else {
                    System.out.println("NOT FOUND: " + name);
                }
            }

            if (!matches.isEmpty()) {
                sendNotification(url, checkedAt, matches);
                System.out.println("Notification email sent.");
            } else {
                System.out.println("No matches; no email sent.");
            }
        } catch (Exception e) {
            System.err.println("Crawler failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void sendNotification(String url, LocalDateTime checkedAt,
                                         List<Match> matches) throws Exception {
        String host = requiredEnv("SMTP_HOST");
        String port = envOrDefault("SMTP_PORT", "587");
        String username = requiredEnv("SMTP_USERNAME");
        String password = requiredEnv("SMTP_PASSWORD");
        String from = requiredEnv("EMAIL_FROM");
        String to = requiredEnv("EMAIL_TO");

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        if ("465".equals(port)) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(matches.size() == 1
                ? "Web crawler match found: " + matches.get(0).name()
                : "Web crawler matches found: " + matches.size());

        StringBuilder body = new StringBuilder();
        body.append("A monitored name was found.\n\n");
        body.append("URL: ").append(url).append("\n");
        body.append("Checked: ").append(checkedAt).append(" UTC\n\n");
        body.append("Matches:\n");
        for (Match m : matches) {
            body.append("- ").append(m.name()).append("\n");
            body.append("  Context: ").append(m.context()).append("\n");
        }
        message.setText(body.toString(), "UTF-8");
        Transport.send(message);
    }

    private static String normalize(String value) {
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ")
                .trim().toLowerCase();
    }

    private static String getParameter(String[] args, String argumentName, String envName) {
        String prefix = "--" + argumentName + "=";
        for (String arg : args) if (arg.startsWith(prefix)) return arg.substring(prefix.length());
        return System.getenv(envName);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Missing environment variable: " + name);
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing parameter: " + name);
    }

    private static void validateUrl(String url) {
        URI uri = URI.create(url);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            throw new IllegalArgumentException("Only HTTP/HTTPS URLs are supported: " + url);
    }

    private record Match(String name, String context) {}
}
