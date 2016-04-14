package ch.vd.uniregctb.activation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalAvecMotifs;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class ActivationServiceImpl implements ActivationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActivationServiceImpl.class);

	private TiersService tiersService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * Désactive un tiers à partir de la date donnée (= dernier jour d'activité)
	 * @param tiers le tiers à désactiver
	 * @param dateAnnulation dernier jour d'activité souhaitée pour le tiers
	 */
	@Override
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
			if (ffp != null && ffp.getMotifFermeture() != MotifFor.ANNULATION && ffp.getDateFin() == dateAnnulation) {
				ffp.setMotifFermeture(MotifFor.ANNULATION);
			}

			// [SIFISC-18773] si nous traitons une entreprise, il faut également désactiver (= annuler, puisqu'ils ne portent jamais de fors)
			// les établissements de tous types
			if (tiers instanceof Entreprise) {
				for (RapportEntreTiers rapportSujet : tiers.getRapportsSujet()) {
					if (rapportSujet.isValidAt(dateAnnulation) && rapportSujet.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE) {
						final Etablissement etablissement = (Etablissement) tiersService.getTiers(rapportSujet.getObjetId());
						if (!etablissement.isAnnule()) {
							desactiveTiers(etablissement, dateAnnulation);
						}
					}
				}
			}
		}

		if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			final ForDebiteurPrestationImposable forDebiteurPrestationImposable = debiteur.getForDebiteurPrestationImposableAt(dateAnnulation);
			tiersService.closeForDebiteurPrestationImposable(debiteur, forDebiteurPrestationImposable, dateAnnulation, MotifFor.ANNULATION, true);
		}

		// s'il n'y a pas de for actif à la date de désactivation, il faut carrément annuler le tiers complètement
		final List<ForFiscal> forsFiscauxValides = tiers.getForsFiscauxValidAt(dateAnnulation);
		if (forsFiscauxValides == null || forsFiscauxValides.isEmpty()) {
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
		if (fors != null && !fors.isEmpty()) {
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

		// puisqu'elles sont triées, il suffit de trouver la dernière déclaration non annulée et de la tester
		final Declaration derniereDeclaration = tiers.getDerniereDeclaration(Declaration.class);
		if (derniereDeclaration != null) {
			final RegDate dateFin = derniereDeclaration.getDateFin();
			if (seuilPourDeclarationBloquante.isBeforeOrEqual(dateFin)) {
				// problème...
				throw new ActivationServiceException("Il est interdit d'annuler un tiers pour lequel il existe encore des déclarations couvrant une période postérieure à la date d'annulation souhaitée.");
			}
		}
	}

	/**
	 * Annule un tiers
	 * @param tiersRemplace
	 * @param tiersRemplacant
	 * @param dateRemplacement
	 */
	@Override
	public void remplaceTiers(Tiers tiersRemplace, Tiers tiersRemplacant, RegDate dateRemplacement) throws ActivationServiceException {

		final RegDate dateDebutActiviteRemplacant = dateRemplacement.getOneDayAfter();

		// [SIFISC-18773] si nous avons une entreprise, la présence d'au moins un établissement secondaire ouvert au lendemain de la date de remplacement
		// bloque le tout... sinon, on essaie de lier les établissements principaux entre eux...
		if (tiersRemplace instanceof Entreprise) {
			Etablissement principalRemplace = null;
			for (RapportEntreTiers rapportSujet : tiersRemplace.getRapportsSujet()) {
				if (!rapportSujet.isAnnule() && rapportSujet.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE) {
					final ActiviteEconomique ae = (ActiviteEconomique) rapportSujet;
					if (ae.isPrincipal()) {
						if (ae.isValidAt(dateDebutActiviteRemplacant)) {
							final Etablissement etablissement = (Etablissement) tiersService.getTiers(rapportSujet.getObjetId());
							if (!etablissement.isAnnule()) {
								principalRemplace = etablissement;
							}
						}
						else if (ae.getDateDebut().isAfter(dateRemplacement)) {
							throw new ActivationServiceException("Un etablissement principal existe avec une date de début de validité postérieure à la date d'annulation souhaitée.");
						}
					}
					else if (ae.isValidAt(dateDebutActiviteRemplacant)) {
						// un établissement secondaire ? booooh !
						throw new ActivationServiceException("Avant de pouvoir effectuer cette opération, il convient de traiter le cas des établissements secondaires de l'entreprise à désactiver.");
					}
				}
			}

			// si on a un établissement principal actif, il faut lui trouver son pendant côté "remplaçant"
			if (principalRemplace != null) {
				Etablissement principalRemplacant = null;
				for (RapportEntreTiers rapportSujet : tiersRemplacant.getRapportsSujet()) {
					if (rapportSujet.isValidAt(dateDebutActiviteRemplacant) && rapportSujet.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE) {
						final ActiviteEconomique ae = (ActiviteEconomique) rapportSujet;
						if (ae.isPrincipal()) {
							principalRemplacant = (Etablissement) tiersService.getTiers(rapportSujet.getObjetId());
							break;
						}
					}
				}

				// si on a trouvé un établissement principal remplaçant, on va commencer par lier les deux
				if (principalRemplacant != null) {
					remplaceTiers(principalRemplace, principalRemplacant, dateRemplacement);
				}
			}
		}

		desactiveTiers(tiersRemplace, dateRemplacement);

		final AnnuleEtRemplace annuleEtRemplace = new AnnuleEtRemplace(dateDebutActiviteRemplacant, null, tiersRemplace, tiersRemplacant);
		tiersService.addRapport(annuleEtRemplace, tiersRemplace, tiersRemplacant);
	}

	/**
	 * Exception spécifique au cas d'un tiers qui n'est pas désactivé, et pour lequel on demande pourtant une désactivation
	 */
	private static class TiersNonDesactiveException extends ActivationServiceException {
		public TiersNonDesactiveException() {
			super("Le tiers n'est pas désactivé");
		}
	}

	/**
	 * Réactive un tiers annulé
	 * @param tiers
	 * @param dateReactivation
	 */
	@Override
	public void reactiveTiers(Tiers tiers, RegDate dateReactivation) throws ActivationServiceException {
		Assert.notNull(dateReactivation);

		// [SIFISC-18773] si nous avons affaire à une entreprise, il faut peut-être aller ré-activer l'établissement principal
		if (tiers instanceof Entreprise) {
			// les liens ne sont pas annulés, il faut juste récupérer le dernier lien vers un établissement principal et
			// réactiver l'établissement derrière le lien
			Etablissement principal = null;
			final SortedSet<ActiviteEconomique> rapportsEtbsPrincipaux = new TreeSet<>(new Comparator<ActiviteEconomique>() {
				@Override
				public int compare(ActiviteEconomique o1, ActiviteEconomique o2) {
					return NullDateBehavior.EARLIEST.compare(o1.getDateDebut(), o2.getDateDebut());
				}
			});
			for (RapportEntreTiers rapportSujet : tiers.getRapportsSujet()) {
				if (!rapportSujet.isAnnule() && rapportSujet.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE) {
					final ActiviteEconomique ae = (ActiviteEconomique) rapportSujet;
					if (ae.isPrincipal()) {
						rapportsEtbsPrincipaux.add(ae);
					}
				}
			}

			// aucun établissement principal trouvé (tous les liens seraient-ils annulés ?)... bizarre, mais bon...
			if (rapportsEtbsPrincipaux.isEmpty()) {
				LOGGER.warn(String.format("Réactivation d'une entreprise (%s) dont tous les liens existants vers un établissements principal ont été annulé... Pas de réactivation de l'établissement principal correspondant.",
				                          FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero())));
			}
			else {
				final ActiviteEconomique aReactiver = rapportsEtbsPrincipaux.last();
				final Etablissement etablissementPrincipal = (Etablissement) tiersService.getTiers(aReactiver.getObjetId());
				try {
					reactiveTiers(etablissementPrincipal, dateReactivation);
				}
				catch (TiersNonDesactiveException e) {
					// l'établissement principal n'avait pas été annulé ? ou alors on a déjà commencé le boulot de réactivation à la main...
					// pas grave, on continue
					LOGGER.warn(String.format("L'établissement principal %s de l'entreprise %s en cours de réactivation a semble-t-il déjà été ré-activé",
					                          FormatNumeroHelper.numeroCTBToDisplay(etablissementPrincipal.getNumero()),
					                          FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero())));
				}
			}
		}

		final RegDate derniereDateDesactivation = tiers.getDateDesactivation();
		if (tiers.isAnnule()) {
			// il faut dés-annuler...
			tiers.setAnnule(false);
		}
		else if (derniereDateDesactivation == null) {
			throw new TiersNonDesactiveException();
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
						// TODO ne faut-il pas revoir cette logique depuis que les fors débiteurs ont également des motifs d'ouverture et de fermeture ?
						final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
						tiersService.openForDebiteurPrestationImposable(debiteur, dateReactivation, MotifFor.REACTIVATION, forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getTypeAutoriteFiscale());

						// [UNIREG-2144] il faut également ré-ouvrir les rapports de travail fermés à la date de désactivation
						tiersService.reopenRapportsPrestation(debiteur, derniereDateDesactivation, dateReactivation);
					}
					if (tiers instanceof Contribuable) {
						final Contribuable contribuable = (Contribuable) tiers;
						if (forFiscal instanceof ForFiscalAvecMotifs && ((ForFiscalAvecMotifs) forFiscal).getMotifFermeture() == MotifFor.ANNULATION) {
							if (forFiscal instanceof ForFiscalAutreElementImposable) {
								final ForFiscalAutreElementImposable forFiscalAutreElementImposable = (ForFiscalAutreElementImposable) forFiscal;
								tiersService.openForFiscalAutreElementImposable(contribuable, forFiscalAutreElementImposable.getGenreImpot(), dateReactivation, forFiscalAutreElementImposable.getMotifRattachement(), forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale(),
								                                                MotifFor.REACTIVATION);
							}
							if (forFiscal instanceof ForFiscalPrincipalPP) {
								final ForFiscalPrincipalPP forFiscalPrincipal = (ForFiscalPrincipalPP) forFiscal;
								tiersService.openForFiscalPrincipal((ContribuableImpositionPersonnesPhysiques) contribuable, dateReactivation, forFiscalPrincipal.getMotifRattachement(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getTypeAutoriteFiscale(), forFiscalPrincipal.getModeImposition(), MotifFor.REACTIVATION);
							}
							if (forFiscal instanceof ForFiscalPrincipalPM) {
								final ForFiscalPrincipalPM forFiscalPrincipal = (ForFiscalPrincipalPM) forFiscal;
								tiersService.openForFiscalPrincipal((ContribuableImpositionPersonnesMorales) contribuable, dateReactivation, forFiscalPrincipal.getMotifRattachement(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getTypeAutoriteFiscale(), MotifFor.REACTIVATION, forFiscalPrincipal.getGenreImpot());
							}
							if (forFiscal instanceof ForFiscalSecondaire) {
								final ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
								tiersService.openForFiscalSecondaire(contribuable, dateReactivation, forFiscalSecondaire.getMotifRattachement(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale(), forFiscalSecondaire.getTypeAutoriteFiscale(), MotifFor.REACTIVATION, forFiscalSecondaire.getGenreImpot());
							}
						}
						else if (forFiscal instanceof ForFiscalAutreImpot) {
							tiersService.openForFiscalAutreImpot(contribuable, forFiscal.getGenreImpot(), dateReactivation, forFiscal.getNumeroOfsAutoriteFiscale());
						}
					}
				}
			}
		}

		final AnnuleEtRemplace annuleEtRemplace = (AnnuleEtRemplace) tiers.getRapportSujetValidAt(dateReactivation, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
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
