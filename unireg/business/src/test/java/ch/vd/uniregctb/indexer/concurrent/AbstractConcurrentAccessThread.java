package ch.vd.uniregctb.indexer.concurrent;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.indexer.GlobalIndexInterface;

public abstract class AbstractConcurrentAccessThread extends Thread {

	private Logger LOGGER = Logger.getLogger(AbstractConcurrentAccessThread.class);
	
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
			LOGGER.error(e,e);
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
