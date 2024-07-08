package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class WikiClient {
    public static final String WIKI = "/wiki/";
    public static final String EN_WIKI_URL = "https://en.wikipedia.org" + WIKI;

    public Set<String> getByTitle(String title) throws IOException {
        Set<String> links = new HashSet<>();
        String url = EN_WIKI_URL + title;
        try {
            Document page = Jsoup.connect(url).followRedirects(true).get();
            for (Element element : page.body().select("a")) {

                if (element.hasAttr("href")) {
                    String href = element.attr("href");
                    if (href.startsWith(WIKI) && !element.text().isEmpty() && !href.contains(":")) {
                        links.add(href.substring(WIKI.length()));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(title + " got error: " + e.getMessage());
        }
        return links;
    }

}

