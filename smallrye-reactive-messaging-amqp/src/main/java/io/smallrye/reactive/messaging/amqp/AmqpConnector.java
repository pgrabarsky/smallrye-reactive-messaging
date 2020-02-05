package io.smallrye.reactive.messaging.amqp;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Flowable;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpReceiverOptions;
import io.vertx.amqp.impl.AmqpMessageBuilderImpl;
import io.vertx.axle.amqp.*;
import io.vertx.axle.amqp.AmqpMessageBuilder;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

@ApplicationScoped
@Connector(AmqpConnector.CONNECTOR_NAME)
public class AmqpConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpConnector.class);
    static final String CONNECTOR_NAME = "smallrye-amqp";

    @Inject
    private Instance<Vertx> instanceOfVertx;

    @Inject
    private Instance<AmqpClientOptions> clientOptions;

    @Inject
    @ConfigProperty(name = "amqp-port", defaultValue = "5672")
    private Integer configuredPort;

    @Inject
    @ConfigProperty(name = "amqp-host", defaultValue = "localhost")
    private String configuredHost;

    @Inject
    @ConfigProperty(name = "amqp-username")
    private Optional<String> configuredUsername;

    @Inject
    @ConfigProperty(name = "amqp-password")
    private Optional<String> configuredPassword;

    @Inject
    @ConfigProperty(name = "amqp-use-ssl")
    private Optional<Boolean> configuredUseSsl;

    @Inject
    @ConfigProperty(name = "amqp-reconnect-attempts", defaultValue = "100")
    private Optional<Integer> configuredReconnectAttempts;

    @Inject
    @ConfigProperty(name = "amqp-reconnect-interval", defaultValue = "10")
    private Optional<Long> configuredReconnectInterval;

    @Inject
    @ConfigProperty(name = "amqp-connect-timeout", defaultValue = "1000")
    private Optional<Integer> configuredConnectTimeout;

    private boolean internalVertxInstance = false;
    private Vertx vertx;
    private final List<AmqpClient> clients = new CopyOnWriteArrayList<>();

    public void terminate(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        if (internalVertxInstance) {
            vertx.close();
        }
    }

    @PostConstruct
    void init() {
        if (instanceOfVertx == null || instanceOfVertx.isUnsatisfied()) {
            internalVertxInstance = true;
            this.vertx = Vertx.vertx();
        } else {
            this.vertx = instanceOfVertx.get();
        }
    }

    AmqpConnector() {
        this.vertx = null;
    }

    private AmqpClient createClient(Config config) {
        AmqpClient client;
        Optional<String> clientOptionsName = config.getOptionalValue("client-options-name", String.class);
        if (clientOptionsName.isPresent()) {
            String optionsName = clientOptionsName.get();
            Instance<AmqpClientOptions> options = clientOptions.select(NamedLiteral.of(optionsName));
            if (options.isUnsatisfied()) {
                throw new IllegalStateException(
                        "Cannot find a " + AmqpClientOptions.class.getName() + " bean named " + optionsName);
            }
            LOGGER.debug("Creating amqp client from bean named '{}'", optionsName);
            client = AmqpClient.create(new io.vertx.axle.core.Vertx(vertx.getDelegate()), options.get());
        } else {
            client = getClient(config);
        }
        clients.add(client);
        return client;
    }

    private synchronized AmqpClient getClient(Config config) {
        try {
            String username = config.getOptionalValue("username", String.class)
                    .orElseGet(() -> {
                        if (this.configuredUsername != null) {
                            return this.configuredUsername.orElse(null);
                        } else {
                            return null;
                        }
                    });
            String password = config.getOptionalValue("password", String.class)
                    .orElseGet(() -> {
                        if (this.configuredPassword != null) {
                            return this.configuredPassword.orElse(null);
                        } else {
                            return null;
                        }
                    });
            String host = config.getOptionalValue("host", String.class)
                    .orElseGet(() -> {
                        if (this.configuredHost == null) {
                            LOGGER.info("No AMQP host configured, using localhost");
                            return "localhost";
                        } else {
                            return this.configuredHost;
                        }
                    });

            int port = config.getOptionalValue("port", Integer.class)
                    .orElseGet(() -> {
                        if (this.configuredPort == null) {
                            return 5672;
                        } else {
                            return this.configuredPort;
                        }
                    });

            boolean useSsl = config.getOptionalValue("use-ssl", Boolean.class)
                    .orElseGet(() -> {
                        if (this.configuredUseSsl == null) {
                            return false;
                        } else {
                            return this.configuredUseSsl.orElse(Boolean.FALSE);
                        }
                    });

            int reconnectAttempts = config.getOptionalValue("reconnect-attempts", Integer.class)
                    .orElseGet(() -> {
                        if (this.configuredReconnectAttempts == null) {
                            return 100;
                        } else {
                            return this.configuredReconnectAttempts.get();
                        }
                    });

            long reconnectInterval = config.getOptionalValue("reconnect-interval", Long.class)
                    .orElseGet(() -> {
                        if (this.configuredReconnectInterval == null) {
                            return 10L;
                        } else {
                            return this.configuredReconnectInterval.get();
                        }
                    });

            int connectTimeout = config.getOptionalValue("connect-timeout", Integer.class)
                    .orElseGet(() -> {
                        if (this.configuredConnectTimeout == null) {
                            return 1000;
                        } else {
                            return this.configuredConnectTimeout.get();
                        }
                    });

            String containerId = config.getOptionalValue("containerId", String.class).orElse(null);

            AmqpClientOptions options = new AmqpClientOptions()
                    .setUsername(username)
                    .setPassword(password)
                    .setHost(host)
                    .setPort(port)
                    .setContainerId(containerId)
                    .setSsl(useSsl)
                    .setReconnectAttempts(reconnectAttempts)
                    .setReconnectInterval(reconnectInterval)
                    .setConnectTimeout(connectTimeout);
            return AmqpClient.create(new io.vertx.axle.core.Vertx(vertx.getDelegate()), options);
        } catch (Exception e) {
            LOGGER.error("Unable to create client", e);
            throw new IllegalStateException("Unable to create a client, probably a config error", e);
        }
    }

    private Flowable<? extends Message<?>> getStreamOfMessages(AmqpReceiver receiver) {
        return Flowable.defer(
                () -> Flowable.fromPublisher(receiver.toPublisher()))
                .map(m -> AmqpMessageHelper.buildIncomingAmqpMessage(m));
    }

    private String getAddressOrFail(Config config) {
        return config.getOptionalValue("address", String.class)
                .orElseGet(
                        () -> config.getOptionalValue("channel-name", String.class)
                                .orElseThrow(() -> new IllegalArgumentException("Address must be set")));
    }

    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
        String address = getAddressOrFail(config);
        boolean broadcast = config.getOptionalValue("broadcast", Boolean.class).orElse(false);
        boolean durable = config.getOptionalValue("durable", Boolean.class).orElse(true);
        boolean autoAck = config.getOptionalValue("auto-acknowledgement", Boolean.class).orElse(false);
        CompletionStage<AmqpReceiver> future = createClient(config)
                .connect()
                .thenCompose(connection -> connection.createReceiver(address, new AmqpReceiverOptions()
                        .setAutoAcknowledgement(autoAck)
                        .setDurable(durable)));

        PublisherBuilder<? extends Message<?>> builder = ReactiveStreams
                .fromCompletionStage(future)
                .flatMapRsPublisher(this::getStreamOfMessages);

        if (broadcast) {
            return ReactiveStreams.fromPublisher(Flowable.fromPublisher(builder.buildRs()).publish().autoConnect());
        }

        return builder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
        String configuredAddress = getAddressOrFail(config);
        boolean durable = config.getOptionalValue("durable", Boolean.class).orElse(true);
        long ttl = config.getOptionalValue("ttl", Long.class).orElse(0L);

        AtomicReference<AmqpSender> sender = new AtomicReference<>();
        AmqpClient client = createClient(config);
        return ReactiveStreams.<Message<?>> builder().flatMapCompletionStage(message -> {
            AmqpSender as = sender.get();

            if (as == null) {
                return client.connect()
                        .thenCompose(AmqpConnection::createAnonymousSender)
                        .thenApply(s -> {
                            sender.set(s);
                            return s;
                        })
                        .thenCompose(s -> {
                            try {
                                return send(s, message, durable, ttl, configuredAddress);
                            } catch (Exception e) {
                                LOGGER.error("Unable to send the message", e);
                                CompletableFuture<Message> future = new CompletableFuture<>();
                                future.completeExceptionally(e);
                                return future;
                            }
                        })
                        .whenComplete((m, e) -> {
                            if (e != null) {
                                if (clients.isEmpty()) {
                                    LOGGER.error("The AMQP message has not been sent, the client is closed");
                                } else {
                                    LOGGER.error("Unable to send the AMQP message", e);
                                }
                            }
                        });
            } else {
                return send(as, message, durable, ttl, configuredAddress);
            }
        }).ignore();
    }

    private String getActualAddress(Message<?> message, io.vertx.axle.amqp.AmqpMessage amqp, String configuredAddress) {
        if (amqp.address() != null) {
            return amqp.address();
        }
        return message.getMetadata(OutgoingAmqpMetadata.class)
                .flatMap(o -> Optional.ofNullable(o.getAddress()))
                .orElse(configuredAddress);
    }

    private CompletionStage send(AmqpSender sender, Message msg, boolean durable, long ttl, String configuredAddress) {
        io.vertx.axle.amqp.AmqpMessage amqp;
        if (msg.getPayload() instanceof io.vertx.axle.amqp.AmqpMessage) {
            amqp = (io.vertx.axle.amqp.AmqpMessage) msg.getPayload();
        } else if (msg.getPayload() instanceof io.vertx.amqp.AmqpMessage) {
            amqp = new io.vertx.axle.amqp.AmqpMessage((io.vertx.amqp.AmqpMessage) msg.getPayload());
        } else {
            amqp = convertToAmqpMessage(msg, durable, ttl);
        }

        String actualAddress = getActualAddress(msg, amqp, configuredAddress);
        if (clients.isEmpty()) {
            LOGGER.error("The AMQP message to address `{}` has not been sent, the client is closed",
                    actualAddress);
            return CompletableFuture.completedFuture(msg);
        }

        if (!actualAddress.equals(amqp.address())) {
            amqp = new io.vertx.axle.amqp.AmqpMessage(
                    new AmqpMessageBuilderImpl(amqp.getDelegate()).address(actualAddress).build());
        }

        LOGGER.debug("Sending AMQP message to address `{}` ",
                actualAddress);
        return sender.sendWithAck(amqp)
                .<Void> thenCompose(x -> msg.ack())
                .thenApply(x -> msg);
    }

    private io.vertx.axle.amqp.AmqpMessage convertToAmqpMessage(Message<?> message, boolean durable, long ttl) {
        Object payload = message.getPayload();
        Optional<OutgoingAmqpMetadata> metadata = message.getMetadata(OutgoingAmqpMetadata.class);
        AmqpMessageBuilder builder = io.vertx.axle.amqp.AmqpMessage.create();

        if (durable) {
            builder.durable(true);
        } else {
            builder.durable(metadata.map(OutgoingAmqpMetadata::isDurable).orElse(false));
        }

        if (ttl > 0) {
            builder.ttl(ttl);
        } else {
            long t = metadata.map(OutgoingAmqpMetadata::getTtl).orElse(-1L);
            if (t > 0) {
                builder.ttl(t);
            }
        }

        if (payload instanceof String) {
            builder.withBody((String) payload);
        } else if (payload instanceof Boolean) {
            builder.withBooleanAsBody((Boolean) payload);
        } else if (payload instanceof Buffer) {
            builder.withBufferAsBody((Buffer) payload);
        } else if (payload instanceof Byte) {
            builder.withByteAsBody((Byte) payload);
        } else if (payload instanceof Character) {
            builder.withCharAsBody((Character) payload);
        } else if (payload instanceof Double) {
            builder.withDoubleAsBody((Double) payload);
        } else if (payload instanceof Float) {
            builder.withFloatAsBody((Float) payload);
        } else if (payload instanceof Instant) {
            builder.withInstantAsBody((Instant) payload);
        } else if (payload instanceof Integer) {
            builder.withIntegerAsBody((Integer) payload);
        } else if (payload instanceof JsonArray) {
            builder.withJsonArrayAsBody((JsonArray) payload);
        } else if (payload instanceof JsonObject) {
            builder.withJsonObjectAsBody((JsonObject) payload);
        } else if (payload instanceof Long) {
            builder.withLongAsBody((Long) payload);
        } else if (payload instanceof Short) {
            builder.withShortAsBody((Short) payload);
        } else if (payload instanceof UUID) {
            builder.withUuidAsBody((UUID) payload);
        } else {
            builder.withBody(payload.toString());
        }

        builder.address(metadata.map(OutgoingAmqpMetadata::getAddress).orElse(null));
        builder.replyTo(metadata.map(OutgoingAmqpMetadata::getReplyTo).orElse(null));
        builder.applicationProperties(metadata.map(OutgoingAmqpMetadata::getProperties).orElseGet(JsonObject::new));

        builder.contentEncoding(metadata.map(OutgoingAmqpMetadata::getContentEncoding).orElse(null));
        builder.contentType(metadata.map(OutgoingAmqpMetadata::getContentType).orElse(null));
        builder.correlationId(metadata.map(OutgoingAmqpMetadata::getCorrelationId).orElse(null));
        builder.groupId(metadata.map(OutgoingAmqpMetadata::getGroupId).orElse(null));
        builder.id(metadata.map(OutgoingAmqpMetadata::getId).orElse(null));
        int priority = metadata.map(OutgoingAmqpMetadata::getPriority).orElse(-1);
        if (priority >= 0) {
            builder.priority((short) priority);
        }
        builder.subject(metadata.map(OutgoingAmqpMetadata::getSubject).orElse(null));
        return builder.build();
    }

    @PreDestroy
    public synchronized void close() {
        clients.forEach(AmqpClient::close);
        clients.clear();
    }
}
