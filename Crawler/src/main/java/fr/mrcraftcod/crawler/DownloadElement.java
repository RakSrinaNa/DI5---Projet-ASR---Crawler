package fr.mrcraftcod.crawler;

import java.net.URL;
import java.util.Objects;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 2018-09-22.
 *
 * @author Thomas Couchoud
 * @since 2018-09-22
 */
public class DownloadElement{
	private final URL source;
	private final URL url;
	
	/**
	 * Constructor.
	 *
	 * @param source The source of the element.
	 * @param url    The url of the element.
	 */
	DownloadElement(URL source, URL url){
		this.source = source;
		this.url = url;
	}
	
	/**
	 * Get the source url.
	 *
	 * @return The url.
	 */
	public URL getSource(){
		return source;
	}
	
	/**
	 * Get the element url.
	 *
	 * @return The element url.
	 */
	URL getUrl(){
		return url;
	}
	
	@Override
	public boolean equals(Object obj){
		return Objects.equals(obj, getUrl());
	}
	
	@Override
	public String toString(){
		return getUrl().toString();
	}
}
