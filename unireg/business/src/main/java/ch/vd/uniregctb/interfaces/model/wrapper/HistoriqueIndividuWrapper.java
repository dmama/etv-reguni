package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;

public class HistoriqueIndividuWrapper implements HistoriqueIndividu {

	private final ch.vd.registre.civil.model.HistoriqueIndividu target;
	private final RegDate dateDebut;

	public static HistoriqueIndividuWrapper get(ch.vd.registre.civil.model.HistoriqueIndividu target) {
		if (target == null) {
			return null;
		}
		return new HistoriqueIndividuWrapper(target);
	}

	private HistoriqueIndividuWrapper(ch.vd.registre.civil.model.HistoriqueIndividu target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
	}

	public String getAutresPrenoms() {
		return target.getAutresPrenoms();
	}

	public String getComplementIdentification() {
		return target.getComplementIdentification();
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public String getNoAVS() {
		return target.getNoAVS();
	}

	public int getNoSequence() {
		return target.getNoSequence();
	}

	public String getNom() {
		return target.getNom();
	}

	public String getNomCourrier1() {
		return target.getNomCourrier1();
	}

	public String getNomCourrier2() {
		return target.getNomCourrier2();
	}

	public String getNomNaissance() {
		return target.getNomNaissance();
	}

	public String getPrenom() {
		return target.getPrenom();
	}

	public String getProfession() {
		return target.getProfession();
	}
}
