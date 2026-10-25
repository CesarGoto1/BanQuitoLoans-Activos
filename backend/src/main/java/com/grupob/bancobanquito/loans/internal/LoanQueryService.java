package com.grupob.bancobanquito.loans.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class LoanQueryService {

    private final LoanQueryRepository operationRepository;

    LoanQueryService(LoanQueryRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    List<OperationResponse> findOperations(Integer clientId) {
        return operationRepository.findByClientIdOrderByDisbursementDateDescOperationNumberAsc(clientId)
                .stream()
                .map(OperationResponse::from)
                .toList();
    }

    String buildReport(Integer clientId) {
        List<OperationResponse> operations = findOperations(clientId);
        StringBuilder csv = new StringBuilder();
        csv.append("operationNumber,disbursementDate,approvedAmount,principalBalance,portfolioStatus,")
                .append("daysPastDue,nextDueDate,nextInstallmentAmount\n");
        for (OperationResponse operation : operations) {
            csv.append(csv(operation.operationNumber())).append(',')
                    .append(operation.disbursementDate()).append(',')
                    .append(operation.approvedAmount()).append(',')
                    .append(operation.principalBalance()).append(',')
                    .append(csv(operation.portfolioStatus())).append(',')
                    .append(operation.daysPastDue()).append(',')
                    .append(operation.nextInstallment() != null
                            ? Objects.toString(operation.nextInstallment().dueDate(), "")
                            : "").append(',')
                    .append(operation.nextInstallment() != null
                            ? Objects.toString(operation.nextInstallment().total(), "")
                            : "")
                    .append('\n');
        }
        return csv.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\"") ? "\"" + escaped + "\"" : escaped;
    }

    record OperationResponse(
            String operationNumber,
            String amortizationSystem,
            BigDecimal approvedAmount,
            BigDecimal netAmount,
            BigDecimal principalBalance,
            BigDecimal assignedTea,
            Integer termMonths,
            BigDecimal monthlyInsurance,
            LocalDate disbursementDate,
            String portfolioStatus,
            Integer daysPastDue,
            InstallmentResponse nextInstallment,
            List<InstallmentResponse> amortizationSchedule,
            List<PaymentResponse> payments) {

        static OperationResponse from(Operation operation) {
            List<InstallmentResponse> schedule = operation.getInstallments().stream()
                    .sorted(Comparator.comparing(Installment::getNumber))
                    .map(InstallmentResponse::from)
                    .toList();
            InstallmentResponse next = schedule.stream()
                    .filter(installment -> !"PAGADA".equals(installment.status()))
                    .min(Comparator.comparing(InstallmentResponse::dueDate))
                    .orElse(null);
            List<PaymentResponse> payments = operation.getPayments().stream()
                    .sorted(Comparator.comparing(Payment::getPaymentDate))
                    .map(PaymentResponse::from)
                    .toList();
            return new OperationResponse(
                    operation.getOperationNumber(),
                    operation.getAmortizationSystem(),
                    operation.getApprovedAmount(),
                    operation.getNetAmount(),
                    operation.getPrincipalBalance(),
                    operation.getAssignedTea(),
                    operation.getTermMonths(),
                    operation.getMonthlyInsurance(),
                    operation.getDisbursementDate(),
                    operation.getPortfolioStatus(),
                    operation.getDaysPastDue(),
                    next,
                    schedule,
                    payments);
        }
    }

    record InstallmentResponse(
            Integer number,
            LocalDate dueDate,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal insurance,
            BigDecimal total,
            BigDecimal paidPrincipal,
            BigDecimal paidInterest,
            BigDecimal paidInsurance,
            BigDecimal paidPastDueInterest,
            BigDecimal accruedPastDueInterest,
            String status,
            LocalDate paymentDate) {

        static InstallmentResponse from(Installment installment) {
            return new InstallmentResponse(
                    installment.getNumber(),
                    installment.getDueDate(),
                    installment.getPrincipal(),
                    installment.getInterest(),
                    installment.getInsurance(),
                    installment.getTotal(),
                    installment.getPaidPrincipal(),
                    installment.getPaidInterest(),
                    installment.getPaidInsurance(),
                    installment.getPaidPastDueInterest(),
                    installment.getAccruedPastDueInterest(),
                    installment.getStatus(),
                    installment.getPaymentDate());
        }
    }

    record PaymentResponse(
            LocalDate paymentDate,
            String origin,
            BigDecimal receivedAmount,
            BigDecimal returnedExcess,
            List<PaymentDetailResponse> details) {

        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.getPaymentDate(),
                    payment.getOrigin(),
                    payment.getReceivedAmount(),
                    payment.getReturnedExcess(),
                    payment.getDetails().stream().map(PaymentDetailResponse::from).toList());
        }
    }

    record PaymentDetailResponse(
            Integer installmentNumber,
            BigDecimal appliedInsurance,
            BigDecimal appliedPastDueInterest,
            BigDecimal appliedInterest,
            BigDecimal appliedPrincipal) {

        static PaymentDetailResponse from(PaymentDetail detail) {
            return new PaymentDetailResponse(
                    detail.installmentNumber(),
                    detail.getAppliedInsurance(),
                    detail.getAppliedPastDueInterest(),
                    detail.getAppliedInterest(),
                    detail.getAppliedPrincipal());
        }
    }
}
