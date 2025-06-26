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

public class Consumer {
    public static void main(String[] args) {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer_group");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);
        String topicName = "api-data";
        consumer.subscribe(List.of(topicName));

        while (true) {
            ConsumerRecords<String, String> recordsRecus = consumer.poll(Duration.ofMillis(500));
            ObjectMapper mapper = new ObjectMapper();
            
            for (ConsumerRecord<String, String> rec : recordsRecus) {
                try {
                    // Parse JSON
                    JsonNode rootNode = mapper.readTree(rec.value());
                    JsonNode currentWeatherNode = rootNode.path("current_weather");

                    // Extraire les valeurs nécessaires
                    double temperature = currentWeatherNode.path("temperature").asDouble();
                    double windspeed = currentWeatherNode.path("windspeed").asDouble();
                    double winddirection = currentWeatherNode.path("winddirection").asDouble();
                    double elevation = rootNode.path("elevation").asDouble();

                    // Créer la métrique Prometheus pour la température
                    String temperatureMetric = String.format("weather_temperature{location=\"amiens\"} %f", temperature);
                    String windspeedMetric = String.format("weather_windspeed_kmh{location=\"amiens\"} %f", windspeed);
                    String winddirectionMetric = String.format("weather_winddirection_degrees{location=\"amiens\"} %f", winddirection);
                    String elevationMetric = String.format("weather_elevation_meters{location=\"amiens\"} %f", elevation);

                    // Ecrire dans les fichiers texte pour Node Exporter
                    try (FileWriter writer = new FileWriter("/app/metrics/temperature.prom", false)) {
                        writer.write(temperatureMetric + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try (FileWriter writer = new FileWriter("/app/metrics/windspeed.prom", false)) {
                        writer.write(windspeedMetric + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try (FileWriter writer = new FileWriter("/app/metrics/winddirection.prom", false)) {
                        writer.write(winddirectionMetric + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try (FileWriter writer = new FileWriter("/app/metrics/elevation.prom", false)) {
                        writer.write(elevationMetric + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (JsonProcessingException e) {
                    // Si pas JSON, afficher la valeur brute
                    System.out.println(rec.value());
                }
            }

            if (!recordsRecus.isEmpty()) {
                System.out.println();
            }
        }
    }
}
