package ch.vd.uniregctb.migration.pm.engine.probe;

/**
 * Interface basique d'une sonde de mesure de progression
 */
public interface ProgressMeasurementProbe {

	/**
	 * @return l'avancement, en pourcentages (0-100)...
	 */
	int getPercentProgress();

	/**
	 * @param percent l'avancement, en pourcentages (0-100)...
	 */
	void setPercentProgress(int percent);
}
