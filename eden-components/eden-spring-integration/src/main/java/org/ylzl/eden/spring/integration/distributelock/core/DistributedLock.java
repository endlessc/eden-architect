package org.ylzl.eden.spring.integration.distributelock.core;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
public interface DistributedLock {

	/**
	 * 阻塞加锁
	 *
	 * @param key 锁对象
	 */
	boolean lock(String key) throws DistributedLockException;

	/**
	 * 加锁
	 *
	 * @param key      锁对象
	 * @param waitTime 等待时间
	 * @param timeUnit 时间单位
	 * @throws DistributedLockException
	 * @return
	 */
	boolean lock(String key, int waitTime, TimeUnit timeUnit) throws DistributedLockException;

	/**
	 * 释放锁
	 *
	 * @param key      锁对象
	 * @throws DistributedLockException
	 */
	void unlock(String key) throws DistributedLockException;
}
