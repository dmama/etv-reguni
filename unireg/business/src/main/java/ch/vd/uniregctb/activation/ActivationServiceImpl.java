package ch.vd.uniregctb.activation;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class ActivationServiceImpl implements ActivationService {

	private TiersService tiersService;

	private TacheDAO tacheDAO;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	/**
	 * Annule un tiers
	 * @param tiers
	 * @param dateAnnulation
	 */
	public void annuleTiers(Tiers tiers, RegDate dateAnnulation) {
		tiers.setAnnulationDate(dateAnnulation.asJavaDate());
		if (AuthenticationHelper.getAuthentication() != null) {
			tiers.setAnnulationUser(AuthenticationHelper.getCurrentPrincipal());
		}
		else {
			tiers.setAnnulationUser("INCONNU");
		}
		if (tiers instanceof Contribuable) {
			Contribuable contribuable = (Contribuable) tiers;
			tiersService.closeAllForsFiscaux(contribuable, dateAnnulation, MotifFor.ANNULATION);
			List<Tache> taches = tacheDAO.find(contribuable.getNumero().longValue());
			for (Tache tache : taches) {
				tache.setAnnulationDate(dateAnnulation.asJavaDate());
				if (AuthenticationHelper.getAuthentication() != null) {
					tache.setAnnulationUser(AuthenticationHelper.getCurrentPrincipal());
				}
				else {
					tache.setAnnulationUser("INCONNU");
				}
			}
		}
		if (tiers instanceof DebiteurPrestationImposable) {
			DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			ForDebiteurPrestationImposable forDebiteurPrestationImposable =debiteur.getForDebiteurPrestationImposableAt(dateAnnulation);
			tiersService.closeForDebiteurPrestationImposable(debiteur, forDebiteurPrestationImposable, dateAnnulation);
		}
	}

	/**
	 * Annule un tiers
	 * @param tiersRemplace
	 * @param tiersRemplacant
	 * @param dateRemplacement
	 */
	public void remplaceTiers(Tiers tiersRemplace, Tiers tiersRemplacant, RegDate dateRemplacement) {
		annuleTiers(tiersRemplace, dateRemplacement);

		AnnuleEtRemplace annuleEtRemplace = new AnnuleEtRemplace(dateRemplacement, null, tiersRemplace, tiersRemplacant);
		tiersService.addRapport(annuleEtRemplace, tiersRemplace, tiersRemplacant);
	}


	/**
	 * Réactive un tiers annulé
	 * @param tiers
	 * @param dateReactivation
	 */
	public void reactiveTiers(Tiers tiers, RegDate dateReactivation) {
		tiers.setAnnulationDate(null);
		tiers.setAnnulationUser(null);
		Set<ForFiscal> forsFiscaux = tiers.getForsFiscaux();
		for (ForFiscal forFiscal : forsFiscaux) {
			if (forFiscal.getDateFin() != null) {
				if (tiers instanceof DebiteurPrestationImposable) {
					DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
					tiersService.openForDebiteurPrestationImposable(debiteur, dateReactivation, forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getTypeAutoriteFiscale());
				}
				if (tiers instanceof Contribuable) {
					Contribuable contribuable = (Contribuable) tiers;
					if (forFiscal instanceof ForFiscalAutreElementImposable) {
						ForFiscalAutreElementImposable forFiscalAutreElementImposable = (ForFiscalAutreElementImposable) forFiscal;
						tiersService.openForFiscalAutreElementImposable(contribuable, forFiscalAutreElementImposable.getGenreImpot(), dateReactivation, forFiscalAutreElementImposable.getMotifRattachement(), forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale(), forFiscalAutreElementImposable.getTypeAutoriteFiscale(), MotifFor.REACTIVATION);
					}
					if (forFiscal instanceof ForFiscalAutreImpot) {
						tiersService.openForFiscalAutreImpot(contribuable, forFiscal.getGenreImpot(), dateReactivation, forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getTypeAutoriteFiscale());
					}
					if (forFiscal instanceof ForFiscalPrincipal) {
						ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) forFiscal;
						tiersService.openForFiscalPrincipal(contribuable, dateReactivation, forFiscalPrincipal.getMotifRattachement(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getTypeAutoriteFiscale(), forFiscalPrincipal.getModeImposition(), MotifFor.REACTIVATION, true);
					}
					if (forFiscal instanceof ForFiscalSecondaire) {
						ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
						tiersService.openForFiscalSecondaire(contribuable, forFiscalSecondaire.getGenreImpot(), dateReactivation, null, forFiscalSecondaire.getMotifRattachement(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale(), forFiscalSecondaire.getTypeAutoriteFiscale(), MotifFor.REACTIVATION, null);
					}
				}
			}
		}
		AnnuleEtRemplace annuleEtRemplace = (AnnuleEtRemplace) tiers.getRapportObjetValidAt(dateReactivation, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
		if (annuleEtRemplace != null) {
			annuleEtRemplace.setDateFin(dateReactivation.addDays(-1));
		}
	}

}
