package org.ylzl.eden.spring.cloud.cat.annotations;

import java.lang.annotation.*;

/**
 * CatLogMetricForCount 注解集
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CatLogMetricForCounts {

	CatLogMetricForCount[] value();
}