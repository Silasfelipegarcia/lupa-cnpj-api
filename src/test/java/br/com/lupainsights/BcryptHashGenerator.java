package br.com.lupainsights;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptHashGenerator {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder(12).encode("feste@123"));
    }
}
