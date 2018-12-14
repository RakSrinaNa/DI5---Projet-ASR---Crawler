package fr.mrcraftcod.crawler;

import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 2018-09-11.
 *
 * @author Thomas Couchoud
 * @since 2018-09-11
 */
public class Main{
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
	private final static List<HashMap<String, String>> HEADERS = new ArrayList<>();
	
	
	public static void main(String[] args){
		final Parameters parameters = new Parameters();
		final CmdLineParser parser = new CmdLineParser(parameters);
		try{
			parser.parseArgument(args);
		}
		catch(final Exception ex){
			parser.printUsage(System.out);
			return;
		}
		
		var headers1 = new HashMap<String, String>();
		headers1.put("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:62.0) Gecko/20100101 Firefox/62.0");
		headers1.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		headers1.put("Accept-Encoding", "gzip, deflate, br");
		headers1.put("Accept-Language", "fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3");
		headers1.put("Cache-Control", "no-cache");
		
		HEADERS.add(headers1);
		
		Set<URL> crawled = ConcurrentHashMap.newKeySet();
		Set<URL> downloaded = ConcurrentHashMap.newKeySet();
		Queue<URL> toCrawl = new ConcurrentLinkedQueue<>(parameters.getSites());
		Queue<DownloadElement> images = new ConcurrentLinkedQueue<>();

		LOGGER.info("Added {} sites with {} threads ({} crawlers, {} downloaders)", parameters.getSites().size(), (int) (1.5 * parameters.getThreadCount()), parameters.getThreadCount(), parameters.getThreadCount());
		
		ExecutorService service = Executors.newFixedThreadPool((int) (1.5 * parameters.getThreadCount()));
		
		List<CrawlerRunner> crawlers = IntStream.range(0, parameters.getThreadCount()).mapToObj(i -> new CrawlerRunner(toCrawl, crawled, images, downloaded, HEADERS.get(ThreadLocalRandom.current().nextInt(HEADERS.size())), parameters.getRecursive(), parameters.getWhole(), parameters.getSites())).collect(Collectors.toList());
		List<DownloaderRunner> downloaders = IntStream.range(0, parameters.getThreadCount()).mapToObj(i -> new DownloaderRunner(parameters.getOutFolder(), images, downloaded, HEADERS.get(ThreadLocalRandom.current().nextInt(HEADERS.size())))).collect(Collectors.toList());
		
		List<Future<Integer>> futuresCrawler = crawlers.stream().map(service::submit).collect(Collectors.toList());
		List<Future<Integer>> futuresDownloader = downloaders.stream().map(service::submit).collect(Collectors.toList());
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOGGER.info("VM Shutting down, closing runners and executor");
			crawlers.forEach(CrawlerRunner::stop);
			downloaders.forEach(DownloaderRunner::stop);
			service.shutdown();
		}));
		
		AtomicInteger crawledCount = new AtomicInteger();
		AtomicInteger downloadedCount = new AtomicInteger();
		futuresCrawler.parallelStream().forEach(future -> {
			try{
				crawledCount.addAndGet(future.get());
			}
			catch(InterruptedException | ExecutionException e){
				e.printStackTrace();
			}
		});
		downloaders.parallelStream().forEach(DownloaderRunner::canStop);
		futuresDownloader.parallelStream().forEach(future -> {
			try{
				downloadedCount.addAndGet(future.get());
			}
			catch(InterruptedException | ExecutionException e){
				e.printStackTrace();
			}
		});
		service.shutdown();
		LOGGER.info("Crawled {} links and downloaded {} elements", crawledCount.get(), downloadedCount.get());
	}
}
