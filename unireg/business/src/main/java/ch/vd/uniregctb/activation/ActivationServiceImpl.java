package ch.vd.uniregctb.activation;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	/**
	 * Désactive un tiers à partir de la date donnée (= dernier jour d'activité)
	 * @param tiers le tiers à désactiver
	 * @param dateAnnulation dernier jour d'activité souhaitée pour le tiers
	 */
	public void desactiveTiers(Tiers tiers, RegDate dateAnnulation) throws ActivationServiceException {

		// [UNIREG-2340] On ne doit pas pouvoir désactiver un tiers s'il possède des déclarations non-annulées qui couvrent des périodes postérieures à la désactivation
		checkDeclarationsEnConflitAvecDesactivation(tiers, dateAnnulation);

		// [UNIREG-2381] L'existence de certains fors doit également bloquer la désactivation du tiers
		checkForsEnConflitAvecDesactivation(tiers, dateAnnulation);

		if (tiers instanceof Contribuable) {
			final Contribuable contribuable = (Contribuable) tiers;
			tiersService.closeAllForsFiscaux(contribuable, dateAnnulation, MotifFor.ANNULATION);

			// s'il existe un for fiscal principal fermé justement à la date d'annulation pour
			// un autre motif, alors on change son motif de fermeture !
			final ForFiscalPrincipal ffp = tiers.getForFiscalPrincipalAt(dateAnnulation);
			if (ffp != null && ffp.getMotifFermeture() != MotifFor.ANNULATION && RegDateHelper.equals(ffp.getDateFin(), dateAnnulation)) {
				ffp.setMotifFermeture(MotifFor.ANNULATION);
			}

			final List<Tache> taches = tacheDAO.find(contribuable.getNumero());
			for (Tache tache : taches) {
				if (tache.getEtat() != TypeEtatTache.TRAITE && tache.getAnnulationDate() == null) {
					tache.setAnnule(true);
				}
			}
		}

		if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			final ForDebiteurPrestationImposable forDebiteurPrestationImposable = debiteur.getForDebiteurPrestationImposableAt(dateAnnulation);
			tiersService.closeForDebiteurPrestationImposable(debiteur, forDebiteurPrestationImposable, dateAnnulation);
		}

		// s'il n'y a pas de for actif à la date de désactivation, il faut carrément annuler le tiers complètement
		final List<ForFiscal> forsFiscauxValides = tiers.getForsFiscauxValidAt(dateAnnulation);
		if (forsFiscauxValides == null || forsFiscauxValides.size() == 0) {
			tiers.setAnnule(true);
		}
	}

	/**
	 * [UNIREG-2381] L'existence de certains fors doit bloquer la désactivation du tiers :
	 * <ul>
	 * <li>Tout for non-annulé dont la date de début est postérieure à la date d'annulation demandée</li>
	 * <li>Tout for non-annulé fermé dont la date de fermeture est postérieure à la date d'annulation demandée</li>
	 * </ul>
	 * @throws ActivationServiceException en cas d'annulation bloquée par la présence de fors non-annulés
	 */
	private static void checkForsEnConflitAvecDesactivation(Tiers tiers, RegDate dateDesactivation) throws ActivationServiceException {
		final RegDate seuilPourForBloquant = dateDesactivation.getOneDayAfter();
		final List<ForFiscal> fors = tiers.getForsFiscauxNonAnnules(false);
		if (fors != null && fors.size() > 0) {
			for (ForFiscal ff : fors) {
				if (RegDateHelper.isAfterOrEqual(ff.getDateDebut(), seuilPourForBloquant, NullDateBehavior.EARLIEST) ||
						(ff.getDateFin() != null && ff.getDateFin().isAfterOrEqual(seuilPourForBloquant))) {

					// problème
					throw new ActivationServiceException("Il est interdit d'annuler un tiers pour lequel il existe des fors dont la date d'ouverture ou de fermeture est postérieure à la date d'annulation souhaitée.");
				}
			}
		}
	}

	/**
	 * [UNIREG-2340] Les déclarations (DI/LR) non-annulées qui couvrent une période postérieure à la date d'annulation doivent empêcher cette annulation
	 * @throws ActivationServiceException en cas d'annulation bloquée par la présence de DI/LR non-annulée(s)
	 */
	private static void checkDeclarationsEnConflitAvecDesactivation(Tiers tiers, RegDate dateAnnulation) throws ActivationServiceException {
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
	}

	/**
	 * Annule un tiers
	 * @param tiersRemplace
	 * @param tiersRemplacant
	 * @param dateRemplacement
	 */
	public void remplaceTiers(Tiers tiersRemplace, Tiers tiersRemplacant, RegDate dateRemplacement) throws ActivationServiceException {
		desactiveTiers(tiersRemplace, dateRemplacement);

		final AnnuleEtRemplace annuleEtRemplace = new AnnuleEtRemplace(dateRemplacement.getOneDayAfter(), null, tiersRemplace, tiersRemplacant);
		tiersService.addRapport(annuleEtRemplace, tiersRemplace, tiersRemplacant);
	}


	/**
	 * Réactive un tiers annulé
	 * @param tiers
	 * @param dateReactivation
	 */
	public void reactiveTiers(Tiers tiers, RegDate dateReactivation) throws ActivationServiceException {
		Assert.notNull(dateReactivation);

		final RegDate derniereDateDesactivation = tiers.getDateDesactivation();
		if (tiers.isAnnule()) {
			// il faut dés-annuler...
			tiers.setAnnule(false);
		}
		else if (derniereDateDesactivation == null) {
			throw new ActivationServiceException("Le tiers n'est pas désactivé");
		}

		// Peut-être y a-t-il des fors à réveiller...
		if (derniereDateDesactivation != null) {

			// on ne demande pas le tri, car on va les trier nous-mêmes ici
			final List<ForFiscal> forsFiscaux = tiers.getForsFiscauxNonAnnules(false);

			// le tri s'effectue maintenant, en faisant passer les fors principaux devant (toujours!)
			Collections.sort(forsFiscaux, new DateRangeComparator<ForFiscal>() {
				@Override
				public int compare(ForFiscal o1, ForFiscal o2) {
					final boolean isPrincipal1 = o1 instanceof ForFiscalPrincipal;
					final boolean isPrincipal2 = o2 instanceof ForFiscalPrincipal;
					if (isPrincipal1 == isPrincipal2) {
						return super.compare(o1, o2);
					}
					else if (isPrincipal1) {
						return -1;
					}
					else {
						return 1;
					}
				}
			});

			for (ForFiscal forFiscal : forsFiscaux) {
				if (derniereDateDesactivation.equals(forFiscal.getDateFin())) {
					if (tiers instanceof DebiteurPrestationImposable) {
						final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
						tiersService.openForDebiteurPrestationImposable(debiteur, dateReactivation, forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getTypeAutoriteFiscale());
					}
					if (tiers instanceof Contribuable) {
						final Contribuable contribuable = (Contribuable) tiers;
						if (forFiscal instanceof ForFiscalRevenuFortune && ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture() == MotifFor.ANNULATION) {
							if (forFiscal instanceof ForFiscalAutreElementImposable) {
								final ForFiscalAutreElementImposable forFiscalAutreElementImposable = (ForFiscalAutreElementImposable) forFiscal;
								tiersService.openForFiscalAutreElementImposable(contribuable, forFiscalAutreElementImposable.getGenreImpot(), dateReactivation, forFiscalAutreElementImposable.getMotifRattachement(), forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale(), forFiscalAutreElementImposable.getTypeAutoriteFiscale(), MotifFor.REACTIVATION);
							}
							if (forFiscal instanceof ForFiscalPrincipal) {
								final ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) forFiscal;
								tiersService.openForFiscalPrincipal(contribuable, dateReactivation, forFiscalPrincipal.getMotifRattachement(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getTypeAutoriteFiscale(), forFiscalPrincipal.getModeImposition(), MotifFor.REACTIVATION, true);
							}
							if (forFiscal instanceof ForFiscalSecondaire) {
								final ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
								tiersService.openForFiscalSecondaire(contribuable, dateReactivation, forFiscalSecondaire.getMotifRattachement(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale(), forFiscalSecondaire.getTypeAutoriteFiscale(), MotifFor.REACTIVATION);
							}
						}
						else if (forFiscal instanceof ForFiscalAutreImpot) {
							tiersService.openForFiscalAutreImpot(contribuable, forFiscal.getGenreImpot(), dateReactivation, forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getTypeAutoriteFiscale());
						}
					}
				}
			}
		}

		final AnnuleEtRemplace annuleEtRemplace = (AnnuleEtRemplace) tiers.getRapportObjetValidAt(dateReactivation, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
		if (annuleEtRemplace != null) {
			final RegDate finRemplacement = dateReactivation.getOneDayBefore();
			if (!RegDateHelper.isAfterOrEqual(finRemplacement, annuleEtRemplace.getDateDebut(), NullDateBehavior.LATEST)) {
				annuleEtRemplace.setAnnule(true);
			}
			else {
				annuleEtRemplace.setDateFin(finRemplacement);
			}
		}
	}
}
