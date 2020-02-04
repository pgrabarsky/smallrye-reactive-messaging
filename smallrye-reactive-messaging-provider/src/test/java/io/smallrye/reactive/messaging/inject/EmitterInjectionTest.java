package io.smallrye.reactive.messaging.inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.messaging.Emitter;
import io.smallrye.reactive.messaging.WeldTestBaseWithoutTails;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Merge;
import io.smallrye.reactive.messaging.annotations.Stream;

public class EmitterInjectionTest extends WeldTestBaseWithoutTails {

    @Test
    public void testWithPayloads() {
        final MyBeanEmittingPayloads bean = installInitializeAndGet(MyBeanEmittingPayloads.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("a", "b", "c");
        assertThat(bean.emitter().isCancelled()).isTrue();
        assertThat(bean.emitter().isRequested()).isFalse();
    }

    @Test
    public void testWithPayloadsAndAck() {
        final MyBeanEmittingPayloadsWithAck bean = installInitializeAndGet(MyBeanEmittingPayloadsWithAck.class);
        bean.run();
        List<CompletionStage<Void>> cs = bean.getCompletionStage();
        assertThat(bean.emitter()).isNotNull();
        assertThat(cs.get(0).toCompletableFuture().isDone()).isTrue();
        assertThat(cs.get(1).toCompletableFuture().isDone()).isTrue();
        assertThat(cs.get(2).toCompletableFuture().isDone()).isFalse();
        await().until(() -> bean.list().size() == 3);
        assertThat(bean.list()).containsExactly("a", "b", "c");
        assertThat(bean.emitter().isCancelled()).isTrue();
        assertThat(bean.emitter().isRequested()).isFalse();
    }

    @Test
    public void testMyMessageBeanWithPayloadsAndAck() {
        final MyMessageBeanEmittingPayloadsWithAck bean = installInitializeAndGet(
                MyMessageBeanEmittingPayloadsWithAck.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("a", "b", "c");
        assertThat(bean.emitter().isCancelled()).isTrue();
        assertThat(bean.emitter().isRequested()).isFalse();
    }

    @Test
    public void testWithPayloadsLegacy() {
        final MyBeanEmittingPayloadsUsingStream bean = installInitializeAndGet(MyBeanEmittingPayloadsUsingStream.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("a", "b", "c");
        assertThat(bean.emitter().isCancelled()).isTrue();
        assertThat(bean.emitter().isRequested()).isFalse();
    }

    @Test
    public void testWithProcessor() {
        final EmitterConnectedToProcessor bean = installInitializeAndGet(EmitterConnectedToProcessor.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("A", "B", "C");
        assertThat(bean.emitter().isCancelled()).isTrue();
        assertThat(bean.emitter().isRequested()).isFalse();
    }

    @Test
    public void testWithMessages() {
        final MyBeanEmittingMessages bean = installInitializeAndGet(MyBeanEmittingMessages.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("a", "b", "c");
        assertThat(bean.emitter().isCancelled()).isFalse();
        assertThat(bean.emitter().isRequested()).isTrue();
    }

    @Test
    public void testWithMessagesLegacy() {
        final MyBeanEmittingMessagesUsingStream bean = installInitializeAndGet(MyBeanEmittingMessagesUsingStream.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("a", "b", "c");
        assertThat(bean.emitter().isCancelled()).isFalse();
        assertThat(bean.emitter().isRequested()).isTrue();
    }

    @Test
    public void testTermination() {
        final MyBeanEmittingDataAfterTermination bean = installInitializeAndGet(
                MyBeanEmittingDataAfterTermination.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("a", "b");
        assertThat(bean.emitter().isCancelled()).isTrue();
        assertThat(bean.emitter().isRequested()).isFalse();
        assertThat(bean.isCaught()).isTrue();
    }

    @Test
    public void testTerminationWithError() {
        final MyBeanEmittingDataAfterTerminationWithError bean = installInitializeAndGet(
                MyBeanEmittingDataAfterTerminationWithError.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("a", "b");
        assertThat(bean.emitter().isCancelled()).isTrue();
        assertThat(bean.emitter().isRequested()).isFalse();
        assertThat(bean.isCaught()).isTrue();
    }

    @Test
    public void testWithNull() {
        final MyBeanEmittingNull bean = installInitializeAndGet(MyBeanEmittingNull.class);
        bean.run();
        assertThat(bean.emitter()).isNotNull();
        assertThat(bean.list()).containsExactly("a", "b", "c");
        assertThat(bean.hasCaughtNullPayload()).isTrue();
        assertThat(bean.hasCaughtNullMessage()).isTrue();
    }

    @Test(expected = IllegalStateException.class)
    public void testWithMissingStream() {
        // The error is only thrown when a message is emitted as the subscription can be delayed.
        installInitializeAndGet(BeanWithMissingStream.class).emitter().send(Message.<String>newBuilder().payload("foo").build());
    }

    @Test(expected = IllegalStateException.class)
    public void testWithMissingChannel() {
        // The error is only thrown when a message is emitted as the subscription can be delayed.
        installInitializeAndGet(BeanWithMissingChannel.class).emitter().send(Message.<String>newBuilder().payload("foo").build());
    }

    @Test
    public void testWithTwoEmittersConnectedToOneProcessor() {
        final TwoEmittersConnectedToProcessor bean = installInitializeAndGet(TwoEmittersConnectedToProcessor.class);
        bean.run();
        assertThat(bean.list()).containsExactly("A", "B", "C");
    }

    @Test
    public void testEmitterAndPublisherInjectedInTheSameClass() {
        EmitterAndPublisher bean = installInitializeAndGet(EmitterAndPublisher.class);
        Emitter<String> emitter = bean.emitter();
        Publisher<String> publisher = bean.publisher();
        assertThat(emitter).isNotNull();
        assertThat(publisher).isNotNull();
        List<String> list = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();
        //noinspection SubscriberImplementation
        publisher.subscribe(new Subscriber<String>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(String s) {
                list.add(s);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                throw new RuntimeException(throwable);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        assertThat(list).isEmpty();
        assertThat(completed).isFalse();
        emitter.send("a");
        emitter.send("b");
        emitter.send("c");
        assertThat(list).containsExactly("a", "b", "c");
        emitter.send("d");
        emitter.complete();
        assertThat(list).containsExactly("a", "b", "c", "d");
        assertThat(completed).isTrue();
    }

    @ApplicationScoped
    public static class EmitterAndPublisher {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;

        @Inject
        @Channel("foo")
        Publisher<String> publisher;

        public Emitter<String> emitter() {
            return emitter;
        }

        public Publisher<String> publisher() {
            return publisher;
        }
    }

    @ApplicationScoped
    public static class MyBeanEmittingPayloads {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();

        public Emitter<String> emitter() {
            return emitter;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send("a");
            emitter.send("b");
            emitter.send("c");
            emitter.complete();
        }

        @Incoming("foo")
        public void consume(final String s) {
            list.add(s);
        }
    }

    @ApplicationScoped
    public static class MyBeanEmittingPayloadsUsingStream {
        @SuppressWarnings("deprecation")
        @Inject
        @Stream("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();

        public Emitter<String> emitter() {
            return emitter;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send("a");
            emitter.send("b");
            emitter.send("c");
            emitter.complete();
        }

        @Incoming("foo")
        public void consume(final String s) {
            list.add(s);
        }
    }

    @ApplicationScoped
    public static class MyBeanEmittingPayloadsWithAck {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();

        private final List<CompletionStage<Void>> csList = new CopyOnWriteArrayList<>();

        public Emitter<String> emitter() {
            return emitter;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            csList.add(emitter.send("a"));
            csList.add(emitter.send("b"));
            csList.add(emitter.send("c"));
            emitter.complete();
        }

        List<CompletionStage<Void>> getCompletionStage() {
            return csList;
        }

        @Incoming("foo")
        @Acknowledgment(Strategy.MANUAL)
        public CompletionStage<Void> consume(final Message<String> s) {
            list.add(s.getPayload());

            if (!"c".equals(s.getPayload())) {
                return s.ack();
            } else {
                return new CompletableFuture<>();

            }

        }
    }

    @ApplicationScoped
    public static class MyMessageBeanEmittingPayloadsWithAck {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();

        public Emitter<String> emitter() {
            return emitter;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send(Message.<String>newBuilder().payload("a").build());
            emitter.send(Message.<String>newBuilder().payload("b").build());
            emitter.send(Message.<String>newBuilder().payload("c").build());
            emitter.complete();
        }

        @Incoming("foo")
        public void consume(final String s) {
            list.add(s);
        }
    }

    @ApplicationScoped
    public static class MyBeanEmittingMessages {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();

        public Emitter<String> emitter() {
            return emitter;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send(Message.<String>newBuilder().payload("a").build());
            emitter.send(Message.<String>newBuilder().payload("b").build());
            emitter.send(Message.<String>newBuilder().payload("c").build());
        }

        @Incoming("foo")
        public void consume(final String s) {
            list.add(s);
        }
    }

    @ApplicationScoped
    public static class MyBeanEmittingMessagesUsingStream {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();

        public Emitter<String> emitter() {
            return emitter;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send(Message.<String>newBuilder().payload("a").build());
            emitter.send(Message.<String>newBuilder().payload("b").build());
            emitter.send(Message.<String>newBuilder().payload("c").build());
        }

        @Incoming("foo")
        public void consume(final String s) {
            list.add(s);
        }
    }

    public static class BeanWithMissingStream {
        @SuppressWarnings("deprecation")
        @Inject
        @Stream("missing")
        Emitter<Message<String>> emitter;

        public Emitter<Message<String>> emitter() {
            return emitter;
        }
    }

    public static class BeanWithMissingChannel {
        @Inject
        @Channel("missing")
        Emitter<Message<String>> emitter;

        public Emitter<Message<String>> emitter() {
            return emitter;
        }
    }

    @ApplicationScoped
    public static class MyBeanEmittingNull {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();
        private boolean caughtNullPayload;
        private boolean caughtNullMessage;

        public Emitter<String> emitter() {
            return emitter;
        }

        boolean hasCaughtNullPayload() {
            return caughtNullPayload;
        }

        boolean hasCaughtNullMessage() {
            return caughtNullMessage;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send("a");
            emitter.send("b");
            try {
                emitter.send((String) null);
            } catch (IllegalArgumentException e) {
                caughtNullPayload = true;
            }

            try {
                emitter.send((Message<String>) null);
            } catch (IllegalArgumentException e) {
                caughtNullMessage = true;
            }
            emitter.send("c");
            emitter.complete();
        }

        @Incoming("foo")
        public void consume(final String s) {
            list.add(s);
        }
    }

    @ApplicationScoped
    public static class MyBeanEmittingDataAfterTermination {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();
        private boolean caught;

        public Emitter<String> emitter() {
            return emitter;
        }

        boolean isCaught() {
            return caught;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send("a");
            emitter.send("b");
            emitter.complete();
            try {
                emitter.send("c");
            } catch (final IllegalStateException e) {
                caught = true;
            }
        }

        @Incoming("foo")
        public void consume(final String s) {
            list.add(s);
        }
    }

    @ApplicationScoped
    public static class MyBeanEmittingDataAfterTerminationWithError {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();
        private boolean caught;

        public Emitter<String> emitter() {
            return emitter;
        }

        boolean isCaught() {
            return caught;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send("a");
            emitter.send("b");
            emitter.error(new Exception("BOOM"));
            try {
                emitter.send("c");
            } catch (final IllegalStateException e) {
                caught = true;
            }
        }

        @Incoming("foo")
        public void consume(final String s) {
            list.add(s);
        }
    }

    @ApplicationScoped
    public static class EmitterConnectedToProcessor {
        @Inject
        @Channel("foo")
        Emitter<String> emitter;
        private final List<String> list = new CopyOnWriteArrayList<>();

        public Emitter<String> emitter() {
            return emitter;
        }

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter.send("a");
            emitter.send("b");
            emitter.send("c");
            emitter.complete();
        }

        @Incoming("foo")
        @Outgoing("bar")
        public String process(final String s) {
            return s.toUpperCase();
        }

        @Incoming("bar")
        public void consume(final String b) {
            list.add(b);
        }
    }

    @ApplicationScoped
    public static class TwoEmittersConnectedToProcessor {
        @Inject
        @Channel("foo")
        Emitter<String> emitter1;

        @Inject
        @Channel("foo")
        Emitter<String> emitter2;

        private final List<String> list = new CopyOnWriteArrayList<>();

        public List<String> list() {
            return list;
        }

        public void run() {
            emitter1.send("a");
            emitter2.send("b");
            emitter1.send("c");
            emitter1.complete();
        }

        @Incoming("foo")
        @Merge
        @Outgoing("bar")
        public String process(final String s) {
            return s.toUpperCase();
        }

        @Incoming("bar")
        public void consume(final String b) {
            list.add(b);
        }
    }

}
