package activity.classifier.utils;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 
 * A generic class that is stores items while they wait to be processed (filled),
 * as well as items that aren't ready to be processed (empty). The items are
 * pre-allocated and meant to be reused to avoid construction/destruction overhead
 * and the garbage collection overhead. The take and put functions of each of the
 * two queues are blocking and hence should be used with care not to cause a dead-lock.  
 * Also, an item taken should be returned in order to avoid dead-locks while taking.
 * 
 * @author Umran
 *
 * @param <InstanceType>
 * 	The instance to keep in the queue.
 * 
 */
public abstract class TwoWayBlockingQueue<InstanceType> {
	
	private int totalInstanceCount;
	private ArrayBlockingQueue<InstanceType> filledInstances;
	private ArrayBlockingQueue<InstanceType> emptyInstances;
	
	public TwoWayBlockingQueue(int totalInstanceCount) {
		
		this.totalInstanceCount = totalInstanceCount;
		this.filledInstances = new ArrayBlockingQueue<InstanceType>(totalInstanceCount, true);
		this.emptyInstances = new ArrayBlockingQueue<InstanceType>(totalInstanceCount, true);
		
		for (int i=0; i<totalInstanceCount; ++i)
			this.emptyInstances.add(getNewInstance());
	}
	
	protected abstract InstanceType getNewInstance();
	
	public int getTotalInstanceCount() {
		return totalInstanceCount;
	}
	
	public InstanceType takeEmptyInstance() throws InterruptedException {
		return emptyInstances.take();
	}
	
	public void returnEmptyInstance(InstanceType instance) throws InterruptedException {
		emptyInstances.put(instance);
	}
	
	public InstanceType takeFilledInstance() throws InterruptedException {
		return filledInstances.take();
	}
	
	public InstanceType peekFilledInstance() {
		return filledInstances.peek();
	} 
	
	public void returnFilledInstance(InstanceType instance) throws InterruptedException {
		filledInstances.put(instance);
	}
	
	public int getPendingFilledInstances() {
		return filledInstances.size();
	}
	
	public int getPendingEmptyInstances() {
		return emptyInstances.size();
	}
	
	public void assertAllAvailable() {
		int filledInst = filledInstances.size();
		int emptyInst = emptyInstances.size();
		if (filledInst+emptyInst!=totalInstanceCount) {
			throw new RuntimeException(
					"Non-matching queue counts: filled queue size="+filledInst+
					", empty queue size="+filledInst+
					", total expected size="+totalInstanceCount);
		}
	}
	
}
