package org.example;

import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.join;

public class MultiThreadCrawler {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ExecutorService executor1 = Executors.newFixedThreadPool(2);

    public static void main(String[] args) throws Exception {
        MultiThreadCrawler crawler = new MultiThreadCrawler();

        long startTime = System.nanoTime();
        String result = crawler.find("British Empire", "Iron", 2, 5, TimeUnit.MINUTES);
        long finishTime = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        System.out.println("Took " + finishTime + " seconds, result is: " + result);
    }

    private Queue<Node> searchQueue = new ConcurrentLinkedQueue<>();

    private Set<String> visited = ConcurrentHashMap.newKeySet();

    private WikiClient client = new WikiClient();

    public String find(String from, String target, int deep, long timeout, TimeUnit timeUnit) throws Exception {
        long deadline = System.nanoTime() + timeUnit.toNanos(timeout);
        searchQueue.offer(new Node(from, null));

        Node resultAllPath = search(deadline, target, deep);

        if (resultAllPath.empty) {
            return "not found";
        }

        return getResult(resultAllPath);
    }

    private Node search(long deadline, String target, int deep) throws TimeoutException {
        while (!searchQueue.isEmpty()) {
            if (deadline < System.nanoTime()) {
                throw new TimeoutException();
            }

            var futures = new ArrayList<Future<Node>>();
            var chekDeep = new ArrayList<Future<Boolean>>();

            futures.add(executor.submit(() -> {
                Node node = searchQueue.poll();
                System.out.println("Get page: " + node.title + " Thread: " + Thread.currentThread().getName());
                Set<String> links = client.getByTitle(node.title);
                if (links.isEmpty()) {
                    //pageNotFound
                    return new Node();
                }
                for (String link : links) {
                    String currentLink = link.toLowerCase();
                    if (visited.add(currentLink)) {
                        Node subNode = new Node(link, node);
                        checkDeep(subNode, deep);
                        if (target.equalsIgnoreCase(currentLink)) {
                            return subNode;
                        }
                        searchQueue.offer(subNode);
                    }
                }
                return new Node();
            }));

            for (Future<Node> f : futures) {
                try {
                    if (!f.get().empty) {
                        executor.shutdownNow();
                        return f.get();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        return new Node();
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

    //Проверка глубина поиска по вложенности нод
    private void checkDeep(Node n, int deep) {
        int tmpDeep = 1;
        var forPrint = n;
        while (n.next != null) {
            n = n.next;
            tmpDeep++;
        }
        if (tmpDeep > deep) {
            System.out.println(getResult(forPrint));
            executor.shutdownNow();
            throw new RuntimeException("Превысили глубину поиска");
        }
    }

    private static class Node {
        String title;
        Node next;
        boolean empty;

        public Node() {
            this.empty = true;
        }

        public Node(String title, Node next) {
            this.title = title;
            this.next = next;
            this.empty = false;
        }
    }
}
