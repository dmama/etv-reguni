package ch.vd.uniregctb.interfaces.model.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class EntiteCivileWrapper implements EntiteCivile {

	private final ch.vd.registre.civil.model.EntiteCivile target;
	private Collection<Adresse> adresses = null;

	public EntiteCivileWrapper(ch.vd.registre.civil.model.EntiteCivile target) {
		this.target = target;
	}

	public EntiteCivileWrapper(EntiteCivileWrapper wrapper, Set<EnumAttributeIndividu> parts) {
		this.target = wrapper.target;
		if (parts != null && parts.contains(EnumAttributeIndividu.ADRESSES)) {
			adresses = wrapper.getAdresses();
		}
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

	public void copyPartsFrom(Individu individu, Set<EnumAttributeIndividu> parts) {
		if (parts != null && parts.contains(EnumAttributeIndividu.ADRESSES)) {
			adresses = individu.getAdresses();
		}
	}
}
