package com.myCafeteria.controllers;

import com.myCafeteria.models.ERole;
import com.myCafeteria.models.Role;
import com.myCafeteria.models.User;
import com.myCafeteria.payload.SignupRequest;
import com.myCafeteria.repository.RoleRepository;
import com.myCafeteria.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.HashSet;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthControllerSignupTests {
    @Test
    void publicSignupCannotAssignAdmin() {
        AuthController controller = new AuthController();
        controller.userRepository = mock(UserRepository.class);
        controller.roleRepository = mock(RoleRepository.class);
        controller.encoder = mock(PasswordEncoder.class);
        when(controller.roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(new Role(ERole.ROLE_USER)));
        when(controller.encoder.encode("password123")).thenReturn("encoded");
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.invalid");
        request.setPassword("password123");
        request.setRole(new HashSet<>(Arrays.asList("admin", "mod")));
        controller.registerUser(request);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(controller.userRepository).save(user.capture());
        assertEquals(1, user.getValue().getRoles().size());
        assertEquals(ERole.ROLE_USER, user.getValue().getRoles().iterator().next().getName());
        verify(controller.roleRepository, never()).findByName(ERole.ROLE_ADMIN);
    }
}
