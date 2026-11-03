package com.gesturetalk.util;

import com.gesturetalk.exception.InvalidImageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageValidator Unit Tests")
class ImageValidatorTest {

    @Test
    @DisplayName("Should detect valid JPEG byte signature")
    void testValidJpeg() {
        byte[] jpeg = new byte[32];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        jpeg[3] = (byte) 0xE0;

        String mime = ImageValidator.validate(jpeg);
        assertEquals("image/jpeg", mime);
    }

    @Test
    @DisplayName("Should detect valid PNG byte signature")
    void testValidPng() {
        byte[] png = new byte[32];
        byte[] magic = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 
                                  (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
        System.arraycopy(magic, 0, png, 0, magic.length);

        String mime = ImageValidator.validate(png);
        assertEquals("image/png", mime);
    }

    @Test
    @DisplayName("Should detect valid WebP byte signature")
    void testValidWebp() {
        byte[] webp = new byte[32];
        byte[] riff = new byte[]{(byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F'};
        byte[] webpHeader = new byte[]{(byte) 'W', (byte) 'E', (byte) 'B', (byte) 'P'};
        System.arraycopy(riff, 0, webp, 0, 4);
        System.arraycopy(webpHeader, 0, webp, 8, 4);

        String mime = ImageValidator.validate(webp);
        assertEquals("image/webp", mime);
    }

    @Test
    @DisplayName("Should reject null or empty byte array with 400 Bad Request")
    void testEmptyPayload() {
        InvalidImageException ex1 = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(null));
        assertEquals(400, ex1.getHttpStatus());
        assertEquals("EMPTY_IMAGE_PAYLOAD", ex1.getErrorCode());

        InvalidImageException ex2 = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(new byte[0]));
        assertEquals(400, ex2.getHttpStatus());
    }

    @Test
    @DisplayName("Should reject file exceeding 5 MB with 413 Payload Too Large")
    void testExceeds5Mb() {
        byte[] largeFile = new byte[ImageValidator.MAX_FILE_SIZE_BYTES + 1];
        largeFile[0] = (byte) 0xFF;
        largeFile[1] = (byte) 0xD8;
        largeFile[2] = (byte) 0xFF;

        InvalidImageException ex = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(largeFile));
        assertEquals(413, ex.getHttpStatus());
        assertEquals("FILE_TOO_LARGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject non-image file with 415 Unsupported Media Type")
    void testUnsupportedFormat() {
        byte[] fakeExe = new byte[32];
        fakeExe[0] = (byte) 0x4D; // MZ
        fakeExe[1] = (byte) 0x5A;

        InvalidImageException ex = assertThrows(InvalidImageException.class, () -> ImageValidator.validate(fakeExe));
        assertEquals(415, ex.getHttpStatus());
        assertEquals("UNSUPPORTED_IMAGE_FORMAT", ex.getErrorCode());
    }
}
