package isri.kafka;

import java.util.List;
import java.util.Properties;

import java.time.Duration;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Consumer
{
    public static void main(String[] args)
    {
        Properties consumerProperties = new Properties();

        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");

        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer_group");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);

        String topicName="api-data";
        consumer.subscribe(List.of(topicName));

        while(true)
        {

            ConsumerRecords<String, String> recordsRecus = consumer.poll(Duration.ofMillis(500));

            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

            for (ConsumerRecord<String, String> rec : recordsRecus)
            {
                try
                {
                    Object json = mapper.readValue(rec.value(), Object.class);
                    String prettyJson = writer.writeValueAsString(json);
                    System.out.println(prettyJson);
                }
                catch (JsonProcessingException e)
                {
                    // If not JSON, print the raw value
                    System.out.println(rec.value());
                }
            }
            if(!recordsRecus.isEmpty())
            {
                System.out.println();
            }
        }

        // Closing the consumer (for good practice, though this code runs indefinitely)
        // consumer.close();
    }
}
