package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;

public class OrigineWrapper implements Origine {

	private final ch.vd.registre.civil.model.Origine target;
	private Commune commune = null;
	private final RegDate dateDebut;

	public static OrigineWrapper get(ch.vd.registre.civil.model.Origine target) {
		if (target == null) {
			return null;
		}
		return new OrigineWrapper(target);
	}

	private OrigineWrapper(ch.vd.registre.civil.model.Origine target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDebutValidite());
	}

	public Commune getCommune() {
		if (commune == null) {
			commune = CommuneWrapper.get(target.getCommune());
		}
		return commune;
	}

	public RegDate getDebutValidite() {
		return dateDebut;
	}

	public Pays getPays() {
		return PaysWrapper.get(target.getPays());
	}

}
