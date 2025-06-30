package isri.kafka;

import java.util.List;
import java.util.Properties;

import java.time.Duration;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.FileWriter;
import java.io.IOException;

public class Consumer
{
    public static void main(String[] args)
    {
        // Kafka setup
        Properties consumerProperties = new Properties();

        // Multiple Kafka brokers for redundancy
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");

        // How to handle deserialization of messages
        // The topic contains JSON strings, so we use StringDeserializer
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Consumer group ID for tracking offsets
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer_group");

        // Creating the Kafka consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);

        // will read from the topic "api-data"
        String topicName = "api-data";
        consumer.subscribe(List.of(topicName));

        // Start consuming messages
        while(true)
        {
            // Poll for new messages from the topic, with a timeout of 10 seconds
            ConsumerRecords<String, String> recordsReceived = consumer.poll(Duration.ofMillis(10000));
            ObjectMapper mapper = new ObjectMapper();  // Jackson ObjectMapper for JSON parsing
            
            // If no messages were received, continue to the next iteration
            for(ConsumerRecord<String, String> rec : recordsReceived)
            {
                try
                {
                    // Parsing the JSON message
                    JsonNode rootNode = mapper.readTree(rec.value());  // Convert the message to a JsonNode
                    JsonNode currentWeatherNode = rootNode.path("current_weather");  // Navigate to the "current_weather" node

                    // Extracting some interesting values from the JSON message
                    double temperature = currentWeatherNode.path("temperature").asDouble();
                    double windspeed = currentWeatherNode.path("windspeed").asDouble();
                    double winddirection = currentWeatherNode.path("winddirection").asDouble();
                    double elevation = rootNode.path("elevation").asDouble();

                    // Formatting the metrics to Prometheus format
                    String temperatureMetric = String.format("weather_temperature{location=\"amiens\"} %f", temperature);
                    String windspeedMetric = String.format("weather_windspeed_kmh{location=\"amiens\"} %f", windspeed);
                    String winddirectionMetric = String.format("weather_winddirection_degrees{location=\"amiens\"} %f", winddirection);
                    String elevationMetric = String.format("weather_elevation_meters{location=\"amiens\"} %f", elevation);

                    // Writing the metrics to files in the Prometheus format
                    // Note : ==> false = overwrite
                    try(FileWriter writer = new FileWriter("/app/metrics/temperature.prom", false))
                    {
                        writer.write(temperatureMetric + "\n");
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }

                    try(FileWriter writer = new FileWriter("/app/metrics/windspeed.prom", false))
                    {
                        writer.write(windspeedMetric + "\n");
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }

                    try(FileWriter writer = new FileWriter("/app/metrics/winddirection.prom", false))
                    {
                        writer.write(winddirectionMetric + "\n");
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }

                    try(FileWriter writer = new FileWriter("/app/metrics/elevation.prom", false))
                    {
                        writer.write(elevationMetric + "\n");
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }

                }
                catch(JsonProcessingException e)
                {
                    // If this is thrown, it means the message was not a valid JSON
                    // Printing it, for debug purposes in docker logs
                    System.out.println(rec.value());
                }
            }
        }
    }
}
