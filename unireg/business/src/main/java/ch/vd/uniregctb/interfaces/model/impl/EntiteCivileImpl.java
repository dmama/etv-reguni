package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class EntiteCivileImpl implements EntiteCivile, Serializable {

	private static final long serialVersionUID = 8648205824787291869L;
	
	private Collection<Adresse> adresses;

	public EntiteCivileImpl(ch.vd.registre.civil.model.EntiteCivile target) {
		this.adresses = new ArrayList<Adresse>();
		if (target.getAdresses() != null) {
			for (Object o : target.getAdresses()) {
				ch.vd.common.model.Adresse a = (ch.vd.common.model.Adresse) o;
				this.adresses.add(AdresseImpl.get(a));
			}
		}
	}

	public EntiteCivileImpl(EntiteCivileImpl right, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			this.adresses = right.adresses;
		}
		else  {
			this.adresses = Collections.emptyList();
		}
	}

	public Collection<Adresse> getAdresses() {
		return adresses;
	}

	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			adresses = individu.getAdresses();
		}
	}
}
