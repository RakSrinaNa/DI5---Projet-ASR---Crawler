package fr.mrcraftcod.crawler;

import com.mashape.unirest.http.HttpResponse;
import fr.mrcraftcod.utils.http.requestssenders.get.StringGetRequestSender;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 2018-09-21.
 *
 * @author Thomas Couchoud
 * @since 2018-09-21
 */
public class CrawlerRunner implements Callable<Integer>{
	private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerRunner.class);
	
	private final static Pattern HTTP_PATTERN = Pattern.compile("^https?://.*");
	private final static Pattern MEDIA_PATTERN = Pattern.compile("^.*\\.(jpg|png|jpeg|gif|webm|mp4|tif)(\\?.*)?$");
	private final static Pattern SKIP_PATTERN = Pattern.compile("^(mailto:|javascript:|#|tel:|data:).*$");
	
	private final Queue<URL> toCrawl;
	private final Set<URL> crawled;
	private final Queue<DownloadElement> images;
	private final Set<URL> downloaded;
	private boolean stop = false;
	
	public CrawlerRunner(Queue<URL> toCrawl, Set<URL> crawled, Queue<DownloadElement> images, Set<URL> downloaded){
		this.toCrawl = toCrawl;
		this.crawled = crawled;
		this.images = images;
		this.downloaded = downloaded;
	}
	
	@Override
	public Integer call() throws Exception{
		LOGGER.info("Crawler started");
		int crawledCount = 0;
		int retryCounter = 0;
		while(!stop){
			try{
				
				URL site;
				if((site = toCrawl.poll()) != null){
					crawled.add(site);
					retryCounter = 0;
					
					LOGGER.info("Crawler is crawling link {}", site);
					
					final HttpResponse<String> requestResult = new StringGetRequestSender(site).getRequestResult();
					if(requestResult.getStatus() == 200){
						final Document rootDocument = Jsoup.parse(requestResult.getBody());
						Stream<URL> stream1 = rootDocument.getElementsByTag("a").parallelStream().filter(aElem -> aElem.hasAttr("href")).map(aElem -> aElem.attr("href")).map(linkStr -> getURL(site, linkStr)).filter(Objects::nonNull).filter(link -> !downloaded.contains(link));
						
						Stream<URL> stream2 = Stream.concat(rootDocument.getElementsByTag("video").stream(), rootDocument.getElementsByTag("source").stream()).parallel().filter(aElem -> aElem.hasAttr("src")).map(aElem -> aElem.attr("src")).map(linkStr -> getURL(site, linkStr)).filter(Objects::nonNull).filter(link -> !downloaded.contains(link));
						
						images.addAll(rootDocument.getElementsByTag("img").parallelStream().filter(aElem -> aElem.hasAttr("src")).map(aElem -> aElem.attr("src")).map(linkStr -> getImageURL(site, linkStr)).filter(Objects::nonNull).filter(link -> !downloaded.contains(link)).map(link -> new DownloadElement(site, link)).collect(Collectors.toSet()));
						
						int added = 0;
						int picAdded = 0;
						for(URL link : Stream.concat(stream1, stream2).collect(Collectors.toSet())){
							String[] paths = link.getPath().split("/");
							if(paths.length > 0){
								String resource = paths[paths.length - 1];
								if(MEDIA_PATTERN.matcher(resource).matches()){
									link = getImageURL(site, link.toString());
									if(!downloaded.contains(link)){
										if(images.offer(new DownloadElement(site, link))){
											picAdded++;
										}
									}
								}
								else if(!crawled.contains(link)){
									if(link.getHost().equals(site.getHost())){
										if(!toCrawl.contains(link)){
											if(toCrawl.add(link)){
												added++;
											}
										}
									}
								}
							}
						}
						crawledCount++;
						LOGGER.info("Found {} new links and {} pics, new queue size is {}, images size: {}, banned list: {}", added, picAdded, toCrawl.size(), images.size(), crawled.size());
					}
					else if(requestResult.getStatus() == 429){
						LOGGER.warn("Response code for page {} was {}", site, requestResult.getStatus());
						toCrawl.offer(site);
					}
					else{
						LOGGER.error("Response code for page {} was {}", site, requestResult.getStatus());
					}
					
					if(images.size() > 1000)
					{
						LOGGER.info("Crawler waiting, too many images to download");
						Thread.sleep(10000);
					}
				}
				else{
					retryCounter++;
					LOGGER.info("Crawler waiting: {}", retryCounter);
					if(retryCounter > 10){
						stop = true;
					}
					Thread.sleep(1000);
				}
			}
			catch(Exception e){
				LOGGER.error("Error in crawler", e);
			}
		}
		LOGGER.info("Crawler stopped");
		return crawledCount;
	}
	
	private URL getImageURL(URL site, String linkStr){
		if(linkStr.contains("#")){
			linkStr = linkStr.substring(0, linkStr.indexOf("#"));
		}
		if(linkStr.contains("?")){
			linkStr = linkStr.substring(0, linkStr.indexOf("?"));
		}
		return getURL(site, linkStr);
	}
	
	private URL getURL(URL site, String linkStr){
		linkStr = linkStr.replace(" ", "%20");
		try{
			if(HTTP_PATTERN.matcher(linkStr).matches()){
				return new URL(linkStr);
			}
			else if(linkStr.startsWith("//")){
				return new URL(site.getProtocol() + ":" + linkStr);
			}
			else if(linkStr.startsWith("/")){
				return new URL(site.getProtocol(), site.getHost(), linkStr);
			}
			else if(linkStr.isEmpty() || SKIP_PATTERN.matcher(linkStr).matches()){
				return null;
			}
			int index;
			StringBuilder path = new StringBuilder(site.getPath());
			if((index = path.lastIndexOf("/")) > -1){
				path.delete(index + 1, path.length());
			}
			path.append(linkStr);
			return new URL(site.getProtocol(), site.getHost(), path.toString());
		}
		catch(Exception e){
			LOGGER.error("{}", e.getMessage());
		}
		return null;
	}
	
	public void stop(){
		this.stop = true;
	}
}
