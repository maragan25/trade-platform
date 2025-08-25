package yes.example.no.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final SessionManagementFilter sessionManagementFilter;

    @Bean
    public FilterRegistrationBean<SessionManagementFilter> sessionFilter() {
        FilterRegistrationBean<SessionManagementFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(sessionManagementFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}