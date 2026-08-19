package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.subscription.request.AccountSubscriptionRequestDto;
import com.talex.server.dtos.subscription.response.AccountSubscriptionResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.subscription.AccountSubscription;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.subscription.AccountSubscriptionRepository;
import com.talex.server.repositories.transaction.InvoiceRepository;
import com.talex.server.repositories.transaction.TransactionRepository;
import com.talex.server.services.payment.impls.AccountFulfillmentLock;
import com.talex.server.services.subscription.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AccountSubscriptionServiceImpl#createAccountSubscription}.
 *
 * Validates: new subscription creation, startTime succession from active previous subscription,
 * account/subscription resolution, orderId propagation, and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountSubscriptionServiceImpl.createAccountSubscription")
class AccountSubscriptionServiceImplTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private AccountSubscriptionRepository accountSubscriptionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountFulfillmentLock accountFulfillmentLock;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private AccountSubscriptionServiceImpl accountSubscriptionService;

    private UUID testAccountId;
    private String testSubscriptionId;
    private String testOrderId;
    private Account testAccount;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        testSubscriptionId = "sub-premium-001";
        testOrderId = "order-001";

        testAccount = Account.builder()
                .accountId(testAccountId)
                .email("user@example.com")
                .accountSubscriptions(new ArrayList<>())
                .build();

        testSubscription = Subscription.builder()
                .subscriptionId(testSubscriptionId)
                .tier("PREMIUM")
                .price(java.math.BigDecimal.TEN)
                .duration(30)
                .durationUnit("DAYS")
                .build();
    }

    @Nested
    @DisplayName("UTCID01: Create new subscription - no prior active subscription")
    class CreateNewSubscriptionNoActiveSubTests {

        @Test
        @DisplayName("Should create AccountSubscription with startTime = now when no active prior sub exists")
        void createNew_noPriorSub_startTimeIsNow() {
            // Arrange
            LocalDateTime beforeCall = LocalDateTime.now();
            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(testSubscription);
            when(accountSubscriptionRepository.save(any(AccountSubscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountSubscriptionResponseDto response = accountSubscriptionService
                    .createAccountSubscription(requestDto);
            LocalDateTime afterCall = LocalDateTime.now();

            // Assert
            assertNotNull(response);
            assertNotNull(response.getStartTime());
            assertTrue(response.getStartTime().isAfter(beforeCall.minusSeconds(1)));
            assertTrue(response.getStartTime().isBefore(afterCall.plusSeconds(1)));
            assertEquals(testOrderId, response.getOrderId());
            assertEquals(testAccountId.toString(), response.getAccountId());
            assertEquals(testSubscriptionId, response.getSubscriptionId());

            // Verify acquisition/release
            verify(accountFulfillmentLock).acquire(testAccountId);
            verify(accountRepository).findById(testAccountId);
            verify(subscriptionService).getSubscriptionByIdEntity(testSubscriptionId);
            verify(accountSubscriptionRepository).save(any(AccountSubscription.class));
        }
    }

    @Nested
    @DisplayName("UTCID02: Create subscription - nối tiếp from active prior subscription")
    class CreateSubscriptionWithActivePriorSubTests {

        @Test
        @DisplayName("Should calculate startTime = endTime of latest valid subscription + 1 second")
        void createNew_activePriorSub_startTimeIsSuccessor() {
            // Arrange
            LocalDateTime priorEndTime = LocalDateTime.now().plus(5, ChronoUnit.DAYS);
            LocalDateTime expectedStartTime = priorEndTime.plusSeconds(1);

            AccountSubscription activeSub = AccountSubscription.builder()
                    .accountSubscriptionId("account-sub-001")
                    .startTime(LocalDateTime.now())
                    .endTime(priorEndTime)
                    .isCancelled(false)
                    .build();

            testAccount.setAccountSubscriptions(List.of(activeSub));

            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(testSubscription);

            ArgumentCaptor<AccountSubscription> savedCaptor =
                    ArgumentCaptor.forClass(AccountSubscription.class);
            when(accountSubscriptionRepository.save(savedCaptor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountSubscriptionResponseDto response = accountSubscriptionService
                    .createAccountSubscription(requestDto);

            // Assert
            assertEquals(expectedStartTime, response.getStartTime());
            verify(accountSubscriptionRepository).save(any(AccountSubscription.class));
        }

        @Test
        @DisplayName("Should choose latest (max endTime) among multiple active subscriptions")
        void createNew_multipleActiveSubs_choosesLatest() {
            // Arrange
            LocalDateTime endTime1 = LocalDateTime.now().plus(3, ChronoUnit.DAYS);
            LocalDateTime endTime2 = LocalDateTime.now().plus(7, ChronoUnit.DAYS); // latest
            LocalDateTime endTime3 = LocalDateTime.now().plus(5, ChronoUnit.DAYS);

            AccountSubscription sub1 = AccountSubscription.builder()
                    .accountSubscriptionId("account-sub-001")
                    .startTime(LocalDateTime.now())
                    .endTime(endTime1)
                    .isCancelled(false)
                    .build();

            AccountSubscription sub2 = AccountSubscription.builder()
                    .accountSubscriptionId("account-sub-002")
                    .startTime(LocalDateTime.now())
                    .endTime(endTime2)
                    .isCancelled(false)
                    .build();

            AccountSubscription sub3 = AccountSubscription.builder()
                    .accountSubscriptionId("account-sub-003")
                    .startTime(LocalDateTime.now())
                    .endTime(endTime3)
                    .isCancelled(false)
                    .build();

            testAccount.setAccountSubscriptions(List.of(sub1, sub2, sub3));

            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(testSubscription);
            when(accountSubscriptionRepository.save(any(AccountSubscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountSubscriptionResponseDto response = accountSubscriptionService
                    .createAccountSubscription(requestDto);

            // Assert
            assertEquals(endTime2.plusSeconds(1), response.getStartTime());
        }
    }

    @Nested
    @DisplayName("UTCID03: Ignore cancelled subscriptions in startTime calculation")
    class IgnoreCancelledSubTests {

        @Test
        @DisplayName("Should skip cancelled subscriptions and use earlier active ones")
        void createNew_ignoreCancelled_usesActiveOnly() {
            // Arrange
            LocalDateTime cancelledEndTime = LocalDateTime.now().plus(10, ChronoUnit.DAYS);
            LocalDateTime activeEndTime = LocalDateTime.now().plus(5, ChronoUnit.DAYS);

            AccountSubscription cancelledSub = AccountSubscription.builder()
                    .accountSubscriptionId("account-sub-001")
                    .startTime(LocalDateTime.now())
                    .endTime(cancelledEndTime)
                    .isCancelled(true)
                    .cancelledAt(LocalDateTime.now())
                    .build();

            AccountSubscription activeSub = AccountSubscription.builder()
                    .accountSubscriptionId("account-sub-002")
                    .startTime(LocalDateTime.now())
                    .endTime(activeEndTime)
                    .isCancelled(false)
                    .build();

            testAccount.setAccountSubscriptions(List.of(cancelledSub, activeSub));

            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(testSubscription);
            when(accountSubscriptionRepository.save(any(AccountSubscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountSubscriptionResponseDto response = accountSubscriptionService
                    .createAccountSubscription(requestDto);

            // Assert
            assertEquals(activeEndTime.plusSeconds(1), response.getStartTime());
        }
    }

    @Nested
    @DisplayName("UTCID04: Calculate endTime correctly based on Subscription duration/unit")
    class CalculateEndTimeTests {

        @Test
        @DisplayName("Should calculate endTime = startTime + duration in specified unit")
        void createNew_durationDays_endTimeCalculatedCorrectly() {
            // Arrange
            LocalDateTime testStartTime = LocalDateTime.of(2026, 8, 18, 10, 0, 0);
            LocalDateTime expectedEndTime = testStartTime.plus(30, ChronoUnit.DAYS);

            Subscription sub30Days = Subscription.builder()
                    .subscriptionId(testSubscriptionId)
                    .tier("PREMIUM")
                    .price(java.math.BigDecimal.TEN)
                    .duration(30)
                    .durationUnit("DAYS")
                    .build();

            testAccount.setAccountSubscriptions(new ArrayList<>());

            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(sub30Days);

            ArgumentCaptor<AccountSubscription> savedCaptor =
                    ArgumentCaptor.forClass(AccountSubscription.class);
            when(accountSubscriptionRepository.save(savedCaptor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountSubscriptionResponseDto response = accountSubscriptionService
                    .createAccountSubscription(requestDto);

            // Assert
            assertNotNull(response.getEndTime());
            AccountSubscription saved = savedCaptor.getValue();
            assertEquals(30, ChronoUnit.DAYS.between(saved.getStartTime(), saved.getEndTime()));
        }

        @Test
        @DisplayName("Should handle MONTHS duration unit")
        void createNew_durationMonths_endTimeCalculatedCorrectly() {
            // Arrange
            Subscription sub1Month = Subscription.builder()
                    .subscriptionId(testSubscriptionId)
                    .tier("PREMIUM")
                    .price(java.math.BigDecimal.TEN)
                    .duration(1)
                    .durationUnit("MONTHS")
                    .build();

            testAccount.setAccountSubscriptions(new ArrayList<>());

            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(sub1Month);

            ArgumentCaptor<AccountSubscription> savedCaptor =
                    ArgumentCaptor.forClass(AccountSubscription.class);
            when(accountSubscriptionRepository.save(savedCaptor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            accountSubscriptionService.createAccountSubscription(requestDto);

            // Assert
            AccountSubscription saved = savedCaptor.getValue();
            assertEquals(1, ChronoUnit.MONTHS.between(saved.getStartTime(), saved.getEndTime()));
        }
    }

    @Nested
    @DisplayName("UTCID05: Propagate orderId to AccountSubscription entity")
    class PropagateOrderIdTests {

        @Test
        @DisplayName("Should save orderId when provided in request")
        void createNew_withOrderId_savesOrderId() {
            // Arrange
            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            testAccount.setAccountSubscriptions(new ArrayList<>());

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(testSubscription);

            ArgumentCaptor<AccountSubscription> savedCaptor =
                    ArgumentCaptor.forClass(AccountSubscription.class);
            when(accountSubscriptionRepository.save(savedCaptor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountSubscriptionResponseDto response = accountSubscriptionService
                    .createAccountSubscription(requestDto);

            // Assert
            assertEquals(testOrderId, response.getOrderId());
            AccountSubscription saved = savedCaptor.getValue();
            assertEquals(testOrderId, saved.getOrderId());
        }

        @Test
        @DisplayName("Should handle null orderId (Admin/Staff manual creation)")
        void createNew_nullOrderId_savesNull() {
            // Arrange
            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(null)
                    .build();

            testAccount.setAccountSubscriptions(new ArrayList<>());

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(testSubscription);

            ArgumentCaptor<AccountSubscription> savedCaptor =
                    ArgumentCaptor.forClass(AccountSubscription.class);
            when(accountSubscriptionRepository.save(savedCaptor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountSubscriptionResponseDto response = accountSubscriptionService
                    .createAccountSubscription(requestDto);

            // Assert
            assertNull(response.getOrderId());
            AccountSubscription saved = savedCaptor.getValue();
            assertNull(saved.getOrderId());
        }
    }

    @Nested
    @DisplayName("UTCID06: Error scenarios")
    class ErrorScenarioTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when accountId is null")
        void createNew_nullAccountId_throwsIllegalArgumentException() {
            // Arrange
            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(null)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> accountSubscriptionService.createAccountSubscription(requestDto));
            assertTrue(exception.getMessage().contains("Account id is required"));

            verify(accountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when account not found")
        void createNew_accountNotFound_throwsResourceNotFoundException() {
            // Arrange
            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> accountSubscriptionService.createAccountSubscription(requestDto));
            assertTrue(exception.getMessage().contains("Account not found"));

            verify(accountFulfillmentLock).acquire(testAccountId);
            verify(accountRepository).findById(testAccountId);
            verify(subscriptionService, never()).getSubscriptionByIdEntity(any());
        }

        @Test
        @DisplayName("Should throw exception when subscription not found")
        void createNew_subscriptionNotFound_throwsException() {
            // Arrange
            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            testAccount.setAccountSubscriptions(new ArrayList<>());

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenThrow(new ResourceNotFoundException("Subscription not found"));

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> accountSubscriptionService.createAccountSubscription(requestDto));

            verify(accountFulfillmentLock).acquire(testAccountId);
            verify(accountRepository).findById(testAccountId);
            verify(subscriptionService).getSubscriptionByIdEntity(testSubscriptionId);
            verify(accountSubscriptionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("AccountFulfillmentLock serialization")
    class FulfillmentLockTests {

        @Test
        @DisplayName("Should acquire lock at start with correct accountId")
        void createNew_acquiresLockWithAccountId() {
            // Arrange
            AccountSubscriptionRequestDto requestDto = AccountSubscriptionRequestDto.builder()
                    .accountId(testAccountId)
                    .subscriptionId(testSubscriptionId)
                    .orderId(testOrderId)
                    .build();

            testAccount.setAccountSubscriptions(new ArrayList<>());

            when(accountRepository.findById(testAccountId))
                    .thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(testSubscriptionId))
                    .thenReturn(testSubscription);
            when(accountSubscriptionRepository.save(any(AccountSubscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            accountSubscriptionService.createAccountSubscription(requestDto);

            // Assert
            verify(accountFulfillmentLock).acquire(testAccountId);
        }
    }
}
