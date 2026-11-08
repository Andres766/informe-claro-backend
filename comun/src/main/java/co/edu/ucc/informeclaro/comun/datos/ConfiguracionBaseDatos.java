package co.edu.ucc.informeclaro.comun.datos;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Conexión a la base de datos ÚNICA que comparten los microservicios (es una demo:
 * cada servicio maneja solo sus propias tablas, pero todas viven en la misma base).
 *
 * <p>Acepta la URL tal como la entregan Render, Neon o Supabase
 * ({@code postgres://usuario:clave@host:puerto/base}) o una URL JDBC directa.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionBaseDatos {

    @Bean
    public DataSource dataSource(
            @Value("${informeclaro.base-datos.url}") String url,
            @Value("${informeclaro.base-datos.usuario:}") String usuario,
            @Value("${informeclaro.base-datos.clave:}") String clave,
            @Value("${informeclaro.base-datos.pool:3}") int pool) {
        HikariConfig config = new HikariConfig();
        ConexionJdbc conexion = ConexionJdbc.desde(url, usuario, clave);
        config.setJdbcUrl(conexion.jdbcUrl());
        config.setUsername(conexion.usuario());
        config.setPassword(conexion.clave());
        config.setMaximumPoolSize(pool); // varios servicios comparten la misma base gratuita
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    /** URL JDBC + credenciales a partir de una URL de PostgreSQL o JDBC. */
    public record ConexionJdbc(String jdbcUrl, String usuario, String clave) {

        public static ConexionJdbc desde(String url, String usuario, String clave) {
            String limpia = url.trim();
            if (limpia.startsWith("jdbc:")) {
                return new ConexionJdbc(limpia, usuario, clave);
            }
            if (!limpia.startsWith("postgres://") && !limpia.startsWith("postgresql://")) {
                throw new IllegalArgumentException("URL de base de datos no soportada: debe ser postgres:// o jdbc:");
            }
            URI uri = URI.create(limpia.replaceFirst("^postgres(ql)?://", "postgresql://"));
            String usuarioUrl = usuario;
            String claveUrl = clave;
            if (uri.getRawUserInfo() != null) {
                String[] partes = uri.getRawUserInfo().split(":", 2);
                usuarioUrl = decodificar(partes[0]);
                claveUrl = partes.length > 1 ? decodificar(partes[1]) : "";
            }
            int puerto = uri.getPort() > 0 ? uri.getPort() : 5432;
            String consulta = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
            String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + puerto + uri.getRawPath() + consulta;
            return new ConexionJdbc(jdbc, usuarioUrl, claveUrl);
        }

        private static String decodificar(String texto) {
            return URLDecoder.decode(texto, StandardCharsets.UTF_8);
        }
    }
}
