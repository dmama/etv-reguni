package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;

public class MockOrigine implements Origine {

	private Commune commune;

	private RegDate debutValidite;

	private Pays pays;

	@Override
	public Commune getCommune() {
		return commune;
	}

	public void setCommune(Commune commune) {
		this.commune = commune;
	}

	@Override
	public RegDate getDebutValidite() {
		return debutValidite;
	}

	public void setDebutValidite(RegDate debutValidite) {
		this.debutValidite = debutValidite;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

	public void setPays(Pays pays) {
		this.pays = pays;
	}
}
