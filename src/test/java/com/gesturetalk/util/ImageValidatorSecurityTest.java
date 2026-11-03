package com.gesturetalk.util;

import com.gesturetalk.exception.InvalidImageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageValidator Security & Boundary Verification Tests")
class ImageValidatorSecurityTest {

    @Test
    @DisplayName("Should accept valid JPEG binary magic bytes")
    void testValidJpeg() {
        byte[] jpeg = new byte[64];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        jpeg[3] = (byte) 0xE0;

        assertEquals("image/jpeg", ImageValidator.validate(jpeg));
    }

    @Test
    @DisplayName("Should accept valid PNG binary magic bytes")
    void testValidPng() {
        byte[] png = new byte[64];
        png[0] = (byte) 0x89;
        png[1] = (byte) 0x50;
        png[2] = (byte) 0x4E;
        png[3] = (byte) 0x47;
        png[4] = (byte) 0x0D;
        png[5] = (byte) 0x0A;
        png[6] = (byte) 0x1A;
        png[7] = (byte) 0x0A;

        assertEquals("image/png", ImageValidator.validate(png));
    }

    @Test
    @DisplayName("Should accept valid WebP binary magic bytes")
    void testValidWebP() {
        byte[] webp = new byte[64];
        // RIFF
        webp[0] = 'R'; webp[1] = 'I'; webp[2] = 'F'; webp[3] = 'F';
        // length (4 bytes)
        webp[4] = 0; webp[5] = 0; webp[6] = 0; webp[7] = 0;
        // WEBP
        webp[8] = 'W'; webp[9] = 'E'; webp[10] = 'B'; webp[11] = 'P';

        assertEquals("image/webp", ImageValidator.validate(webp));
    }

    @Test
    @DisplayName("Should strictly reject Windows PE/EXE executables")
    void testRejectExecutable() {
        byte[] exe = new byte[64];
        exe[0] = 'M'; exe[1] = 'Z'; // DOS MZ header

        InvalidImageException ex = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(exe));
        assertEquals(415, ex.getHttpStatus());
        assertEquals("UNSUPPORTED_IMAGE_FORMAT", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should strictly reject PDF files")
    void testRejectPdf() {
        byte[] pdf = "%PDF-1.7\n".getBytes();
        InvalidImageException ex = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(pdf));
        assertEquals(415, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should strictly reject ZIP/JAR archives")
    void testRejectZip() {
        byte[] zip = new byte[]{'P', 'K', 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0};
        InvalidImageException ex = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(zip));
        assertEquals(415, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should strictly reject ELF Linux binaries")
    void testRejectElf() {
        byte[] elf = new byte[]{0x7F, 'E', 'L', 'F', 2, 1, 1, 0, 0, 0, 0, 0};
        InvalidImageException ex = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(elf));
        assertEquals(415, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should reject empty or null payloads with 400 Bad Request")
    void testRejectNullOrEmpty() {
        InvalidImageException exNull = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(null));
        assertEquals(400, exNull.getHttpStatus());
        assertEquals("EMPTY_IMAGE_PAYLOAD", exNull.getErrorCode());

        InvalidImageException exEmpty = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(new byte[0]));
        assertEquals(400, exEmpty.getHttpStatus());
        assertEquals("EMPTY_IMAGE_PAYLOAD", exEmpty.getErrorCode());
    }

    @Test
    @DisplayName("Should accept exactly 5 MB image and reject 5 MB + 1 byte")
    void testBoundarySizeValidation() {
        int maxAllowed = ImageValidator.MAX_FILE_SIZE_BYTES; // 5 MB

        // Create buffer of maxAllowed with valid JPEG header
        byte[] exact5Mb = new byte[maxAllowed];
        exact5Mb[0] = (byte) 0xFF;
        exact5Mb[1] = (byte) 0xD8;
        exact5Mb[2] = (byte) 0xFF;

        assertEquals("image/jpeg", ImageValidator.validate(exact5Mb));

        // Create buffer of 5 MB + 1 byte
        byte[] over5Mb = new byte[maxAllowed + 1];
        over5Mb[0] = (byte) 0xFF;
        over5Mb[1] = (byte) 0xD8;
        over5Mb[2] = (byte) 0xFF;

        InvalidImageException ex = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(over5Mb));
        assertEquals(413, ex.getHttpStatus());
        assertEquals("FILE_TOO_LARGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject truncated payloads (< 12 bytes)")
    void testTruncatedPayload() {
        byte[] tooShort = new byte[]{(byte) 0xFF, (byte) 0xD8};
        assertNull(ImageValidator.detectMimeType(tooShort));
        assertThrows(InvalidImageException.class, () -> ImageValidator.validate(tooShort));
    }
}

