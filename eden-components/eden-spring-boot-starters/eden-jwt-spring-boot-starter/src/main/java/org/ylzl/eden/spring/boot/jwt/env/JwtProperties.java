package org.ylzl.eden.spring.boot.jwt.env;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * JWT 配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

	private boolean enabled;

	private String header = "Authorization";

	private String base64Secret;

	private String secret;

	private long tokenValidityInSeconds = 1800;

	private long tokenValidityInSecondsForRememberMe = 2592000;

	private List<String> anonymousUrls;

	private List<String> permitAllUrls;

	private List<String> authenticatedUrls;
}
