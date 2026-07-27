package com.ssa.lms.common.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesCryptoConverterTest {

    private final AesCryptoConverter converter = new AesCryptoConverter("test-secret");

    @Test
    void 암호화_후_복호화하면_원문이_나온다() {
        String plain = "010-1234-5678";
        String encrypted = converter.convertToDatabaseColumn(plain);

        assertNotEquals(plain, encrypted, "DB에는 암호문이 저장되어야 한다");
        assertEquals(plain, converter.convertToEntityAttribute(encrypted));
    }

    @Test
    void 같은_평문도_IV가_달라_암호문이_매번_다르다() {
        String plain = "trainee@example.com";
        assertNotEquals(converter.convertToDatabaseColumn(plain),
                converter.convertToDatabaseColumn(plain));
    }

    @Test
    void null과_빈문자열은_그대로_통과한다() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals("", converter.convertToDatabaseColumn(""));
    }

    @Test
    void 다른_키로는_복호화할_수_없다() {
        String encrypted = converter.convertToDatabaseColumn("1999-07-07");
        AesCryptoConverter otherKey = new AesCryptoConverter("other-secret");
        assertThrows(IllegalStateException.class, () -> otherKey.convertToEntityAttribute(encrypted));
    }
}
