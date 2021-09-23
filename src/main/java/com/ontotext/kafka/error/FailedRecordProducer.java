package com.ontotext.kafka.error;

import com.google.common.annotations.VisibleForTesting;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.connect.runtime.SinkConnectorConfig;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

class FailedRecordProducer implements FailedProducer {

	private static final Logger LOGGER = LoggerFactory.getLogger(FailedRecordProducer.class);
	private final String topicName;
	private final Producer<String, String> producer;

	FailedRecordProducer(Properties properties) {
		producer = new KafkaProducer<>(properties);
		topicName = properties.getProperty(SinkConnectorConfig.DLQ_TOPIC_NAME_CONFIG);
	}

	@VisibleForTesting
	FailedRecordProducer(Producer<String, String> producer) {
		this.producer = producer;
		this.topicName = "test";
	}

	@Override
	public void returnFailed(SinkRecord record) {
		if (topicName != null && !topicName.isBlank()) {
			String recordKey = record.key() == null ? "null" : record.key().toString();
			String recordValue = record.value() == null ? "null" : record.value().toString();
			try {
				ProducerRecord<String, String> pr = new ProducerRecord<>(topicName, recordKey, recordValue);
				producer.send(pr, (metadata, exception) -> {
					if (exception == null) {
						LOGGER.info("Successfully returned failed record to kafka. Record (key={} value={}) meta(partition={}, offset={}) to kafka topic: {}",
							recordKey, recordValue, metadata == null ? 0 : metadata.partition(), metadata == null ? 0 : metadata.offset(), topicName);
					} else {
						LOGGER.error("Returning failed record to kafka: UNSUCCESSFUL. Record (key={} value={}) meta(partition={}, offset={}) to kafka topic: {}",
							recordKey, recordValue, metadata == null ? 0 : metadata.partition(), metadata == null ? 0 : metadata.offset(), topicName,
							exception);
					}
				});
			} finally {
				producer.flush();
			}
		}
	}

}
