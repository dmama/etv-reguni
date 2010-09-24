package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;

public class OrigineWrapper implements Origine, Serializable {

	private static final long serialVersionUID = -3970848418800378247L;
	
	private final RegDate dateDebut;
	private Commune commune = null;
	private ch.vd.infrastructure.model.Commune targetCommune;
	private Pays pays;
	private ch.vd.infrastructure.model.Pays targetPays;

	public static OrigineWrapper get(ch.vd.registre.civil.model.Origine target) {
		if (target == null) {
			return null;
		}
		return new OrigineWrapper(target);
	}

	private OrigineWrapper(ch.vd.registre.civil.model.Origine target) {
		this.dateDebut = RegDate.get(target.getDebutValidite());
		this.targetCommune = target.getCommune();
		this.targetPays = target.getPays();
	}

	public Commune getCommune() {
		if (commune == null && targetCommune != null) {
			commune = CommuneWrapper.get(targetCommune);
			targetCommune = null;
		}
		return commune;
	}

	public RegDate getDebutValidite() {
		return dateDebut;
	}

	public Pays getPays() {
		if (pays == null && targetPays != null) {
			pays = PaysWrapper.get(targetPays);
			targetPays = null;
		}
		return pays;
	}

}
