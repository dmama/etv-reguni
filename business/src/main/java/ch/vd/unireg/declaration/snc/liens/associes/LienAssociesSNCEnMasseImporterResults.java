package ch.vd.unireg.declaration.snc.liens.associes;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AbstractJobResults;
import ch.vd.unireg.tiers.Contribuable;

public class LienAssociesSNCEnMasseImporterResults extends AbstractJobResults<DonneesLienAssocieEtSNC, LienAssociesSNCEnMasseImporterResults> {


	private final RegDate dateTraitement;

	private final List<Erreur> erreurs = new LinkedList<>();
	private final List<String> lignesIgnorees = new LinkedList<>();
	private final List<DonneesLienAssocieEtSNC> liensCrees = new LinkedList<>();
	private boolean interrupted;


	public LienAssociesSNCEnMasseImporterResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}


	@Override
	public void addErrorException(DonneesLienAssocieEtSNC element, Exception e) {
		erreurs.add(new Erreur(element, ExceptionUtils.getStackTrace(e)));
	}

	@Override
	public void addAll(LienAssociesSNCEnMasseImporterResults right) {
		this.liensCrees.addAll(right.liensCrees);
		this.lignesIgnorees.addAll(right.lignesIgnorees);
		this.erreurs.addAll(right.erreurs);
	}

	public void addContribuableInconnu(DonneesLienAssocieEtSNC data, String message) {
		erreurs.add(new LienAssociesSNCEnMasseImporterResults.Erreur(data, message));
	}

	public void addContribuableNonAcceptable(Contribuable ctb, DonneesLienAssocieEtSNC data, String message) {
		erreurs.add(new Erreur(data, message));
	}

	public void addLienCree(DonneesLienAssocieEtSNC data) {
		liensCrees.add(data);
	}

	public void addLigneInvalide(String csvRecord) {
		lignesIgnorees.add(csvRecord);
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public List<String> getLignesIgnorees() {
		return lignesIgnorees;
	}

	public List<DonneesLienAssocieEtSNC> getLiensCrees() {
		return liensCrees;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}


	public static class Erreur {
		public final DonneesLienAssocieEtSNC data;
		public final String erreur;

		public Erreur(DonneesLienAssocieEtSNC data, String erreur) {
			this.data = data;
			this.erreur = erreur;
		}
	}
}
