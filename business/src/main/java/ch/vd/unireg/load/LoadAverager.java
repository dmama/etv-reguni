package ch.vd.unireg.load;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.stats.LoadMonitorable;

/**
 * Classe qui est capable de calculer la moyenne de la charge sur un ws donné<p/>
 * Pour le démarrer, appeler {@link #start} et pour l'arrêter, appeler {@link #stop}
 */
public class LoadAverager {

	/**
	 * Service à monitorer
	 */
	private final LoadMonitorable service;

	/**
	 * Identifiant lisible pour le service à monitorer
	 */
	private final String nomService;

	/**
	 * Nombre de points d'échantillonnage
	 */
	private final int nbSamplingPoints;

	/**
	 * Période d'échantillonnage (en milli-secondes)
	 */
	private final int samplingPeriod;

	/**
	 * Timer pour les échantillonages
	 */
	private Timer timer;

	/**
	 * Données échantillonnées
	 */
	private final int[] samplingData;

	/**
	 * Index où stocker la prochaine donnée échantillonnée dans le tableau {@link #samplingData}
	 */
	private int samplingCursor;

	/**
	 * Nombre de données échantillonnées présentes dans le tableau {@link #samplingData}
	 */
	private int samplingHighWaterMark;

	public LoadAverager(LoadMonitorable service, String nomService, int nbSamplingPoints, int samplingPeriod) {
		if (service == null || StringUtils.isBlank(nomService) || nbSamplingPoints <= 0 || samplingPeriod <= 0) {
			throw new IllegalArgumentException();
		}

		this.service = service;
		this.nomService = nomService;
		this.nbSamplingPoints = nbSamplingPoints;
		this.samplingPeriod = samplingPeriod;
		this.samplingData = new int[nbSamplingPoints];
		this.samplingCursor = 0;
		this.samplingHighWaterMark = 0;
	}

	public double getAverageLoad() {
		return getAverage();
	}

	/**
	 * Tâche de récupération des données sur le service à échantilloner
	 */
	private final class SamplingTask extends TimerTask {
		@Override
		public void run() {
			final int charge = service.getLoad();
			addSamplingData(charge);
		}
	}

	public void start() {
		if (timer != null) {
			throw new IllegalArgumentException();
		}

		// un thread daemon est tout-à-fait suffisant, cela ne doit en aucun cas bloquer l'arrêt de l'application
		timer = new Timer(String.format("Load-%s", nomService), true);
		timer.schedule(new SamplingTask(), samplingPeriod, samplingPeriod);
	}

	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	protected void addSamplingData(int charge) {
		synchronized (samplingData) {
			samplingData[samplingCursor ++] = charge;
			if (samplingHighWaterMark < samplingCursor) {
				samplingHighWaterMark = samplingCursor;
			}
			samplingCursor %= nbSamplingPoints;
		}
	}

	protected double getAverage() {
		synchronized (samplingData) {
			int somme = 0;
			for (int i = 0 ; i < samplingHighWaterMark ; ++ i) {
				somme += samplingData[i];
			}
			if (samplingHighWaterMark == 0) {
				return 0.0;
			}
			else {
				return (1.0 * somme) / samplingHighWaterMark;
			}
		}
	}
}
