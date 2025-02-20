package com.kafka.streams.topology;

import com.kafka.streams.domain.Greeting;
import com.kafka.streams.serdes.SerdesFactory;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.kafka.streams.util.GreetingsStreamUtil.DESTINATION_TOPIC;
import static com.kafka.streams.util.GreetingsStreamUtil.SOURCE_TOPIC;
import static com.kafka.streams.util.GreetingsStreamUtil.SOURCE_TOPIC_SPANISH;

public class GreetingsTopology {

    public static Topology buildStringSerdeTopology() {

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        // 1. Source Processing
        KStream<String, String> greetingsStream = streamsBuilder.stream(SOURCE_TOPIC);
        KStream<String, String> greetingsStreamSpanish = streamsBuilder.stream(SOURCE_TOPIC_SPANISH);

        KStream<String, String> mergedKStream = greetingsStream.merge(greetingsStreamSpanish);
        // behind the scene, source processing uses consumer apis

        greetingsStream.print(Printed.<String, String>toSysOut().withLabel("GreetingsStringStream"));

        // 2. Stream Processing
        KStream<String, String> greetingsStreamModified = mergedKStream
                .filter((key, value) -> value.length() > 5)
                .map((key, value) -> KeyValue.pair(key.toUpperCase(), value.toUpperCase()))
                .flatMap((key, value) -> {
                    List<String> strings = Arrays.asList(value.split(""));
                    return strings.stream()
                            .map(val -> KeyValue.pair(key, val))
                            .collect(Collectors.toList());
                });

        // map(): use map() if you want to transform both key and value.
        // mapValues(): use mapValues() if you just want to transform the value.

        greetingsStreamModified.print(Printed.<String, String>toSysOut().withLabel("GreetingsStringStreamModified"));

        // 3. Sink Processing
        greetingsStreamModified.to(DESTINATION_TOPIC);
        // behind the scene, sink processing uses producer apis

        return streamsBuilder.build();
    }

    public static Topology buildCustomSerdeTopology() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        // 1. Source Processing
        KStream<String, Greeting> greetingsStream = streamsBuilder.stream(SOURCE_TOPIC, Consumed.with(Serdes.String(), SerdesFactory.greetingSerde()));
        KStream<String, Greeting> greetingsStreamSpanish = streamsBuilder.stream(SOURCE_TOPIC_SPANISH, Consumed.with(Serdes.String(), SerdesFactory.greetingSerde()));

        KStream<String, Greeting> mergedKStream = greetingsStream.merge(greetingsStreamSpanish);
        // behind the scene, source processing uses consumer apis

        greetingsStream.print(Printed.<String, Greeting>toSysOut().withLabel("GreetingsCustomStream"));

        // 2. Stream Processing
        KStream<String, Greeting> greetingsStreamModified = mergedKStream
                .mapValues((readOnlyKey, value) -> new Greeting(value.getMessage().toUpperCase(), value.getTimeStamp()));

        // map(): use map() if you want to transform both key and value.
        // mapValues(): use mapValues() if you just want to transform the value.

        greetingsStreamModified.print(Printed.<String, Greeting>toSysOut().withLabel("GreetingsCustomStreamModified"));

        // 3. Sink Processing
        greetingsStreamModified.to(DESTINATION_TOPIC, Produced.with(Serdes.String(), SerdesFactory.greetingSerde()));
        // behind the scene, sink processing uses producer apis

        return streamsBuilder.build();

    }
}
