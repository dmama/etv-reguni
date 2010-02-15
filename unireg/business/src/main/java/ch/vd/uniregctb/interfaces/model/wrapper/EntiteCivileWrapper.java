package ch.vd.uniregctb.interfaces.model.wrapper;

import java.util.ArrayList;
import java.util.Collection;

import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;

public abstract class EntiteCivileWrapper implements EntiteCivile {

	private final ch.vd.registre.civil.model.EntiteCivile target;
	private Collection<Adresse> adresses = null;

	public EntiteCivileWrapper(ch.vd.registre.civil.model.EntiteCivile target) {
		this.target = target;
	}

	public Collection<Adresse> getAdresses() {
		if (adresses == null) {
			adresses = new ArrayList<Adresse>();
			final Collection<?> targetAdresses = target.getAdresses();
			if (targetAdresses != null) {
				for (Object o : targetAdresses) {
					ch.vd.common.model.Adresse a = (ch.vd.common.model.Adresse) o;
					adresses.add(AdresseWrapper.get(a));
				}
			}
		}
		return adresses;
	}

}
