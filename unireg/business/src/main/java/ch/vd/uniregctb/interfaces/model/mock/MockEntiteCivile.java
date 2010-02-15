package ch.vd.uniregctb.interfaces.model.mock;

import java.util.Collection;

import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;

public abstract class MockEntiteCivile implements EntiteCivile {

	private Collection<Adresse> adresses;

	public Collection<Adresse> getAdresses() {
		return adresses;
	}

	public void setAdresses(Collection<Adresse> adresses) {
		this.adresses = adresses;
	}
}
