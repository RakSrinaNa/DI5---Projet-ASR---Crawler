package fr.mrcraftcod.crawler;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 01/09/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-09-01
 */
@SuppressWarnings("unused")
public class Parameters{
	private static final Logger LOGGER = LoggerFactory.getLogger(Parameters.class);
	private Set<URL> sites = new HashSet<>();
	private int threadCount = 4;
	private File outFolder = null;
	
	
	public File getOutFolder(){
		return outFolder;
	}
	
	public Parameters(){
	}
	
	public Set<URL> getSites(){
		return sites;
	}
	
	public int getThreadCount(){
		return threadCount;
	}
	
	@Option(name = "-o", aliases = "--out_folder", usage = "The folder where to download images", required = true)
	public void setOutFolder(File value){
		this.outFolder = value;
	}
	
	@Option(name = "-l", aliases = "--link", usage = "Add website to crawl")
	public void addSite(URL url){
		if(!url.toString().endsWith("/")){
			try{
				url = new URL(url.toString() + "/");
			}
			catch(final MalformedURLException e){
				LOGGER.error("", e);
			}
		}
		this.sites.add(url);
	}
	
	@Option(name = "-f", aliases = "--file", usage = "Add websites to crawl")
	public void addSites(File file){
		if(file.isFile()){
			try{
				
				for(String line : Files.readAllLines(Paths.get(file.toURI()))){
					try{
						sites.add(new URL(line));
					}
					catch(Exception e){
						LOGGER.error("Error parsing link {}", line);
					}
				}
			}
			catch(Exception e){
				LOGGER.error("Error reading file {}", file, e);
			}
		}
	}
	
	@Option(name = "-t", aliases = "--thread_count", usage = "The number of thread to create")
	public void setThreadCount(int threadCount){
		this.threadCount = threadCount;
	}
}
