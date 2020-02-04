package io.smallrye.reactive.messaging.ack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;

public class SpiedBeanHelper {

    protected static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private class Entry {
        final String value;
        final long timeStamp;

        public Entry(String value) {
            this.value = value;
            this.timeStamp = System.nanoTime();
        }

        String value() {
            return value;
        }

        long timestamp() {
            return timeStamp;
        }
    }

    private Map<String, List<Entry>> processed = new ConcurrentHashMap<>();
    private Map<String, List<Entry>> acknowledged = new ConcurrentHashMap<>();

    protected Publisher<Message<String>> source(String id) {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> CompletableFuture.runAsync(() -> {
                    nap();
                    acknowledged(id, payload);
                    nap();
                }, EXECUTOR)).build());
    }

    public List<String> received(String key) {
        if (processed.get(key) == null) {
            return Collections.emptyList();
        }
        return processed.get(key).stream().map(Entry::value).collect(Collectors.toList());
    }

    public List<String> acknowledged(String key) {
        if (acknowledged.get(key) == null) {
            return Collections.emptyList();
        }
        return acknowledged.get(key).stream().map(Entry::value).collect(Collectors.toList());
    }

    public List<Long> receivedTimeStamps(String key) {
        return processed.get(key).stream().map(Entry::timestamp).collect(Collectors.toList());
    }

    public List<Long> acknowledgeTimeStamps(String key) {
        return acknowledged.get(key).stream().map(Entry::timestamp).collect(Collectors.toList());
    }

    protected void processed(String name, String value) {
        processed.computeIfAbsent(name, x -> new CopyOnWriteArrayList<>()).add(new Entry(value));
    }

    protected void processed(String name, Message<String> value) {
        processed.computeIfAbsent(name, x -> new CopyOnWriteArrayList<>()).add(new Entry(value.getPayload()));
    }

    protected void acknowledged(String name, String value) {
        acknowledged.computeIfAbsent(name, x -> new CopyOnWriteArrayList<>()).add(new Entry(value));
    }

    protected void microNap() {
        try {
            Thread.sleep(1);
        } catch (Exception e) {
            // Ignore me.
        }
    }

    protected void nap() {
        try {
            Thread.sleep(10);
        } catch (Exception e) {
            // Ignore me.
        }
    }
}
