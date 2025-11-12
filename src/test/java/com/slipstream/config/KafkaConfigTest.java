package com.slipstream.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        kafkaConfig = new KafkaConfig();
    }

    @Test
    void testDefaultConfiguration() {
        assertEquals("slipstream-anomaly-detector", kafkaConfig.getApplicationId());
        assertEquals("localhost:9092", kafkaConfig.getBootstrapServers());
        assertEquals("transactions", kafkaConfig.getInputTopic());
        assertEquals("anomalies", kafkaConfig.getOutputTopic());
        assertEquals("alerts", kafkaConfig.getAlertsTopic());
        assertEquals(1, kafkaConfig.getNumStreamThreads());
        assertEquals("/tmp/kafka-streams", kafkaConfig.getStateDir());
        assertEquals(30000L, kafkaConfig.getCommitIntervalMs());
        assertEquals(1, kafkaConfig.getReplicationFactor());
    }

    @Test
    void testGettersAndSetters() {
        kafkaConfig.setApplicationId("test-app");
        assertEquals("test-app", kafkaConfig.getApplicationId());

        kafkaConfig.setBootstrapServers("test-server:9092");
        assertEquals("test-server:9092", kafkaConfig.getBootstrapServers());

        kafkaConfig.setInputTopic("test-input");
        assertEquals("test-input", kafkaConfig.getInputTopic());

        kafkaConfig.setOutputTopic("test-output");
        assertEquals("test-output", kafkaConfig.getOutputTopic());

        kafkaConfig.setAlertsTopic("test-alerts");
        assertEquals("test-alerts", kafkaConfig.getAlertsTopic());

        kafkaConfig.setNumStreamThreads(4);
        assertEquals(4, kafkaConfig.getNumStreamThreads());

        kafkaConfig.setStateDir("/custom/state");
        assertEquals("/custom/state", kafkaConfig.getStateDir());

        kafkaConfig.setCommitIntervalMs(5000L);
        assertEquals(5000L, kafkaConfig.getCommitIntervalMs());

        kafkaConfig.setReplicationFactor(3);
        assertEquals(3, kafkaConfig.getReplicationFactor());
    }

    @Test
    void testStreamsProperties() {
        Properties props = kafkaConfig.getStreamsProperties();

        assertNotNull(props);
        assertEquals("slipstream-anomaly-detector", props.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));
        assertEquals("localhost:9092", props.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(Serdes.String().getClass(), props.get(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG));
        assertEquals(Serdes.String().getClass(), props.get(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG));
        assertEquals(1, props.get(StreamsConfig.NUM_STREAM_THREADS_CONFIG));
        assertEquals(30000L, props.get(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG));
        assertEquals(1048576L, props.get(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG));
        assertEquals("/tmp/kafka-streams", props.getProperty(StreamsConfig.STATE_DIR_CONFIG));
        assertEquals(1, props.get(StreamsConfig.REPLICATION_FACTOR_CONFIG));
        assertEquals("org.apache.kafka.streams.errors.LogAndContinueExceptionHandler", 
                    props.getProperty(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG));
        assertEquals(StreamsConfig.EXACTLY_ONCE_V2, props.getProperty(StreamsConfig.PROCESSING_GUARANTEE_CONFIG));
    }

    @Test
    void testConsumerProperties() {
        Properties props = kafkaConfig.getConsumerProperties();

        assertNotNull(props);
        assertEquals("localhost:9092", props.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("slipstream-anomaly-detector-consumer", props.getProperty(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals("org.apache.kafka.common.serialization.StringDeserializer",
                    props.getProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals("org.apache.kafka.common.serialization.StringDeserializer",
                    props.getProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals("earliest", props.getProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        assertEquals(true, props.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        assertEquals(5000, props.get(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
    }

    @Test
    void testProducerProperties() {
        Properties props = kafkaConfig.getProducerProperties();

        assertNotNull(props);
        assertEquals("localhost:9092", props.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("org.apache.kafka.common.serialization.StringSerializer",
                    props.getProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertEquals("org.apache.kafka.common.serialization.StringSerializer",
                    props.getProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
        assertEquals("all", props.getProperty(ProducerConfig.ACKS_CONFIG));
        assertEquals(3, props.get(ProducerConfig.RETRIES_CONFIG));
        assertEquals(16384, props.get(ProducerConfig.BATCH_SIZE_CONFIG));
        assertEquals(5, props.get(ProducerConfig.LINGER_MS_CONFIG));
        assertEquals(33554432, props.get(ProducerConfig.BUFFER_MEMORY_CONFIG));
    }

    @Test
    void testCustomConfiguration() {
        kafkaConfig.setApplicationId("custom-app-id");
        kafkaConfig.setBootstrapServers("custom-server:9093");
        kafkaConfig.setNumStreamThreads(8);
        kafkaConfig.setCommitIntervalMs(15000L);

        Properties streamsProps = kafkaConfig.getStreamsProperties();
        assertEquals("custom-app-id", streamsProps.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));
        assertEquals("custom-server:9093", streamsProps.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(8, streamsProps.get(StreamsConfig.NUM_STREAM_THREADS_CONFIG));
        assertEquals(15000L, streamsProps.get(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG));

        Properties consumerProps = kafkaConfig.getConsumerProperties();
        assertEquals("custom-server:9093", consumerProps.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("custom-app-id-consumer", consumerProps.getProperty(ConsumerConfig.GROUP_ID_CONFIG));

        Properties producerProps = kafkaConfig.getProducerProperties();
        assertEquals("custom-server:9093", producerProps.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }
}