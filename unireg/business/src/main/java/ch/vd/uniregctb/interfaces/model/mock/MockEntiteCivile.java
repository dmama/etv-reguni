package ch.vd.uniregctb.interfaces.model.mock;

import java.util.Collection;
import java.util.Set;

import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class MockEntiteCivile implements EntiteCivile {

	private Collection<Adresse> adresses;

	protected MockEntiteCivile() {
	}

	protected MockEntiteCivile(MockEntiteCivile right, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			adresses = right.adresses;
		}
	}

	@Override
	public Collection<Adresse> getAdresses() {
		return adresses;
	}

	public void setAdresses(Collection<Adresse> adresses) {
		this.adresses = adresses;
	}

	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			adresses = individu.getAdresses();
		}
	}
}
