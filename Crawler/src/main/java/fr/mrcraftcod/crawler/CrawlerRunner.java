package fr.mrcraftcod.crawler;

import fr.mrcraftcod.utils.http.requestssenders.get.StringGetRequestSender;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The crawler itself.
 * <p>
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
	private final HashMap<String, String> headers;
	private final boolean recursive;
	private final boolean whole;
	private final Collection<URL> baseLinks;
	private boolean stop = false;
	
	/**
	 * Constructor.
	 *
	 * @param toCrawl    The queue of the links to crawl.
	 * @param crawled    The set of crawled links.
	 * @param images     The queue of images to download.
	 * @param downloaded The set of downloaded images.
	 * @param headers    The set of headers to use.
	 * @param recursive  True if the crawler is crawling recursively.
	 */
	public CrawlerRunner(Queue<URL> toCrawl, Set<URL> crawled, Queue<DownloadElement> images, Set<URL> downloaded, HashMap<String, String> headers, boolean recursive, boolean whole, Collection<URL> baseLinks){
		this.toCrawl = toCrawl;
		this.crawled = crawled;
		this.images = images;
		this.downloaded = downloaded;
		this.headers = headers;
		this.recursive = recursive;
		this.whole = whole;
		this.baseLinks = baseLinks;
	}
	
	@Override
	public Integer call(){
		LOGGER.info("Crawler started");
		var crawledCount = 0;
		var retryCounter = 0;
		while(!stop){
			try{
				URL site;
				if((site = toCrawl.poll()) != null){
					crawled.add(site);
					retryCounter = 0;
					
					LOGGER.info("Crawler is crawling link {}", site);
					
					final var requestResult = new StringGetRequestSender(site, headers).getRequestResult();
					if(requestResult.getStatus() == 200){
						final var rootDocument = Jsoup.parse(requestResult.getBody());
						var stream1 = rootDocument.getElementsByTag("a").parallelStream().map(aElem -> {
							if(aElem.hasAttr("href")){
								return aElem.attr("href");
							}
							if(aElem.hasAttr("data-image")){
								return aElem.attr("data-image");
							}
							return null;
						}).filter(Objects::nonNull);
						var stream2 = Stream.concat(rootDocument.getElementsByTag("video").stream(), rootDocument.getElementsByTag("source").stream()).parallel().filter(aElem -> aElem.hasAttr("src")).map(aElem -> aElem.attr("src"));
						var streamLinks = Stream.concat(stream1, stream2).map(linkStr -> getURL(site, linkStr)).filter(Objects::nonNull).filter(link -> !downloaded.contains(link));
						images.addAll(rootDocument.getElementsByTag("img").parallelStream().filter(aElem -> aElem.hasAttr("src")).map(aElem -> aElem.absUrl("src")).map(linkStr -> getImageURL(site, linkStr)).filter(Objects::nonNull).filter(link -> !downloaded.contains(link)).map(link -> new DownloadElement(site, link)).collect(Collectors.toSet()));
						
						var added = 0;
						var picAdded = 0;
						for(var link : streamLinks.collect(Collectors.toSet())){
							var paths = link.getPath().split("/");
							if(paths.length > 0){
								var resource = paths[paths.length - 1];
								if(MEDIA_PATTERN.matcher(resource).matches()){
									link = getImageURL(site, link.toString());
									if(!downloaded.contains(link)){
										if(images.offer(new DownloadElement(site, link))){
											picAdded++;
										}
									}
								}
								else if(!crawled.contains(link)){
									if(Objects.equals(site.getHost(), link.getHost()) && (whole || isBaseLink(link))){
										if(recursive && !toCrawl.contains(link)){
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
					
					if(images.size() > 1000){
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
	
	private boolean isBaseLink(URL link){
		return baseLinks.stream().anyMatch(l -> Objects.equals(l.getHost(), link.getHost()) && link.getPath().startsWith(l.getPath()));
	}
	
	/**
	 * Get the full url of a link.
	 *
	 * @param site    The url from where the link have been found.
	 * @param linkStr The url.
	 *
	 * @return The full url.
	 */
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
	
	/**
	 * Get the full url of an image.
	 *
	 * @param site    The url from where the image have been found.
	 * @param linkStr The url of the image.
	 *
	 * @return A full image url.
	 */
	private URL getImageURL(URL site, String linkStr){
		if(linkStr.contains("#")){
			linkStr = linkStr.substring(0, linkStr.indexOf("#"));
		}
		if(linkStr.contains("?")){
			linkStr = linkStr.substring(0, linkStr.indexOf("?"));
		}
		return getURL(site, linkStr);
	}
	
	/**
	 * Stops this crawler.
	 */
	public void stop(){
		this.stop = true;
	}
}
