package me.hikaricp.yellowexchange.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

@Configuration
@EnableWebMvc
public class MvcConfig implements WebMvcConfigurer {

    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(dirName);
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        if (dirName.startsWith("../")) {
            dirName = dirName.replace("../", "");
        }

        registry.addResourceHandler("/" + dirName + "/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .setCacheControl(cacheControl());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory(Resources.USER_PROFILES_PHOTO_DIR, registry);
        exposeDirectory(Resources.DOMAIN_ICONS_DIR, registry);
        exposeDirectory(Resources.ADMIN_COIN_ICONS_DIR, registry);
        exposeDirectory(Resources.ADMIN_ICON_DIR, registry);
        exposeDirectory(Resources.SUPPORT_IMAGES, registry);
        exposeDirectory(Resources.USER_KYC_PHOTO_DIR, registry);

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(cacheControl());
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(cacheControl());
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/")
                .setCacheControl(cacheControl());
    }

    private CacheControl cacheControl() {
        return CacheControl.maxAge(Duration.ofDays(365));
    }

    @Bean
    @Primary
    public LocaleResolver localeResolver() {
        return new CompositeLocaleResolver(userLocaleResolver(), adminLocaleResolver());
    }

    @Bean
    public LocaleResolver userLocaleResolver() {
        CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
        cookieLocaleResolver.setCookieName("lang");
        cookieLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        return cookieLocaleResolver;
    }

    @Bean
    public LocaleChangeInterceptor userLocaleChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    @Bean
    public LocaleResolver adminLocaleResolver() {
        CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
        cookieLocaleResolver.setCookieName("panel_lang");
        cookieLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        return cookieLocaleResolver;
    }

    @Bean
    public LocaleChangeInterceptor adminLocaleChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("panel_lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminLocaleChangeInterceptor())
                .addPathPatterns("/admin/**", "/worker/**", "/supporter/**", "/api/admin/**", "/api/worker/**", "/api/supporter/**", "/panel/**");
        registry.addInterceptor(userLocaleChangeInterceptor());
    }

    @Bean
    public MessageSource userMessageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public SpringResourceTemplateResolver userTemplateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(1);
        resolver.setCheckExistence(true);
        return resolver;
    }

    @Bean
    @Primary
    public SpringTemplateEngine userTemplateEngine(
            @Qualifier("userMessageSource") MessageSource userMessageSource,
            @Qualifier("userTemplateResolver") SpringResourceTemplateResolver userTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(userTemplateResolver);
        engine.setTemplateEngineMessageSource(userMessageSource);
        return engine;
    }

    @Bean
    public ThymeleafViewResolver userViewResolver(
            @Qualifier("userTemplateEngine") SpringTemplateEngine userTemplateEngine) {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(userTemplateEngine);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setContentType("text/html; charset=UTF-8");
        resolver.setOrder(1);
        resolver.setViewNames(new String[]{"exchange/*", "exchange/**"});
        return resolver;
    }

    @Bean
    public MessageSource adminMessageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:panel_messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public SpringResourceTemplateResolver adminTemplateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(2);
        resolver.setCheckExistence(true);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine adminTemplateEngine(
            @Qualifier("adminMessageSource") MessageSource adminMessageSource,
            @Qualifier("adminTemplateResolver") SpringResourceTemplateResolver adminTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(adminTemplateResolver);
        engine.setTemplateEngineMessageSource(adminMessageSource);
        return engine;
    }

    @Bean
    public ThymeleafViewResolver adminViewResolver(
            @Qualifier("adminTemplateEngine") SpringTemplateEngine adminTemplateEngine) {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(adminTemplateEngine);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setContentType("text/html; charset=UTF-8");
        resolver.setOrder(2);
        resolver.setViewNames(new String[]{"panel/*", "panel/**"});
        return resolver;
    }
}

