package io.smallrye.reactive.messaging.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.smallrye.reactive.messaging.WeldTestBaseWithoutTails;

public class PublisherPropagationTest extends WeldTestBaseWithoutTails {

    @Test
    public void test() {
        addBeanClass(SimplePropagationTest.Source.class, SimplePropagationTest.Sink.class);
        addBeanClass(ProcessorReturningPublisherOfPayload.class, ProcessorRetuningPublisherOfMessage.class);
        initialize();
        SimplePropagationTest.Sink sink = container.select(SimplePropagationTest.Sink.class).get();
        await().until(() -> sink.list().size() == 40);
        assertThat(sink.list()).allSatisfy(message -> {
            assertThat(message.getMetadata(SimplePropagationTest.MsgMetadata.class)
                    .map(SimplePropagationTest.MsgMetadata::getMessage)).hasValue("foo");
            assertThat(message.getMetadata(MessageTest.MyMetadata.class)
                    .map(m -> m.v)).hasValue("hello");
            assertThat(message.getMetadata(SimplePropagationTest.CounterMetadata.class)
                    .map(SimplePropagationTest.CounterMetadata::getCount))
                            .hasValueSatisfying(x -> assertThat(x).isNotEqualTo(0));
            assertThat(message.getMetadata()).hasSize(3);
        }).hasSize(40);
    }

    @ApplicationScoped
    public static class ProcessorReturningPublisherOfPayload {
        @Incoming("source")
        @Outgoing("intermediate")
        public Publisher<String> process(String payload) {
            return Flowable.just(payload, payload);
        }

    }

    @ApplicationScoped
    public static class ProcessorRetuningPublisherOfMessage {
        @Incoming("intermediate")
        @Outgoing("sink")
        public Publisher<Message<String>> process(Message<String> input) {
            return Flowable.just(
                Message.<String>newBuilder().payload(input.getPayload()).metadata(input.getMetadata().with(new MessageTest.MyMetadata<>("hello"))).ack(input.getAck()).build(),
                Message.<String>newBuilder().payload(input.getPayload()).metadata(input.getMetadata().with(new MessageTest.MyMetadata<>("hello"))).ack(input.getAck()).build());
        }
    }

}
