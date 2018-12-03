package fr.mrcraftcod.crawler;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 2018-12-03.
 *
 * @author Thomas Couchoud
 * @since 2018-12-03
 */
public class RandomConcurrentLinkedList<E> extends LinkedList<E>{
	private static final long serialVersionUID = 7047058767066331815L;
	private final Object lock = new Object();
	private int nextIndex = 0;
	
	public RandomConcurrentLinkedList(){
		super();
	}
	
	public RandomConcurrentLinkedList(Collection<? extends E> c){
		super(c);
	}
	
	@Override
	public E peek(){
		synchronized(lock){
			int s = size();
			if(s < 1){
				return null;
			}
			int i = nextIndex % s;
			return super.get(i);
		}
	}
	
	@Override
	public E poll(){
		synchronized(lock){
			int s = size();
			if(s < 1){
				return null;
			}
			int i = nextIndex % s;
			E obj = super.get(i);
			super.remove(i);
			nextIndex = ThreadLocalRandom.current().nextInt(size());
			return obj;
		}
	}
}
