package br.ufes.moviemonitor.config;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;

public final class KafkaSupport {
    private KafkaSupport() {}
    public static KafkaProducer<String, String> producer() {
        Properties p = new Properties(); p.put("bootstrap.servers", Config.BOOTSTRAP);
        p.put("key.serializer", StringSerializer.class.getName()); p.put("value.serializer", StringSerializer.class.getName());
        p.put("acks", "all"); return new KafkaProducer<>(p);
    }
    public static KafkaConsumer<String, String> consumer(String group) {
        Properties p = new Properties(); p.put("bootstrap.servers", Config.BOOTSTRAP); p.put("group.id", group);
        p.put("key.deserializer", StringDeserializer.class.getName()); p.put("value.deserializer", StringDeserializer.class.getName());
        p.put("auto.offset.reset", "earliest"); p.put("enable.auto.commit", "false");
        return new KafkaConsumer<>(p);
    }
}
