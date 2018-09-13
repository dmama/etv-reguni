package ch.vd.unireg.indexer.tiers;

import org.slf4j.Logger;

import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.stats.ServiceStats;
import ch.vd.unireg.stats.StatsService;

/**
 * Classe spécialisée pour stocker les différentes statistiques temporelles de l'indexation.
 */
public class TimeLog {

	private final StatsService statsService;
	public long startTime;
    public long startTimeInfra;
	public long startTimeCivil;
	public long startTimeEntreprise;
	public long startTimeIndex;
    public long endTime;
    public long endTimeInfra;
	public long endTimeCivil;
	public long endTimeEntreprise;
	public long endTimeIndex;
    public long indexerCpuTime;
    public long indexerExecTime;

	public TimeLog(StatsService statsService) {
		this.statsService = statsService;
	}

	public void start() {
		startTime = System.nanoTime() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		startTimeInfra = getNanoInfra() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		startTimeCivil = getNanoCivil() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		startTimeEntreprise = getNanoEntreprise() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		startTimeIndex = getNanoIndex() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
	}

	public void end() {
		endTime = System.nanoTime() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		endTimeInfra = getNanoInfra() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		endTimeCivil = getNanoCivil() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		endTimeEntreprise = getNanoEntreprise() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		endTimeIndex = getNanoIndex() / GlobalTiersIndexerImpl.NANO_TO_MILLI;
		indexerCpuTime = 0;     // TODO (msi) trouver un moyen de déterminer ce temps
		indexerExecTime = 0;    // TODO (msi) trouver un moyen de déterminer ce temps
	}

	public static class Stats {

		public final long indexerCpuTime;
		public final long indexerExecTime;
		public final long timeTotal ;
		public final long timeWait ;
		public final long timeWaitInfra ;
		public final long timeWaitCivil ;
		public final long timeWaitEntreprise ;
		public final long timeWaitIndex ;
		public final long timeWaitAutres ;
		public final int percentCpu ;
		public final int percentWait ;
		public final int percentWaitInfra ;
		public final int percentWaitCivil ;
		public final int percentWaitEntreprise ;
		public final int percentWaitIndex ;
		public final int percentWaitAutres ;

		public Stats(TimeLog timeLog) {

			this.indexerCpuTime = timeLog.indexerCpuTime;
			this.indexerExecTime = timeLog.indexerExecTime;

			// détermine les différents statistiques de temps en millisecondes
			this.timeTotal = timeLog.endTime - timeLog.startTime;
			this.timeWait = timeLog.indexerExecTime - timeLog.indexerCpuTime;
			this.timeWaitInfra = timeLog.endTimeInfra - timeLog.startTimeInfra;
			this.timeWaitCivil = timeLog.endTimeCivil - timeLog.startTimeCivil;
			this.timeWaitEntreprise = timeLog.endTimeEntreprise - timeLog.startTimeEntreprise;
			this.timeWaitIndex = timeLog.endTimeIndex - timeLog.startTimeIndex;
			this.timeWaitAutres = this.timeWait - this.timeWaitInfra - this.timeWaitCivil - this.timeWaitEntreprise - this.timeWaitIndex;

			if (this.indexerExecTime == 0 || this.timeWait == 0) {
				this.percentCpu = -1;
				this.percentWait = -1;
				this.percentWaitInfra = -1;
				this.percentWaitCivil = -1;
				this.percentWaitEntreprise = -1;
				this.percentWaitIndex = -1;
				this.percentWaitAutres = -1;
			}
			else {
				this.percentCpu = (int) (100 * timeLog.indexerCpuTime / timeLog.indexerExecTime);
				this.percentWait = 100 - this.percentCpu;
				this.percentWaitInfra = (int) (100 * this.timeWaitInfra / this.timeWait);
				this.percentWaitCivil = (int) (100 * this.timeWaitCivil / this.timeWait);
				this.percentWaitEntreprise = (int) (100 * this.timeWaitEntreprise / this.timeWait);
				this.percentWaitIndex = (int) (100 * this.timeWaitIndex / this.timeWait);
				this.percentWaitAutres = 100 - this.percentWaitInfra - this.percentWaitCivil - this.percentWaitEntreprise - this.percentWaitIndex;
			}
		}

		public boolean isDispo() {
			return indexerExecTime > 0 && timeWait > 0;
		}
	}

	public Stats createStats() {
		return new Stats(this);
	}

	public void logStats(Logger logger) {

        // détermine les différents statistiques de temps en millisecondes
		final Stats stats = createStats();
		if (!stats.isDispo()) {
            logger.debug("Statistiques d'indexation indisponibles !");
            return;
        }

	    String log = "Temps total d'exécution         : " + stats.timeTotal + " ms\n";
	    log += "Temps 'exec' threads indexation : " + stats.indexerExecTime + " ms\n";
	    log += "Temps 'cpu' threads indexation  : " + stats.indexerCpuTime + " ms" + " (" + stats.percentCpu + "%)\n";
	    log += "Temps 'wait' threads indexation : " + stats.timeWait + " ms" + " (" + stats.percentWait + "%)\n";
	    log += " - service infrastructure       : " + (stats.timeWaitInfra == 0 ? "<indisponible>\n" : stats.timeWaitInfra + " ms" + " (" + stats.percentWaitInfra + "%)\n");
	    log += " - service civil                : " + (stats.timeWaitCivil == 0 ? "<indisponible>\n" : stats.timeWaitCivil + " ms" + " (" + stats.percentWaitCivil + "%)\n");
	    log += " - service entreprise           : " + (stats.timeWaitEntreprise == 0 ? "<indisponible>\n" : stats.timeWaitEntreprise + " ms" + " (" + stats.percentWaitEntreprise + "%)\n");
	    log += " - indexer                      : " + (stats.timeWaitIndex == 0 ? "<indisponible>\n" : stats.timeWaitIndex + " ms" + " (" + stats.percentWaitIndex + "%)\n");
	    log += " - autre (scheduler, jdbc, ...) : " + stats.timeWaitAutres + " ms" + " (" + stats.percentWaitAutres + "%)";
	    logger.info(log);
    }

    private long getNanoCivil() {
        long timecivil = 0;
	    final ServiceStats stats = statsService.getServiceStats(ServiceCivilService.SERVICE_NAME);
        if (stats != null) {
            timecivil = stats.getTotalTime();
        }
        return timecivil;
    }

    private long getNanoEntreprise() {
        long timecivil = 0;
	    final ServiceStats stats = statsService.getServiceStats(ServiceEntreprise.SERVICE_NAME);
        if (stats != null) {
            timecivil = stats.getTotalTime();
        }
        return timecivil;
    }

    private long getNanoInfra() {
        long timeinfra = 0;
	    final ServiceStats stats = statsService.getServiceStats(ServiceInfrastructureService.SERVICE_NAME);
        if (stats != null) {
            timeinfra = stats.getTotalTime();
        }
        return timeinfra;
    }

    private long getNanoIndex() {
        long timeindex = 0;
	    final ServiceStats stats = statsService.getServiceStats(GlobalTiersIndexer.SERVICE_NAME);
        if (stats != null) {
            timeindex = stats.getTotalTime();
        }
        return timeindex;
    }
}
