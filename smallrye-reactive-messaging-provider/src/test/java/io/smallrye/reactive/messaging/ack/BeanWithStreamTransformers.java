package io.smallrye.reactive.messaging.ack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;

@ApplicationScoped
public class BeanWithStreamTransformers extends SpiedBeanHelper {

    static final String NO_ACKNOWLEDGMENT_BUILDER = "no-acknowledgment-builder";
    static final String NO_ACKNOWLEDGMENT = "no-acknowledgment";

    static final String PAYLOAD_NO_ACKNOWLEDGMENT_BUILDER = "payload-no-acknowledgment-builder";
    static final String PAYLOAD_NO_ACKNOWLEDGMENT = "payload-no-acknowledgment";

    static final String MANUAL_ACKNOWLEDGMENT = "manual-acknowledgment";
    static final String MANUAL_ACKNOWLEDGMENT_BUILDER = "manual-acknowledgment-builder";

    static final String PRE_ACKNOWLEDGMENT = "pre-acknowledgment";
    static final String PRE_ACKNOWLEDGMENT_BUILDER = "pre-acknowledgment-builder";

    static final String PAYLOAD_PRE_ACKNOWLEDGMENT = "payload-pre-acknowledgment";
    static final String PAYLOAD_PRE_ACKNOWLEDGMENT_BUILDER = "payload-pre-acknowledgment-builder";

    static final String DEFAULT_ACKNOWLEDGMENT = "default-acknowledgment";
    static final String DEFAULT_ACKNOWLEDGMENT_BUILDER = "default-acknowledgment-builder";

    static final String PAYLOAD_DEFAULT_ACKNOWLEDGMENT = "payload-default-acknowledgment";
    static final String PAYLOAD_DEFAULT_ACKNOWLEDGMENT_BUILDER = "payload-default-acknowledgment-builder";

    @Incoming("sink-" + MANUAL_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkManual(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + NO_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkNo(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PAYLOAD_NO_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkNoMessage(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PRE_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkPre(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + DEFAULT_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkDefault(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + MANUAL_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkManualForBuilder(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + NO_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkNoForBuilder(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PAYLOAD_NO_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkNoForMessageBuilder(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PRE_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkPreBuilder(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + DEFAULT_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkDefaultBuilder(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PAYLOAD_DEFAULT_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkPayloadDefault(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PAYLOAD_DEFAULT_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkPayloadDefaultBuilder(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PAYLOAD_PRE_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkPayloadPreBuilder(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("sink-" + PAYLOAD_PRE_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> sinkPayloadPre(Message<String> ignored) {
        return CompletableFuture.completedFuture(null);
    }

    @Incoming(NO_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    @Outgoing("sink-" + NO_ACKNOWLEDGMENT)
    public Publisher<Message<String>> processorWithNoAck(Publisher<Message<String>> input) {
        return ReactiveStreams.fromPublisher(input)
                .flatMap(m -> ReactiveStreams.of(Message.<String>newBuilder().payload(m.getPayload()).build(), Message.<String>newBuilder().payload(m.getPayload()).build()))
                .peek(m -> processed(NO_ACKNOWLEDGMENT, m.getPayload()))
                .buildRs();
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

    @Incoming(NO_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    @Outgoing("sink-" + NO_ACKNOWLEDGMENT_BUILDER)
    public PublisherBuilder<Message<String>> processorWithNoAckWithBuilder(PublisherBuilder<Message<String>> input) {
        return input
                .flatMap(m -> ReactiveStreams.of(Message.<String>newBuilder().payload(m.getPayload()).build(), Message.<String>newBuilder().payload(m.getPayload()).build()))
                .peek(m -> processed(NO_ACKNOWLEDGMENT_BUILDER, m.getPayload()));
    }

    @Outgoing(NO_ACKNOWLEDGMENT_BUILDER)
    public Publisher<Message<String>> sourceToNoAckWithBuilder() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(NO_ACKNOWLEDGMENT_BUILDER, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(MANUAL_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    @Outgoing("sink-" + MANUAL_ACKNOWLEDGMENT)
    public Publisher<Message<String>> processorWithAck(Publisher<Message<String>> input) {
        return ReactiveStreams.fromPublisher(input)
                .flatMapCompletionStage(m -> m.ack().thenApply(x -> m))
                .flatMap(m -> ReactiveStreams.of(Message.<String>newBuilder().payload(m.getPayload()).build(), Message.<String>newBuilder().payload(m.getPayload()).build()))
                .peek(m -> processed(MANUAL_ACKNOWLEDGMENT, m.getPayload()))
                .buildRs();
    }

    @Outgoing(MANUAL_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToManualAck() {
        return ReactiveStreams.of("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> CompletableFuture.runAsync(() -> {
                    nap();
                    acknowledged(MANUAL_ACKNOWLEDGMENT, payload);
                })).build()).buildRs();
    }

    @Incoming(MANUAL_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    @Outgoing("sink-" + MANUAL_ACKNOWLEDGMENT_BUILDER)
    public PublisherBuilder<Message<String>> processorWithAckBuilder(PublisherBuilder<Message<String>> input) {
        return input
                .flatMapCompletionStage(m -> m.ack().thenApply(x -> m))
                .flatMap(m -> ReactiveStreams.of(Message.<String>newBuilder().payload(m.getPayload()).build(), Message.<String>newBuilder().payload(m.getPayload()).build()))
                .peek(m -> processed(MANUAL_ACKNOWLEDGMENT_BUILDER, m.getPayload()));
    }

    @Outgoing(MANUAL_ACKNOWLEDGMENT_BUILDER)
    public Publisher<Message<String>> sourceToManualAckBuilder() {
        return ReactiveStreams.of("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> CompletableFuture.runAsync(() -> {
                    nap();
                    acknowledged(MANUAL_ACKNOWLEDGMENT_BUILDER, payload);
                })).build()).buildRs();
    }

    @Incoming(PRE_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    @Outgoing("sink-" + PRE_ACKNOWLEDGMENT)
    public Publisher<Message<String>> processorWitPreAck(Publisher<Message<String>> input) {
        return ReactiveStreams.fromPublisher(input)
                .flatMap(m -> ReactiveStreams.of(Message.<String>newBuilder().payload(m.getPayload()).build(), Message.<String>newBuilder().payload(m.getPayload()).build()))
                .peek(m -> processed(PRE_ACKNOWLEDGMENT, m.getPayload()))
                .buildRs();
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

    @Incoming(PRE_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    @Outgoing("sink-" + PRE_ACKNOWLEDGMENT_BUILDER)
    public PublisherBuilder<Message<String>> processorWithPreAckWithBuilder(PublisherBuilder<Message<String>> input) {
        return input
                .flatMap(m -> ReactiveStreams.of(Message.<String>newBuilder().payload(m.getPayload()).build(), Message.<String>newBuilder().payload(m.getPayload()).build()))
                .peek(m -> processed(PRE_ACKNOWLEDGMENT_BUILDER, m.getPayload()));
    }

    @Outgoing(PRE_ACKNOWLEDGMENT_BUILDER)
    public Publisher<Message<String>> sourceToPreAckWithBuilder() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PRE_ACKNOWLEDGMENT_BUILDER, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(DEFAULT_ACKNOWLEDGMENT)
    @Outgoing("sink-" + DEFAULT_ACKNOWLEDGMENT)
    public Publisher<Message<String>> processorWithDefAck(Publisher<Message<String>> input) {
        return ReactiveStreams.fromPublisher(input)
                .flatMap(m -> ReactiveStreams.of(Message.<String>newBuilder().payload(m.getPayload()).build(), Message.<String>newBuilder().payload(m.getPayload()).build()))
                .peek(m -> processed(DEFAULT_ACKNOWLEDGMENT, m.getPayload()))
                .buildRs();
    }

    @Outgoing(DEFAULT_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToDefAck() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(DEFAULT_ACKNOWLEDGMENT, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(DEFAULT_ACKNOWLEDGMENT_BUILDER)
    @Outgoing("sink-" + DEFAULT_ACKNOWLEDGMENT_BUILDER)
    public PublisherBuilder<Message<String>> processorWithDefaultAckWithBuilder(PublisherBuilder<Message<String>> input) {
        return input
                .flatMap(m -> ReactiveStreams.of(Message.<String>newBuilder().payload(m.getPayload()).build(), Message.<String>newBuilder().payload(m.getPayload()).build()))
                .peek(m -> processed(DEFAULT_ACKNOWLEDGMENT_BUILDER, m.getPayload()));
    }

    @Outgoing(DEFAULT_ACKNOWLEDGMENT_BUILDER)
    public Publisher<Message<String>> sourceToDefaultAckWithBuilder() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(DEFAULT_ACKNOWLEDGMENT_BUILDER, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(PAYLOAD_NO_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    @Outgoing("sink-" + PAYLOAD_NO_ACKNOWLEDGMENT)
    public Publisher<String> processorWithNoAckMessage(Publisher<String> input) {
        return ReactiveStreams.fromPublisher(input)
                .flatMap(p -> ReactiveStreams.of(p, p))
                .peek(m -> processed(PAYLOAD_NO_ACKNOWLEDGMENT, m))
                .buildRs();
    }

    @Outgoing(PAYLOAD_NO_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToNoAckMessage() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PAYLOAD_NO_ACKNOWLEDGMENT, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(PAYLOAD_NO_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    @Outgoing("sink-" + PAYLOAD_NO_ACKNOWLEDGMENT_BUILDER)
    public PublisherBuilder<String> processorWithNoAckWithPayloadBuilder(PublisherBuilder<String> input) {
        return input
                .flatMap(p -> ReactiveStreams.of(p, p))
                .peek(m -> processed(PAYLOAD_NO_ACKNOWLEDGMENT_BUILDER, m));
    }

    @Outgoing(PAYLOAD_NO_ACKNOWLEDGMENT_BUILDER)
    public Publisher<Message<String>> sourceToNoAckWithMessageBuilder() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PAYLOAD_NO_ACKNOWLEDGMENT_BUILDER, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(PAYLOAD_DEFAULT_ACKNOWLEDGMENT)
    @Outgoing("sink-" + PAYLOAD_DEFAULT_ACKNOWLEDGMENT)
    public Publisher<String> processorWithDefPayloadAck(Publisher<String> input) {
        return ReactiveStreams.fromPublisher(input)
                .flatMap(p -> ReactiveStreams.of(p, p))
                .peek(m -> processed(PAYLOAD_DEFAULT_ACKNOWLEDGMENT, m))
                .buildRs();
    }

    @Outgoing(PAYLOAD_DEFAULT_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToPayloadDefAck() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PAYLOAD_DEFAULT_ACKNOWLEDGMENT, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(PAYLOAD_DEFAULT_ACKNOWLEDGMENT_BUILDER)
    @Outgoing("sink-" + PAYLOAD_DEFAULT_ACKNOWLEDGMENT_BUILDER)
    public PublisherBuilder<String> processorWithDefaultAckWithBuilderUsingPayload(PublisherBuilder<String> input) {
        return input
                .flatMap(p -> ReactiveStreams.of(p, p))
                .peek(m -> processed(PAYLOAD_DEFAULT_ACKNOWLEDGMENT_BUILDER, m));
    }

    @Outgoing(PAYLOAD_DEFAULT_ACKNOWLEDGMENT_BUILDER)
    public Publisher<Message<String>> sourceToDefaultWithPayloadAckWithBuilder() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PAYLOAD_DEFAULT_ACKNOWLEDGMENT_BUILDER, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(PAYLOAD_PRE_ACKNOWLEDGMENT)
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    @Outgoing("sink-" + PAYLOAD_PRE_ACKNOWLEDGMENT)
    public Publisher<String> processorWithPrePayloadAck(Publisher<String> input) {
        return ReactiveStreams.fromPublisher(input)
                .flatMap(p -> ReactiveStreams.of(p, p))
                .peek(m -> processed(PAYLOAD_PRE_ACKNOWLEDGMENT, m))
                .buildRs();
    }

    @Outgoing(PAYLOAD_PRE_ACKNOWLEDGMENT)
    public Publisher<Message<String>> sourceToPayloadPreAck() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PAYLOAD_PRE_ACKNOWLEDGMENT, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

    @Incoming(PAYLOAD_PRE_ACKNOWLEDGMENT_BUILDER)
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    @Outgoing("sink-" + PAYLOAD_PRE_ACKNOWLEDGMENT_BUILDER)
    public PublisherBuilder<String> processorWithPreAckWithBuilderWithPayload(PublisherBuilder<String> input) {
        return input
                .flatMap(p -> ReactiveStreams.of(p, p))
                .peek(m -> processed(PAYLOAD_PRE_ACKNOWLEDGMENT_BUILDER, m));
    }

    @Outgoing(PAYLOAD_PRE_ACKNOWLEDGMENT_BUILDER)
    public Publisher<Message<String>> sourceToPreWithPayloadAckWithBuilder() {
        return Flowable.fromArray("a", "b", "c", "d", "e")
                .map(payload -> Message.<String>newBuilder().payload(payload).ack(() -> {
                    nap();
                    acknowledged(PAYLOAD_PRE_ACKNOWLEDGMENT_BUILDER, payload);
                    return CompletableFuture.completedFuture(null);
                }).build());
    }

}
