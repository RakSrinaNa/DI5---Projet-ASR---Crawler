package fr.mrcraftcod.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class LimitedGlobalConcurrentLinkedList<E> extends RandomConcurrentLinkedList<E>{
	private static final long serialVersionUID = 1210224138182261156L;
	private final Logger LOGGER = LoggerFactory.getLogger(LimitedGlobalConcurrentLinkedList.class);
	private final AtomicLong lastAccess;
	private final long DELTA_MS = 1000L;
	
	public LimitedGlobalConcurrentLinkedList(){
		this.lastAccess = new AtomicLong(0);
	}
	
	public LimitedGlobalConcurrentLinkedList(final Collection<? extends E> c){
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
