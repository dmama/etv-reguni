package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Localite;

public class LocaliteWrapper implements Localite {

	private final ch.vd.infrastructure.model.Localite target;
	private CommuneSimple commune = null;
	private final RegDate dateFin;

	public static LocaliteWrapper get(ch.vd.infrastructure.model.Localite target) {
		if (target == null) {
			return null;
		}
		return new LocaliteWrapper(target);
	}

	private LocaliteWrapper(ch.vd.infrastructure.model.Localite target) {
		this.target = target;
		this.dateFin = RegDate.get(target.getDateFinValidite());
	}

	public Integer getChiffreComplementaire() {
		return target.getChiffreComplementaire();
	}

	public CommuneSimple getCommuneLocalite() {
		if (commune == null) {
			commune = CommuneSimpleWrapper.get(target.getCommuneLocalite());
		}
		return commune;
	}

	public Integer getComplementNPA() {
		final Integer c = target.getComplementNPA();
		if (c == null || c.intValue() == 0) {	// un complément de 0 signifie pas de complément
			return null;
		}
		else {
			return c;
		}
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public Integer getNPA() {
		return target.getNPA();
	}

	public Integer getNoCommune() {
		return target.getNoCommune();
	}

	public Integer getNoOrdre() {
		return target.getNoOrdre();
	}

	public String getNomAbregeMajuscule() {
		return target.getNomAbregeMajuscule();
	}

	public String getNomAbregeMinuscule() {
		return target.getNomAbregeMinuscule();
	}

	public String getNomCompletMajuscule() {
		return target.getNomCompletMajuscule();
	}

	public String getNomCompletMinuscule() {
		return target.getNomCompletMinuscule();
	}

	public boolean isValide() {
		return target.isValide();
	}

	public ch.vd.infrastructure.model.Localite getTarget() {
		return target;
	}
}
