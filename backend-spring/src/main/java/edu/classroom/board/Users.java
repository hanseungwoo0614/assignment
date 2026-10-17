package edu.classroom.board;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class Users {
  public record PublicUser(long id, String name, String email) {}

  private record Account(PublicUser user, byte[] salt, byte[] hash) {}

  private final List<Account> accounts = new ArrayList<>();

  public Users() {
    for (int i = 0; i < 2; i++) {
      String name = i == 0 ? "Alice" : "Bob";
      byte[] salt = new byte[16];
      new SecureRandom().nextBytes(salt);
      accounts.add(
          new Account(
              new PublicUser(i + 1, name, name.toLowerCase() + "@class.test"),
              salt,
              hash("Classroom!2026", salt)));
    }
  }

  private byte[] hash(String password, byte[] salt) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210000, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    } finally {
      spec.clearPassword();
    }
  }

  public PublicUser find(long id) {
    return accounts.stream()
        .filter(a -> a.user.id() == id)
        .map(Account::user)
        .findFirst()
        .orElse(null);
  }

  public PublicUser verify(String email, String password) {
    Account found =
        accounts.stream().filter(a -> a.user.email().equals(email)).findFirst().orElse(null);
    Account reference = found == null ? accounts.get(0) : found;
    boolean ok = MessageDigest.isEqual(hash(password, reference.salt), reference.hash);
    return found != null && ok ? found.user : null;
  }
}
