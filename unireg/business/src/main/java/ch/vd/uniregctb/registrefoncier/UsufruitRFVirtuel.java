package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;

public class UsufruitRFVirtuel extends ServitudeRF {

	private List<DroitRF> chemin;

	public List<DroitRF> getChemin() {
		return chemin;
	}

	public void setChemin(List<DroitRF> chemin) {
		this.chemin = chemin;
	}

	@Override
	public ServitudeRF duplicate() {
		throw new NotImplementedException();
	}
}
