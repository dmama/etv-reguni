package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.ForFiscal;

public class ComparerForFiscalEtCommuneResults extends JobResults<Long, ComparerForFiscalEtCommuneResults> {
	public boolean isInterrompu() {
		return interrompu;
	}

	public static class InfoForCommune {
		public final long id;


		public InfoForCommune(long id) {
			this.id = id;

		}
	}

	public static class CommunesDifferentes extends InfoForCommune {

		public final long numeroContribuable;
		public final String nomCommuneFor;
		public final RegDate dateDebutFor;
		public final String nomCommuneAdresse;
		public final RegDate dateDebutAdresse;


		public CommunesDifferentes(long id, long numeroContribuable, String nomCommuneFor, RegDate dateDebutFor, String nomCommuneAdresse,
		                           RegDate dateDebutAdresse) {

			super(id);
			this.numeroContribuable = numeroContribuable;
			this.nomCommuneFor = nomCommuneFor;
			this.dateDebutFor = dateDebutFor;
			this.nomCommuneAdresse = nomCommuneAdresse;
			this.dateDebutAdresse = dateDebutAdresse;

		}
	}

	public static class Erreur extends InfoForCommune {
		public final String message;

		public Erreur(long id, String message) {
			super(id);
			this.message = message;
		}
	}

	public final List<CommunesDifferentes> listeCommunesDifferentes = new ArrayList<CommunesDifferentes>();
	public boolean interrompu;
	public final List<Erreur> erreurs = new ArrayList<Erreur>();
	public int nbCtbTotal;
	public final RegDate dateTraitement;

	public ComparerForFiscalEtCommuneResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new Erreur(element, e.getMessage()));
	}

	@Override
	public void addAll(ComparerForFiscalEtCommuneResults right) {
		this.nbCtbTotal += right.nbCtbTotal;
		listeCommunesDifferentes.addAll(right.listeCommunesDifferentes);
		erreurs.addAll(right.erreurs);
	}

	public void addCommunesDifferentes(ForFiscal forFiscal,String communeFor, AdresseGenerique adresse,String communeAdresse) {
		final Long id = forFiscal.getId();
		final Long contribuableId = forFiscal.getTiers().getNumero();
		final RegDate debut = forFiscal.getDateDebut();
		final RegDate debutAdresse = adresse.getDateDebut();
		listeCommunesDifferentes.add(new CommunesDifferentes(id, contribuableId,communeFor,debut,communeAdresse,debutAdresse));


	}
}
