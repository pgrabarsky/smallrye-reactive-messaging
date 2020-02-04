package io.smallrye.reactive.messaging.ack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;

@ApplicationScoped
public class BeanWithProcessorsManipulatingPayloads extends SpiedBeanHelper {

    static final String NO_ACKNOWLEDGMENT = "no-acknowledgment";
    static final String NO_ACKNOWLEDGMENT_CS = "no-acknowledgment-cs";

    static final String PRE_ACKNOWLEDGMENT = "pre-acknowledgment";
    static final String PRE_ACKNOWLEDGMENT_CS = "pre-acknowledgment-cs";

    static final String POST_ACKNOWLEDGMENT = "post-acknowledgment";
    static final String POST_ACKNOWLEDGMENT_CS = "post-acknowledgment-cs";

    static final String DEFAULT_ACKNOWLEDGMENT = "default-acknowledgment";
    static final String DEFAULT_ACKNOWLEDGMENT_CS = "default-acknowledgment-cs";

    @Incoming("sink-" + NO_ACKNOWLEDGMENT_CS)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkNoCS(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + NO_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkNo(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PRE_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkPre(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PRE_ACKNOWLEDGMENT_CS)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkPreCS(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + POST_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public CompletionStage<Void> sinkPost(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + POST_ACKNOWLEDGMENT_CS)
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public CompletionStage<Void> sinkPostCS(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + DEFAULT_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public CompletionStage<Void> sinkDefault(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + DEFAULT_ACKNOWLEDGMENT_CS)
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public CompletionStage<Void> sinkDefaultCS(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming(NO_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    @Outgoing("sink-" + NO_ACKNOWLEDGMENT)
    public String processorWithNoAck(String input) {
        processed(NO_ACKNOWLEDGMENT, input);
        return input + "1";
    }

    @Outgoing(NO_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToNoAck() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(NO_ACKNOWLEDGMENT, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(NO_ACKNOWLEDGMENT_CS)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    @Outgoing("sink-" + NO_ACKNOWLEDGMENT_CS)
    public CompletionStage<String> processorWithNoAckCS(String input) {
        return CompletableFuture.completedFuture(input)
                .thenApply(m -> {
                    processed(NO_ACKNOWLEDGMENT_CS, input);
                    return m + "1";
                });
    }

    @Outgoing(NO_ACKNOWLEDGMENT_CS)
    public Publisher<Message<String>> sourceToNoAckCS() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(NO_ACKNOWLEDGMENT_CS, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(PRE_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    @Outgoing("sink-" + PRE_ACKNOWLEDGMENT)
    public String processorWithPreAck(String input) {
        processed(PRE_ACKNOWLEDGMENT, input);
        return input + "1";
    }

    @Outgoing(PRE_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToPreAck() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PRE_ACKNOWLEDGMENT, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(PRE_ACKNOWLEDGMENT_CS)
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    @Outgoing("sink-" + PRE_ACKNOWLEDGMENT_CS)
    public CompletionStage<String> processorWithPreAckCS(String input) {
        processed(PRE_ACKNOWLEDGMENT_CS, input);
        return CompletableFuture.completedFuture(input + "1");
    }

    @Outgoing(PRE_ACKNOWLEDGMENT_CS)
    public Publisher<Message<String>> sourceToPreAckCS() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PRE_ACKNOWLEDGMENT_CS, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(POST_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    @Outgoing("sink-" + POST_ACKNOWLEDGMENT)
    public String processorWithPostAck(String input) {
        processed(POST_ACKNOWLEDGMENT, input);
        return input + "1";
    }

    @Outgoing(POST_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToPostAck() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(POST_ACKNOWLEDGMENT, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(POST_ACKNOWLEDGMENT_CS)
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    @Outgoing("sink-" + POST_ACKNOWLEDGMENT_CS)
    public CompletionStage<String> processorWithPostAckCS(String input) {
        processed(POST_ACKNOWLEDGMENT_CS, input);
        return CompletableFuture.completedFuture(input + "1");
    }

    @Outgoing(POST_ACKNOWLEDGMENT_CS)
    public Publisher<Message<String>> sourceToPostCSAck() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(POST_ACKNOWLEDGMENT_CS, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(DEFAULT_ACKNOWLEDGMENT)
    @Outgoing("sink-" + DEFAULT_ACKNOWLEDGMENT)
    public String processorWithDefaultAck(String input) {
        processed(DEFAULT_ACKNOWLEDGMENT, input);
        return input + "1";
    }

    @Outgoing(DEFAULT_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToDefaultAck() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(DEFAULT_ACKNOWLEDGMENT, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(DEFAULT_ACKNOWLEDGMENT_CS)
    @Outgoing("sink-" + DEFAULT_ACKNOWLEDGMENT_CS)
    public CompletionStage<String> processorWithDefaultAckCS(String input) {
        processed(DEFAULT_ACKNOWLEDGMENT_CS, input);
        return CompletableFuture.completedFuture(input + "1");
    }

    @Outgoing(DEFAULT_ACKNOWLEDGMENT_CS)
    public Publisher<Message<String>> sourceToDefaultAckCS() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(DEFAULT_ACKNOWLEDGMENT_CS, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

}
