package com.myCafeteria.foodMenu;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodServiceTests {
    @Test
    void malformedFoodJsonIsRejectedWithoutWritingToDatabase() {
        FoodService service = new FoodService();
        service.foodRepository = mock(FoodRepository.class);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[] {1, 2, 3});

        assertThrows(JsonProcessingException.class,
                () -> service.store(file, "{invalid-json"));
        verifyNoInteractions(service.foodRepository);
    }

    @Test
    void unknownFoodIdDoesNotThrow() {
        FoodService service = new FoodService();
        service.foodRepository = mock(FoodRepository.class);
        when(service.foodRepository.findById("missing")).thenReturn(Optional.empty());

        assertNull(service.getFood("missing"));
    }
}
