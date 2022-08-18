package org.ylzl.eden.spring.integration.cat.integration.rest.interceptor;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Http 客户端链路过滤器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
public class RestTemplateTraceInterceptor implements ClientHttpRequestInterceptor {

	public static final String HTTP_CLIENT_REQUEST = "Http.client.request";

	@Override
	public @NotNull ClientHttpResponse intercept(HttpRequest req, byte[] body,
												 ClientHttpRequestExecution execution) throws IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		Transaction t = Cat.newTransaction(HTTP_CLIENT_REQUEST, request.getRequestURI());

		try {
			return execution.execute(req, body);
		} catch (Exception e) {
			t.setStatus(e);
			Cat.logError(e);
			throw e;
		} finally {
			t.complete();
		}
	}
}
