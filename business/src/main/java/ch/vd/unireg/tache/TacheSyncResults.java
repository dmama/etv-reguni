package ch.vd.uniregctb.tache;

import java.util.LinkedList;
import java.util.List;

import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;

public class TacheSyncResults extends AbstractJobResults<Long, TacheSyncResults> {

	public abstract static class CtbInfo {
		public final long ctbId;
		protected CtbInfo(long ctbId) {
			this.ctbId = ctbId;
		}
	}

	public static class ActionInfo extends CtbInfo {
		public final String actionMsg;
		public ActionInfo(long ctbId, SynchronizeAction action) {
			super(ctbId);
			this.actionMsg = action.toString();
		}
	}

	public static class ExceptionInfo extends CtbInfo {
		public final String exceptionMsg;
		public ExceptionInfo(long ctbId, Exception e) {
			super(ctbId);
			this.exceptionMsg = String.format("%s: %s", e.getClass().getName(), e.getMessage());
		}
	}

	private final List<ActionInfo> actions = new LinkedList<>();
	private final List<ExceptionInfo> exceptions = new LinkedList<>();
	private final boolean cleanupOnly;
	private boolean interrupted = false;

	public TacheSyncResults(boolean cleanupOnly) {
		this.cleanupOnly = cleanupOnly;
	}

	public void addAction(Long ctbId, SynchronizeAction action) {
		actions.add(new ActionInfo(ctbId, action));
	}

	@Override
	public void addErrorException(Long ctbId, Exception e) {
		exceptions.add(new ExceptionInfo(ctbId, e));
	}

	@Override
	public void addAll(TacheSyncResults right) {
		actions.addAll(right.actions);
		exceptions.addAll(right.exceptions);
	}

	public boolean isCleanupOnly() {
		return cleanupOnly;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public List<ActionInfo> getActions() {
		return actions;
	}

	public List<ExceptionInfo> getExceptions() {
		return exceptions;
	}
}
