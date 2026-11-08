package co.edu.ucc.informeclaro.infraestructura.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/** CORS para el frontend (Vercel / localhost). CORS_ORIGINS acepta una lista separada por comas. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origenes;

    public CorsConfig(@Value("${informeclaro.cors-origins}") String origenes) {
        this.origenes = Arrays.stream(origenes.replace("[", "").replace("]", "").split(","))
                .map(o -> o.strip().replace("\"", "").replace("'", ""))
                .map(o -> o.endsWith("/") ? o.substring(0, o.length() - 1) : o)
                .filter(o -> !o.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**").allowedOrigins(origenes).allowedMethods("*").allowedHeaders("*");
    }
}
