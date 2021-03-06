package gcp.pubsub;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.reactive.messaging.Emitter;
import io.smallrye.reactive.messaging.annotations.Channel;

@ApplicationScoped
public class BeanUsingAnEmitter {

    @Inject
    @Channel("my-channel")
    Emitter<String> emitter;

    public void periodicallySendMessage() {
        final AtomicInteger counter = new AtomicInteger();
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    String message = "Hello " + counter.getAndIncrement();
                    System.out.println("Emitting: " + message);
                    emitter.send(message);
                },
                        5, 10, TimeUnit.SECONDS);
    }

}
