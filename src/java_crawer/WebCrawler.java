package java_crawer;

import java_crawer.WebCrawlerUI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebCrawler {
	// 存储已访问URL的集合，避免重复爬取
    private final Set<String> visitedUrls = new HashSet<>();
    // 线程池，用于并发执行爬取任务
    private final ExecutorService executorService;
    // 控制爬虫运行状态的volatile变量
    private volatile boolean running = true;
    // 存储URL对应的HTML内容的映射
    private final Map<String, String> htmlContents = new HashMap<>();
    // 存储URL对应的文本内容的映射
    private final Map<String, String> textContents = new HashMap<>();

    public WebCrawler(int numThreads) {
    	// 使用固定大小的线程池 如果网页太大可以调大一点
        executorService = Executors.newFixedThreadPool(numThreads);
    }

    public void startCrawling(String startUrl) {
    	// 开始爬取，设置运行状态为true，并提交起始URL的爬取任务
        running = true;
        executorService.submit(() -> crawl(startUrl));
    }

    public void stopCrawling() {
    	// 停止爬取，设置运行状态为false，并尝试关闭线程池
        running = false;
        executorService.shutdown();
        try {
        	// 等待线程池中正在执行的任务完成，最大等待时间为60
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
            	// 如果超时，则尝试立即停止所有正在执行的任务
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
        	// 如果当前线程在等待时被中断，也尝试立即停止线程池
            executorService.shutdownNow();
        }
    }

    private void crawl(String url) {
    	// 爬取指定URL的内容，如果爬虫未运行或URL已访问，则直接返回
        if (!running || visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);
        try {
        	// 使用Jsoup连接到URL并获取HTML文档
            Document document = Jsoup.connect(url).get();
            System.out.println("Crawling: " + url);
            
            // 将获取的HTML和文本内容存储到映射中
            htmlContents.put(url, document.html());
            textContents.put(url, document.text());

            // 选择页面上所有的链接并递归爬取
            Elements links = document.select("a[href]");
            for (Element link : links) {
                String nextUrl = link.absUrl("href");
                // 如果链接以http开头且尚未访问，则提交爬取任务
                if (nextUrl.startsWith("http") && !visitedUrls.contains(nextUrl)) {
                    executorService.submit(() -> crawl(nextUrl));
                }
            }
        } catch (IOException e) {
        	// 打印爬取过程中发生的异常信息
            System.err.println("Error crawling " + url + ": " + e.getMessage());
        }
    }

    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    public String getHtmlContent(String url) {
        return htmlContents.get(url);
    }

    public String getTextContent(String url) {
        return textContents.get(url);
    }
}