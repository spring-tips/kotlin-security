package com.example.kotlinsecurity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.support.beans
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.core.userdetails.User
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.router

@SpringBootApplication
class KotlinSecurityApplication

@EnableWebSecurity
class KotlinSecurityConfiguration : WebSecurityConfigurerAdapter() {

	override fun configure(http: HttpSecurity?) {
		http {
			httpBasic {}
			authorizeRequests {
				authorize("/greetings/**", hasAuthority("ROLE_ADMIN"))
				authorize("/**", permitAll)
			}
		}
	}
}

fun main(args: Array<String>) {
	runApplication<KotlinSecurityApplication>(*args) {
		addInitializers(beans {
			bean {
				fun user(user: String, pw: String, vararg roles: String) = User.withDefaultPasswordEncoder().username(user).password(pw).roles(*roles).build()
				InMemoryUserDetailsManager(user("jlong", "pw", "USER"), user("rwinch", "pw1", "USER", "ADMIN"))
			}
			bean {
				router {
					GET("/greetings") { request ->
						request.principal().map { it.name }.map { ServerResponse.ok().body(mapOf("greeting" to "Hello, $it")) }.orElseGet { ServerResponse.badRequest().build() }
					}
				}
			}
		})
	}
}