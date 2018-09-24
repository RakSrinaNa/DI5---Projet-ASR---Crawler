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
	
	public DownloadElement(URL source, URL url){
		this.source = source;
		this.url = url;
	}
	
	public URL getSource(){
		return source;
	}
	
	public URL getUrl(){
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
