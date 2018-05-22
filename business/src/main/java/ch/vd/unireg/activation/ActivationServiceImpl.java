package ch.vd.unireg.activation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.AnnuleEtRemplace;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalAutreElementImposable;
import ch.vd.unireg.tiers.ForFiscalAutreImpot;
import ch.vd.unireg.tiers.ForFiscalAvecMotifs;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeRapportEntreTiers;

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
		if (tiers instanceof Entreprise) {
			_desactiveEntreprise((Entreprise) tiers, dateAnnulation);
		}
		else if (tiers instanceof Etablissement) {
			_desactiveEtablissement((Etablissement) tiers, dateAnnulation, false);
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			_desactiveDebiteurPrestationImposable((DebiteurPrestationImposable) tiers, dateAnnulation);
		}
		else if (tiers instanceof Contribuable) {
			_desactiveContribuable((Contribuable) tiers, dateAnnulation);
		}
		else {
			_desactiveTiers(tiers, dateAnnulation);
		}
	}

	private void _desactiveTiers(@NotNull Tiers tiers, RegDate dateAnnulation) throws ActivationServiceException {
		// [UNIREG-2340] On ne doit pas pouvoir désactiver un tiers s'il possède des déclarations non-annulées qui couvrent des périodes postérieures à la désactivation
		checkDeclarationsEnConflitAvecDesactivation(tiers, dateAnnulation);

		// [UNIREG-2381] L'existence de certains fors doit également bloquer la désactivation du tiers
		checkForsEnConflitAvecDesactivation(tiers, dateAnnulation);

		// s'il n'y a pas de for actif à la date de désactivation, il faut carrément annuler le tiers complètement
		final List<ForFiscal> forsFiscauxValides = tiers.getForsFiscauxValidAt(dateAnnulation);
		if (forsFiscauxValides.isEmpty()) {
			tiers.setAnnule(true);
		}
	}

	private void _desactiveContribuable(@NotNull Contribuable ctb, RegDate dateAnnulation) throws ActivationServiceException {
		tiersService.closeAllForsFiscaux(ctb, dateAnnulation, MotifFor.ANNULATION);

		// s'il existe un for fiscal principal fermé justement à la date d'annulation pour
		// un autre motif, alors on change son motif de fermeture !
		final ForFiscalPrincipal ffp = ctb.getForFiscalPrincipalAt(dateAnnulation);
		if (ffp != null && ffp.getMotifFermeture() != MotifFor.ANNULATION && ffp.getDateFin() == dateAnnulation) {
			ffp.setMotifFermeture(MotifFor.ANNULATION);
		}

		_desactiveTiers(ctb, dateAnnulation);
	}

	private void _desactiveDebiteurPrestationImposable(@NotNull DebiteurPrestationImposable dpi, RegDate dateAnnulation) throws ActivationServiceException {
		final ForDebiteurPrestationImposable forDebiteurPrestationImposable = dpi.getForDebiteurPrestationImposableAt(dateAnnulation);
		tiersService.closeForDebiteurPrestationImposable(dpi, forDebiteurPrestationImposable, dateAnnulation, MotifFor.ANNULATION, true);

		_desactiveTiers(dpi, dateAnnulation);
	}

	private void _desactiveEntreprise(@NotNull Entreprise entreprise, RegDate dateAnnulation) throws ActivationServiceException {
		final DateRange rangeInterdit = new DateRangeHelper.Range(dateAnnulation.getOneDayAfter(), null);

		// [SIFISC-18536] dans le cas d'une entreprise, s'il existe des liens vers des établissements secondaires ouverts après la date d'annulation,
		// on refuse la désactivation de l'entreprise
		for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
			if (!ret.isAnnule() && ret instanceof ActiviteEconomique && DateRangeHelper.intersect(ret, rangeInterdit)) {
				final ActiviteEconomique ae = (ActiviteEconomique) ret;
				if (ae.isPrincipal()) {
					// on annule l'établissement principal qui est derrière
					final Etablissement etablissement = (Etablissement) tiersService.getTiers(ret.getObjetId());
					if (!etablissement.isAnnule()) {
						_desactiveEtablissement(etablissement, dateAnnulation, true);
					}
				}
				else {
					// Stop !! on arrête tout, il ne devrait plus y avoir de lien secondaire ouvert !!
					throw new ActivationServiceException("L'entreprise possède des liens vers des établissements secondaires encore valides après la date d'annulation demandée.");
				}
			}
		}

		_desactiveContribuable(entreprise, dateAnnulation);
	}

	private void _desactiveEtablissement(Etablissement etablissement, RegDate dateAnnulation, boolean principal) throws ActivationServiceException {
		final DateRange rangeInterdit = new DateRangeHelper.Range(dateAnnulation.getOneDayAfter(), null);

		for (RapportEntreTiers ret : etablissement.getRapportsObjet()) {
			if (!ret.isAnnule() && ret instanceof ActiviteEconomique && DateRangeHelper.intersect(ret, rangeInterdit)) {
				final ActiviteEconomique ae = (ActiviteEconomique) ret;
				if (ae.isPrincipal() != principal) {
					// on a un problème... on nous demande de désactiver l'établissement "principal" ou "secondaire" selon le flag
					// "principal" passé en paramètre, mais il se trouve que l'établissement a également un rôle opposé après la date d'annulation...
					throw new ActivationServiceException(String.format("L'établissement %s possède à la fois les rôles d'établissement principal et secondaire après le %s. Traitement non-supporté.",
					                                                   FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
					                                                   RegDateHelper.dateToDisplayString(dateAnnulation)));
				}

				// pour l'établissement principal, rien de spécial à faire
				// (mais il faut annuler le rapport entre tiers entre un établissement secondaire annulé et son entreprise)
				if (!principal) {
					ae.setAnnule(true);
				}
			}
		}

		_desactiveContribuable(etablissement, dateAnnulation);
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
		if (dateReactivation == null) {
			throw new IllegalArgumentException();
		}

		// [SIFISC-18773] si nous avons affaire à une entreprise, il faut peut-être aller ré-activer l'établissement principal
		if (tiers instanceof Entreprise) {
			// les liens ne sont pas annulés, il faut juste récupérer le dernier lien vers un établissement principal et
			// réactiver l'établissement derrière le lien
			final SortedSet<ActiviteEconomique> rapportsEtbsPrincipaux = new TreeSet<>((o1, o2) -> NullDateBehavior.EARLIEST.compare(o1.getDateDebut(), o2.getDateDebut()));
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
				LOGGER.warn(String.format("Réactivation d'une entreprise (%s) dont tous les liens existants vers un établissements principal ont été annulés... Pas de réactivation de l'établissement principal correspondant.",
				                          FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero())));
			}
			else {
				final ActiviteEconomique aReactiver = rapportsEtbsPrincipaux.last();
				final Etablissement etablissementPrincipal = (Etablissement) tiersService.getTiers(aReactiver.getObjetId());
				try {
					_reactiveEtablissement(etablissementPrincipal, dateReactivation, true);
				}
				catch (TiersNonDesactiveException e) {
					// l'établissement principal n'avait pas été annulé ? ou alors on a déjà commencé le boulot de réactivation à la main...
					// pas grave, on continue
					LOGGER.warn(String.format("L'établissement principal %s de l'entreprise %s en cours de réactivation a semble-t-il déjà été ré-activé",
					                          FormatNumeroHelper.numeroCTBToDisplay(etablissementPrincipal.getNumero()),
					                          FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero())));
				}
			}

			_reactiveTiers(tiers, dateReactivation);
		}

		// [SIFISC-18536] si nous sommes sur un établissement, il faut différencier les cas "principal" et "secondaire"
		// (parce que le cas de l'établissement principal n'est pas sensé passer par ici...)
		else if (tiers instanceof Etablissement) {
			// on ne s'intéresse donc qu'aux établissements secondaires...
			_reactiveEtablissement((Etablissement) tiers, dateReactivation, false);
		}
		else {
			// cas général
			_reactiveTiers(tiers, dateReactivation);
		}
	}

	private void _reactiveTiers(@NotNull Tiers tiers, @NotNull RegDate dateReactivation) throws ActivationServiceException {

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
			forsFiscaux.sort(new DateRangeComparator<ForFiscal>() {
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

	private void _reactiveEtablissement(@NotNull  Etablissement etablissement, @NotNull RegDate dateReactivation, boolean principal) throws ActivationServiceException {

		final Set<Long> idsEntreprisesTousLiens = new HashSet<>();
		final Set<Long> idsEntreprisesLiensNonAnnules = new HashSet<>();
		final List<ActiviteEconomique> liensNonAnnules = new ArrayList<>();
		for (RapportEntreTiers ret : etablissement.getRapportsObjet()) {
			if (ret instanceof ActiviteEconomique) {
				final ActiviteEconomique ae = (ActiviteEconomique) ret;
				if (!ae.isAnnule() && !principal && RegDateHelper.isAfterOrEqual(ae.getDateFin(), dateReactivation, NullDateBehavior.LATEST)) {
					throw new ActivationServiceException(String.format("L'établissement %s possède déjà un rapport d'activité économique non-annulé valide après la date de réactivation demandée...",
					                                                   FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
				}

				if (!ae.isAnnule()) {
					idsEntreprisesLiensNonAnnules.add(ae.getSujetId());
					liensNonAnnules.add(ae);
				}
				idsEntreprisesTousLiens.add(ae.getSujetId());
			}
		}

		// si y a eu plusieurs entreprises connues...
		if ((principal && idsEntreprisesLiensNonAnnules.size() > 1) || (!principal && idsEntreprisesTousLiens.size() > 1)) {
			throw new ActivationServiceException(String.format("L'établissement %s a des liens vers plusieurs entreprises distinctes, impossible de réactiver...",
			                                                   FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
		}
		else if (idsEntreprisesTousLiens.isEmpty()) {
			throw new ActivationServiceException(String.format("L'établissement %s n'a aucun lien connu vers une entreprise, impossible de réactiver...",
			                                                   FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
		}
		else if (!principal) {
			// il n'y a rien à faire sur les liens principaux, qui existent normalement toujours sans être annulés

			// plage de valeur du lien re-créé
			final DateRange plageNouveauLien = new DateRangeHelper.Range(dateReactivation, null);

			// on valide que le nouveau lien ne serait pas en conflit avec un lien existant...
			liensNonAnnules.sort(new DateRangeComparator<>());
			final List<DateRange> plageLiensNonAnnules = DateRangeHelper.merge(liensNonAnnules);
			if (plageLiensNonAnnules != null && DateRangeHelper.intersect(plageNouveauLien, plageLiensNonAnnules)) {
				throw new ActivationServiceException(String.format("La date de réactivation au %s entre en conflit avec un lien d'activité économique non-annulé existant.",
				                                                   RegDateHelper.dateToDisplayString(dateReactivation)));
			}

			// et finalement on re-crée le lien d'activité économique
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(idsEntreprisesTousLiens.iterator().next());
			final ActiviteEconomique nouveauLien = new ActiviteEconomique(plageNouveauLien.getDateDebut(), plageNouveauLien.getDateFin(), entreprise, etablissement, false);
			tiersService.addRapport(nouveauLien, entreprise, etablissement);
		}

		_reactiveTiers(etablissement, dateReactivation);
	}
}
