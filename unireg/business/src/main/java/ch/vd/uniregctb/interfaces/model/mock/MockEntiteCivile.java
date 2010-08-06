package ch.vd.uniregctb.interfaces.model.mock;

import java.util.Collection;
import java.util.Set;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class MockEntiteCivile implements EntiteCivile {

	private Collection<Adresse> adresses;

	protected MockEntiteCivile() {
	}

	protected MockEntiteCivile(MockEntiteCivile right, Set<EnumAttributeIndividu> parts) {
		if (parts != null && parts.contains(EnumAttributeIndividu.ADRESSES)) {
			adresses = right.adresses;
		}
	}

	public Collection<Adresse> getAdresses() {
		return adresses;
	}

	public void setAdresses(Collection<Adresse> adresses) {
		this.adresses = adresses;
	}

	public void copyPartsFrom(Individu individu, Set<EnumAttributeIndividu> parts) {
		if (parts != null && parts.contains(EnumAttributeIndividu.ADRESSES)) {
			adresses = individu.getAdresses();
		}
	}
}
