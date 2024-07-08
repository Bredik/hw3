package org.example;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.join;

public class MultiThreadCrawler {
    private final ExecutorService executor = Executors.newFixedThreadPool(20);
    public static void main(String[] args) throws Exception {
        MultiThreadCrawler crawler = new MultiThreadCrawler();

        long startTime = System.nanoTime();
        String result = crawler.find("British Empire", "Iron", 5, TimeUnit.MINUTES);
        long finishTime = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        System.out.println("Took "+finishTime+" seconds, result is: " + result);
    }

    private Queue<Node> searchQueue = new  ConcurrentLinkedQueue<>();

    private Set<String> visited = ConcurrentHashMap.newKeySet();

    private WikiClient client = new WikiClient();

    public String find(String from, String target, long timeout, TimeUnit timeUnit) throws Exception {
        long deadline = System.nanoTime() + timeUnit.toNanos(timeout);
        searchQueue.offer(new Node(from, null));

        Node resultAllPath = search(deadline, target);

        if (resultAllPath != null) {
            return getResult(resultAllPath);
        }

        return "not found";
    }

    private Node search(long deadline, String target) throws TimeoutException, IOException {
        Node result = null;
        while (result == null && !searchQueue.isEmpty()) {
            if (deadline < System.nanoTime()) {
                throw new TimeoutException();
            }

//            executor.submit(() -> {
//
//            });

            Node node = searchQueue.poll();
            System.out.println("Get page: " + node.title + " Thread: " + Thread.currentThread().getName());
            Set<String> links = client.getByTitle(node.title);
            if (links.isEmpty()) {
                //pageNotFound
                continue;
            }
            for (String link : links) {
                String currentLink = link.toLowerCase();
                if (visited.contains(currentLink)) {
                    continue;
                }
                visited.add(currentLink);
                Node subNode = new Node(link, node);
                if (target.equalsIgnoreCase(currentLink)) {
                    result = subNode;
                    continue;
                }
                searchQueue.offer(subNode);
            }

        }
        return result;
    }

    private String getResult(Node result) {
        List<String> resultList = new ArrayList<>();
        Node search = result;
        while (true) {
            resultList.add(search.title);
            if (search.next == null) {
                break;
            }
            search = search.next;
        }
        Collections.reverse(resultList);

        return join(" > ", resultList);
    }

    @AllArgsConstructor
    private static class Node {
        String title;
        Node next;
    }
}
