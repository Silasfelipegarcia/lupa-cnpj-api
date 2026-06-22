package br.com.dadoscnpj.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final String JDBC_QUERY =
            "useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    /**
     * Railway MySQL plugin exposes {@code MYSQL_URL} as {@code mysql://user:pass@host:port/db},
     * not as a JDBC URL. Spring/Hikari cannot connect with that format.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "MYSQL_URL")
    public DataSource railwayMySqlDataSource() {
        String mysqlUrl = System.getenv("MYSQL_URL");
        if (!StringUtils.hasText(mysqlUrl)) {
            throw new IllegalStateException("MYSQL_URL está definida mas vazia");
        }

        URI uri = URI.create(mysqlUrl.replaceFirst("^mysql://", "http://"));
        String jdbcUrl = "jdbc:mysql://" + uri.getHost() + ':' + uri.getPort() + uri.getPath()
                + '?' + JDBC_QUERY;

        String username = primeiroEnv("MYSQLUSER", "DB_USERNAME");
        String password = primeiroEnv("MYSQLPASSWORD", "DB_PASSWORD");

        if (!StringUtils.hasText(username) && uri.getUserInfo() != null) {
            String[] credenciais = uri.getUserInfo().split(":", 2);
            username = urlDecode(credenciais[0]);
            if (credenciais.length > 1) {
                password = urlDecode(credenciais[1]);
            }
        }

        log.info("DataSource Railway MySQL configurado (host={})", uri.getHost());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password != null ? password : "");
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    private static String primeiroEnv(String... nomes) {
        for (String nome : nomes) {
            String valor = System.getenv(nome);
            if (StringUtils.hasText(valor)) {
                return valor;
            }
        }
        return null;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
