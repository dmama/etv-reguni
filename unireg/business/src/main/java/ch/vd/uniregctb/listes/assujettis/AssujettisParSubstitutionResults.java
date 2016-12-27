package ch.vd.uniregctb.listes.assujettis;

import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.AssujettissementParSubstitution;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe de résultats du batch d'extraction des assujettis d'une période fiscale
 */
public class AssujettisParSubstitutionResults extends AbstractJobResults<Long,AssujettisParSubstitutionResults> {


	private final List<InfoRapportSubstitution> rapportsSubstitutions = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();
	public int nbctbSubstitutions = 0;
	private RegDate dateTraitement;
	public boolean interrupted;
	public int nbThreads;

	private final AssujettissementService assujettissementService;
	private final TiersService tiersService;

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public List<InfoRapportSubstitution> getRapportsSubstitutions() {
		return rapportsSubstitutions;
	}

	public void setNbThreads(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}



	public static class Erreur{
		public long idRapport;
		public String description;
		public Erreur(long idRapport,String description) {
			this.idRapport=idRapport;
			this.description = description;
		}

	}


	public AssujettisParSubstitutionResults(RegDate dateTraitement, int nbThreads, TiersService tiersService, AssujettissementService assujettissementService) {
		this.assujettissementService= assujettissementService;
		this.tiersService = tiersService;
		this.dateTraitement = dateTraitement;
		this.nbThreads = nbThreads;
	}


	public void addInfosRapports(AssujettissementParSubstitution rapport) {

		PersonnePhysique pSubstituant = (PersonnePhysique) tiersService.getTiers(rapport.getObjetId());
		PersonnePhysique pSubstitue = (PersonnePhysique) tiersService.getTiers(rapport.getSujetId());

		try{

			final InfoCtb substitutant = createInfoCtb(pSubstituant);
			final InfoCtb substitue = createInfoCtb(pSubstitue);
			InfoRapportSubstitution info = new InfoRapportSubstitution(substitutant, substitue, rapport.getDateDebut(), rapport.getDateFin());
			nbctbSubstitutions++;
			rapportsSubstitutions.add(info);
		}
		catch (Exception e){
			addErrorException(rapport.getId(),e);
		}

	}

	private InfoCtb createInfoCtb(PersonnePhysique pp) throws AssujettissementException {
		final boolean assujettiOrdinaire = isSousAssujettissementOrdinaire(pp);
		final String nomPrenom = tiersService.getNomPrenom(pp);
		final RegDate dateNaissance = tiersService.getDateNaissance(pp);
		final String numeroAssureSocial = tiersService.getNumeroAssureSocial(pp);
		return new InfoCtb(pp.getNumero(), nomPrenom,dateNaissance,numeroAssureSocial, assujettiOrdinaire);
	}


	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new Erreur(element,e.getMessage()));
	}

	@Override
	public void addAll(AssujettisParSubstitutionResults right) {
		rapportsSubstitutions.addAll(right.rapportsSubstitutions);
		erreurs.addAll(right.erreurs);
		nbctbSubstitutions = nbctbSubstitutions +right.nbctbSubstitutions;
	}

	public static class InfoCtb {
		public final long noCtb;
		public final String nomPrenom;
		public final RegDate dateNaissance;
		public final String navs13;
		public final boolean assujetti;

		public InfoCtb(long noCtb, String nomPrenom, RegDate dateNaissance, String navs13, boolean assujetti) {
			this.noCtb = noCtb;
			this.nomPrenom = nomPrenom;
			this.dateNaissance = dateNaissance;
			this.navs13 = navs13;
			this.assujetti = assujetti;
		}

	}

	public static class InfoRapportSubstitution {
		public final AssujettisParSubstitutionResults.InfoCtb substituant;
		public final AssujettisParSubstitutionResults.InfoCtb substitue;
		public final RegDate dateOuvertureRapport;
		public final RegDate dateFermetureRapport;

		public InfoRapportSubstitution(AssujettisParSubstitutionResults.InfoCtb substituant, AssujettisParSubstitutionResults.InfoCtb substitue,
		                               RegDate dateOuvertureRapport, RegDate dateFermetureRapport) {
			this.substituant = substituant;
			this.substitue = substitue;
			this.dateOuvertureRapport = dateOuvertureRapport;
			this.dateFermetureRapport = dateFermetureRapport;
		}

	}

	public boolean isAssujettiOrdinaire(Contribuable ctb) throws AssujettissementException {
		boolean isAssujetti = false;

		final int anneeFiscale = dateTraitement.year();
		final List<Assujettissement> assujettissements = assujettissementService.determine(ctb, anneeFiscale);
		if (assujettissements == null || assujettissements.isEmpty()) {
			return false;
		}
		else {

			//  on ne doit pas tenir compte des assujettissement source pure, on les enlève maintenant
			assujettissements.removeIf(assujettissement -> assujettissement instanceof SourcierPur);

			// s'il ne reste plus rien, c'est que le contribuable était toujours sourcier pur...
			if (assujettissements.isEmpty()) {
				return false;
			}

			//le contribuable a un assujetissement autre que sourcier pur
			return true;

		}
	}

	private boolean isSousAssujettissementOrdinaire(PersonnePhysique pp) throws AssujettissementException {
		boolean isAssujetti = isAssujettiOrdinaire(pp);
		if (!isAssujetti){
			EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(pp,dateTraitement);
			if (ensembleTiersCouple != null) {
				isAssujetti = isAssujettiOrdinaire(ensembleTiersCouple.getMenage());
			}
		}
		return isAssujetti;

	}
}
