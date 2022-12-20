/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ylzl.eden.dynamic.cache.support.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.support.AbstractCacheManager;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring 缓存管理器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
@RequiredArgsConstructor
@Slf4j
public class SpringCacheManager extends AbstractCacheManager {

	private Collection<? extends SpringCache> caches = Collections.emptySet();

	public void setCaches(Collection<? extends SpringCache> caches) {
		this.caches = caches;
	}

	@Override
	public @NotNull Collection<? extends SpringCache> loadCaches() {
		return this.caches;
	}
}
