package com.bank.notificationservice.controller;

import com.bank.notificationservice.dto.AccountEventNotificationRequest;
import com.bank.notificationservice.dto.AccountStatusChangeNotificationRequest;
import com.bank.notificationservice.dto.HighValueTransactionNotificationRequest;
import com.bank.notificationservice.dto.NotificationLogEntry;
import com.bank.notificationservice.dto.NotificationResponse;
import com.bank.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Dispatch account notifications and review recent deliveries")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/transactions/high-value")
    @Operation(
            summary = "Dispatch high value transaction notification",
            description = "Validates the transaction payload and dispatches a notification when it exceeds the configured threshold.",
            responses = {
                @ApiResponse(
                        responseCode = "202",
                        description = "Notification accepted for delivery",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class))),
                @ApiResponse(
                        responseCode = "200",
                        description = "Transaction below threshold; notification skipped",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class)))
            })
    public ResponseEntity<NotificationResponse> handleHighValueTransaction(
            @Valid @RequestBody HighValueTransactionNotificationRequest request) {
        boolean dispatched = notificationService.handleHighValueTransaction(request);
        if (dispatched) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(NotificationResponse.of("High value transaction notification dispatched."));
        }
        return ResponseEntity.ok(NotificationResponse.of("Transaction below configured threshold; notification skipped."));
    }

    @PostMapping("/accounts/status-change")
    @Operation(
            summary = "Dispatch account status change notification",
            description = "Emits a notification whenever an account transitions between states (for example ACTIVE to SUSPENDED).",
            responses = @ApiResponse(
                    responseCode = "202",
                    description = "Notification accepted for delivery",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class))))
    public ResponseEntity<NotificationResponse> handleAccountStatusChange(
            @Valid @RequestBody AccountStatusChangeNotificationRequest request) {
        notificationService.handleAccountStatusChange(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(NotificationResponse.of("Account status change notification dispatched."));
    }

    @PostMapping("/accounts/events")
    @Operation(
            summary = "Dispatch account event notification",
            description = "Handles lower severity account events such as statement availability or KYC reminders.",
            responses = @ApiResponse(
                    responseCode = "202",
                    description = "Notification accepted for delivery",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class))))
    public ResponseEntity<NotificationResponse> handleAccountEvents(
            @Valid @RequestBody AccountEventNotificationRequest request) {
        notificationService.handleAccountEvent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(NotificationResponse.of("Account event notification dispatched."));
    }

    @GetMapping("/history")
    @Operation(
            summary = "View recently sent notifications",
            description = "Returns notifications that have been dispatched within the past week.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Collection of notification log entries",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NotificationLogEntry.class)))))
    public ResponseEntity<List<NotificationLogEntry>> recentlySentNotifications() {
        List<NotificationLogEntry> entries = notificationService.fetchNotificationsForPastWeek().stream()
                .map(NotificationLogEntry::from)
                .toList();
        return ResponseEntity.ok(entries);
    }
}
