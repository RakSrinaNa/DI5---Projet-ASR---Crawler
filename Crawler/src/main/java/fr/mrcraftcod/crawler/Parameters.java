package fr.mrcraftcod.crawler;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 01/09/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-09-01
 */
@SuppressWarnings("unused")
public class Parameters{
	private static final Logger LOGGER = LoggerFactory.getLogger(Parameters.class);
	private List<URL> sites = new LinkedList<>();
	private int threadCount = 4;
	private boolean recursive = false;
	private File outFolder = null;
	private boolean whole = false;
	
	public boolean getWhole(){
		return this.whole;
	}
	
	public boolean getRecursive(){
		return this.recursive;
	}
	
	public File getOutFolder(){
		return outFolder;
	}
	
	public Parameters(){
	}
	
	public List<URL> getSites(){
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
	
	@Option(name = "-r", aliases = "--recursive", usage = "Set the recursive status of the crawlers")
	public void setRecursive(boolean recursive){
		this.recursive = recursive;
	}
	
	@Option(name = "-w", aliases = "--whole", usage = "Crawl the whole site")
	public void setWhole(boolean whole){
		this.whole = whole;
	}
}
