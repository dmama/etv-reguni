package ch.vd.uniregctb.activation;

import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.Declaration;
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
import ch.vd.uniregctb.type.TypeEtatTache;
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
	public void annuleTiers(Tiers tiers, RegDate dateAnnulation) throws ActivationServiceException {

		// [UNIREG-2340] On ne doit pas pouvoir annuler un tiers s'il possède des déclarations non-annulées qui couvrent des périodes postérieures à l'annulation
		final RegDate seuilPourDeclarationBloquante = dateAnnulation.getOneDayAfter();
		final List<Declaration> declarations = tiers.getDeclarationsSorted();
		if (declarations != null && declarations.size() > 0) {

			// puisqu'elles sont triées, il suffit de trouver la dernière déclaration non annulée et de la tester
			Declaration nonAnnulee = null;
			final ListIterator<Declaration> iterator = declarations.listIterator(declarations.size());
			while (iterator.hasPrevious()) {
				final Declaration candidate = iterator.previous();
				if (!candidate.isAnnule()) {
					nonAnnulee = candidate;
					break;
				}
			}

			// si toutes sont annulées, pas de souci...
			if (nonAnnulee != null) {
				final RegDate dateFin = nonAnnulee.getDateFin();
				if (seuilPourDeclarationBloquante.isBeforeOrEqual(dateFin)) {
					// problème...
					throw new ActivationServiceException("Il est interdit d'annuler un tiers pour lequel il existe encore des déclarations couvrant une période postérieure à la date d'annulation souhaitée.");
				}
			}
		}

		final Date dateOperation = dateAnnulation.asJavaDate();
		tiers.annulerPourDate(dateOperation);

		if (tiers instanceof Contribuable) {
			final Contribuable contribuable = (Contribuable) tiers;
			tiersService.closeAllForsFiscaux(contribuable, dateAnnulation, MotifFor.ANNULATION);
			final List<Tache> taches = tacheDAO.find(contribuable.getNumero());
			for (Tache tache : taches) {
				if (tache.getEtat() != TypeEtatTache.TRAITE && tache.getAnnulationDate() == null) {
					tache.annulerPourDate(dateOperation);
				}
			}
		}

		if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			final ForDebiteurPrestationImposable forDebiteurPrestationImposable =debiteur.getForDebiteurPrestationImposableAt(dateAnnulation);
			tiersService.closeForDebiteurPrestationImposable(debiteur, forDebiteurPrestationImposable, dateAnnulation);
		}
	}

	/**
	 * Annule un tiers
	 * @param tiersRemplace
	 * @param tiersRemplacant
	 * @param dateRemplacement
	 */
	public void remplaceTiers(Tiers tiersRemplace, Tiers tiersRemplacant, RegDate dateRemplacement) throws ActivationServiceException {
		annuleTiers(tiersRemplace, dateRemplacement);

		final AnnuleEtRemplace annuleEtRemplace = new AnnuleEtRemplace(dateRemplacement, null, tiersRemplace, tiersRemplacant);
		tiersService.addRapport(annuleEtRemplace, tiersRemplace, tiersRemplacant);
	}


	/**
	 * Réactive un tiers annulé
	 * @param tiers
	 * @param dateReactivation
	 */
	public void reactiveTiers(Tiers tiers, RegDate dateReactivation) {
		tiers.setAnnule(false);

		final Set<ForFiscal> forsFiscaux = tiers.getForsFiscaux();
		for (ForFiscal forFiscal : forsFiscaux) {
			if (forFiscal.getDateFin() != null) {
				if (tiers instanceof DebiteurPrestationImposable) {
					final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
					tiersService.openForDebiteurPrestationImposable(debiteur, dateReactivation, forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getTypeAutoriteFiscale());
				}
				if (tiers instanceof Contribuable) {
					final Contribuable contribuable = (Contribuable) tiers;
					if (forFiscal instanceof ForFiscalAutreElementImposable) {
						final ForFiscalAutreElementImposable forFiscalAutreElementImposable = (ForFiscalAutreElementImposable) forFiscal;
						tiersService.openForFiscalAutreElementImposable(contribuable, forFiscalAutreElementImposable.getGenreImpot(), dateReactivation, forFiscalAutreElementImposable.getMotifRattachement(), forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale(), forFiscalAutreElementImposable.getTypeAutoriteFiscale(), MotifFor.REACTIVATION);
					}
					if (forFiscal instanceof ForFiscalAutreImpot) {
						tiersService.openForFiscalAutreImpot(contribuable, forFiscal.getGenreImpot(), dateReactivation, forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getTypeAutoriteFiscale());
					}
					if (forFiscal instanceof ForFiscalPrincipal) {
						final ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) forFiscal;
						tiersService.openForFiscalPrincipal(contribuable, dateReactivation, forFiscalPrincipal.getMotifRattachement(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getTypeAutoriteFiscale(), forFiscalPrincipal.getModeImposition(), MotifFor.REACTIVATION, true);
					}
					if (forFiscal instanceof ForFiscalSecondaire) {
						final ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
						tiersService.openForFiscalSecondaire(contribuable, forFiscalSecondaire.getGenreImpot(), dateReactivation, null, forFiscalSecondaire.getMotifRattachement(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale(), forFiscalSecondaire.getTypeAutoriteFiscale(), MotifFor.REACTIVATION, null);
					}
				}
			}
		}

		final AnnuleEtRemplace annuleEtRemplace = (AnnuleEtRemplace) tiers.getRapportObjetValidAt(dateReactivation, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
		if (annuleEtRemplace != null) {
			annuleEtRemplace.setDateFin(dateReactivation.getOneDayBefore());
		}
	}

}
