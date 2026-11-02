package br.com.obrix.config;

import br.com.obrix.seguranca.AutenticacaoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutenticacaoInterceptor autenticacao;

    public WebConfig(AutenticacaoInterceptor autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacao)
                .addPathPatterns("/api/v1/**")
                // Publico: catalogo, busca e perfil publico (a conta so e exigida
                // na hora de solicitar), alem do cadastro e do login.
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/sessao",
                        "/api/v1/auth/sair",
                        "/api/v1/cadastro/**",
                        "/api/v1/catalogo/**",
                        "/api/v1/prestadores/busca",
                        "/api/v1/prestadores/*/publico");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // O front-end e servido pela propria aplicacao; em desenvolvimento,
        // tambem pode ser aberto direto do disco.
        registry.addMapping("/api/v1/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}
