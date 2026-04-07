package com.coupon.config

import com.coupon.filter.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.core.GrantedAuthorityDefaults
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val objectMapper: ObjectMapper,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun grantedAuthorityDefaults(): GrantedAuthorityDefaults = GrantedAuthorityDefaults("")

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        // JWT 필터 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        http
            .cors { }
            .headers { it.frameOptions { option -> option.disable() } }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(CustomAuthenticationEntryPoint(objectMapper)) }

        http.authorizeHttpRequests { authorize ->
            // Swagger 인증
            authorize
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                ).permitAll()

            // 인증 없이 허용할 API
            authorize
                .requestMatchers(
                    "/signup",
                    "/signin",
                    "/load-test/**",
                ).permitAll()

            // 추가로 열어줄 API
            authorize.requestMatchers("/h2-console/**", "/actuator/**", "/ping").permitAll()

            // 나머지는 인증 필요 (권한 체크는 @PreAuthorize로 처리)
            authorize.anyRequest().authenticated()
        }

        return http.build()
    }
}
