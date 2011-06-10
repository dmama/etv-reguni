package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * Données générées par l'impression en masses des chemises TO
 */
public class ImpressionChemisesTOResults extends JobResults<Long, ImpressionChemisesTOResults> {

	public final static class ChemiseTO extends Info {

		private final RegDate dateDebutDi;
		private final RegDate dateFinDi;
		private final RegDate dateSommationDi;

		public ChemiseTO(long noCtb, Integer officeImpotID, RegDate dateDebutDi, RegDate dateFinDi, RegDate dateSommationDi) {
			super(noCtb, officeImpotID, genererDetails(noCtb, dateDebutDi, dateFinDi, dateSommationDi));
			this.dateDebutDi = dateDebutDi;
			this.dateFinDi = dateFinDi;
			this.dateSommationDi = dateSommationDi;
		}

		@Override
		public String getDescriptionRaison() {
			return String.format("Ctb %s, DI [%s ; %s] sommée le %s", noCtb, dateDebutDi, dateFinDi, dateSommationDi);
		}

		private static String genererDetails(long noCtb, RegDate dateDebutDi, RegDate dateFinDi, RegDate dateSommationDi) {
			return String.format("Chemise de taxation d'office imprimée pour la DI du contribuable %d sur la période [%s ; %s] sommée le %s.",
								noCtb, dateDebutDi, dateFinDi, dateSommationDi);
		}

		public RegDate getDateDebutDi() {
			return dateDebutDi;
		}

		public RegDate getDateFinDi() {
			return dateFinDi;
		}

		public RegDate getDateSommationDi() {
			return dateSommationDi;
		}
	}

	public static class Erreur {

		private final long idDeclaration;
		private final String details;

		public Erreur(long idDeclaration, String details) {
			this.idDeclaration = idDeclaration;
			this.details = details;
		}

		public long getIdDeclaration() {
			return idDeclaration;
		}

		public String getDetails() {
			return details;
		}
	}

	private final List<ChemiseTO> chemisesImprimees = new LinkedList<ChemiseTO>();
	private final List<Erreur> erreurs = new LinkedList<Erreur>();
	private final RegDate dateTraitement = RegDate.get();
	private final int nbMax;
	private final String nomOID;
	private boolean interrompu = false;

	public ImpressionChemisesTOResults(int nbMax, String nomOID) {
		this.nbMax = nbMax;
		this.nomOID = nomOID;
	}

	@Override
	public void addAll(ImpressionChemisesTOResults autre) {
		chemisesImprimees.addAll(autre.chemisesImprimees);
		erreurs.addAll(autre.erreurs);
	}

	public int getNbChemisesImprimees() {
		return chemisesImprimees.size();
	}

	public void addChemiseTO(DeclarationImpotOrdinaire di) {
		final Tiers tiers = di.getTiers();
		final EtatDeclaration etatSomme = di.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE);
		chemisesImprimees.add(new ChemiseTO(tiers.getNumero(), tiers.getOfficeImpotId(), di.getDateDebut(), di.getDateFin(), etatSomme != null ? etatSomme.getDateObtention() : null));
	}

	public void addOnCommitException(long id, Exception e) {
		erreurs.add(new Erreur(id, e.getMessage()));
	}

	public List<ChemiseTO> getChemisesImprimees() {
		return Collections.unmodifiableList(chemisesImprimees);
	}

	public List<Erreur> getErreurs() {
		return Collections.unmodifiableList(erreurs);
	}

	public int getNbMax() {
		return nbMax;
	}

	public String getNomOid() {
		return nomOID;
	}

	public void setInterrompu() {
		interrompu = true;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		addOnCommitException(element, e);
	}
}
