package ch.vd.unireg.indexer.jobs;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.indexer.tiers.TimeLog;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.tiers.TypeTiers;

public class DatabaseIndexationResults extends JobResults<Long, DatabaseIndexationResults> {

	public static class Error {

		private final long id;
		private final String message;

		public Error(long id, String message) {
			this.id = id;
			this.message = message;
		}

		public long getId() {
			return id;
		}

		public String getMessage() {
			return message;
		}
	}

	// paramètres du job
	private final GlobalTiersIndexer.Mode mode;
	private final Set<TypeTiers> typesTiers;
	private final int nbThreads;
	private final RegDate dateTraitement = RegDate.get();

	// info du job
	private boolean interrompu = false;
	private final List<Long> indexes = new LinkedList<>();
	private final List<Long> supprimes = new LinkedList<>();
	private final List<Error> errors = new LinkedList<>();
	private final TimeLog timeLog;

	public DatabaseIndexationResults(@NotNull GlobalTiersIndexer.Mode mode, @NotNull Set<TypeTiers> typesTiers, int nbThreads, @NotNull StatsService statsService) {
		super(null, null);
		this.mode = mode;
		this.typesTiers = typesTiers;
		this.nbThreads = nbThreads;
		this.timeLog = new TimeLog(statsService);
		this.timeLog.start();
	}

	@Override
	public void end() {
		super.end();
		timeLog.end();
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public GlobalTiersIndexer.Mode getMode() {
		return mode;
	}

	public synchronized Set<TypeTiers> getTypesTiers() {
		return typesTiers;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public synchronized List<Long> getIndexes() {
		return indexes;
	}

	public synchronized List<Error> getErrors() {
		return errors;
	}

	public synchronized List<Long> getSupprimes() {
		return supprimes;
	}

	public TimeLog getTimeLog() {
		return timeLog;
	}

	public synchronized void addTiersIndexe(@NotNull Long id) {
		this.indexes.add(id);
	}

	public synchronized void addTiersSupprime(Long id) {
		this.supprimes.add(id);
	}

	@Override
	public synchronized void addErrorException(Long id, Exception e) {
		errors.add(new Error(id, ExceptionUtils.extractCallStack(e)));
	}

	@Override
	public synchronized void addAll(DatabaseIndexationResults right) {
		this.indexes.addAll(right.indexes);
		this.supprimes.addAll(right.supprimes);
		this.errors.addAll(right.errors);
	}
}
