package org.ylzl.eden.spring.integration.messagequeue.rocketmq;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.ylzl.eden.commons.lang.StringConstants;
import org.ylzl.eden.spring.integration.messagequeue.common.MessageQueueType;
import org.ylzl.eden.spring.integration.messagequeue.core.MessageQueueConsumer;
import org.ylzl.eden.spring.integration.messagequeue.core.MessageQueueListener;
import org.ylzl.eden.spring.integration.messagequeue.core.consumer.MessageQueueConsumerException;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RocketMQ 消费者
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
@RequiredArgsConstructor
@Slf4j
public class RocketMQConsumer implements InitializingBean, DisposableBean {

	private static final String INITIALIZING_ROCKETMQ_CONSUMER = "Initializing RocketMQConsumer";

	private static final String DESTROY_ROCKETMQ_CONSUMER = "Destroy RocketMQConsumer";

	private static final String ROCKETMQ_CONSUMER_CONSUME_ERROR = "RocketMQConsumer consume error: {}";

	private static final String CREATE_DEFAULT_MQPUSH_CONSUMER_GROUP_NAMESPACE_TOPIC = "Create DefaultMQPushConsumer, group: {}, namespace: {}, topic: {}";

	private final List<DefaultMQPushConsumer> consumers = Lists.newArrayList();

	private final RocketMQProperties rocketMQProperties;

	private final FixedRocketMQConsumerProperties fixedRocketMQConsumerProperties;

	private final List<MessageQueueConsumer> messageQueueConsumers;

	@Override
	public void afterPropertiesSet() throws Exception {
		log.debug(INITIALIZING_ROCKETMQ_CONSUMER);
		if (CollectionUtils.isEmpty(messageQueueConsumers)) {
			return;
		}
		try {
			for (MessageQueueConsumer messageQueueConsumer : messageQueueConsumers) {
				DefaultMQPushConsumer consumer = createConsumer(messageQueueConsumer);
				if (consumer == null) {
					continue;
				}
				consumers.add(consumer);
				consumer.registerMessageListener((MessageListenerConcurrently) (messageExts, context) -> {
					AtomicReference<ConsumeConcurrentlyStatus> status = new AtomicReference<>(ConsumeConcurrentlyStatus.RECONSUME_LATER);
					List<String> messages = Lists.newArrayListWithCapacity(messageExts.size());
					messageExts.forEach(messageExt -> messages.add(new String(messageExt.getBody())));
					messageQueueConsumer.consume(messages,
						() -> {
							if (status.get() != ConsumeConcurrentlyStatus.CONSUME_SUCCESS) {
								status.set(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
							}
						}
					);
					return status.get();
				});
				consumer.start();
			}
		} catch (MQClientException e) {
			log.error(ROCKETMQ_CONSUMER_CONSUME_ERROR, e.getMessage(), e);
			throw new MessageQueueConsumerException(e.getMessage());
		}
	}

	@Override
	public void destroy() throws Exception {
		log.debug(DESTROY_ROCKETMQ_CONSUMER);
		consumers.forEach(DefaultMQPushConsumer::shutdown);
	}

	private DefaultMQPushConsumer createConsumer(MessageQueueConsumer messageQueueConsumer) throws MQClientException {
		Class<? extends MessageQueueConsumer> clazz = messageQueueConsumer.getClass();
		MessageQueueListener annotation = clazz.getAnnotation(MessageQueueListener.class);

		if (StringUtils.isNotBlank(annotation.messageQueueType()) &&
			!MessageQueueType.ROCKETMQ.equalsIgnoreCase(annotation.messageQueueType())) {
			return null;
		}

		String namespace = null;
		if (StringUtils.isNotBlank(fixedRocketMQConsumerProperties.getNamespace())) {
			namespace = fixedRocketMQConsumerProperties.getNamespace();
		}

		String topic = null;
		if (StringUtils.isNotBlank(annotation.topic())) {
			topic = annotation.topic();
		} else if (StringUtils.isNotBlank(fixedRocketMQConsumerProperties.getTopic())) {
			topic = fixedRocketMQConsumerProperties.getTopic();
		}

		String group = null;
		if (StringUtils.isNotBlank(annotation.group())) {
			group = annotation.group();
		} else if (StringUtils.isNotBlank(fixedRocketMQConsumerProperties.getGroup())) {
			group = fixedRocketMQConsumerProperties.getGroup() + StringConstants.UNDERLINE + topic;
		}

		String selectorExpression = null;
		if (StringUtils.isNotBlank(annotation.selectorExpression())) {
			selectorExpression = annotation.selectorExpression();
		} else if (StringUtils.isNotBlank(fixedRocketMQConsumerProperties.getSelectorExpression())) {
			selectorExpression = fixedRocketMQConsumerProperties.getSelectorExpression();
		}

		RPCHook rpcHook = null;
		if (StringUtils.isNotBlank(fixedRocketMQConsumerProperties.getAccessKey()) &&
			StringUtils.isNotBlank(fixedRocketMQConsumerProperties.getSecretKey())) {
			rpcHook =  new AclClientRPCHook(new SessionCredentials(
				fixedRocketMQConsumerProperties.getAccessKey(),
				fixedRocketMQConsumerProperties.getSecretKey()));
		}

		int pullBatchSize = 32;
		if (annotation.pullBatchSize() > 0) {
			pullBatchSize = annotation.pullBatchSize();
		} else if (fixedRocketMQConsumerProperties.getPullBatchSize() > 0) {
			pullBatchSize = fixedRocketMQConsumerProperties.getPullBatchSize();
		}

		int consumeMessageBatchMaxSize = 1;
		if (annotation.consumeMessageBatchMaxSize() > 0) {
			consumeMessageBatchMaxSize = annotation.consumeMessageBatchMaxSize();
		}

		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(namespace, group, rpcHook);
		consumer.setNamesrvAddr(rocketMQProperties.getNameServer());
		consumer.subscribe(topic, selectorExpression);
		consumer.setPullBatchSize(pullBatchSize);
		consumer.setConsumeMessageBatchMaxSize(consumeMessageBatchMaxSize);
		consumer.setMessageModel(MessageModel.valueOf(fixedRocketMQConsumerProperties.getMessageModel()));

		log.debug(CREATE_DEFAULT_MQPUSH_CONSUMER_GROUP_NAMESPACE_TOPIC, group, namespace, topic);
		return consumer;
	}
}