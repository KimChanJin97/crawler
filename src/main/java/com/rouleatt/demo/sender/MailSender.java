package com.rouleatt.demo.sender;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailSender {

    @Value("${gmail.to}")
    private String to;


    private static final String INTERRUPT_EXCEPTION_TITLE = "인터럽트 예외로 인한 재시도";
    private static final String INTERRUPT_EXCEPTION_CONTENT = "재시도 횟수 : %d";
    private static final String INTERRUPT_EXCEPTION_RETRY_MAX_TITLE = "인터럽트 예외로 인한 최대 재시도 초과";
    private static final String INTERRUPT_EXCEPTION_RETRY_MAX_CONTENT = "인터럽트 예외로 인한 재시도 최대 횟수를 초과했습니다";
    private static final String INTERRUPT_EXCEPTION_SLEEP_INTERRUPT_TITLE = "인터럽트 슬립 예외 발생";
    private static final String INTERRUPT_EXCEPTION_SLEEP_INTERRUPT_CONTENT = "인터럽트 슬립 예외가 발생했습니다";

    private static final String BLOCK_EXCEPTION_TITLE = "블락 예외로 인한 재시도";
    private static final String BLOCK_EXCEPTION_CONTENT = "재시도 횟수 : %d";
    private static final String BLOCK_EXCEPTION_RETRY_MAX_TITLE = "블락 예외로 인한 최대 재시도 초과";
    private static final String BLOCK_EXCEPTION_RETRY_MAX_CONTENT = "블락 예외로 인한 재시도 최대 횟수를 초과했습니다";
    private static final String BLOCK_EXCEPTION_SLEEP_INTERRUPT_TITLE = "블락 슬립 예외 발생";
    private static final String BLOCK_EXCEPTION_SLEEP_INTERRUPT_CONTENT = "블락 슬립 예외가 발생했습니다";

    private static final String DONE_TITLE = "크롤링 완료";
    private static final String DONE_CONTENT = "크롤링이 완료되었습니다";

    private static final String IO_EXCEPTION_TITLE = "입출력 예외로 인한 재시도";
    private static final String IO_EXCEPTION_CONTENT = "재시도 횟수 : %d";
    private static final String IO_EXCEPTION_RETRY_MAX_TITLE = "입출력 예외로 인한 최대 재시도 초과";
    private static final String IO_EXCEPTION_RETRY_MAX_CONTENT = "입출력 예외로 인한 재시도 최대 횟수를 초과했습니다";
    private static final String IO_EXCEPTION_SLEEP_INTERRUPT_TITLE = "블락 슬립 예외 발생";
    private static final String IO_EXCEPTION_SLEEP_INTERRUPT_CONTENT = "블락 슬립 예외가 발생했습니다";

    private static final String READ_CSV_EXCEPTION_TITLE = "CSV 파일 읽기 예외";
    private static final String READ_CSV_EXCEPTION_CONTENT = "CSV 파일을 읽는 데 실패했습니다";

    private static final String WRONG_BOUND_TITLE = "잘못된 좌표 예외";
    private static final String WRONG_BOUND_CONTENT = "%f < %f < %f 이 아니거나 %f < %f < %f 가 아닙니다";

    private final JavaMailSender sender;

    public void sendInterruptException(int retryCount) {
        send(to, INTERRUPT_EXCEPTION_TITLE, retryContent(INTERRUPT_EXCEPTION_CONTENT, retryCount));
    }

    public void sendInterruptExceptionMaxRetry() {
        send(to, INTERRUPT_EXCEPTION_RETRY_MAX_TITLE, INTERRUPT_EXCEPTION_RETRY_MAX_CONTENT);
    }

    public void sendInterruptExceptionSleepInterrupt() {
        send(to, INTERRUPT_EXCEPTION_SLEEP_INTERRUPT_TITLE, INTERRUPT_EXCEPTION_SLEEP_INTERRUPT_CONTENT);
    }

    public void sendBlockException(int retryCount) {
        send(to, BLOCK_EXCEPTION_TITLE, retryContent(BLOCK_EXCEPTION_CONTENT, retryCount));
    }

    public void sendBlockExceptionMaxRetry() {
        send(to, BLOCK_EXCEPTION_RETRY_MAX_TITLE, BLOCK_EXCEPTION_RETRY_MAX_CONTENT);
    }

    public void sendBlockExceptionSleepInterrupt() {
        send(to, BLOCK_EXCEPTION_SLEEP_INTERRUPT_TITLE, BLOCK_EXCEPTION_SLEEP_INTERRUPT_CONTENT);
    }

    public void sendIOException(int retryCount) {
        send(to, IO_EXCEPTION_TITLE, retryContent(IO_EXCEPTION_CONTENT, retryCount));
    }

    public void sendIOExceptionMaxRetry() {
        send(to, IO_EXCEPTION_RETRY_MAX_TITLE, IO_EXCEPTION_RETRY_MAX_CONTENT);
    }

    public void sendIOExceptionSleepInterrupt() {
        send(to, IO_EXCEPTION_SLEEP_INTERRUPT_TITLE, IO_EXCEPTION_SLEEP_INTERRUPT_CONTENT);
    }

    public void sendReadCsvException() {
        send(to, READ_CSV_EXCEPTION_TITLE, READ_CSV_EXCEPTION_CONTENT);
    }

    public void sendWrongBound(double minX, double currentX, double maxX, double minY, double currentY, double maxY) {
        send(to, WRONG_BOUND_TITLE, String.format(WRONG_BOUND_CONTENT, minX, currentX, maxX, minY, currentY, maxY));
    }

    public void sendDone() {
        send(to, DONE_TITLE, DONE_CONTENT);
    }

    private void send(String to, String title, String content) {
        MimeMessage mimeMessage = sender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false);
            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(title);
            mimeMessageHelper.setText(content);
            sender.send(mimeMessage);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException();
        }
    }

    private String retryContent(String content, int retryCount) {
        return String.format(content, retryCount);
    }
}
