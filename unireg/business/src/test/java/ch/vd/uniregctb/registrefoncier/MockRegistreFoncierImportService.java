package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.List;

import org.quartz.SchedulerException;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;

public class MockRegistreFoncierImportService implements RegistreFoncierImportService {

	private final List<Long> startedImports = new ArrayList<>();
	private final List<Long> startedMutations = new ArrayList<>();
	private final List<Long> deletedImportMutations = new ArrayList<>();

	@Override
	public int deleteAllMutations(long importId, StatusManager statusManager) {
		deletedImportMutations.add(importId);
		return 0;
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
	public void forceMutation(long mutId) {
		throw new NotImplementedException();
	}

	@Override
	public void forceAllMutations(long importId) {
		throw new NotImplementedException();
	}

	public List<Long> getStartedImports() {
		return startedImports;
	}

	public List<Long> getDeletedImportMutations() {
		return deletedImportMutations;
	}

	public List<Long> getStartedMutations() {
		return startedMutations;
	}
}
