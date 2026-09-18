package com.cluster.coordinator.security;

import com.cluster.coordinator.service.cluster.ClusterEnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cluster.coordinator.repository.WorkerRepository;
import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerState;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class SecurityConfigTest {

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private SecurityConfig securityConfig;

    private AuthenticationProvider authProvider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityConfig, "username", "admin");
        ReflectionTestUtils.setField(securityConfig, "password", "admin_secret");
        authProvider = securityConfig.authenticationProvider(workerRepository);
    }

    @Test
    void testAdminAuthentication_Success() {
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", "admin_secret");
        Authentication result = authProvider.authenticate(auth);

        assertNotNull(result);
        assertEquals("admin", result.getName());
        assertTrue(result.isAuthenticated());
        
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_WORKER")));
    }

    @Test
    void testAdminAuthentication_Failure() {
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", "wrong_secret");
        assertThrows(BadCredentialsException.class, () -> authProvider.authenticate(auth));
    }

    private String hashCredential(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encodedhash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }

    @Test
    void testWorkerAuthentication_Success() {
        String workerId = "worker-123";
        String rawCredential = "VALID-RUNTIME-CREDENTIAL";
        String hashedCredential = hashCredential(rawCredential);
        
        Worker mockWorker = new Worker(workerId, "unknown", WorkerState.REGISTERING);
        mockWorker.setRuntimeCredentialHash(hashedCredential);
        
        when(workerRepository.findById(workerId)).thenReturn(Optional.of(mockWorker));

        Authentication auth = new UsernamePasswordAuthenticationToken(workerId, rawCredential);
        Authentication result = authProvider.authenticate(auth);

        assertNotNull(result);
        assertEquals(workerId, result.getName());
        assertTrue(result.isAuthenticated());
        
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_WORKER")));
        assertFalse(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testWorkerAuthentication_Failure() {
        String workerId = "worker-123";
        String rawCredential = "INVALID-RUNTIME-CREDENTIAL";
        String correctCredential = "VALID-RUNTIME-CREDENTIAL";
        
        Worker mockWorker = new Worker(workerId, "unknown", WorkerState.REGISTERING);
        mockWorker.setRuntimeCredentialHash(hashCredential(correctCredential));
        
        when(workerRepository.findById(workerId)).thenReturn(Optional.of(mockWorker));

        Authentication auth = new UsernamePasswordAuthenticationToken(workerId, rawCredential);
        assertThrows(BadCredentialsException.class, () -> authProvider.authenticate(auth));
    }
    
    @Test
    void testSupportsUsernamePasswordToken() {
        assertTrue(authProvider.supports(UsernamePasswordAuthenticationToken.class));
    }
}
