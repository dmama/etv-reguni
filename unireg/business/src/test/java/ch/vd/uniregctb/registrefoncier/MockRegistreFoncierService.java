package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.List;

import org.quartz.SchedulerException;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;

public class MockRegistreFoncierService implements RegistreFoncierService {

	private final List<Long> startedImports = new ArrayList<>();
	private final List<Long> startedMutations = new ArrayList<>();

	@Override
	public int deleteExistingMutations(long importId) {
		throw new NotImplementedException();
	}

	@Override
	public void startImport(long importId) throws JobAlreadyStartedException, SchedulerException {
		startedImports.add(importId);
	}

	@Override
	public void forceImport(long importId) {
		throw new NotImplementedException();
	}

	@Override
	public void startMutations(long importId) {
		startedMutations.add(importId);
	}

	@Override
	public void forceMutations(long importId) {
		throw new NotImplementedException();
	}

	public List<Long> getStartedImports() {
		return startedImports;
	}

	public List<Long> getStartedMutations() {
		return startedMutations;
	}
}
