package fr.mrcraftcod.crawler;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 2018-10-16.
 *
 * @author Thomas Couchoud
 * @since 2018-10-16
 */
public class LimitedConcurrentLinkedQueue extends ConcurrentLinkedQueue{
	private final ConcurrentHashMap<Long, Long> lastAccess;
	
	public LimitedConcurrentLinkedQueue(){
		super();
		this.lastAccess = new ConcurrentHashMap<>();
	}
	
	public LimitedConcurrentLinkedQueue(Collection c){
		super(c);
		this.lastAccess = new ConcurrentHashMap<>();
	}
	
	@Override
	public Object poll(){
		return super.poll();
	}
}
