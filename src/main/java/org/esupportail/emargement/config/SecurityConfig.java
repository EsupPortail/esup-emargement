package org.esupportail.emargement.config;

import java.util.Arrays;

import javax.annotation.Resource;

import org.esupportail.emargement.security.ContextCasAuthenticationProvider;
import org.esupportail.emargement.security.UserDetailsServiceImpl;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private AuthenticationProvider authenticationProvider;

    private AuthenticationEntryPoint authenticationEntryPoint;

    private SingleSignOutFilter singleSignOutFilter;

    private LogoutFilter logoutFilter;
    
    @Resource
    UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    public SecurityConfig(ContextCasAuthenticationProvider casAuthenticationProvider, AuthenticationEntryPoint eP,
                          LogoutFilter lF
                          , SingleSignOutFilter ssF) {
        this.authenticationProvider = casAuthenticationProvider;
        this.authenticationEntryPoint = eP;
        this.logoutFilter = lF;
        this.singleSignOutFilter = ssF;

    }

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		 http
			.anonymous().disable()
	        .authorizeRequests()
	        .regexMatchers("/login")
	        .authenticated()
	        .and()
	        .authorizeRequests()
	        .regexMatchers("/all/.*")
	        .hasRole("SUPER_ADMIN")
	        .and()
	        .authorizeRequests()
	        .regexMatchers("/[^/]*/admin(/.*|/?)")
	        .hasAnyRole("SUPER_ADMIN","ADMIN")
	        .and()
	        .authorizeRequests()
	        .regexMatchers("/[^/]*/manager(/.*|/?)")
	        .hasAnyRole("ADMIN", "MANAGER")
	        .and()
	        .authorizeRequests()
	        .regexMatchers("/[^/]*/supervisor(/.*|/?)")
	        .hasAnyRole("ADMIN","MANAGER","SUPERVISOR")
	        .and()	        
	        .authorizeRequests()
	        .regexMatchers("/")
	        .permitAll()
	        .and()
	        .httpBasic()
	        .authenticationEntryPoint(authenticationEntryPoint)
	        .and()
	        .logout().logoutSuccessUrl("/logout")
	        .and()
	        .addFilterBefore(singleSignOutFilter, CasAuthenticationFilter.class)
	        .addFilterBefore(logoutFilter, LogoutFilter.class)
	        .csrf().disable();

	}

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
      auth.authenticationProvider(authenticationProvider);
    }

    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
      return new ProviderManager(Arrays.asList(authenticationProvider));
    }

    @Bean
    public CasAuthenticationFilter casAuthenticationFilter(ServiceProperties sP) throws Exception {
      CasAuthenticationFilter filter = new CasAuthenticationFilter();
      filter.setServiceProperties(sP);
      filter.setAuthenticationManager(authenticationManager());
      return filter;
    }
    

    @Bean
    public SwitchUserFilter switchUserFilter() {
        SwitchUserFilter filter = new SwitchUserFilter();
        filter.setUserDetailsService(userDetailsServiceImpl);
        filter.setSwitchUserUrl("/all/superadmin/impersonate");
        filter.setSwitchFailureUrl("/all/superadmin/switchUser");
        filter.setTargetUrl("/");
        filter.setExitUserUrl("/logout/switchUser");
        return filter;
    }


}
