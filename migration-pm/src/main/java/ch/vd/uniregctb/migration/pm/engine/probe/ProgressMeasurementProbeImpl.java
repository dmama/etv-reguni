package ch.vd.uniregctb.migration.pm.engine.probe;

/**
 * Sonde de mesure de l'avancement de la migration
 */
public class ProgressMeasurementProbeImpl implements ProgressMeasurementProbe {

	private volatile int percentProgress = 0;

	@Override
	public int getPercentProgress() {
		return percentProgress;
	}

	@Override
	public void setPercentProgress(int percent) {
		percentProgress = percent;
	}
}
