package com.sdl.dxa.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.SecureRandom;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FastUUIDTest {
    @Test
    public void generating() {
        final SecureRandom random = mock(SecureRandom.class);
        when(random.nextLong())
                .thenReturn(0xb8d59fd5bc12dbb4L, 0x31e9f344b73ee369L, 0xb8d59fd5bc12dbb4L, 0x31e9f344b73ee369L);

        final FastUUID generator = new FastUUID(random);
        assertThat(generator.generate().toString()).isEqualTo("88e6891a-3dbc-423c-b51a-127083468307");
        assertThat(generator.generate().toString()).isEqualTo("ad173908-cbe5-49a7-8a36-b516b033b4bd");
        assertThat(generator.generate().version()).isEqualTo(4);
        assertThat(generator.generate().variant()).isEqualTo(2);

        generator.reseed();
        assertThat(generator.generate().toString()).isEqualTo("88e6891a-3dbc-423c-b51a-127083468307");
    }

    @Test
    public void generateLong() {
        final SecureRandom random = new SecureRandom();

        final FastUUID generator = new FastUUID(random);
        generator.reseed();

        String uuid = generator.generate().toString();
        for (int i=0; i< 100_000_000; i++) {
            if (uuid.equals(generator.generate().toString())) {
                fail();
            }
        }
    }

}
