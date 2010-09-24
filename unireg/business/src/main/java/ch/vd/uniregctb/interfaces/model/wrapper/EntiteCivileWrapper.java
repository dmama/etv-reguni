package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class EntiteCivileWrapper implements EntiteCivile, Serializable {

	private static final long serialVersionUID = -8177351795556884820L;
	
	private Collection<?> targetAdresses = null;
	private Collection<Adresse> adresses = null;

	public EntiteCivileWrapper(ch.vd.registre.civil.model.EntiteCivile target) {
		this.targetAdresses = target.getAdresses();
	}

	public EntiteCivileWrapper(EntiteCivileWrapper wrapper, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			this.adresses = wrapper.adresses;
			this.targetAdresses = wrapper.targetAdresses;
		}
	}

	public Collection<Adresse> getAdresses() {
		if (adresses == null) {
			initAdresses();
		}
		return adresses;
	}

	private void initAdresses() {
		synchronized (this) {
			if (adresses == null) {
				adresses = new ArrayList<Adresse>();
				if (targetAdresses != null) {
					for (Object o : targetAdresses) {
						ch.vd.common.model.Adresse a = (ch.vd.common.model.Adresse) o;
						adresses.add(AdresseWrapper.get(a));
					}
				}
				targetAdresses = null;
			}
		}
	}

	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			adresses = individu.getAdresses();
		}
	}
}
