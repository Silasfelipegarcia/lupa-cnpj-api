import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptHashOnce {
    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "";
        System.out.print(new BCryptPasswordEncoder(12).encode(password));
    }
}
