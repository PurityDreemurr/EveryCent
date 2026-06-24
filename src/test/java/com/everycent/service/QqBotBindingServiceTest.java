package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.QqBotBinding;
import com.everycent.domain.User;
import com.everycent.repository.QqBotBindingRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QqBotBindingServiceTest {

    private final QqBotBindingRepository repository = org.mockito.Mockito.mock(QqBotBindingRepository.class);
    private final QqBotBindingService service = new QqBotBindingService(repository);

    @Test
    void resetBindingShouldDeleteOldBindingAndCreateNewCode() {
        User user = user();
        QqBotBinding oldBinding = new QqBotBinding();
        oldBinding.setId(1L);
        oldBinding.setUser(user);
        oldBinding.setBindingCode("ECQQ-OLD12345");
        oldBinding.setQqOpenId("openid-1");
        oldBinding.setCreatedDate(Instant.now());
        when(repository.findOneByUser(user)).thenReturn(Optional.of(oldBinding));
        when(repository.existsByBindingCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any(QqBotBinding.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.resetBinding(user);

        verify(repository).delete(oldBinding);
        verify(repository).flush();
        ArgumentCaptor<QqBotBinding> bindingCaptor = ArgumentCaptor.forClass(QqBotBinding.class);
        verify(repository).save(bindingCaptor.capture());
        QqBotBinding saved = bindingCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getQqOpenId()).isNull();
        assertThat(saved.getBindingCode()).startsWith("ECQQ-").isNotEqualTo("ECQQ-OLD12345");
        assertThat(result.isBound()).isFalse();
        assertThat(result.getBindingCode()).isEqualTo(saved.getBindingCode());
    }

    private User user() {
        User user = new User();
        user.setId(10L);
        user.setLogin("alice");
        return user;
    }
}
