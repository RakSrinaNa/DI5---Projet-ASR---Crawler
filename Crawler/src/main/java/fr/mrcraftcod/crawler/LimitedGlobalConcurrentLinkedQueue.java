package fr.mrcraftcod.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class LimitedGlobalConcurrentLinkedQueue<E> extends ConcurrentLinkedQueue<E>{
	private static final long serialVersionUID = 5437264731468295683L;
	private final Logger LOGGER = LoggerFactory.getLogger(LimitedGlobalConcurrentLinkedQueue.class);
	private final AtomicLong lastAccess;
	private final long DELTA_MS = 1000L;
	
	public LimitedGlobalConcurrentLinkedQueue(){
		this.lastAccess = new AtomicLong(0);
	}
	
	public LimitedGlobalConcurrentLinkedQueue(final Collection<? extends E> c){
		super(c);
		this.lastAccess = new AtomicLong(0);
	}
	
	@SuppressWarnings("Duplicates")
	@Override
	public E poll(){
		var diff = 0L;
		do{
			if(diff > 0){
				try{
					LOGGER.info("Trying to get element too soon, sleeping thread for {} ms", diff);
					Thread.sleep(diff);
				}
				catch(final InterruptedException e){
					LOGGER.error("", e);
				}
			}
			diff = lastAccess.get() + DELTA_MS - System.currentTimeMillis();
		}
		while(diff > 0);
		lastAccess.set(System.currentTimeMillis());
		return super.poll();
	}
}
