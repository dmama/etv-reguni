package ch.vd.unireg.indexer.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.indexer.GlobalIndexInterface;

public abstract class AbstractConcurrentAccessThread extends Thread {

	private Logger LOGGER = LoggerFactory.getLogger(AbstractConcurrentAccessThread.class);
	
	private boolean stopPlease = false;
	protected GlobalIndexInterface globalIndex;
	private boolean inError = false;


	public AbstractConcurrentAccessThread(GlobalIndexInterface globalIndex) {
		this.globalIndex = globalIndex;
		setName(getClass().getSimpleName());
	}

	public boolean isInError() {
		return inError;
	}

	protected abstract void doRun() throws Exception;

	@Override
	public void run() {
		
		try {
			doRun();
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			inError = true;
		}
	}

	public void stopPlease() {
		stopPlease = true;
	}

	public boolean isStopPlease() {
		return stopPlease;
	}

}
