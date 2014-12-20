package ru.spbau.mit.networks.client;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Objects;

public class Main {
    private static final ArrayList<Thread> threads = new ArrayList<>();

    public static void main(String[] args) {
        // socat tcp-l:8888,fork exec:'/bin/cat'

        String host = args[0];
        int port = Integer.valueOf(args[1]);
        int threadsCount = Integer.valueOf(args[2]);
        int messageSize = Integer.valueOf(args[3]);
        int reportFrequency = Integer.valueOf(args[4]);
        String clientType = args[5];

        Runtime.getRuntime().addShutdownHook(new Thread(() -> threads.forEach(Thread::interrupt)));

        for (int i = 0; i < threadsCount; i++) {
            Client client = Objects.equals(clientType, "b") ?
                    new BlockingClient(host, port, messageSize, reportFrequency) :
                    new NonBlockingClient(host, port, messageSize, reportFrequency);
            Thread thread = new Thread(client);
            threads.add(thread);
            thread.start();
        }

        Thread totalMessagesSent = new Thread(() -> {
            int oldSent = Client.totalSentMessages.get();
            int time = 0;
            while (!Thread.interrupted()) {
                int newSent = Client.totalSentMessages.get();
                int mps = time == 0 ? 0 : newSent / time;
                System.out.println(MessageFormat.format("MPS: {0} (last second = {1})", mps, newSent - oldSent));
                time += 1;
                oldSent = newSent;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        totalMessagesSent.setDaemon(true);
        threads.add(totalMessagesSent);
        totalMessagesSent.start();
    }
}