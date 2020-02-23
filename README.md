# Spring Tips: Kotlin Security

Hi, Spring fans! Welcome to another installment of _Spring Tips_. In this episode we're going to look at the new Kotlin DSL for Spring Security. I love Kotlin. I introduced Kotlin in several other Spring Tips videos: [The Kotlin Programming Language](https://spring.io/blog/2016/10/19/spring-tips-the-kotlin-programming-language), [Bootiful Kotlin Redux](https://spring.io/blog/2017/11/08/spring-tips-bootiful-kotlin-redux), and [Spring's Support for Coroutines](https://spring.io/blog/2019/06/12/spring-tips-spring-s-support-for-kotlin-coroutines). Some of those videos are _very_ old! There are already a number of different projects in the Spring diaspora that are shipping Kotlin DSLs. They include, among others, Spring Framework, Spring Webflux, Spring Data, Spring Cloud Contract and Spring Cloud Gateway. And now, Spring Security!  

Spring Security is an amazing project - it solves some of the hardest problems in the industry and helps people secure their applications. And, as if that weren't enough, it's displayed a steadfast determination to make security _easy_. If you ever used Spring Security in its earliest incarnations, you'd know that it required _loads_ of XML - pages! - to get anything done. That improved to the point where in Spring Security 3 you could lock down an applicatino with common sense defaults with one or two stanzas of XML. Then, in Spring Security 4 they introduced a Java DSL that gave people the power of their compilers to help them validate things. Gradually, over time, Spring Security also introduced common sense defaults. Nowadays, you can register a `UserDetailsService` and Spring Security will lock down all HTTP endpoints in a Servlet-based HTTP application and require authentication. Couldn't be easier! Or could it? In Spring Security 5.2, they introduced some much-appreciated refinements to Spring Security. Now, in addition to using the fluid Java config DSL of yore, there's also a new approach where you can provide a lambda and be given a context object that you can then use. No longer do you need to indent your Spring Security APIs for their intent to be understood! And now, in this installment, we're going to take things to the next level with a very quick look at the brand new Spring Security Kotlin DSL. 

Remember, Spring Security addresses two orthagonal concerns: authentication and authorization. Authentication answers the question: "_who_ is making the request?" Is it Josh, or Jane? Authorization answers the question "what permissions does the requester have once inside the system?" Authentication is all about plugging in identity providers. There are a million ways to do that (Active Directory, in-memory usernames and passwords, LDAP, SAML, etc.) It's more about plugging in implementations. We're just going to use an in-memory username and password authentication manager since we need it and that's not where the DSL really shines. 

DSLs are most useful not when swapping out implementations of a given type, but when describing rules or customizing behavior. So, we'll use the Spring Security DSL to customize the authorization behavior. 

The following is a Spring Boot-based application that uses Spring Security. It uses the functional bean registration DSL to programatically register beans. We talked about programatic bean registration [in a _Spring Tips_ video from waaaaay back in 2017](https://spring.io/blog/2017/03/01/spring-tips-programmatic-bean-registration-in-spring-framework-5). Granted, that video demonstrated its use in Java, but the application is basically the same in Kotlin: you register a bean by wrapping it a call to the `bean` function. 

The first bean is the `InMemoryUserDetailsManager`. The second bean is a functional HTTP endpoint, `/greetings`. When an HTTP request comes in, we extract the authenticated principal from the current request, extract the name and then build a `ServerResponse` whose body will be represented by a `Map<String,String>`. 

The interesting bit is the class `KotlinSecurityConfiguration`. It extends `WebSecurityConfigurerAdapter`. There are a few methods we might override there, but I chose to override the `configure` method to specify two things: that I wanted to opt-in to HTTP BASIC authentication, and to specify what routes require authentication and which are wide-open. The configuration below stipultes that all requests to `/greetings/**` (the `/greetings/` endpoint, and anything below it, like `/greetings/foo`) must be authenticated. The second rule says that everything else is wide-open. It's very important that the more specific rule - `/greetings/**` - come before the more wide-open rule. The rules are evaluated in order, from top to bottom. If we'd put the second rule first, then it would match for every request and we'd never need to evaluate the rule for `/greetings` - it would be left effectively wide-open! 

```kotlin
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

				fun user(user: String, pw: String, vararg roles: String) =
                    User.withDefaultPasswordEncoder().username(user).password(pw).roles(*roles).build()

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
```

## Conclusion 

In this installment, we introduced 