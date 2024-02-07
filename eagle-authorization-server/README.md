# eagle-authorization-server


### 操作步骤
前期可完全根据spring官方文档[^1]操作

1. 添加依赖
>    implementation("org.springframework.security:spring-security-oauth2-authorization-server")



2. 配置文件
```yaml
# security 日志，一定开启方便排查问题
logging:
  level:
    org.springframework.security: trace

```

3. 添加SecurityConfig配置类
配置文件示例：

```kotlin

@Configuration
@EnableWebSecurity
class SecurityConfig {


    /**
     *
     * OAuth2AuthorizationEndpointConfigurer提供自定义OAuth2 授权端点的能力。它定义了扩展点，使您可以自定义OAuth2 授权请求的预处理、主处理和后处理逻辑。
     */
    @Bean
    @Order(1)
    @Throws(
        Exception::class
    )
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)
        http.getConfigurer<OAuth2AuthorizationServerConfigurer>(OAuth2AuthorizationServerConfigurer::class.java)
            .oidc(Customizer.withDefaults()) // Enable OpenID Connect 1.0
        http // Redirect to the login page when not authenticated from the
            // authorization endpoint
            .exceptionHandling { exceptions: ExceptionHandlingConfigurer<HttpSecurity?> ->
                exceptions.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint("/login"),
                    MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            } // Accept access tokens for User Info and/or Client Registration
            .oauth2ResourceServer { resourceServer: OAuth2ResourceServerConfigurer<HttpSecurity?> ->
                resourceServer.jwt(Customizer.withDefaults())
            }
        return http.build()
    }


    /**
     * Security  配置类 和  authorization Config 可以独立存在
     * 一个是 Security 的配置 一个是 authorization的
     */
    @Bean
    @Order(2)
    @Throws(
        Exception::class
    )
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests { authorize ->
            authorize.anyRequest().authenticated()
        } // Form login handles the redirect to the login page from the
            // authorization server filter chain
            .formLogin(Customizer.withDefaults())
        return http.build()
    }

    /**
     * Security 用户，用于登录security弹出的默认登录页面
     */
    @Bean
    fun userDetailsService(): UserDetailsService {
        val userDetails: UserDetails =
            User.withUsername("user").password("{noop}111111").roles("USER").build()
        return InMemoryUserDetailsManager(userDetails)
    }

    @Bean
    fun registeredClientRepository(): RegisteredClientRepository {
        val oidcClient =
            RegisteredClient.withId(UUID.randomUUID().toString()).clientId("oidc-client")
                .clientSecret("{noop}111111")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/oauth2/code/callback")
                .postLogoutRedirectUri("http://127.0.0.1:8080/").scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build()
        return InMemoryRegisteredClientRepository(oidcClient)
    }

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val keyPair = generateRsaKey()
        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey
        val rsaKey: RSAKey =
            RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString())
                .build()
        val jwkSet = JWKSet(rsaKey)
        return ImmutableJWKSet(jwkSet)
    }

    private fun generateRsaKey(): KeyPair {
        val keyPair: KeyPair = try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            keyPairGenerator.generateKeyPair()
        } catch (ex: Exception) {
            throw IllegalStateException(ex)
        }
        return keyPair
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext?>?): JwtDecoder {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
    }

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder().build()
    }
}

```

* @EnableWebSecurity 开启了security，默认有 Security 登录
* authorizationServerSecurityFilterChain用于开启 OAuth2 授权端点


* 授权码登录方式获取 token 流程
首先调用/oauth2/authorize跳转登录界面，登录后进行授权，跳转回调页面并携带授权码，然后通过/oauth2/token接口获取token
获取token，header 头携带 basic token, token生成方式为，base64(clientId:secret)

    - 获取授权码地址：
    http://127.0.0.1:8080/oauth2/authorize?response_type=code&client_id=oidc-client&scope=profile&openid&redirect_uri=http://127.0.0.1:8080/oauth2/code/callback
    
    - 登录界面
    
    ![img.png](img.png)
    - 授权页面

    ![img_1.png](img_1.png)   

    - 获取 token 地址：
    ```bash
    curl --location 'http://127.0.0.1:8080/oauth2/token' \
    --header 'Authorization: Basic b2lkYy1jbGllbnQ6c2VjcmV0' \
    --header 'Cookie: JSESSIONID=78952BB6C05EE7286A2ED1D942FE2B5D' \
    --form 'code="-YNgWDsqbDT4DhLeLfqiGTBT5A-JhUR-7cfut5lD5MfYtWYFDBMXedxQPuy_ZM5Mh9vI4OCjWCAOuzDq9-0USINZ0BJ1fZd-ih9tnU3qgcpQ-U7hd6bgrtGV0VqnaU68"' \
    --form 'grant_type="authorization_code"' \
    --form 'redirect_uri="http://127.0.0.1:8080/oauth2/code/callback"'
    ```

**NOTICE**：
1. 基于内存的security的用户配置，密码处理有两种方式
   1. 指定密码时，指定密码编码方式,比如：...password("{noop}111111")...
   2. 显示配置密码编码方式,
   ```kotlin
    @Bean
    fun password():PasswordEncoder{
        return DelegatingPasswordEncoder("noop", hashMapOf(Pair("noop",NoOpPasswordEncoder.getInstance())))
    }
   ```
2. 获取 token 需要POST请求，用 form data 方式提交参数，并且携带 basic token 




[^1]:https://docs.spring.io/spring-authorization-server/reference/getting-started.html