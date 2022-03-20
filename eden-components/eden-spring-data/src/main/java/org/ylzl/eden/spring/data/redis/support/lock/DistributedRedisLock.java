/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ylzl.eden.spring.data.redis.support.lock;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.ylzl.eden.commons.lang.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Redis 分布式锁
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
public class DistributedRedisLock extends AbstractRedisLock {

	private static final String UNLOCK_LUA =
		"if redis.call(\"get\",KEYS[1]) == ARGV[1] "
			+ "then return redis.call(\"del\",KEYS[1])"
			+ "else return 0 end";
	private final RedisTemplate<String, Object> redisTemplate;
	private final ThreadLocal<String> lock = new ThreadLocal<String>();

	public DistributedRedisLock(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public boolean lock(String key, int secondsToExpire, int retryTimes, long sleepMillis) {
		boolean result = set(key, secondsToExpire);
		while ((!result) && retryTimes-- > 0) {
			try {
				Thread.sleep(sleepMillis);
			} catch (InterruptedException e) {
				return false;
			}
			result = set(key, secondsToExpire);
		}
		return result;
	}

	@Override
	public boolean unlock(final String key) {
		final List<String> keys = Collections.singletonList(key);
		final List<String> args = Collections.singletonList(lock.get());
		Long result =
			redisTemplate.execute(
				(RedisCallback<Long>) connection -> { // 集群模式不支持执行 LUA 脚本
					Object nativeConnection = connection.getNativeConnection();
					if (nativeConnection instanceof JedisCluster) { // 集群模式
						return (Long) ((JedisCluster) nativeConnection).eval(UNLOCK_LUA, keys, args);
					}
					if (nativeConnection instanceof Jedis) {
						return (Long) ((Jedis) nativeConnection).eval(UNLOCK_LUA, keys, args);
					}
					return 0L;
				});

		return result != null && result > 0L;
	}

	private boolean set(final String key, final int secondsToExpire) {
		String result =
			redisTemplate.execute(
				(RedisCallback<String>) connection -> {
					JedisCommands commands = (JedisCommands) connection.getNativeConnection();
					String value = UUID.randomUUID().toString();
					lock.set(value);
					SetParams setParams = new SetParams();
					setParams.ex(secondsToExpire);
					return commands.set(key, value, setParams);
				});
		return StringUtils.isNotEmpty(result);
	}
}
