package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.springframework.beans.factory.InitializingBean;

public class EvenementCivilEchStrategyParametersImpl implements EvenementCivilEchStrategyParameters, InitializingBean {

	private int decalageMaxPourDepart;

	public void setDecalageMaxPourDepart(int decalageMaxPourDepart) {
		this.decalageMaxPourDepart = decalageMaxPourDepart;
	}

	public int getDecalageMaxPourDepart() {
		return decalageMaxPourDepart;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (decalageMaxPourDepart < 0) {
			throw new IllegalArgumentException("La valeur de la constante 'decalageMaxPourDepart' doit être positive ou nulle.");
		}
	}
}
