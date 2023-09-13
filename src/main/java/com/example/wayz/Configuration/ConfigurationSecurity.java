package com.example.wayz.Configuration;

import com.example.wayz.Service.UserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ConfigurationSecurity {

    private final UserDetailsService userDetailsService;

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {

        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(new BCryptPasswordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf().disable()
                .sessionManagement()

                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and()
                .authenticationProvider(daoAuthenticationProvider())
                .authorizeHttpRequests()

                .requestMatchers("/api/v1/auth/register/**").permitAll()
                .requestMatchers("/api/v1/driver//get-top").permitAll()
                .requestMatchers("/api/v1/driver/update/{updatedName}").hasAuthority("DRIVER")
                .requestMatchers("/api/v1/driver/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/car/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/driver-trips/get").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/driver-trips/**").hasAuthority("DRIVER")
                .requestMatchers("/api/v1/files/get-allowed").permitAll()
                .requestMatchers("/api/v1/files/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/orders/add").hasAuthority("STUDENT")
                .requestMatchers("/api/v1/orders/update/{id}").hasAuthority("STUDENT")
                .requestMatchers("/api/v1/orders/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/report/add-report/{id}").hasAuthority("STUDENT")
                .requestMatchers("/api/v1/report/delete-report/{id}").hasAuthority("STUDENT")
                .requestMatchers("/api/v1/report/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/student/update/{id}").hasAuthority("STUDENT")
                .requestMatchers("/api/v1/student/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/student-trips/get-all").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/student-trips/**").hasAnyAuthority("STUDENT", "ADMIN")
                .requestMatchers("/api/v1/user-trips/complete/{userTripId}").hasAuthority("DRIVER")
                .requestMatchers(HttpMethod.POST, "/api/v1/user-trips/cashOut").hasAuthority("DRIVER")


                .requestMatchers("/api/v1/user-trips/**").hasAuthority("ADMIN")


                //// TODO add student trip & user trip
                .anyRequest().permitAll()
                .and()
                .logout().logoutUrl("/api/v1/auth/logout")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .and()
                .httpBasic();
        return http.build();

    }

}
