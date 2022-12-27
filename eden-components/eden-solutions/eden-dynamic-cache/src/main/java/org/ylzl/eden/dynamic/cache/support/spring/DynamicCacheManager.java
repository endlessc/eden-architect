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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;
import org.ylzl.eden.dynamic.cache.CacheType;
import org.ylzl.eden.dynamic.cache.builder.CacheBuilder;
import org.ylzl.eden.dynamic.cache.config.CacheConfig;
import org.ylzl.eden.dynamic.cache.l1cache.L1CacheLoader;
import org.ylzl.eden.dynamic.cache.l1cache.L1CacheRemovalListener;
import org.ylzl.eden.extension.ExtensionLoader;

import java.util.Collection;
import java.util.Collections;

/**
 * 动态缓存管理器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
@Setter
@Getter
@RequiredArgsConstructor
@Slf4j
public class DynamicCacheManager extends AbstractCacheManager {

	private Collection<? extends DynamicCache> caches = Collections.emptySet();

	private final CacheConfig cacheConfig;

	private L1CacheRemovalListener l1CacheRemovalListener;

	private L1CacheLoader l1CacheLoader;

	private Object l2CacheClient;

	public void setCaches(Collection<? extends DynamicCache> caches) {
		this.caches = caches;
	}

	@Override
	public @NotNull Collection<? extends DynamicCache> loadCaches() {
		return this.caches;
	}

	@Override
	protected Cache getMissingCache(String name) {
		String type = cacheConfig.getCacheType();
		CacheBuilder cacheBuilder = ExtensionLoader.getExtensionLoader(CacheBuilder.class).getExtension(type);
		cacheBuilder.cacheName(name).cacheConfig(cacheConfig);

		if (l1CacheRemovalListener != null) {
			cacheBuilder.l1CacheRemovalListener(l1CacheRemovalListener);
		}
		if (l2CacheClient != null) {
			cacheBuilder.l2CacheClient(l2CacheClient);
		}

		CacheType cacheType = CacheType.parse(type);
		org.ylzl.eden.dynamic.cache.Cache cache = cacheType.getLevel() == 1 && l1CacheLoader != null ?
			cacheBuilder.build(l1CacheLoader) : cacheBuilder.build();
		return new DynamicCache(cacheConfig.isAllowNullValues(), name, cache);
	}
}
