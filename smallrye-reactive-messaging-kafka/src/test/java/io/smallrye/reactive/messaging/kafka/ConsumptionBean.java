package io.smallrye.reactive.messaging.kafka;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class ConsumptionBean {

    private List<Integer> list = new ArrayList<>();
    private List<Message<Integer>> kafka = new ArrayList<>();

    @Incoming("data")
    @Outgoing("sink")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public Message<Integer> process(Message<Integer> input) {
        kafka.add(input);
        return Message.<Integer>newBuilder().payload(input.getPayload() + 1).ack(input::ack).build();
    }

    @Incoming("sink")
    public void sink(int val) {
        list.add(val);
    }

    public List<Integer> getResults() {
        return list;
    }

    public List<Message<Integer>> getKafkaMessages() {
        return kafka;
    }
}
