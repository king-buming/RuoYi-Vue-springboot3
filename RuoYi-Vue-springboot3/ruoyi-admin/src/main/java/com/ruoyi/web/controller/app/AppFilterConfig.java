package com.ruoyi.web.controller.app;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppFilterConfig
{
    @Bean
    public FilterRegistrationBean<AppTokenFilter> appTokenFilterReg()
    {
        FilterRegistrationBean<AppTokenFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new AppTokenFilter());
        reg.addUrlPatterns("/app/*");
        reg.setName("appTokenFilter");
        reg.setOrder(1);
        return reg;
    }
}
