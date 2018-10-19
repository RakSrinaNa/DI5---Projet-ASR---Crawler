package fr.mrcraftcod.crawler;

import com.mashape.unirest.http.HttpResponse;
import fr.mrcraftcod.utils.base.FileUtils;
import fr.mrcraftcod.utils.http.requestssenders.get.BinaryGetRequestSender;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 2018-09-21.
 *
 * @author Thomas Couchoud
 * @since 2018-09-21
 */
public class DownloaderRunner implements Callable<Integer>{
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloaderRunner.class);
	private final File outFolder;
	private final Queue<DownloadElement> toDownload;
	private final Set<URL> downloaded;
	private final HashMap<String, String> headers;
	private boolean stop = false;
	private boolean canStop = false;
	
	public DownloaderRunner(File outFolder, Queue<DownloadElement> toDownload, Set<URL> downloaded, HashMap<String, String> headers){
		this.outFolder = outFolder;
		this.toDownload = toDownload;
		this.downloaded = downloaded;
		this.headers = headers;
	}
	
	@Override
	public Integer call() throws Exception{
		LOGGER.info("Downloader starting");
		int downloadedCount = 0;
		int retryCounter = 0;
		while(!stop){
			DownloadElement downloadElement;
			if((downloadElement = toDownload.poll()) != null){
				downloaded.add(downloadElement.getUrl());
				retryCounter = 0;
				
				LOGGER.info("Downloader is downloading link {}", downloadElement);
				
				File out = new File(outFolder, FileUtils.sanitizeFileName(downloadElement.getUrl().getHost()));
				
				final String[] urlPaths = downloadElement.getUrl().getPath().split("/");
				final String fileName = urlPaths[urlPaths.length - 1];
				final HttpResponse<InputStream> requestResult = new BinaryGetRequestSender(downloadElement.getUrl(), headers).getRequestResult();
				if(requestResult.getStatus() == 200){
					final InputStream finalStream = requestResult.getBody();
					try(finalStream){
						File file = new File(out, FileUtils.sanitizeFileName(fileName));
						if(file.exists()){
							file = new File(new File(out, "Duplicated"), FileUtils.sanitizeFileName(fileName) + ".txt");
							file.getParentFile().mkdirs();
							try(PrintWriter pw = new PrintWriter(new FileOutputStream(file, true)))
							{
								pw.println("Found image " + downloadElement.getUrl() + " from " + downloadElement.getSource());
							}
						}
						else{
							file.getParentFile().mkdirs();
							LOGGER.info("Saving to {}", file.getAbsolutePath());
							try(FileOutputStream fos = new FileOutputStream(file)){
								IOUtils.copy(finalStream, fos);
							}
						}
					}
					catch(final IOException e){
						e.printStackTrace();
					}
					downloadedCount += 1;
					LOGGER.info("Downloaded {}, images: {}, images banned: {}", downloadElement, toDownload.size(), downloaded.size());
				}
				else if(requestResult.getStatus() == 429){
					toDownload.offer(downloadElement);
					LOGGER.warn("Response code for page {} was {}, retrying later", downloadElement, requestResult.getStatus());
				}
				else{
					LOGGER.error("Response code for page {} was {}", downloadElement, requestResult.getStatus());
				}
			}
			else{
				retryCounter++;
				LOGGER.info("Downloader waiting: {}", retryCounter);
				if(retryCounter > 10){
					if(canStop){
						stop = true;
					}
				}
				Thread.sleep(5000);
			}
		}
		LOGGER.info("Downloader stopped");
		return downloadedCount;
	}
	
	public void stop(){
		this.stop = true;
	}
	
	public void canStop(){
		this.canStop = true;
	}
}
