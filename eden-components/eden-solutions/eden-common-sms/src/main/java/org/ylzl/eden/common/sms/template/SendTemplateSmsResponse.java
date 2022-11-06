package org.ylzl.eden.common.sms.template;

import lombok.*;

import java.io.Serializable;

/**
 * 发送短信响应
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
public class SendTemplateSmsResponse implements Serializable {

	/**
	 * 处理成功
	 */
	private boolean success;

	/**
	 * 错误码
	 */
	private String errCode;

	/**
	 * 错误描述
	 */
	private String errMessage;
}