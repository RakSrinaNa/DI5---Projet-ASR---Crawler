package fr.mrcraftcod.crawler;

import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import java.util.List;
import java.util.Queue;
import java.util.Set;
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
		
		Set<URL> crawled = ConcurrentHashMap.newKeySet();
		Set<URL> downloaded = ConcurrentHashMap.newKeySet();
		Queue<DownloadElement> images = new ConcurrentLinkedQueue<>();
		Queue<URL> toCrawl = new ConcurrentLinkedQueue<>(parameters.getSites());
		
		LOGGER.info("Added {} sites with {} threads ({} crawlers, {} downloaders)", parameters.getSites().size(), (int) (1.5 * parameters.getThreadCount()), parameters.getThreadCount(), parameters.getThreadCount());
		
		ExecutorService service = Executors.newFixedThreadPool((int) (1.5 * parameters.getThreadCount()));
		
		List<CrawlerRunner> crawlers = IntStream.range(0, parameters.getThreadCount()).mapToObj(i -> new CrawlerRunner(toCrawl, crawled, images, downloaded)).collect(Collectors.toList());
		List<DownloaderRunner> downloaders = IntStream.range(0, parameters.getThreadCount()).mapToObj(i -> new DownloaderRunner(parameters.getOutFolder(), images, downloaded)).collect(Collectors.toList());
		
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
