package org.entur.example.web.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf( c -> c.disable() )
				.authorizeHttpRequests((authorize) -> {
					authorize.requestMatchers("/api/secured/endpoint").fullyAuthenticated();

					authorize.anyRequest().permitAll();
					}
				);
				http.addFilterBefore(new ReturnHttp401AuthenticationHeaderFilter(), UsernamePasswordAuthenticationFilter.class);
				http.addFilterBefore(new UserIdEnricherFilter(), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}