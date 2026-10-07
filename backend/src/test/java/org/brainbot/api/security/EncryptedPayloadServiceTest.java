package org.brainbot.api.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class EncryptedPayloadServiceTest {
    private EncryptedPayloadService service(){return new EncryptedPayloadService(Base64.getEncoder().encodeToString(new byte[32]));}

    @Test void encryptsAndDecryptsWithOwnerBinding(){
        var service=service(); var encrypted=service.encrypt(42,"private journal text");
        assertEquals(12,encrypted.iv().length);
        assertEquals("private journal text",service.decrypt(42,encrypted.iv(),encrypted.ciphertext()));
        assertNotEquals("private journal text",new String(encrypted.ciphertext()));
        assertThrows(IllegalStateException.class,()->service.decrypt(43,encrypted.iv(),encrypted.ciphertext()));
    }

    @Test void usesFreshIvForEachEntry(){
        var service=service(); var first=service.encrypt(42,"same text"); var second=service.encrypt(42,"same text");
        assertFalse(java.util.Arrays.equals(first.iv(),second.iv()));
        assertFalse(java.util.Arrays.equals(first.ciphertext(),second.ciphertext()));
    }

    @Test void rejectsMissingOrMalformedKey(){
        var service=new EncryptedPayloadService("");
        assertThrows(IllegalStateException.class,()->service.encrypt(1,"text"));
    }
}
