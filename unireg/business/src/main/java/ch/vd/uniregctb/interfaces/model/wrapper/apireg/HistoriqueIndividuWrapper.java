package ch.vd.uniregctb.interfaces.model.wrapper.apireg;

import ch.vd.apireg.datamodel.CaractIndividu;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;

public class HistoriqueIndividuWrapper implements HistoriqueIndividu {

	private final RegDate dateDebut;
	private final String autresPrenoms;
	private final String complementIdentif;
	private final String noAvs;
	private final int noSequence;
	private final String nom;
	private final String nomNaissance;
	private final String prenom;
	private final String profession;

	public static HistoriqueIndividuWrapper get(CaractIndividu target) {
		if (target == null) {
			return null;
		}
		return new HistoriqueIndividuWrapper(target);
	}

	private HistoriqueIndividuWrapper(CaractIndividu target) {
		this.dateDebut = RegDate.get(target.getDaValidite());
		this.autresPrenoms = target.getAutresPrenoms();
		this.complementIdentif = target.getComplementIdentif();
		this.noAvs = extractNoAvs(target);
		this.noSequence = target.getId().getNoSequence();
		this.nom = target.getNom();
		this.nomNaissance = target.getNomNaissance();
		this.prenom = target.getPrenom();
		this.profession = target.getProfession();
	}

	private static String extractNoAvs(CaractIndividu target) {
		Long noAvs = target.getNoAvs();
		if (noAvs == null) {
			return null;
		}
		else {
			return noAvs.toString();
		}
	}

	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	public String getComplementIdentification() {
		return complementIdentif;
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public String getNoAVS() {
		return noAvs;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public String getNom() {
		return nom;
	}

	public String getNomCourrier1() {
		throw new NotImplementedException();
	}

	public String getNomCourrier2() {
		throw new NotImplementedException();
	}

	public String getNomNaissance() {
		return nomNaissance;
	}

	public String getPrenom() {
		return prenom;
	}

	public String getProfession() {
		return profession;
	}
}
