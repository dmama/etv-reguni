package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.tiers.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.modeimposition.DecesModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.DivorceModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.FusionMenagesResolver;
import ch.vd.uniregctb.metier.modeimposition.MariageModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolverException;
import ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolver.Imposition;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.Tiers.ForsParType;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class MetierServiceImpl implements MetierService {

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService serviceInfra;
	private ServiceCivilService serviceCivilService;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private SituationFamilleService situationFamilleService;
	private GlobalTiersSearcher tiersSearcher;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public ServiceInfrastructureService getInfrastructureService() {
		return serviceInfra;
	}

	public void setInfrastructureService(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public ServiceCivilService getServiceCivilService() {
		return serviceCivilService;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	/**
	 * @param adresseService
	 *            the adresseService to set
	 */
	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public SituationFamilleService getSituationFamilleService() {
		return situationFamilleService;
	}

	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	public void setTiersSearcher(GlobalTiersSearcher globalTiersSearcher) {
		this.tiersSearcher = globalTiersSearcher;
	}

	private void checkRapportsMenage(PersonnePhysique pp, RegDate dateMariage, ValidationResults results) {
		for (RapportEntreTiers rapport : pp.getRapportsSujet()) {
			if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && rapport.getDateFin() == null) {
				Long mcId = rapport.getObjetId();
				results.addError("Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()) +
						" appartient déjà au ménage commun n° " + FormatNumeroHelper.numeroCTBToDisplay(mcId));
			}
		}
	}

	private void checkAppartenanceMenage(PersonnePhysique pp, RegDate date, ValidationResults resultat) {
		/*
		 * Vérifie la non-appartenance du tiers à un autre ménage
		 */
		final EnsembleTiersCouple ensemblePrincipal = tiersService.getEnsembleTiersCouple(pp, date);
		if (ensemblePrincipal != null && ensemblePrincipal.getMenage() != null) {
			resultat.addError("Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()) + " appartient déjà au ménage commun n° " + FormatNumeroHelper.numeroCTBToDisplay(ensemblePrincipal.getMenage().getNumero()));
		}
		else {
			checkRapportsMenage(pp, date, resultat);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ValidationResults validateMariage(RegDate dateMariage, PersonnePhysique principal, PersonnePhysique conjoint) {

		ValidationResults results = new ValidationResults();

		checkUnion(dateMariage, principal, conjoint, results);

		// vérification que le dernier for actif ne soit ouvert a une date postérieure à celle du mariage
		if (principal != null) {
			ForFiscalPrincipal ffp = principal.getDernierForFiscalPrincipal();
			if (ffp != null && ffp.getDateDebut().isAfter(dateMariage) && !isArrivee(ffp.getMotifOuverture())) {
				results.addError("Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()) + " a des fors ouverts à une date postérieure à celle du mariage");
			}
		}
		if (conjoint != null) {
			ForFiscalPrincipal ffp = conjoint.getDernierForFiscalPrincipal();
			if (ffp != null && ffp.getDateDebut().isAfter(dateMariage) && !isArrivee(ffp.getMotifOuverture())) {
				results.addError("Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()) + " a des fors ouverts à une date postérieure à celle du mariage");
			}
		}
		return results;
	}

	private void checkUnion(RegDate date, PersonnePhysique principal, PersonnePhysique conjoint, ValidationResults results) {
		/*
		 * Vérifie que les tiers soient pas le même
		 */
		if (principal == conjoint) {
			results.addError("Impossible de marier quelqu'un avec lui-même");
		}

		/*
		 * Vérifie que les tiers n'appartiennent déjà à un ménage commun ouvert
		 */
		checkAppartenanceMenage(principal, date, results);
		if (conjoint != null) {
			checkAppartenanceMenage(conjoint, date, results);
		}
	}

	private boolean isArrivee(MotifFor motif) {
		return (MotifFor.ARRIVEE_HC == motif || MotifFor.ARRIVEE_HS == motif);
	}

	private boolean isDepart(MotifFor motif) {
		return (MotifFor.DEPART_HC == motif || MotifFor.DEPART_HS == motif);
	}

	private MenageCommun doMariageReconciliation(MenageCommun menageCommun, RegDate date, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille, Long numeroEvenement, boolean changeHabitantFlag) {

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, date);

		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();

		RegDate dateEffective = date;

		final ModeImpositionResolver mariageResolver = new MariageModeImpositionResolver(tiersService, numeroEvenement);
		final Imposition imposition;
		try {
			imposition = mariageResolver.resolve(menageCommun, dateEffective, null);
		}
		catch (ModeImpositionResolverException ex) {
			throw new EvenementCivilHandlerException(ex.getMessage(), ex);
		}

		// si le mode d'imposition a pu être déterminé
		if (imposition != null) {

			dateEffective = imposition.getDateDebut();
			ModeImposition modeImposition = imposition.getModeImposition();

			/*
			 * sauvegarde des fors secondaires et autre élément imposable de chaque tiers
			 */
			final List<ForFiscalSecondaire> ffsPrincipal;
			final List<ForFiscalAutreElementImposable> ffaeiPrincipal;
			{
				final ForsParType fors = principal.getForsParType(false);
				ffsPrincipal = fors.secondaires;
				ffaeiPrincipal = fors.autreElementImpot;
			}

			final List<ForFiscalSecondaire> ffsConjoint;
			final List<ForFiscalAutreElementImposable> ffaeiConjoint;
			if (conjoint != null) {
				final ForsParType fors = conjoint.getForsParType(false);
				ffsConjoint = fors.secondaires;
				ffaeiConjoint = fors.autreElementImpot;
			}
			else {
				ffsConjoint = Collections.emptyList();
				ffaeiConjoint = Collections.emptyList();
			}

			/*
			 * le traitement met fin aux fors de chacun des tiers
			 */
			final RegDate veilleMariage = dateEffective.getOneDayBefore();
			final ForFiscalPrincipal forPrincipal = principal.getForFiscalPrincipalAt(veilleMariage);
			ForFiscalPrincipal forConjoint = null;
			if (conjoint != null) {
				forConjoint = conjoint.getForFiscalPrincipalAt(veilleMariage);
			}

			/*
			 * fermeture des fors des tiers
			 */
			tiersService.closeAllForsFiscaux(principal, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			if (conjoint != null) {
				tiersService.closeAllForsFiscaux(conjoint, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			}
			Audit.info(numeroEvenement, "Fermeture des fors fiscaux des membres du ménage");

			/* On récupère la commune de résidence du contribuable principal, ou à défaut celle de son conjoint. */
			Integer noOfsCommune = null;
			TypeAutoriteFiscale typeAutoriteCommune = null;
			MotifFor motifOuverture = MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;

			// Plusieurs cas se présentent, dans le cas ou l'événement de mariage nous parvient après les événements d'arrivée,
			// alors que le mariage a en fait eu lieu avant l'arrivée d'au moins l'un des deux)
			// 1. le mariage a eu lieu avant l'arrivée des deux protagonistes : il faut alors annuler les fors des deux
			//		membres du couple, créer un nouveau contribuable ménage et lui ouvrir un for à la date d'arrivée
			//		du premier arrivé des deux membres du couple
			// 2. le mariage a eu lieu entre l'arrivée de l'un et l'arrivée de l'autre : il faut alors fermer le for de la personne
			//		déjà là à la veille du mariage (déjà fait plus haut - closeAllForsFiscaux), annuler le for de la personne
			//		effectivement arrivée après le mariage, créer un contribuable couple et lui ouvrir un for à la date du mariage
			if (forPrincipal == null || (conjoint != null && forConjoint == null)) {

				final DonneesOuvertureFor donneesPrincipal = traiterCasEvenementMariageRecuApresEvenementArriveeMaisMariageAnterieur(principal, dateEffective);
				final DonneesOuvertureFor donneesConjoint = conjoint != null ? traiterCasEvenementMariageRecuApresEvenementArriveeMaisMariageAnterieur(conjoint, dateEffective) : null;

				// le mariage a été prononcé avant l'arrivée d'au moins un des conjoints dans le canton,
				// mais on ne l'a su qu'après
				if (donneesPrincipal != null || donneesConjoint != null) {

					// on prend la date d'arrivée du premier (après mariage) des deux, et les données qui vont avec
					final DonneesOuvertureFor source;
					if (donneesPrincipal != null && (donneesConjoint == null || donneesPrincipal.getDateDebut().isBeforeOrEqual(donneesConjoint.getDateDebut()))) {
						source = donneesPrincipal;
					}
					else {
						source = donneesConjoint;
					}

					final boolean conjointInconnuAuFiscal = conjoint == null || conjoint.getForsFiscauxNonAnnules(false).size() == 0;
					final boolean tousArrivesMariesOuSeulUnDeDeuxConnuAuFiscal = (donneesPrincipal != null && (conjointInconnuAuFiscal || donneesConjoint != null))
							|| (donneesPrincipal == null && donneesConjoint != null && principal.getForsFiscauxNonAnnules(false).isEmpty());

					final boolean unDesConjointEstPartiAvantMariage = (getDernierForFermePourDepart(principal) != null || (conjoint != null && getDernierForFermePourDepart(conjoint) != null));

					if (tousArrivesMariesOuSeulUnDeDeuxConnuAuFiscal || unDesConjointEstPartiAvantMariage) {

						// le motif d'ouverture doit être le mariage dès qu'au moins un des futurs
						// conjoints était déjà présent dans le canton à la date du mariage ;
						// dans le cas où ils étaient tous deux HC/HS à la date du mariage, le
						// motif d'ouverture du for du ménage commun doit être l'arrivée
						motifOuverture = source.getMotifOuverture();

						// la date effective d'ouverture du for du couple doit être la date du mariage
						// dès qu'un au moins des futurs conjoints était déjà présent dans le canton
						// à la date du mariage, et ne prend la valeur de la date d'arrivée du premier
						// des deux conjoints que s'ils étaient déjà mariés en arrivant (mais on ne le
						// savait pas)
						dateEffective = source.getDateDebut();

						// la commune du for du ménage commun, maintenant...
						// si les deux conjoints sont arrivés en fait mariés (mais on ne le savait pas), c'est
						// l'arrivée du premier qui détermine le for du couple ; mais si l'un était déjà là
						// avant le mariage, la commune du for sera la sienne (donc on laisse la variable
						// à null ici, elle sera renseignée plus bas)
						noOfsCommune = source.getNumeroOfsAutoriteFiscale();
						typeAutoriteCommune = source.getTypeAutoriteFiscale();
					}
				}
				else {

					// pas de mariage prononcé-avant-mais-reçu-après une arrivée, et aucun for ouvert à la veille du mariage
					// mais si on a pu trouver un mode d'imposition, c'est qu'il y a un for actif maintenant (on suppose ici
					// que la date du mariage est dans le passé), donc celui-ci a été ouvert après la date du mariage
					if (forPrincipal == null && (conjoint != null && forConjoint == null)) {
						final Long noCtb;
						if (forPrincipal == null) {
							noCtb = principal.getNumero();
						}
						else {
							noCtb = conjoint.getNumero();
						}

						final String msg = String.format("Le contribuable %s possède déjà un for ouvert après la date du mariage", FormatNumeroHelper.numeroCTBToDisplay(noCtb));
						throw new EvenementCivilHandlerException(msg);
					}
					else {
						final ForFiscalPrincipal ffpPrincipal = principal.getForFiscalPrincipalAt(dateEffective);
						if (ffpPrincipal != null && dateEffective.equals(ffpPrincipal.getDateDebut())) {
							if (MotifFor.MAJORITE == ffpPrincipal.getMotifOuverture()) {
								// mariage d'une personne physique avec un for le même jour du mariage (motif majorité)

								// annulation du for puisqu'il va être "remplacé" par celui du couple
								ffpPrincipal.setAnnule(true);
								noOfsCommune = ffpPrincipal.getNumeroOfsAutoriteFiscale();
								typeAutoriteCommune = ffpPrincipal.getTypeAutoriteFiscale();
							}
							else {
								final String msg = String.format("Le contribuable %s possède déjà un for qui s'ouvre à la date du mariage", FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()));
								throw new EvenementCivilHandlerException(msg);
							}
						}

						if (conjoint != null) {
							final ForFiscalPrincipal ffpConjoint = conjoint.getForFiscalPrincipalAt(dateEffective);
							if (ffpConjoint != null && dateEffective.equals(ffpConjoint.getDateDebut())) {
								if (MotifFor.MAJORITE == ffpConjoint.getMotifOuverture()) {
									// mariage d'une personne physique avec un for le même jour du mariage (motif majorité)

									// annulation du for puisqu'il va être "remplacé" par celui du couple
									ffpConjoint.setAnnule(true);
									if (noOfsCommune == null) {
										noOfsCommune = ffpConjoint.getNumeroOfsAutoriteFiscale();
										typeAutoriteCommune = ffpConjoint.getTypeAutoriteFiscale();
									}
								}
								else {
									final String msg = String.format("Le contribuable %s possède déjà un for qui s'ouvre à la date du mariage", FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()));
									throw new EvenementCivilHandlerException(msg);
								}
							}
						}
					}
				}
			}

			if (noOfsCommune == null) {
				// il n'est apparemment pas possible que les deux fors soient nuls, sinon on n'aurait pas pu trouver de
				// mode d'imposition
				ForFiscalPrincipal forPourMenage = null;

				if ((forPrincipal != null && forConjoint == null) || (forPrincipal == null && forConjoint != null)) {
					// si un seul for existe, on utilise ses données
					if (forPrincipal == null) {
						forPourMenage = forConjoint;
					}
					else {
						forPourMenage  = forPrincipal;
					}
				}
				else {
					// sinon il faut determiner selon le principe dans [UNIREG-1462]
					final boolean principalDansCanton = forPrincipal != null && TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forPrincipal.getTypeAutoriteFiscale();
					final boolean conjointDansCanton = forConjoint != null && TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forConjoint.getTypeAutoriteFiscale();
					if (principalDansCanton || !conjointDansCanton) {
						// si le for du principal est dans le canton ou celui de son conjoint ne l'est pas, on utilise le for du principal
						forPourMenage = forPrincipal;
					}
					else if (conjointDansCanton) {
						// si le conjoint est habitant
						forPourMenage = forConjoint;
					}
				}

				noOfsCommune = forPourMenage.getNumeroOfsAutoriteFiscale();
				typeAutoriteCommune = forPourMenage.getTypeAutoriteFiscale();
			}

			if (menageCommun.getForFiscalPrincipalAt(dateEffective) == null) {
				/*
				 * Cas ou l'on créé un nouveau ménage commun, il aura pas de for ouvert
				 */
				tiersService.openForFiscalPrincipal(menageCommun, dateEffective, MotifRattachement.DOMICILE, noOfsCommune,
						typeAutoriteCommune, modeImposition, motifOuverture, changeHabitantFlag);
			}

			/*
			 * réouverture des autres fors
			 */
			createForsSecondaires(dateEffective, menageCommun, ffsPrincipal, motifOuverture);
			createForsAutreElementImpossable(dateEffective, menageCommun, ffaeiPrincipal, motifOuverture);
			createForsSecondaires(dateEffective, menageCommun, ffsConjoint, motifOuverture);
			createForsAutreElementImpossable(dateEffective, menageCommun, ffaeiConjoint, motifOuverture);
		}

		if (!StringUtils.isBlank(remarque)) {
			principal.setRemarque((principal.getRemarque() != null ? principal.getRemarque() : "") + remarque);
			if (conjoint != null) {
				conjoint.setRemarque((conjoint.getRemarque() != null ? conjoint.getRemarque() : "") + remarque);
			}
			menageCommun.setRemarque((menageCommun.getRemarque() != null ? menageCommun.getRemarque() : "") + remarque);
		}

		updateSituationFamilleMariage(menageCommun, dateEffective, etatCivilFamille);

		return menageCommun;
	}

	private ForFiscalPrincipal getDernierForFermePourDepart(final PersonnePhysique pp) {
		ForFiscalPrincipal dernierFor = null;
		for (ForFiscal ff : pp.getForsFiscaux()) {
			if (!ff.isAnnule() && ff.isPrincipal()) {
				ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
				if (isDepart(ffp.getMotifFermeture()) && (dernierFor == null || RegDateHelper.isAfterOrEqual(dernierFor.getDateFin(), ffp.getDateFin(), NullDateBehavior.EARLIEST))) {
					dernierFor = ffp;
				}
			}
		}
		return dernierFor;
	}

	/**
	 * Classe interne qui permet de conserver les données qui peuvent passer d'un for à
	 * l'autre suite à l'annulation du premier en vu de création du deuxième
	 */
	private static final class DonneesOuvertureFor {

		private final MotifFor motifOuverture;
		private final RegDate dateDebut;
		private final Integer numeroOfsAutoriteFiscale;
		private final TypeAutoriteFiscale typeAutoriteFiscale;

		public DonneesOuvertureFor(MotifFor motifOuverture, RegDate dateDebut, Integer numeroOfsAutoriteFiscale,
				TypeAutoriteFiscale typeAutoriteFiscale) {
			this.motifOuverture = motifOuverture;
			this.dateDebut = dateDebut;
			this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
			this.typeAutoriteFiscale = typeAutoriteFiscale;
		}

		public MotifFor getMotifOuverture() {
			return motifOuverture;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public Integer getNumeroOfsAutoriteFiscale() {
			return numeroOfsAutoriteFiscale;
		}

		public TypeAutoriteFiscale getTypeAutoriteFiscale() {
			return typeAutoriteFiscale;
		}
	}

	private DonneesOuvertureFor traiterCasEvenementMariageRecuApresEvenementArriveeMaisMariageAnterieur(
			PersonnePhysique membreCouple, RegDate dateMariage) {

		final ForFiscalPrincipal ffp = membreCouple.getDernierForFiscalPrincipal();

		// arrivé en fait après le mariage mais déjà connu avant?
		DonneesOuvertureFor retour = null;
		if (ffp != null && ffp.getDateDebut().isAfter(dateMariage) && isArrivee(ffp.getMotifOuverture())) {

			// il faut annuler les fors individuels créés par l'arrivée
			ffp.setAnnule(true);

			// on garde le motif d'ouverture, la date d'ouverture, ainsi que la commune d'ouverture pour le nouveau for à créer
			retour = new DonneesOuvertureFor(ffp.getMotifOuverture(),  ffp.getDateDebut(), ffp.getNumeroOfsAutoriteFiscale(), ffp.getTypeAutoriteFiscale());
		}

		return retour;
	}

	private void updateSituationFamilleMariage(MenageCommun menageCommun, RegDate date, ch.vd.uniregctb.type.EtatCivil etatCivilFamille) {

		/*
		 * S'il y a un non-habitant dans le couple, la situation de
		 * famille doit être générée pour que celui-ci ait un état civil.
		 *
		 * S'il y a pas de non habitant dans le couple,
		 * il faut comparer les état civils des mariés:
		 *            |            Etat civil
		 * --------------------------------------------------------
		 * 	principal | marié/pacsé | autre
		 * --------------------------------------------------------
		 *  conjoint  | marié/pacsé | autre
		 * --------------------------------------------------------
		 *   Action   | rien faire  | création de SF sur le ménage
		 */
		boolean auMoinsUnNonHabitant = false;
		boolean auMoinsUnSansEtatCivil = false;
		boolean etatsCivilsDifferents = false;

		EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menageCommun, date);
		PersonnePhysique principal = couple.getPrincipal();
		ch.vd.uniregctb.type.EtatCivil etatCivilPrincipal = situationFamilleService.getEtatCivil(principal, date);
		if (!principal.isHabitant()) {
			auMoinsUnNonHabitant = true;
		}
		PersonnePhysique conjoint = couple.getConjoint();
		ch.vd.uniregctb.type.EtatCivil etatCivilConjoint = null;
		if (conjoint != null) {
			etatCivilConjoint = situationFamilleService.getEtatCivil(conjoint, date);
			if (!conjoint.isHabitant()) {
				auMoinsUnNonHabitant = true;
			}
		}

		boolean estMarieSeul = (conjoint == null);
		if (etatCivilPrincipal == null || (!estMarieSeul && etatCivilConjoint == null)) {
			auMoinsUnSansEtatCivil = true;
		}
		else {
			etatsCivilsDifferents = (etatCivilPrincipal != etatCivilFamille) || (!estMarieSeul && !etatCivilConjoint.equals(etatCivilFamille));
		}
		if (auMoinsUnNonHabitant || auMoinsUnSansEtatCivil || etatsCivilsDifferents) {
			/*
			 * Fermeture de la situation de famille actuelle sur les membres du ménage
			 */
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menageCommun)) {
				situationFamilleService.closeSituationFamille(pp, date.getOneDayBefore());
			}

			/*
			 * Création de la nouvelle situation de famille pour le ménage
			 */
			SituationFamilleMenageCommun situationFamilleMenage = new SituationFamilleMenageCommun();
			situationFamilleMenage.setContribuablePrincipal(principal);
			situationFamilleMenage.setDateDebut(date);
			situationFamilleMenage.setEtatCivil(etatCivilFamille);
			situationFamilleMenage.setNombreEnfants(0);
			situationFamilleMenage.setTarifApplicable(TarifImpotSource.NORMAL);
			// ajoute la nouvelle situation de famille au ménage
			situationFamilleService.addSituationFamille(situationFamilleMenage, menageCommun);
		}
	}

	public MenageCommun marie(RegDate dateMariage, PersonnePhysique principal, PersonnePhysique conjoint, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille, boolean changeHabitantFlag, Long numeroEvenement) {
		/*
		 * Création d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = new MenageCommun();
		// savegarde des complements
		setComplements(principal, conjoint, menageCommun);

		menageCommun = (MenageCommun) getTiersDAO().save(menageCommun);
		Audit.info("Création d'un tiers MenageCommun");

		return rattachToMenage(menageCommun, principal, conjoint, dateMariage, remarque, etatCivilFamille, changeHabitantFlag, numeroEvenement);
	}

	private void setComplements(PersonnePhysique principal, PersonnePhysique conjoint, MenageCommun menageCommun) {
		if (principal.getPersonneContact() != null) {
			menageCommun.setPersonneContact(principal.getPersonneContact());
		}
		else if (conjoint != null && conjoint.getPersonneContact() != null) {
			menageCommun.setPersonneContact(conjoint.getPersonneContact());
		}
		if (principal.getComplementNom() != null) {
			menageCommun.setComplementNom(principal.getComplementNom());
		}
		else if (conjoint != null && conjoint.getComplementNom() != null) {
			menageCommun.setComplementNom(conjoint.getComplementNom());

		}
		if (principal.getNumeroTelephonePrive() != null) {
			menageCommun.setNumeroTelephonePrive(principal.getNumeroTelephonePrive());
		}
		else if (conjoint != null && conjoint.getNumeroTelephonePrive() != null) {
			menageCommun.setNumeroTelephonePrive(conjoint.getNumeroTelephonePrive());
		}
		if (principal.getNumeroTelephonePortable() != null) {
			menageCommun.setNumeroTelephonePortable(principal.getNumeroTelephonePortable());
		}
		else if (conjoint != null && conjoint.getNumeroTelephonePortable() != null) {
			menageCommun.setNumeroTelephonePortable(conjoint.getNumeroTelephonePortable());
		}
		if (principal.getNumeroTelephoneProfessionnel() != null) {
			menageCommun.setNumeroTelephoneProfessionnel(principal.getNumeroTelephoneProfessionnel());
		}
		else if (conjoint != null && conjoint.getNumeroTelephoneProfessionnel() != null) {
			menageCommun.setNumeroTelephoneProfessionnel(conjoint.getNumeroTelephoneProfessionnel());
		}
		if (principal.getNumeroTelecopie() != null) {
			menageCommun.setNumeroTelecopie(principal.getNumeroTelecopie());
		}
		else if (conjoint != null && conjoint.getNumeroTelecopie() != null) {
			menageCommun.setNumeroTelecopie(conjoint.getNumeroTelecopie());
		}
		if (principal.getAdresseCourrierElectronique() != null) {
			menageCommun.setAdresseCourrierElectronique(principal.getAdresseCourrierElectronique());
		}
		else if (conjoint != null && conjoint.getAdresseCourrierElectronique() != null) {
			menageCommun.setAdresseCourrierElectronique(conjoint.getAdresseCourrierElectronique());
		}
		if (principal.getNumeroCompteBancaire() != null) {
			menageCommun.setNumeroCompteBancaire(principal.getNumeroCompteBancaire());
		}
		else if (conjoint != null && conjoint.getNumeroCompteBancaire() != null) {
			menageCommun.setNumeroCompteBancaire(conjoint.getNumeroCompteBancaire());
		}
		if (principal.getTitulaireCompteBancaire() != null) {
			menageCommun.setTitulaireCompteBancaire(principal.getTitulaireCompteBancaire());
		}
		else if (conjoint != null && conjoint.getTitulaireCompteBancaire() != null) {
			menageCommun.setTitulaireCompteBancaire(conjoint.getTitulaireCompteBancaire());
		}
		if (principal.getAdresseBicSwift() != null) {
			menageCommun.setAdresseBicSwift(principal.getAdresseBicSwift());
		}
		else if (conjoint != null && conjoint.getAdresseBicSwift() != null) {
			menageCommun.setAdresseBicSwift(conjoint.getAdresseBicSwift());
		}
	}

    public MenageCommun rattachToMenage(MenageCommun menage, PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille, boolean changeHabitantFlag, Long numeroEvenement) {
		/*
		 * Création des rapports entre tiers ménage commun
		 */
		Audit.info("Création des rapports entre tiers ménage commun et tiers");
		getTiersService().addTiersToCouple(menage, principal, date, null);
		if (conjoint != null) {
			getTiersService().addTiersToCouple(menage, conjoint, date, null);
		}

		/*
		 * Mariage de 2 personnes
		 */
		return doMariageReconciliation(menage, date, remarque, etatCivilFamille, null, changeHabitantFlag);
	}

	private boolean isValidSituationFamille(RegDate date, Contribuable contribuable) {
		/*
		 * Vérifie que la situation de famille à la date donnée n'a pas été surchargée
		 */
		SituationFamille situationFamilleActuelle = contribuable.getSituationFamilleActive();
		SituationFamille situationFamille = contribuable.getSituationFamilleAt(date);
		if (situationFamilleActuelle != null) {
			if (!situationFamilleActuelle.equals(situationFamille))
				return false;
		}
		else if (situationFamille != null) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public ValidationResults validateReconstitution(MenageCommun menage, PersonnePhysique pp, RegDate date) {
		ValidationResults results = new ValidationResults();

		if (pp == null) {
			results.addError("La personne à ajouter au ménage à réconstituer est null");
		}
		else {
			/*
			 * Vérifications communes
			 */
			checkUnion(date, pp, null, results);
		}
		if (menage == null) {
			results.addError("Le ménage à réconstituer est null");
		}
		else {
			// vérification que le ménage soit bien incomplet
			checkMenageIsNotComplete(menage, results);
		}

		RegDate dateMariage = date;
		RapportEntreTiers rapport = menage.getRapportObjetValidAt(date, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		if (rapport != null) {
			dateMariage = rapport.getDateDebut();
		}
		if (!RegDateHelper.equals(date, dateMariage)) {
			results.addError("La date ne correspond pas à celle du mariage.");
		}
		return results;
	}

	private void checkMenageIsNotComplete(MenageCommun menage, ValidationResults results) {
		if (tiersService.getPersonnesPhysiques(menage).size() > 1) {
			results.addError("Le ménage n° " + FormatNumeroHelper.numeroCTBToDisplay(menage.getNumero()) + " semble complet");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public MenageCommun reconstitueMenage(MenageCommun menage, PersonnePhysique pp, RegDate date, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille) {

		/*
		 * ajout du tiers précédemment absent au ménage commun
		 */
		Audit.info("Ajout du membre au ménage commun");
		getTiersService().addTiersToCouple(menage, pp, date, null);

		/*
		 * Sauvegarde des fors secondaires et autre élément imposable de chaque tiers
		 */
		final ForsParType forsParType = pp.getForsParType(false);
		List<ForFiscalSecondaire> ffsPP = forsParType.secondaires;
		List<ForFiscalAutreElementImposable> ffaeiPP = forsParType.autreElementImpot;

		/*
		 * Fermeture de tous les fors du tiers à la veille du mariage
		 */
		getTiersService().closeAllForsFiscaux(pp, date.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		/*
		 * Réouverture des fors secondaires et autre élément imposable
		 */
		createForsSecondaires(date, menage, ffsPP, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		createForsAutreElementImpossable(date, menage, ffaeiPP, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		/*
		 * Pas de calcul du mode d'imposition pour l'instant
		ModeImpositionResolver reconstitutionResolver = new ReconstitutionMenageResolver(tiersService, menage);
		Imposition imposition = null;
		try {
			imposition = reconstitutionResolver.resolve(pp, date, null);
		}
		catch (ModeImpositionResolverException ex) {
			throw new EvenementCivilHandlerException(ex.getMessage(), ex);
		}

		// si le nouveau mode d'imposition a pu être déterminé et n'est pas le même
		if (imposition != null && !imposition.getModeImposition().equals(impositionMenage)) {
			tiersService.changeModeImposition(menage, RegDate.get(), imposition.getModeImposition(), MotifFor.CHGT_MODE_IMPOSITION);
		}
		*/

		/*
		 * Mise à jour de la situation de famille
		 */
		updateSituationFamilleMariage(menage, date, etatCivilFamille);

		if (!StringUtils.isBlank(remarque)) {
			menage.setRemarque((menage.getRemarque() != null ? menage.getRemarque() : "") + remarque);
			pp.setRemarque((pp.getRemarque() != null ? pp.getRemarque() : "") + remarque);
		}
		return menage;
	}

	/**
	 * {@inheritDoc}
	 */
	public ValidationResults validateFusion(MenageCommun menagePrincipal, MenageCommun menageConjoint) {
		ValidationResults results = new ValidationResults();

		if (menagePrincipal == null) {
			results.addError("Le ménage du conjoint principal est null");
		}
		else {
			checkMenageIsNotComplete(menagePrincipal, results);
		}
		if (menageConjoint == null) {
			results.addError("Le ménage du conjoint est null");
		}
		else {
			checkMenageIsNotComplete(menageConjoint, results);
		}

		return results;
	}

	public MenageCommun getMenageForFusion(MenageCommun menage1, MenageCommun menage2) {
		final ForFiscalPrincipal premierForPrincipal = menage1.getPremierForFiscalPrincipal();
		final ForFiscalPrincipal premierForConjoint = menage2.getPremierForFiscalPrincipal();
		if (premierForPrincipal != null && premierForConjoint != null) {
			final int compare = premierForPrincipal.compareTo(premierForConjoint);
			if (compare < 0) {
				// le premier ménage a le for le plus ancien
				return menage1;
			}
			else if (compare > 0) {
				// le second ménage a le for le plus ancien
				return menage2;
			}
		}
		else if (premierForPrincipal != null) {
			return menage1;
		}
		else if (premierForConjoint != null) {
			return menage2;
		}

		// les deux ménages on un for à la même date ou pas de for
		final PersonnePhysique pp1 = tiersService.getPersonnesPhysiques(menage1).toArray(new PersonnePhysique[0])[0];
		final PersonnePhysique pp2 = tiersService.getPersonnesPhysiques(menage2).toArray(new PersonnePhysique[0])[0];
		final PersonnePhysique principal = tiersService.getPrincipal(pp1, pp2);
		if (principal == pp1) {
			return menage1;
		}
		else {
			return menage2;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public MenageCommun fusionneMenages(MenageCommun menagePrincipal, MenageCommun menageConjoint, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille) {

		final MenageCommun menageChoisi = getMenageForFusion(menagePrincipal, menageConjoint);
		final MenageCommun autreMenage = (menageChoisi == menagePrincipal ? menageConjoint : menagePrincipal);

		final PersonnePhysique principal = tiersService.getPersonnesPhysiques(menageChoisi).toArray(new PersonnePhysique[0])[0];
		final PersonnePhysique conjoint = tiersService.getPersonnesPhysiques(autreMenage).toArray(new PersonnePhysique[0])[0];

		final ForFiscalPrincipal forFPMenage = menageChoisi.getForFiscalPrincipalAt(null);
		final ModeImposition impositionMenage = (forFPMenage == null ? null : forFPMenage.getModeImposition());

		final RapportEntreTiers premierRapport = menageChoisi.getPremierRapportObjet(TypeRapportEntreTiers.APPARTENANCE_MENAGE, principal);
		final RegDate dateDebut = premierRapport.getDateDebut();

		ModeImpositionResolver fusionResolver = new FusionMenagesResolver(tiersService, menageChoisi, autreMenage);
		Imposition imposition = null;
		try {
			imposition = fusionResolver.resolve(menageChoisi, dateDebut, null);
		}
		catch (ModeImpositionResolverException ex) {
			throw new EvenementCivilHandlerException(ex.getMessage(), ex);
		}

		/*
		 * Sauvegarde des fors secondaires et autre élément imposable du ménage non choisi
		 */
		final ForsParType forsParType = autreMenage.getForsParType(false);
		List<ForFiscalSecondaire> ffsAutreMenage = forsParType.secondaires;
		List<ForFiscalAutreElementImposable> ffaeiAutreMenage = forsParType.autreElementImpot;

		// annulation du ménage n'ayant plus d'intérêt
		tiersService.annuleTiers(autreMenage);

		// fermeture des rapport du ménage (1 seul doit exister);
		for (RapportEntreTiers rapport : autreMenage.getRapportsObjet()) {
			if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() &&
					rapport.getDateFin() == null && rapport.getSujetId().equals(conjoint.getId())) {
				rapport.setAnnule(true);
			}
		}

		// ajout du tiers au ménage sélectionné
		final PersonnePhysique tiersManquant = conjoint;
		tiersService.addTiersToCouple(menageChoisi, tiersManquant, dateDebut, null);

		/*
		 * Réouverture des fors secondaires et autre élément imposable sur le ménage commun choisi
		 */
		createForsSecondaires(dateDebut, menageChoisi, ffsAutreMenage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		createForsAutreElementImpossable(dateDebut, menageChoisi, ffaeiAutreMenage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		// si le nouveau mode d'imposition a pu être déterminé et n'est pas le même
		if (imposition != null && imposition.getModeImposition() != impositionMenage) {
			forFPMenage.setModeImposition(imposition.getModeImposition());
		}

		/*
		 * Mise à jour de la situation de famille
		 */
		updateSituationFamilleMariage(menageChoisi, dateDebut, etatCivilFamille);

		// ajout de la remarque aux tiers mis à jour
		if (!StringUtils.isBlank(remarque)) {
			principal.setRemarque((principal.getRemarque() != null ? principal.getRemarque() : "") + remarque);
			if (conjoint != null) {
				conjoint.setRemarque((conjoint.getRemarque() != null ? conjoint.getRemarque() : "") + remarque);
			}
			menageChoisi.setRemarque((menageChoisi.getRemarque() != null ? menageChoisi.getRemarque() : "") + remarque);
			autreMenage.setRemarque((autreMenage.getRemarque() != null ? autreMenage.getRemarque() : "") + remarque);
		}

		return menageChoisi;
	}

	public void annuleMariage(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, Long numeroEvenement) {
		annuleCouple(principal, conjoint, date, numeroEvenement, false);
	}

	private void annuleCouple(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, Long numeroEvenement, boolean annulationMenage) {
		// Recherche l'existence d'un ménage commun
		RapportEntreTiers dernierRapportMenage = principal.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		if (dernierRapportMenage == null) {
			throw new EvenementCivilHandlerException("Aucun ménage n'a pas été trouvé");
		}
		else {
			if (!date.equals(dernierRapportMenage.getDateDebut())) {
				throw new EvenementCivilHandlerException("La date du dernier rapport entre tiers ne correspond pas à celle de l'événement");
			}
			else if (dernierRapportMenage.getDateFin() != null) {
				throw new EvenementCivilHandlerException("Il y a eu d'autres opérations après le mariage/réconciliation");
			}
		}
		MenageCommun menage = (MenageCommun) tiersDAO.get(dernierRapportMenage.getObjetId());
		EnsembleTiersCouple ensembleTiersCouple = getTiersService().getEnsembleTiersCouple(menage, dernierRapportMenage.getDateDebut());
		if (!ensembleTiersCouple.estComposeDe(principal, conjoint)) {
			String message = "Le dernier ménage n'est pas composé";
			if (conjoint != null) {
				message += " des tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()) + " et " + FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero());
			}
			else {
				message += " du tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero());
			}
			throw new EvenementCivilHandlerException(message);
		}

		final ForFiscalPrincipal dernierFFP = menage.getDernierForFiscalPrincipal();
		if (dernierFFP != null && !date.equals(dernierFFP.getDateDebut())) {
			throw new EvenementCivilHandlerException("Il y a eu d'autres opérations après le mariage/réconciliation");
		}

		/*
		 * Si appelé depuis l'IHM, vérifie que la situation de famille depuis le mariage n'a pas été surchargée
		 */
		if (numeroEvenement == null && !isValidSituationFamille(date, menage)) {
			throw new EvenementCivilHandlerException("La situation de famille a changée depuis le mariage/réconciliation. Veuillez corriger cela avant de procéder à cette annulation.");
		}

		// Annulation des rapports entre tiers créés à la date effective
		for (RapportEntreTiers rapport : menage.getRapportsObjet()) {
			if (!rapport.isAnnule() && date.equals(rapport.getDateDebut())) {
				rapport.setAnnule(true);
			}
		}
		// Réouverture des fors fermés lors du mariage (renvoi une exception si des fors ont été annulé après la date de mariage)
		reouvreForFermePourMariage(principal, date);
		// même chose pour le conjoint
		if (conjoint != null) {
			reouvreForFermePourMariage(conjoint, date);
		}

		if (annulationMenage) {
			// annulation du tiers ménage
			tiersService.annuleTiers(menage);
		}
		else {
			// Fermeture des fors créés lors de la création du couple
			cancelForsOpenedSince(date, menage);
		}

		updateSituationFamilleAnnulationCouple(menage, date);
	}

	/**
	 * Réouvre le for fermé lors du mariage
	 */
	private void reouvreForFermePourMariage(PersonnePhysique pp, RegDate date) {
		final List<ForFiscalPrincipal> forsFiscaux = pp.getForsFiscauxPrincipauxApres(date);
		final int nombreFors = forsFiscaux.size();
		if (nombreFors == 1) {
			final ForFiscalPrincipal ff = forsFiscaux.get(0);
			if (isAnnuleEtOuvert(ff) && isArrivee(ff.getMotifOuverture())) {
				tiersService.reopenFor(ff, pp);
			}
		}
		else if (nombreFors == 0) {
			tiersService.reopenForsClosedAt(date.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, pp);
		}
	}

	public boolean isAnnuleEtOuvert(ForFiscalPrincipal ffp) {
		return ffp.isAnnule() && ffp.getDateFin() == null;
	}

	private void updateSituationFamilleAnnulationCouple(MenageCommun menage, RegDate date) {
		SituationFamille situationFamille = menage.getSituationFamilleAt(date);
		if (situationFamille != null) {
			situationFamille.setAnnule(true);
		}
		reopenSituationFamilleMembresMenage(date.getOneDayBefore(), menage);
	}

	private void reopenSituationFamilleMembresMenage(RegDate date, MenageCommun menage) {
		/*
		 * Réouverture de la situation de famille actuelle sur les membres du ménage
		 */
		for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
			reopenSituationFamille(date, pp);
		}
	}

	private void reopenSituationFamille(RegDate date, Contribuable contribuable) {

		final SituationFamille situationFamille = contribuable.getSituationFamilleAt(date);
		if (situationFamille != null) {

			final SituationFamille nouvelleSituationFamille = situationFamille.duplicate();

			// annulation de l'ancienne situation
			situationFamille.setAnnule(true);

			// ajoute de la nouvelle
			nouvelleSituationFamille.setDateFin(null);
			situationFamilleService.addSituationFamille(nouvelleSituationFamille, contribuable);
		}
	}

	private void cancelSituationFamilleMembresMenage(RegDate date, MenageCommun menage) {
		/*
		 * Réouverture de la situation de famille actuelle sur les membres du ménage
		 */
		for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
			cancelSituationFamillePP(date, pp);
		}
	}

	private void cancelSituationFamillePP(RegDate date, PersonnePhysique personePhysique) {
		SituationFamille situationFamille = personePhysique.getSituationFamilleAt(date);
		if (situationFamille != null) {
			// annulation de la situation
			situationFamille.setAnnule(true);
		}
	}

	public ValidationResults validateReconciliation(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date) {

		ValidationResults results = new ValidationResults();

		/*
		 * Vérifie que les tiers soient pas le même
		 */
		if (principal == conjoint) {
			results.addError("Impossible de réconcilier quelqu'un avec lui-même");
		}

		/*
		 * Validation de l'existence d'un ménage commun
		 */
		EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(principal, date);
		if (ensemble != null) {
			if (ensemble.estComposeDe(principal, conjoint)) {
				results.addError("Le couple n'est pas séparé");
			}
			else {
				results.addError("Les deux tiers ne sont pas mariés");
			}
		}

		return results;

	}

	/**
	 * {@inheritDoc}
	 */
	public MenageCommun reconcilie(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, String remarque, boolean changeHabitantFlag, Long numeroEvenement) {

		/*
		 * Recherche du dernier ménage
		 */
		Set<RapportEntreTiers> rapports = principal.getRapportsSujet();
		RapportEntreTiers dernierRapportMenage = null;
		for (RapportEntreTiers rapportEntreTiers : rapports) {
			if (!rapportEntreTiers.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapportEntreTiers.getType()) {
				if (dernierRapportMenage == null || RegDateHelper.isAfterOrEqual(rapportEntreTiers.getDateDebut(), dernierRapportMenage.getDateDebut(), NullDateBehavior.EARLIEST)) {
					dernierRapportMenage = rapportEntreTiers;
				}
			}
		}
		Assert.notNull(dernierRapportMenage, "Le dernier ménage n'a pas été trouvé");
		MenageCommun menageCommun = (MenageCommun) tiersDAO.get(dernierRapportMenage.getObjetId());
		EnsembleTiersCouple ensembleTiersCouple = getTiersService().getEnsembleTiersCouple(menageCommun, dernierRapportMenage.getDateDebut());
		if (!ensembleTiersCouple.estComposeDe(principal, conjoint)) {
			String message = "Le dernier ménage n'est pas composé";
			if (conjoint != null) {
				message += " des tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()) + " et " + FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero());
			}
			else {
				message += " du tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero());
			}
			throw new EvenementCivilHandlerException(message);
		}

		/*
		 * Réouverture des rapports entre tiers du ménage commun
		 */
		Audit.info(numeroEvenement, "Réouverture des rapports entre tiers ménage commun et tiers");
		getTiersService().addTiersToCouple(menageCommun, principal, date, null);
		if (conjoint != null) {
			getTiersService().addTiersToCouple(menageCommun, conjoint, date, null);
		}

		/*
		 * Réunification du couple
		 */
		return doMariageReconciliation(menageCommun, date, remarque, ch.vd.uniregctb.type.EtatCivil.MARIE, numeroEvenement, changeHabitantFlag);
	}


	public void annuleReconciliation(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, Long numeroEvenement) {
		annuleCouple(principal, conjoint, date, numeroEvenement, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isMajeurAt(PersonnePhysique pp, RegDate dateReference) {
		Assert.notNull(dateReference);

		final RegDate dateNaissance = tiersService.getDateNaissance(pp);
		if (dateNaissance == null) {
			/*
			 * La date de naissance peut-être dans uniquement avec un non-habitant.
			 *
			 * (18.07.2998) Discussion avec Thierry Declercq : la logique veut qu'un non-habitant soit créé lorsque - d'une manière ou d'une
			 * autre - cette personne est assujetti à l'impôt. On peut donc déduire implicitement que - sauf cas particulier - un
			 * non-habitant est majeur.
			 *
			 * Thierry pense que la manière correcte est de ne pas vérifier la majorité d'un non-habitant si celui-ci ne possède pas de date
			 * de naissance. Ce qui revient à de dire qu'un non-habitant sans date de naissance est considéré comme majeur.
			 */
			return true;
		}

		return FiscalDateHelper.isMajeur(dateReference, dateNaissance);
	}

	/**
	 * {@inheritDoc}
	 */
	public OuvertureForsResults ouvertureForsContribuablesMajeurs(RegDate dateReference, StatusManager status) {
		OuvertureForsContribuablesMajeursProcessor processor = new OuvertureForsContribuablesMajeursProcessor(transactionManager,
				hibernateTemplate, tiersService, adresseService, serviceInfra, tiersSearcher);
		return processor.run(dateReference, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public FusionDeCommunesResults fusionDeCommunes(Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, RegDate dateTraitement, StatusManager status) {
		FusionDeCommunesProcessor processor = new FusionDeCommunesProcessor(transactionManager, hibernateTemplate, tiersService, serviceInfra);
		return processor.run(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, status);
	}

	/** (non-Javadoc)
	 * @see ch.vd.uniregctb.metier.MetierService#validateSeparation(ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.tiers.PersonnePhysique, ch.vd.uniregctb.tiers.PersonnePhysique)
	 */
	public ValidationResults validateSeparation(MenageCommun menage, RegDate date) {

		ValidationResults results = new ValidationResults();

		EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, date);

		PersonnePhysique principal = ensemble.getPrincipal();
		PersonnePhysique conjoint = ensemble.getConjoint();
		if ((principal == null) && (conjoint == null)) {
			results.addError("Le ménage doit déjà être séparé à cette date");
		}
		else {
			// [UNIREG-1057] y aura plus de génération de warnings
			// en cas de divorce/séparation avec des fors secondaires

			if (menage.getDernierForFiscalPrincipal() != null && menage.getDernierForFiscalPrincipal().getDateDebut().isAfter(date)) {
				results.addError("Il y a des fors ouverts après la date de séparation");
			}
			/*
			 * Vérifie que la nationalité peut être déterminée
			 */
			try {
				tiersService.isSuisse(principal, date); // renvoi une exception si la nationalité n'est peut pas être déterminée
				if (conjoint != null) {
					tiersService.isSuisse(conjoint, date);
				}
			}
			catch (TiersException e) {
				results.addError(e.getMessage());
			}
		}
		return results;
	}

	public void separe(MenageCommun menage, RegDate date, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille, boolean changeHabitantFlag, Long numeroEvenement) {
		if (menage == null) {
			throw new EvenementCivilHandlerException("Le ménage est null");
		}

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, date);
		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();

		final boolean separesFiscalement = isSeparesFiscalement(date, principal, conjoint);
		if (!separesFiscalement) {
			// Recupération du for principal du menage
			final ForFiscalPrincipal forMenage = menage.getForFiscalPrincipalAt(null);

			// Fermeture des fors du MenageCommun
			tiersService.closeAllForsFiscaux(menage, date.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);

			// Fermeture des RapportEntreTiers du menage
			tiersService.closeAppartenanceMenage(principal, menage, date.getOneDayBefore());
			if (conjoint != null) { // null si marié seul
				tiersService.closeAppartenanceMenage(conjoint, menage, date.getOneDayBefore());
			}

			// S'il existe un for sur le ménage (non indigents)
			if (forMenage != null) {
				final ModeImposition imposition = forMenage.getModeImposition();
				final Integer noOfsEtendu = forMenage.getNumeroOfsAutoriteFiscale();
				final TypeAutoriteFiscale typeAutoriteFiscale = forMenage.getTypeAutoriteFiscale();

				final ModeImpositionResolver divorceResolver = new DivorceModeImpositionResolver(tiersService, numeroEvenement);

				// on ouvre un nouveau for fiscal pour chaque tiers
				createForFiscalPrincipal(date, principal, imposition, noOfsEtendu, typeAutoriteFiscale, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, divorceResolver, changeHabitantFlag);
				if (conjoint != null) {
					// null si marié seul
					createForFiscalPrincipal(date, conjoint, imposition, noOfsEtendu, typeAutoriteFiscale, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, divorceResolver, changeHabitantFlag);
				}
			}

			if (!StringUtils.isBlank(remarque)) {
				principal.setRemarque((principal.getRemarque() != null ? principal.getRemarque() : "") + remarque);
				if (conjoint != null) {
					conjoint.setRemarque((conjoint.getRemarque() != null ? conjoint.getRemarque() : "") + remarque);
				}
				menage.setRemarque((menage.getRemarque() != null ? menage.getRemarque() : "") + remarque);
			}
			updateSituationFamilleSeparation(menage, date, etatCivilFamille);
		}
	}

	private void updateSituationFamilleSeparation(MenageCommun menageCommun, RegDate date, ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (menageCommun.getSituationFamilleActiveSorted() != null) {
			for (SituationFamille sf : menageCommun.getSituationFamilleActiveSorted()) {
				if (sf.getDateDebut().isAfter(date)) {
					throw new EvenementCivilHandlerException("Des situations famille actives existent après la date de séparation. Veuillez les annuler manuellement.");
				}
			}
		}
		/*
		 * Fermeture de la situation de famille actuelle du ménage
		 */
		SituationFamille situationFamilleActive = menageCommun.getSituationFamilleActive();
		situationFamilleService.closeSituationFamille(menageCommun, date.getOneDayBefore());

		for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menageCommun)) {

			boolean nonHabitant = false;
			boolean sansEtatCivil = false;
			boolean etatCivilDifferent = false;

			ch.vd.uniregctb.type.EtatCivil etatCivilActif = situationFamilleService.getEtatCivil(pp, date);

			if (!pp.isHabitant()) {
				nonHabitant = true;
			}

			if (etatCivilActif == null) {
				sansEtatCivil = true;
			}
			else {
				etatCivilDifferent = etatCivilActif != etatCivil;
			}

			if (nonHabitant || sansEtatCivil || etatCivilDifferent) {
				int nombreEnfants = 0;
				if (situationFamilleActive != null) {
					nombreEnfants = situationFamilleActive.getNombreEnfants();
				}

				SituationFamille situationFamille = new SituationFamille();
				situationFamille.setContribuable(pp);
				situationFamille.setDateDebut(date);
				situationFamille.setEtatCivil(etatCivil);
				situationFamille.setNombreEnfants(nombreEnfants);
				// ajout de la nouvelle situation de famille sur le tiers
				situationFamilleService.addSituationFamille(situationFamille, pp);
			}
		}
	}

	public void annuleSeparation(MenageCommun menage, RegDate date, Long numeroEvenement) {
		if (menage == null)
			throw new EvenementCivilHandlerException("Le ménage est null");
		/*
		 * Recherche du dernier ménage
		 */
		RapportEntreTiers dernierRapportMenage = menage.getDernierRapportObjet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);

		if (dernierRapportMenage == null) {
			throw new EvenementCivilHandlerException("Aucun rapport trouvé pour le ménage");
		}
		// vérification que d'autres opérations n'aient été faites aprés le décès
		if (NullDateBehavior.EARLIEST.compare(dernierRapportMenage.getDateFin(), date) > 0) {
			Assert.fail("La date du dernier rapport entre tiers est postérieure à celle de l'événement");
		}

		PersonnePhysique principal = null;
		PersonnePhysique conjoint = null;
		// récuperer le ménage que si celui-ci est valid au moment de la séparation
		if (date.getOneDayBefore().equals(dernierRapportMenage.getDateFin())) {
			EnsembleTiersCouple ensembleTiersCouple = getTiersService().getEnsembleTiersCouple(menage, dernierRapportMenage.getDateDebut());
			principal = ensembleTiersCouple.getPrincipal();
			conjoint = ensembleTiersCouple.getConjoint(principal);
			if (!ensembleTiersCouple.estComposeDe(principal, conjoint)) {
				String message = "Le dernier ménage n'est pas composé";
				if (conjoint != null) {
					message += " des tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()) + " et " + FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero());
				}
				else {
					message += " du tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero());
				}
				throw new EvenementCivilHandlerException(message);
			}
		}
		else {
			throw new EvenementCivilHandlerException("La date de séparation n'est pas correcte");
		}
		/*
		 * Si appelé depuis l'IHM, vérifie que la situation de famille depuis la séparation n'a pas été surchargée
		 */
		if (numeroEvenement == null && !isValidSituationFamille(date, menage)) {
			throw new EvenementCivilHandlerException("La situation de famille a changée depuis la séparation. Veuillez corriger cela avant de procéder à cette annulation.");
		}

		/*
		 * Fermeture des fors ouverts sur les contribuables
		 */
		cancelForsOpenedSince(date, principal, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
		if (conjoint != null) {
			cancelForsOpenedSince(date, conjoint, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
		}

		/*
		 * Réouverture des rapports tiers-ménage
		 */
		List<RapportEntreTiers> rapportOuverts = new ArrayList<RapportEntreTiers>();
		for (RapportEntreTiers rapport : menage.getRapportsObjet()) {
			if (!rapport.isAnnule() && date.getOneDayBefore().equals(rapport.getDateFin())) {
				RapportEntreTiers nouveauRapport = reopenRapportEntreTiers(rapport);
				nouveauRapport.setSujetId(rapport.getSujetId());
				nouveauRapport.setObjetId(rapport.getObjetId());
				// ajout à la liste des rapports à ajouter
				rapportOuverts.add(nouveauRapport);
			}
		}
		for (RapportEntreTiers rapport : rapportOuverts) {
			// assigner le nouveau rapport au tiers
			final Tiers sujet = tiersDAO.get(rapport.getSujetId());
			final Tiers objet = tiersDAO.get(rapport.getObjetId());
	  		tiersService.addRapport(rapport, sujet, objet);
		}

		/*
		 * Réouverture des fors du ménage commun
		 */
		tiersService.reopenForsClosedAt(date.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, menage);

		/*
		 * Mise à jour de la situation de famille
		 */
		PersonnePhysique[] personnes = tiersService.getPersonnesPhysiques(menage).toArray(new PersonnePhysique[0]);
		PersonnePhysique tiers1 = personnes[0];
		PersonnePhysique tiers2 = null;
		if (personnes.length > 1) {
			tiers2 = personnes[1];
		}
		final ch.vd.uniregctb.type.EtatCivil etatCivilFamille;
		if (tiersService.isMemeSexe(tiers1, tiers2)) {
			etatCivilFamille = ch.vd.uniregctb.type.EtatCivil.LIE_PARTENARIAT_ENREGISTRE;
		}
		else {
			etatCivilFamille = ch.vd.uniregctb.type.EtatCivil.MARIE;
		}
		updateSituationFamilleAnnulationSeparation(menage, date, etatCivilFamille);

	}

	private void updateSituationFamilleAnnulationSeparation(MenageCommun menage, RegDate date, ch.vd.uniregctb.type.EtatCivil etatCivilFamille) {
		// Annulation de la situation de famille des membres du ménage
		cancelSituationFamilleMembresMenage(date, menage);
		// Réouverture de la situation de famille du ménage
		reopenSituationFamille(date.getOneDayBefore(), menage);
		// Création de la situation de famille si l'état civil est différent de celui attendu
		final SituationFamille sf = menage.getSituationFamilleAt(date);
		if (sf == null) {
			boolean auMoinsUnNonHabitant = false;
			boolean etatsCivilsDifferents = false;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				if (!pp.isHabitant()) {
					auMoinsUnNonHabitant = true;
				}
				else {
					ch.vd.uniregctb.type.EtatCivil etatCivilActif = situationFamilleService.getEtatCivil(pp, date);
					if (etatCivilActif != etatCivilFamille) {
						etatsCivilsDifferents = true;
					}
				}
			}

			if (auMoinsUnNonHabitant || etatsCivilsDifferents) {
				/*
				 * Création de la nouvelle situation de famille pour le ménage
				 */
				SituationFamilleMenageCommun situationFamilleMenage = new SituationFamilleMenageCommun();
				situationFamilleMenage.setContribuablePrincipal(tiersService.getPrincipal(menage));
				situationFamilleMenage.setDateDebut(date);
				situationFamilleMenage.setEtatCivil(etatCivilFamille);
				situationFamilleMenage.setNombreEnfants(0);
				situationFamilleMenage.setTarifApplicable(TarifImpotSource.NORMAL);
				// ajoute la nouvelle situation de famille au ménage
				situationFamilleService.addSituationFamille(situationFamilleMenage, menage);
			}
		}
	}

	/**
	 * Retourne true si les tiers appartenant au ménage sont séparés fiscalement.
	 * @param date date pour laquelle la vérification s'effectue
	 * @param principal l'habitant
	 * @param conjoint son conjoint (peut être null si le tiers est marié seul).
	 * @return
	 */
	private boolean isSeparesFiscalement(RegDate date, PersonnePhysique principal, PersonnePhysique conjoint) {
		EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(principal, date);
		return !couple.estComposeDe(principal, conjoint);
	}

	/**
	 * Crée un nouveau for fiscal principal pour le tiers en suivant les règles pour divorce/séparation.
	 *
	 * @param contribuable
	 *            le contribuable pour qui on ouvre le nouveau for.
	 * @param modeImpotCouple
	 *            ancien mode d'imposition du couple/ménage.
	 * @param numeroOfsAutoriteFiscale
	 *            autorité fiscale.
	 * @param typeAutoriteFiscale
	 *            le type d'autorité fiscale du for à ouvrir
	 * @param motifOuverture
	 *            le motif d'ouverture.
	 * @param changeHabitantFlag
	 * @return le for créé.
	 */
	private ForFiscalPrincipal createForFiscalPrincipal(RegDate date, PersonnePhysique contribuable, ModeImposition modeImpotCouple,
			Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture,
			ModeImpositionResolver modeImpositionResolver, boolean changeHabitantFlag) {

		final Imposition nouveauMode;
		try {
			nouveauMode = modeImpositionResolver.resolve(contribuable, date, modeImpotCouple);
		}
		catch (ModeImpositionResolverException ex) {
			throw new EvenementCivilHandlerException(ex.getMessage(), ex);
		}

		ForFiscalPrincipal forFiscalPrincipal = tiersService.openForFiscalPrincipal(contribuable, date, MotifRattachement.DOMICILE,
				numeroOfsAutoriteFiscale, typeAutoriteFiscale, ModeImposition.ORDINAIRE, motifOuverture, changeHabitantFlag);

		forFiscalPrincipal.setModeImposition(nouveauMode.getModeImposition());
		forFiscalPrincipal.setDateDebut(nouveauMode.getDateDebut());

		return forFiscalPrincipal;
	}


	/**
	 * Ouvre les fors secondaires sur le contribuable.
	 * @param date la date des nouveaux fors secondaires.
	 * @param contribuable le contribuable.
	 * @param forsSecondaires liste de fors secondaires à créer.
	 * @param motifOverture motif d'ouverture assigner aux nouveaux fors.
	 */
	private void createForsSecondaires(RegDate date, Contribuable contribuable, List<ForFiscalSecondaire> forsSecondaires, MotifFor motifOverture) {
		for (ForFiscalSecondaire forFiscalSecondaire : forsSecondaires) {
			if (forFiscalSecondaire.isValidAt(date.getOneDayBefore())) {
				tiersService.openForFiscalSecondaire(contribuable, forFiscalSecondaire.getGenreImpot(),
						date, null, forFiscalSecondaire.getMotifRattachement(),
						forFiscalSecondaire.getNumeroOfsAutoriteFiscale(),
						forFiscalSecondaire.getTypeAutoriteFiscale(), motifOverture, null);
			}
		}
	}

	/**
	 * Ouvre les fors de type autre élément imposable sur le contribuable.
	 * @param date la date des nouveaux fors secondaires.
	 * @param contribuable le contribuable.
	 * @param fors liste de fors autre élément imposable à créer.
	 * @param motifOuverture motif d'ouverture assigner aux nouveaux fors.
	 */
	private void createForsAutreElementImpossable(RegDate date, Contribuable contribuable, List<ForFiscalAutreElementImposable> fors, MotifFor motifOuverture) {
		for (ForFiscalAutreElementImposable forFiscalAutreElementImposable : fors) {
			if (forFiscalAutreElementImposable.isValidAt(date.getOneDayBefore())) {
				tiersService.openForFiscalAutreElementImposable(contribuable, forFiscalAutreElementImposable.getGenreImpot(),
						date, forFiscalAutreElementImposable.getMotifRattachement(),
						forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale(),
						forFiscalAutreElementImposable.getTypeAutoriteFiscale(), motifOuverture);
			}
		}
	}

	/**
	 * Annule, pour un tiers, tous ses fors ouverts qui débutent à une date donnée.
	 *
	 * @param date
	 *            la date d'overture
	 * @param tiers
	 *            le tiers pour qui les fors seront annulés
	 */
	private void cancelForsOpenedSince(RegDate date, Tiers tiers, MotifFor motifOverture) {
		for (ForFiscal forFiscal : tiers.getForsFiscaux()) {
			if (!forFiscal.isAnnule() && forFiscal.getDateFin() == null && date.equals(forFiscal.getDateDebut())) {
				boolean isForFiscalRevenuFortune = forFiscal instanceof ForFiscalRevenuFortune;
				boolean isForFiscalAutreImpot = forFiscal instanceof ForFiscalAutreImpot;
				if (isForFiscalAutreImpot ||
						(isForFiscalRevenuFortune && motifOverture == ((ForFiscalRevenuFortune) forFiscal).getMotifOuverture())) {
					forFiscal.setAnnule(true);
				}
			}
		}
	}

	private void cancelForsOpenedSince(RegDate date, Tiers tiers) {
		for (ForFiscal forFiscal : tiers.getForsFiscaux()) {
			if (!forFiscal.isAnnule() && forFiscal.getDateFin() == null && date.equals(forFiscal.getDateDebut())) {
				boolean isForFiscalRevenuFortune = forFiscal instanceof ForFiscalRevenuFortune;
				boolean isForFiscalAutreImpot = forFiscal instanceof ForFiscalAutreImpot;
				if (isForFiscalAutreImpot || isForFiscalRevenuFortune) {
					forFiscal.setAnnule(true);
				}
			}
		}
	}

	/**
	 * @param rapport le rapport entre tiers à réovrir.
	 */
	private RapportEntreTiers reopenRapportEntreTiers(RapportEntreTiers rapport) {
		// Duplication du rapport entre tiers
		RapportEntreTiers nouveauRapport = rapport.duplicate();
		// réouvrir le nouveau rapport
		nouveauRapport.setDateFin(null);
		// annuler l'ancien rapport
		rapport.setAnnule(true);
		return nouveauRapport;
	}

	public ValidationResults validateDeces(PersonnePhysique defunt, RegDate date) {

		ValidationResults results = new ValidationResults();

		/*
		 * Récupération de l'ensemble decede-veuf-menageCommun
		 */
		EnsembleTiersCouple menageComplet = tiersService.getEnsembleTiersCouple(defunt, date);

		if (menageComplet != null && menageComplet.getMenage() != null) {

			/*
			 * Obtention du tiers correspondant au veuf.
			 */
			PersonnePhysique veuf = menageComplet.getConjoint(defunt);

			if(veuf != null && veuf.getForFiscalPrincipalAt(null) != null) {
				results.addError("Le conjoint survivant possède un for fiscal principal actif" );
			}
		}

		{
			/*
			 * Vérifications de la non existence de fors après la date de décès
			 */

			// Pour le ménage (si existant)
			EnsembleTiersCouple menageApresDeces = tiersService.getEnsembleTiersCouple(defunt, null);
			if (menageApresDeces != null && menageApresDeces.getMenage() != null) {
				ForFiscalPrincipal ffpMenage = menageApresDeces.getMenage().getDernierForFiscalPrincipal();
				if (ffpMenage != null && ffpMenage.getDateDebut().isAfter(date)) {
					results.addError("Le ménage du défunt possède un for fiscal principal ouvert après la date de décès" );
				}
			}
			else {

				// Pour le défunt
				final ForFiscalPrincipal ffpDefunt = defunt.getDernierForFiscalPrincipal();
				if (ffpDefunt != null && ffpDefunt.getDateDebut().isAfter(date) && MotifFor.VEUVAGE_DECES != ffpDefunt.getMotifOuverture()) {
					results.addError("Le défunt possède un for fiscal principal ouvert après la date de décès" );
				}
			}
		}

		return results;
	}

	public void deces(PersonnePhysique defunt, RegDate date, String remarque, Long numeroEvenement) {

		/*
		 * Récupération de l'ensemble decede-veuf-menageCommun
		 */
		EnsembleTiersCouple menageComplet = tiersService.getEnsembleTiersCouple(defunt, date);

		MenageCommun menage = null;

		if (menageComplet != null) {
			// On récupère le tiers MenageCommun
			menage = menageComplet.getMenage();
		}

		if (!defunt.isHabitant() ||
				numeroEvenement == null) {//si décès via IHM on surcharge la date de décès du civil
			defunt.setDateDeces(date);
		}

		RegDate lendemainDeces = date.getOneDayAfter();
		ForFiscalPrincipal ffpApresDeces = defunt.getForFiscalPrincipalAt(lendemainDeces);
		if (ffpApresDeces != null && ffpApresDeces.getDateDebut().equals(lendemainDeces) && MotifFor.VEUVAGE_DECES == ffpApresDeces.getMotifOuverture()) {
			// si le défunt posède un for le lendemain du décès, lui et son conjoint sont décédés le même jour
			Audit.info(numeroEvenement, "Les deux conjoints sont décédés le même jour");
			Audit.info(numeroEvenement, "Annulation du for fiscal du deuxième défunt");
			tiersService.annuleForFiscal(ffpApresDeces, true);

			defunt.setBlocageRemboursementAutomatique(true);

			// le deuxième défunt doit avoir une situation de famille VEUF
			SituationFamille sf = defunt.getSituationFamilleAt(lendemainDeces);
			if (sf != null) {
				situationFamilleService.annulerSituationFamilleSansRouvrirPrecedente(sf.getId());
			}
			return;
		}

		/*
		 * Fermeture de tous les fors du defunt au jour du décès.
		 */
		Audit.info(numeroEvenement, "Fermeture des fors fiscaux du défunt");
		tiersService.closeAllForsFiscaux(defunt, date, MotifFor.VEUVAGE_DECES);

		PersonnePhysique veuf = null;
		/*
		 * Deux cas de figure :
		 * - le défunt vivait en couple
		 * - le défunt était célibataire
		 */
		if (menage != null) {

			// On récupère le conjoint s'il existe
			veuf = menageComplet.getConjoint(defunt);

			/*
			 * Sauvegarde des fors secondaires du ménage pour réouverture sur le tiers survivant
			 */
			final ForsParType forsParType = menage.getForsParType(false);
			List<ForFiscalSecondaire> forsSecondaires = forsParType.secondaires;
			List<ForFiscalAutreElementImposable> forsAutreElement = forsParType.autreElementImpot;

			ForFiscalPrincipal forMenage = menage.getForFiscalPrincipalAt(null);

			// Fermeture des fors du MenageCommun
			tiersService.closeAllForsFiscaux(menage, date, MotifFor.VEUVAGE_DECES);

			// le blocage des remboursements automatiques se fait normalement à la fermeture du For principal,
			// ce qui est correct en ce qui concerne le ménage, dont les remboursements automatiques doivent
			// être bloqués ; en ce qui concerne le défunt, qui ne possédait pas de For actif, aucun For
			// ne sera fermé ici et le blocage des remboursements automatiques n'aura pas été fait : il faut
			// donc le faire explicitement ici
			defunt.setBlocageRemboursementAutomatique(true);

			// Fermeture des RapportEntreTiers du menage
			tiersService.closeAllRapports(defunt, date);
			if (veuf != null) {// null si marié seul
				tiersService.closeAppartenanceMenage(veuf, menage, date);
			}

			// vérification de l'assujettissement du ménage
			if (forMenage != null) {
				ModeImposition imposition = forMenage.getModeImposition();
				Integer noOfsEtendu = forMenage.getNumeroOfsAutoriteFiscale();
				TypeAutoriteFiscale typeAutoriteFiscale = forMenage.getTypeAutoriteFiscale();

				if (veuf != null) {

					// résolution du mode d'imposition du tiers survivant
					ModeImpositionResolver decesResolver = new DecesModeImpositionResolver(tiersService, numeroEvenement);

					/*
					 * on ouvre un nouveau for fiscal sur le tiers survivant
					 */
					createForFiscalPrincipal(lendemainDeces, veuf, imposition, noOfsEtendu, typeAutoriteFiscale, MotifFor.VEUVAGE_DECES, decesResolver, true);

					/*
					 * réouverture des fors secondaires actifs à la date du décès sur le tiers survivant
					 */
					createForsSecondaires(lendemainDeces, veuf, forsSecondaires, MotifFor.VEUVAGE_DECES);
					createForsAutreElementImpossable(lendemainDeces, veuf, forsAutreElement, MotifFor.VEUVAGE_DECES);
				}
			}
		}
		else {
			// Fermeture de tous les rapports
			tiersService.closeAllRapports(defunt, date);
		}

		if (!StringUtils.isBlank(remarque)) {
			defunt.setRemarque((defunt.getRemarque() != null ? defunt.getRemarque() : "") + remarque);
			if (veuf != null) {
				veuf.setRemarque((veuf.getRemarque() != null ? veuf.getRemarque() : "") + remarque);
			}
			if (menage != null) {
				menage.setRemarque((menage.getRemarque() != null ? menage.getRemarque() : "") + remarque);
			}
		}

		updateSituationFamilleDeces(defunt, date);
	}

	private void doUpdateSituationFamilleDeces(PersonnePhysique defunt, PersonnePhysique veuf, MenageCommun menage, RegDate dateDeces) {

		final RegDate lendemainDeces = dateDeces.getOneDayAfter();

		// [UNIREG-823] dans tous les cas on ferme la situation de famille sur le défunt
		if (defunt != null) {
			situationFamilleService.closeSituationFamille(defunt, dateDeces);
		}

		if (menage != null) {
			SituationFamille situationFamilleMenage = menage.getSituationFamilleActive();

			// on ferme la situation de famille sur le ménage commun qui vient d'être fermé suite au décès
			situationFamilleService.closeSituationFamille(menage, dateDeces);

			if (veuf != null) {

				EtatCivil etatCivilActif = null;
				if (veuf.isHabitant()) {
					etatCivilActif = serviceCivilService.getEtatCivilActif(veuf.getNumeroIndividu(), lendemainDeces);
				}

				/*
				 * on ferme la situation de famille courante sur le conjoint survivant et on ouvre une nouvelle situation 'veuf'. Cependant
				 * on ne fait rien si son état civil est déjà 'veuf' dans le registre civil.
				 */

				if (etatCivilActif == null || !EtatCivilHelper.estVeuf(etatCivilActif)) {

					int nombreEnfants = 0;
					if (situationFamilleMenage != null) {
						nombreEnfants = situationFamilleMenage.getNombreEnfants();
					}

					// [UNIREG-823] fermeture de la situation de famille 'marié' du conjoint survivant
					situationFamilleService.closeSituationFamille(veuf, dateDeces);

					// ajout de la nouvelle situation 'veuf' de famille sur le conjoint survivant
					SituationFamille situationFamille = new SituationFamille();
					situationFamille.setDateDebut(lendemainDeces);
					situationFamille.setEtatCivil(ch.vd.uniregctb.type.EtatCivil.VEUF);
					situationFamille.setNombreEnfants(nombreEnfants);
					situationFamilleService.addSituationFamille(situationFamille, veuf);
				}
			}
		}
	}

	private void updateSituationFamilleDeces(PersonnePhysique defunt, RegDate date) {
		EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(defunt, date);
		PersonnePhysique veuf = null;
		MenageCommun menage = null;
		if (couple != null && couple.getMenage() != null) {
			menage = couple.getMenage();
			veuf = couple.getConjoint(defunt);
		}
		doUpdateSituationFamilleDeces(defunt, veuf, menage, date);
	}

	public void annuleDeces(PersonnePhysique tiers, RegDate date) {

		/*
		 * Recherche du dernier ménage
		 */
		RapportEntreTiers dernierRapportMenage = tiers.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);

		MenageCommun menageCommun = null;
		PersonnePhysique conjoint = null;

		if (dernierRapportMenage != null) {
			// vérification que d'autres opérations n'aient été faites aprés le décès
			if (NullDateBehavior.EARLIEST.compare(dernierRapportMenage.getDateFin(), date) > 0) {
				Assert.fail("La date du dernier rapport entre tiers est postérieure à celle de l'événement");
			}

			// récuperer le ménage que si celui-ci est valide au moment du décès
			if (date.equals(dernierRapportMenage.getDateFin())) {
				menageCommun = (MenageCommun) tiersDAO.get(dernierRapportMenage.getObjetId());
				EnsembleTiersCouple ensembleTiersCouple = getTiersService().getEnsembleTiersCouple(menageCommun, dernierRapportMenage.getDateDebut());
				conjoint = ensembleTiersCouple.getConjoint(tiers);
				if (!ensembleTiersCouple.estComposeDe(tiers, conjoint)) {
					String message = "Le dernier ménage n'est pas composé";
					if (conjoint != null) {
						message += " des tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()) + " et " + FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero());
					}
					else {
						message += " du tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero());
					}
					throw new EvenementCivilHandlerException(message);
				}
			}
		}

		tiers.setDateDeces(null);

		/*
		 * Annulation des fors ouverts sur le tiers
		 */
		cancelForsOpenedSince(date.getOneDayAfter(), tiers, MotifFor.VEUVAGE_DECES);

		if (menageCommun == null) {
			reopenRapportsEntreTiers(tiers, date);

			/*
			 * Réouverture des fors du célibataire/divorcé/séparé
			 */
			tiersService.reopenForsClosedAt(date, MotifFor.VEUVAGE_DECES, tiers);

			// [UNIREG-823] Réouverture de la situation de famille sur le tiers
			reopenSituationFamille(date, tiers);
		}
		else {

			/*
			 * Annulation des fors ouverts sur le conjoint
			 */
			if (conjoint != null) {
				cancelForsOpenedSince(date.getOneDayAfter(), conjoint, MotifFor.VEUVAGE_DECES);
			}

			/*
			 * Réouverture des rapports APPARTENANCE_MENAGE & ceux des membres du ménage
			 */
			reopenRapportsEntreTiers(menageCommun, date);

			/*
			 * Réouverture des fors du ménage commun
			 */
			tiersService.reopenForsClosedAt(date, MotifFor.VEUVAGE_DECES, menageCommun);

			/*
			 * [UNIREG-823] Réouverture de la situation de famille actuelle sur tous les membres du ménage
			 */
			reopenSituationFamille(date, tiers);

			if (conjoint != null) {
				cancelSituationFamillePP(date.getOneDayAfter(), conjoint);
				reopenSituationFamille(date, conjoint);
			}

			reopenSituationFamille(date, menageCommun);
		}

		// on reset le flag de traitement de majorité, par mesure de précaution
		tiers.setMajoriteTraitee(Boolean.FALSE);
	}

	/**
	 * Réouvre les rapports entre tiers fermés à cette date.
	 * Si le tiers est un menage commun, réouvre les rapports des membres de celui-ci.
	 *
	 * @param tiers
	 * @param date
	 */
	private void reopenRapportsEntreTiers(Tiers tiers, RegDate date) {

		if (tiers instanceof MenageCommun) {
			MenageCommun menageCommun = (MenageCommun) tiers;

			/*
			 * Réouverture des rapports sur les membres du ménage
			 */
			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menageCommun, date);
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(couple.getMenage())) {
				reopenRapportsEntreTiers(pp, date);
			}
		}
		else {
			/*
			 * Réouverture des rapports fermés lors du décès
			 */
			List<RapportEntreTiers> rapportAOuvrir = new ArrayList<RapportEntreTiers>();
			/*
			 * Réouverture des rapports du tiers
			 */
			for (RapportEntreTiers rapport : tiers.getRapportsSujet()) {
				if (!rapport.isAnnule() && date.equals(rapport.getDateFin())) {

					// duplique le rapport et réouvre le nouveau
					RapportEntreTiers nouRapport = reopenRapportEntreTiers(rapport);
					nouRapport.setSujetId(rapport.getSujetId());
					nouRapport.setObjetId(rapport.getObjetId());
					// ajout à la liste des rapports à ajouter
					rapportAOuvrir.add(nouRapport);
				}
			}
			for (RapportEntreTiers rapport : rapportAOuvrir) {
				// assigner le nouveau rapport au tiers
				final Tiers sujet = tiersDAO.get(rapport.getSujetId());
				final Tiers objet = tiersDAO.get(rapport.getObjetId());
				tiersService.addRapport(rapport, sujet, objet);
			}
		}
	}

	public ValidationResults validateVeuvage(PersonnePhysique veuf, RegDate date) {
		final ValidationResults results = new ValidationResults();
		/*
		 * Récupération du ménage du veuf
		 */
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(veuf, date);
		if (couple == null || couple.getMenage() == null || (couple.getConjoint(veuf) != null && couple.getConjoint(veuf).isConnuAuCivil())) {
			/*
			 * Normalement le veuvage ne doit s'appliquer qu'aux personnes mariées seules.
			 */
			results.addError("Le veuvage ne peut s'appliquer qu'à une personne mariée seule dans le civil.");
		}

		if (!results.hasErrors())
		{
			/*
			 * Vérifications de la non existence de fors après la date de veuvage
			 */

			// Pour le ménage (si existant)
			final EnsembleTiersCouple dernierCouple = tiersService.getEnsembleTiersCouple(veuf, null);
			if (dernierCouple != null && dernierCouple.getMenage() != null) {
				final ForFiscalPrincipal ffpMenage = dernierCouple.getMenage().getDernierForFiscalPrincipal();
				if (ffpMenage != null && ffpMenage.getDateDebut().isAfter(date)) {
					results.addError("Le ménage du veuf possède un for fiscal principal ouvert après la date de décès" );
				}
			}
			else {

				// Pour le veuf
				final ForFiscalPrincipal ffpDefunt = veuf.getDernierForFiscalPrincipal();
				if (ffpDefunt != null && ffpDefunt.getDateDebut().isAfter(date)) {
					results.addError("Le veuf possède un for fiscal principal ouvert après la date de décès" );
				}
			}
		}

		return results;
	}

	public void veuvage(PersonnePhysique veuf, RegDate date, String remarque, Long numeroEvenement) {

		/*
		 * Récupération de l'ensemble veuf-menageCommun
		 */
		final EnsembleTiersCouple menageComplet = tiersService.getEnsembleTiersCouple(veuf, date);
		final MenageCommun menage = menageComplet.getMenage();
		final PersonnePhysique conjointDecede = menageComplet.getConjoint(veuf);

		final ForFiscalPrincipal forMenage = menage.getForFiscalPrincipalAt(null);

		/*
		 * Sauvegarde des fors secondaires et autre élément imposable du ménage pour réouverture sur le veuf
		 */
		final ForsParType forsParType = menage.getForsParType(false);
		final List<ForFiscalSecondaire> forsSecondaires = forsParType.secondaires;
		final List<ForFiscalAutreElementImposable> forsAutreElement = forsParType.autreElementImpot;

		// Fermeture des fors du MenageCommun
		tiersService.closeAllForsFiscaux(menage, date, MotifFor.VEUVAGE_DECES);

		// Fermeture des RapportEntreTiers du menage
		tiersService.closeAppartenanceMenage(veuf, menage, date);

		// [UNIREG-2242] Il peut y avoir un conjoint (non-habitant) pour lequel le civil nous aurait envoyé un événement
		// de veuvage (car le non-habitant ne serait pas connu dans le civil)
		if (conjointDecede != null) {
			tiersService.closeAppartenanceMenage(conjointDecede, menage, date);

			// on ne renseigne la date de décès que si elle n'est pas renseignée du tout 
			if (!conjointDecede.isHabitant() && conjointDecede.getDateDeces() == null) {
				conjointDecede.setDateDeces(date);
			}
		}

		if (forMenage != null) {
			final ModeImposition imposition = forMenage.getModeImposition();
			final Integer noOfsEtendu = forMenage.getNumeroOfsAutoriteFiscale();
			final TypeAutoriteFiscale typeAutoriteFiscale = forMenage.getTypeAutoriteFiscale();

			/*
			 * Résolution du mode d'imposition du veuf
			 */
			final ModeImpositionResolver decesResolver = new DecesModeImpositionResolver(tiersService, numeroEvenement);
			/*
			 * ouverture d'un nouveau for fiscal sur le veuf
			 */
			createForFiscalPrincipal(date.getOneDayAfter(), veuf, imposition, noOfsEtendu, typeAutoriteFiscale, MotifFor.VEUVAGE_DECES, decesResolver, true);
			/*
			 * Réouverture des fors secondaire et autre element sur le veuf
			 */
			createForsSecondaires(date.getOneDayAfter(), veuf, forsSecondaires, MotifFor.VEUVAGE_DECES);
			createForsAutreElementImpossable(date.getOneDayAfter(), veuf, forsAutreElement, MotifFor.VEUVAGE_DECES);
		}

		if (!StringUtils.isBlank(remarque)) {
			veuf.setRemarque((veuf.getRemarque() != null ? veuf.getRemarque() : "") + remarque);
			menage.setRemarque((menage.getRemarque() != null ? menage.getRemarque() : "") + remarque);
		}

		if (!isVeuvageApresDeces(veuf, menage, date)) {
			// pas besoin de mettre à jour la situation de famille si le veuvage arrive apres décès
			updateSituationFamilleVeuvage(veuf, date);
		}
	}

	private boolean isVeuvageApresDeces(PersonnePhysique veuf, MenageCommun menage, RegDate date) {

		final ForFiscalPrincipal forMenage = menage.getForFiscalPrincipalAt(null);
		if (forMenage == null) {
			final ForFiscalPrincipal dernierForMenage = menage.getDernierForFiscalPrincipal();
			final ForFiscalPrincipal forCourantVeuf = veuf.getForFiscalPrincipalAt(null);

			if (dernierForMenage != null && date.equals(dernierForMenage.getDateFin()) && MotifFor.VEUVAGE_DECES == dernierForMenage.getMotifFermeture()
					&& forCourantVeuf == null ) {
				return true;
			}
		}

		return false;
	}

	private void updateSituationFamilleVeuvage(PersonnePhysique veuf, RegDate date) {
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(veuf, date);
		final MenageCommun menage = couple.getMenage();
		doUpdateSituationFamilleDeces(null, veuf, menage, date);
	}

	public void annuleVeuvage(PersonnePhysique tiers, RegDate date, Long numeroEvenement) {

		/*
		 * Recherche du dernier ménage
		 */
		final RapportEntreTiers dernierRapportMenage = tiers.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		Assert.notNull(dernierRapportMenage, "Le dernier ménage n'a pas été trouvé");
		Assert.isEqual(dernierRapportMenage.getDateFin(), date, "La date du dernier rapport entre tiers n'est pas la même que celle de l'événement");
		final MenageCommun menageCommun = (MenageCommun) tiersDAO.get(dernierRapportMenage.getObjetId());
		final EnsembleTiersCouple ensembleTiersCouple = getTiersService().getEnsembleTiersCouple(menageCommun, dernierRapportMenage.getDateDebut());
		if (!ensembleTiersCouple.contient(tiers)) {
			 throw new EvenementCivilHandlerException("Le dernier ménage n'est pas composé du tiers n° " + FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()));
		}
		final PersonnePhysique conjointDecede = ensembleTiersCouple.getConjoint(tiers);

		final RegDate lendemain = date.getOneDayAfter();
		final ForFiscalPrincipal ffp = tiers.getForFiscalPrincipalAt(lendemain);

		/*
		 * Fermeture des fors ouverts sur le tiers
		 */
		cancelForsOpenedSince(lendemain, tiers, MotifFor.VEUVAGE_DECES);

		if (ffp != null && MotifFor.VEUVAGE_DECES == ffp.getMotifOuverture()) {
			/*
			 * Réouverture du rapport tiers-ménage
			 */
			final RapportEntreTiers rapport = reopenRapportEntreTiers(dernierRapportMenage);
			tiersService.addRapport(rapport, tiers, menageCommun);

			// [UNIREG-1422] Traitement du conjoint décédé inconnu du civil
			if (conjointDecede != null) {
				final RapportEntreTiers rapportDecede = conjointDecede.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				final RapportEntreTiers nouveauRapportDecede = reopenRapportEntreTiers(rapportDecede);
				tiersService.addRapport(nouveauRapportDecede, conjointDecede, menageCommun);

				if (!conjointDecede.isHabitant() && conjointDecede.getDateDeces() != null) {
					conjointDecede.setDateDeces(null);
				}
			}

			/*
			 * Réouverture des fors du ménage commun
			 */
			tiersService.reopenForsClosedAt(date, MotifFor.VEUVAGE_DECES, menageCommun);
		}
		else {
			throw new EvenementCivilHandlerException("Opération erronée: aucun veuvage à la date " + RegDateHelper.dateToDisplayString(date));
		}

		cancelSituationFamillePP(lendemain, tiers);
		reopenSituationFamille(date, menageCommun);
	}

}
