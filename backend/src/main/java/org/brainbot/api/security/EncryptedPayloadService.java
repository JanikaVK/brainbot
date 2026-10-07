package org.brainbot.api.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EncryptedPayloadService {
    public record Encrypted(byte[] iv,byte[] ciphertext) { }
    private final String encodedKey; private final SecureRandom random=new SecureRandom();
    public EncryptedPayloadService(@Value("${brainbot.data-key-base64:}") String encodedKey){this.encodedKey=encodedKey;}
    private SecretKeySpec key(){
        try { byte[] decoded=Base64.getDecoder().decode(encodedKey); if(decoded.length!=32)throw new IllegalStateException(); return new SecretKeySpec(decoded,"AES"); }
        catch(Exception ex){throw new IllegalStateException("Journal encryption key is not configured correctly");}
    }
    public Encrypted encrypt(long userId,String value){
        try { byte[] iv=new byte[12];random.nextBytes(iv);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(),new GCMParameterSpec(128,iv));cipher.updateAAD(Long.toString(userId).getBytes(StandardCharsets.UTF_8));return new Encrypted(iv,cipher.doFinal(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception ex){throw new IllegalStateException("Journal content could not be protected");}
    }
    public String decrypt(long userId,byte[] iv,byte[] ciphertext){
        try {Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));cipher.updateAAD(Long.toString(userId).getBytes(StandardCharsets.UTF_8));return new String(cipher.doFinal(ciphertext),StandardCharsets.UTF_8);}
        catch(Exception ex){throw new IllegalStateException("Journal content could not be opened");}
    }
}
