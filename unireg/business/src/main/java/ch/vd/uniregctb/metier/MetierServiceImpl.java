package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.modeimposition.DecesModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.DivorceModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.FusionMenagesResolver;
import ch.vd.uniregctb.metier.modeimposition.MariageModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolver.Imposition;
import ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolverException;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.Tiers.ForsParType;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
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
	private RemarqueDAO remarqueDAO;

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

	public void setRemarqueDAO(RemarqueDAO remarqueDAO) {
		this.remarqueDAO = remarqueDAO;
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
			createForsSecondairesApresMariage(dateEffective, menageCommun, ffsPrincipal, ffsConjoint, motifOuverture, null, null);
			createForsAutreElementImpossable(dateEffective, menageCommun, ffaeiPrincipal, motifOuverture, null, null);
			createForsAutreElementImpossable(dateEffective, menageCommun, ffaeiConjoint, motifOuverture, null, null);
		}

		if (!StringUtils.isBlank(remarque)) {
			addRemarque(principal, remarque);
			if (conjoint != null) {
				addRemarque(conjoint, remarque);
			}
			addRemarque(menageCommun, remarque);
		}

		updateSituationFamilleMariage(menageCommun, dateEffective, etatCivilFamille);

		return menageCommun;
	}

	private static final class ForSecondaireWrapper {
		public final ForFiscalSecondaire forFiscal;

		private ForSecondaireWrapper(ForFiscalSecondaire forFiscal) {
			this.forFiscal = forFiscal;
		}

		private static <T> boolean areEqual(T o1, T o2) {
			if (o1 == o2) {
				return true;
			}
			if (o1 == null || o2 == null) {
				return false;
			}
			return o1.equals(o2);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final ForSecondaireWrapper that = (ForSecondaireWrapper) o;

			return forFiscal.getTypeAutoriteFiscale() == that.forFiscal.getTypeAutoriteFiscale() &&
					forFiscal.getMotifFermeture() == that.forFiscal.getMotifFermeture() &&
					areEqual(forFiscal.getNumeroOfsAutoriteFiscale(), that.forFiscal.getNumeroOfsAutoriteFiscale()) &&
					areEqual(forFiscal.getDateDebut(), that.forFiscal.getDateDebut()) &&
					areEqual(forFiscal.getDateFin(), that.forFiscal.getDateFin());
		}

		@Override
		public int hashCode() {
			final RegDate dateOuverture = forFiscal.getDateDebut();
			final RegDate dateFermeture = forFiscal.getDateFin();
			final MotifFor motifFermeture = forFiscal.getMotifFermeture();
			final TypeAutoriteFiscale typeAutoriteFiscale = forFiscal.getTypeAutoriteFiscale();
			final Integer noOfsAutoriteFiscale = forFiscal.getNumeroOfsAutoriteFiscale();

			int result = noOfsAutoriteFiscale != null ? noOfsAutoriteFiscale.hashCode() : 0;
			result = 31 * result + (typeAutoriteFiscale != null ? typeAutoriteFiscale.hashCode() : 0);
			result = 31 * result + (dateOuverture != null ? dateOuverture.hashCode() : 0);
			result = 31 * result + (dateFermeture != null ? dateFermeture.hashCode() : 0);
			result = 31 * result + (motifFermeture != null ? motifFermeture.hashCode() : 0);
			return result;
		}
	}

	/**
	 * [UNIREG-2323] si les deux membres du couple ont deux fois les mêmes fors secondaires, il ne faut les recopier qu'une seule fois sur le ménage
	 */
	private void createForsSecondairesApresMariage(RegDate date, MenageCommun menage, List<ForFiscalSecondaire> forsSecondairesDuPrincipal, List<ForFiscalSecondaire> forsSecondairesDuConjoint, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture) {
		final boolean principalHasFors = forsSecondairesDuPrincipal != null && forsSecondairesDuPrincipal.size() > 0;
		final boolean conjointHasFors = forsSecondairesDuConjoint != null && forsSecondairesDuConjoint.size() > 0;
		if (principalHasFors && conjointHasFors) {
			// les deux ont des fors secondaires : on va éliminer les doublons
			final int sumOfSizes = forsSecondairesDuPrincipal.size() + forsSecondairesDuConjoint.size();
			final Set<ForSecondaireWrapper> set = new HashSet<ForSecondaireWrapper>(sumOfSizes);
			for (ForFiscalSecondaire ffs : forsSecondairesDuPrincipal) {
				set.add(new ForSecondaireWrapper(ffs));
			}
			for (ForFiscalSecondaire ffs : forsSecondairesDuConjoint) {
				set.add(new ForSecondaireWrapper(ffs));
			}
			if (set.size() == sumOfSizes) {
				// pas de doublons : on ne va pas s'embêter à créer une liste supplémentaire
				createForsSecondaires(date, menage, forsSecondairesDuPrincipal, motifOuverture, dateFermeture, motifFermeture);
				createForsSecondaires(date, menage, forsSecondairesDuConjoint, motifOuverture, dateFermeture, motifFermeture);
			}
			else {
				final List<ForFiscalSecondaire> fors = new ArrayList<ForFiscalSecondaire>(set.size());
				for (ForSecondaireWrapper wrapper : set) {
					fors.add(wrapper.forFiscal);
				}
				createForsSecondaires(date, menage, fors, motifOuverture, dateFermeture, motifFermeture);
			}
		}
		else if (principalHasFors) {
			createForsSecondaires(date, menage, forsSecondairesDuPrincipal, motifOuverture, dateFermeture, motifFermeture);
		}
		else if (conjointHasFors) {
			createForsSecondaires(date, menage, forsSecondairesDuConjoint, motifOuverture, dateFermeture, motifFermeture);
		}
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
		ch.vd.uniregctb.type.EtatCivil etatCivilPrincipal = situationFamilleService.getEtatCivil(principal, date, true);
		if (!principal.isHabitantVD()) {
			auMoinsUnNonHabitant = true;
		}
		PersonnePhysique conjoint = couple.getConjoint();
		ch.vd.uniregctb.type.EtatCivil etatCivilConjoint = null;
		if (conjoint != null) {
			etatCivilConjoint = situationFamilleService.getEtatCivil(conjoint, date, true);
			if (!conjoint.isHabitantVD()) {
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
			situationFamilleMenage.setContribuablePrincipalId(principal.getId());
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
		createForsSecondaires(date, menage, ffsPP, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, null);
		createForsAutreElementImpossable(date, menage, ffaeiPP, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, null);

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
			addRemarque(menage, remarque);
			addRemarque(pp, remarque);
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

		//UNIREG-27771
		//Si le ménage à annuler possède encore des fors ou des declarations, on doit remonter une exception
		if(isTiersActifFiscalement(autreMenage)){
			final String messageErreur = String.format("le ménage n°%s  du contribuable n°%s  ne peut pas être annulé car il possède un for actif ou une Déclaration active",
					FormatNumeroHelper.numeroCTBToDisplay(autreMenage.getNumero()),FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()));
			throw new EvenementCivilHandlerException(messageErreur);
		}
		else{
			// annulation du ménage n'ayant plus d'intérêt
			tiersService.annuleTiers(autreMenage);

		}

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
		createForsSecondaires(dateDebut, menageChoisi, ffsAutreMenage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, null);
		createForsAutreElementImpossable(dateDebut, menageChoisi, ffaeiAutreMenage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, null);

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
			addRemarque(principal, remarque);
			if (conjoint != null) {
				addRemarque(conjoint, remarque);
			}
			addRemarque(menageChoisi, remarque);
			addRemarque(autreMenage, remarque);
		}

		return menageChoisi;
	}

	/**
	 * Permet de savoir si un tiers possède un for ou une déclaration d'impot
	 *
	 * @param tiers
	 * @return
	 */
	private boolean isTiersActifFiscalement(Tiers tiers) {
		if (tiers.getForFiscalPrincipalAt(null) != null || tiers.getDeclarationActive(null) != null) {
	 	    return true;
		}
		return false;
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
		reouvreForsFermesPourMariage(principal, date);
		// même chose pour le conjoint
		if (conjoint != null) {
			reouvreForsFermesPourMariage(conjoint, date);
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
	 * Ré-ouvre les fors fermés lors du mariage (traitement de l'annulation du mariage)
	 * @param pp personne physique sur laquelle le ou les fors doivent être ré-ouverts
	 * @param date date du mariage que l'on annule maintenant
	 */
	private void reouvreForsFermesPourMariage(PersonnePhysique pp, RegDate date) {
		final List<ForFiscalPrincipal> forsFiscaux = pp.getForsFiscauxPrincipauxOuvertsApres(date);

		// s'il y a au moins un for principal non-annulé ouvert après la date du mariage
		// que l'on annule, c'est qu'il y a un gros problème...
		for (ForFiscalPrincipal ffp : forsFiscaux) {
			if (!ffp.isAnnule()) {
				throw new EvenementCivilHandlerException(String.format("Le tiers %s a déjà un for ouvert après la date du mariage annulé", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero())));
			}
		}

		// donc ici, tous les fors de la collection forsFiscaux sont annulés...

		final int nombreFors = forsFiscaux.size();
		if (nombreFors == 1 && isAnnuleEtOuvertPourMotifArrivee(forsFiscaux.get(0))) {
			// [UNIREG-1157] si le seul for fiscal principal trouvé après la date de mariage est un for annulé, ouvert, et dont
			// le motif d'ouverture était un motif d'arrivée, c'est celui-là que l'on ouvre (on suppose alors que
			// ce for a été annulé parce que le mariage a été connu après l'arrivée, alors qu'il s'était produit avant)
			tiersService.reopenFor(forsFiscaux.get(0), pp);
		}
		else {
			tiersService.reopenForsClosedAt(date.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, pp);
		}
	}

	public boolean isAnnuleEtOuvertPourMotifArrivee(ForFiscalPrincipal ffp) {
		return ffp.isAnnule() && ffp.getDateFin() == null && isArrivee(ffp.getMotifOuverture());
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
				final ModeImpositionResolver divorceResolver = new DivorceModeImpositionResolver(tiersService, numeroEvenement);

				// on ouvre un nouveau for fiscal pour chaque tiers
				createForFiscalPrincipalApresFermetureMenage(date, principal, forMenage, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, divorceResolver, changeHabitantFlag, numeroEvenement, false);
				if (conjoint != null) {
					// null si marié seul
					createForFiscalPrincipalApresFermetureMenage(date, conjoint, forMenage, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, divorceResolver, changeHabitantFlag, numeroEvenement, false);
				}
			}

			if (!StringUtils.isBlank(remarque)) {
				addRemarque(principal, remarque);
				if (conjoint != null) {
					addRemarque(conjoint, remarque);
				}
				addRemarque(menage, remarque);
			}

			updateSituationFamilleSeparation(menage, date, etatCivilFamille);
		}
	}

	private void updateSituationFamilleSeparation(MenageCommun menageCommun, RegDate date, ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (menageCommun.getSituationsFamilleSorted() != null) {
			for (SituationFamille sf : menageCommun.getSituationsFamilleSorted()) {
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

			ch.vd.uniregctb.type.EtatCivil etatCivilActif = situationFamilleService.getEtatCivil(pp, date, true);

			if (!pp.isHabitantVD()) {
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

				SituationFamille situationFamille = new SituationFamillePersonnePhysique();
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
		reopenRapportsObjetAt(menage, date.getOneDayBefore());

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
				if (!pp.isHabitantVD()) {
					auMoinsUnNonHabitant = true;
				}
				else {
					ch.vd.uniregctb.type.EtatCivil etatCivilActif = situationFamilleService.getEtatCivil(pp, date, true);
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
				situationFamilleMenage.setContribuablePrincipalId(tiersService.getPrincipal(menage).getId());
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
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(principal, date);
		return !couple.estComposeDe(principal, conjoint);
	}

	/**
	 * Crée un nouveau for fiscal principal pour le tiers en suivant les règles pour divorce/séparation.
	 *
	 * @param pp le contribuable pour qui on ouvre le nouveau for.
	 * @param motifOuverture le motif d'ouverture
	 * @param changeHabitantFlag
	 * @param numeroEvenement
	 * @param autoriseSortieDuCantonVersEtranger si <code>true</code>, un for couple vaudois pourra donner naissance à un for individuel HS, si <code>false</code> une erreur est levée dans ce cas
	 * @return le for créé.
	 */
	private ForFiscalPrincipal createForFiscalPrincipalApresFermetureMenage(RegDate date, PersonnePhysique pp, ForFiscalPrincipal forMenage, MotifFor motifOuverture,
	                                                                        ModeImpositionResolver modeImpositionResolver, boolean changeHabitantFlag, Long numeroEvenement,
	                                                                        boolean autoriseSortieDuCantonVersEtranger) {

		try {
			// [UNIREG-2143] prendre en compte l'adresse de domicile pour établissement du for
			final AdresseGenerique adresseDomicile = adresseService.getAdresseFiscale(pp, TypeAdresseFiscale.DOMICILE, date, false);

			final Integer noOfsEtendu;
			final TypeAutoriteFiscale typeAutoriteFiscale;
			if (adresseDomicile == null) {
				// pas d'adresse de domicile connue -> on n'ouvre aucun for
				final String message = String.format("Adresse de domicile du contribuable %s inconnue au %s : pas d'ouverture de for", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), RegDateHelper.dateToDisplayString(date));
				Audit.warn(numeroEvenement, message);

				noOfsEtendu = null;
				typeAutoriteFiscale = null;
			}
			else if (adresseDomicile.getNoOfsPays() == null || adresseDomicile.getNoOfsPays() == ServiceInfrastructureService.noOfsSuisse) {
				// en Suisse
				final CommuneSimple commune = serviceInfra.getCommuneByAdresse(adresseDomicile);
				if (commune != null) {
					noOfsEtendu = commune.getNoOFSEtendu();
					typeAutoriteFiscale = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
				}
				else {
					// pas de commune identifiable -> c'est une erreur grave (adresse suisse sans commune...)
					final String message = String.format("Commune non identifiable pour l'adresse de domicile du contribuable %s au %s", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), RegDateHelper.dateToDisplayString(date));
					throw new EvenementCivilHandlerException(message);
				}
			}
			else {
				// à l'étranger
				noOfsEtendu = adresseDomicile.getNoOfsPays();
				typeAutoriteFiscale = TypeAutoriteFiscale.PAYS_HS;

				// erreur si sortie du canton vers l'étranger n'est pas autorisée
				if (!autoriseSortieDuCantonVersEtranger && forMenage.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {

					// [UNIREG-2143] le départ vers l'étranger n'est interdit que si l'adresse de domicile étrangère a une date de début de validité dans la même année
					// que la date de l'événement (et avant)
					final RegDate dateDebutDomicileHS = adresseDomicile.getDateDebut();
					if (dateDebutDomicileHS != null && dateDebutDomicileHS.year() == date.year()) {
						final String message = String.format("D'après son adresse de domicile, on devrait ouvrir un for hors-Suisse pour le contribuable %s (apparemment parti avant la clôture du ménage, mais dans la même période fiscale) alors que le for du ménage %s était vaudois",
															FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(forMenage.getTiers().getNumero()));
						throw new EvenementCivilHandlerException(message);
					}
				}
			}

			if (noOfsEtendu != null) {
				final Imposition nouveauMode = modeImpositionResolver.resolve(pp, date, forMenage.getModeImposition());
				return tiersService.openForFiscalPrincipal(pp, nouveauMode.getDateDebut(), MotifRattachement.DOMICILE, noOfsEtendu, typeAutoriteFiscale, nouveauMode.getModeImposition(), motifOuverture, changeHabitantFlag);
			}
			else {
				return null;
			}
		}
		catch (AdresseException e) {
			final String message = String.format("Impossible de déterminer l'adresse de domicile du contribuable %s : pas d'ouverture de for", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
			Audit.warn(message);
			return null;
		}
		catch (ModeImpositionResolverException ex) {
			throw new EvenementCivilHandlerException(ex.getMessage(), ex);
		}
		catch (InfrastructureException ex) {
			throw new EvenementCivilHandlerException(ex.getMessage(), ex);
		}
	}


	/**
	 * Ouvre les fors secondaires sur le contribuable.
	 * @param date la date d'ouverture des nouveaux fors secondaires.
	 * @param contribuable le contribuable.
	 * @param forsSecondaires liste de fors secondaires à recopier (ceux qui sont valides à la veille de la date donnée, et fermés depuis avec le motif qui sera utilisé comme motif d'ouverture des nouveaux fors)
	 * @param motifOuverture motif d'ouverture à assigner aux nouveaux fors.
	 * @param dateFermeture (nullable) si les fors secondaires doivent être créés fermés, date de fermeture
	 * @param motifFermeture (nullable) si les fors secondaires doivent être créés fermés, motif de fermeture
	 */
	private void createForsSecondaires(RegDate date, Contribuable contribuable, List<ForFiscalSecondaire> forsSecondaires, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture) {
		for (ForFiscalSecondaire forFiscalSecondaire : forsSecondaires) {
			if (forFiscalSecondaire.isValidAt(date.getOneDayBefore()) && forFiscalSecondaire.getMotifFermeture() == motifOuverture) {
				final ForFiscalSecondaire ffs = tiersService.openForFiscalSecondaire(contribuable,
																					date, forFiscalSecondaire.getMotifRattachement(),
																					forFiscalSecondaire.getNumeroOfsAutoriteFiscale(),
																					forFiscalSecondaire.getTypeAutoriteFiscale(), motifOuverture);
				if (ffs != null && dateFermeture != null && motifFermeture != null) {
					tiersService.closeForFiscalSecondaire(contribuable, ffs, dateFermeture, motifFermeture);
				}
			}
		}
	}

	/**
	 * Ouvre les fors de type autre élément imposable sur le contribuable.
	 * @param date la date d'ouverture des nouveaux fors secondaires.
	 * @param contribuable le contribuable.
	 * @param fors liste de fors autre élément imposable à recopier (ceux qui sont valides à la veille de la date donnée, et fermés depuis avec le motif qui sera utilisé comme motif d'ouverture des nouveaux fors)
	 * @param motifOuverture motif d'ouverture assigner aux nouveaux fors.
	 * @param dateFermeture (nullable) si les fors secondaires doivent être créés fermés, date de fermeture
	 * @param motifFermeture (nullable) si les fors secondaires doivent être créés fermés, motif de fermeture
	 */
	private void createForsAutreElementImpossable(RegDate date, Contribuable contribuable, List<ForFiscalAutreElementImposable> fors, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture) {
		for (ForFiscalAutreElementImposable forFiscalAutreElementImposable : fors) {
			if (forFiscalAutreElementImposable.isValidAt(date.getOneDayBefore()) && forFiscalAutreElementImposable.getMotifFermeture() == motifOuverture) {
				final ForFiscalAutreElementImposable ffaei = tiersService.openForFiscalAutreElementImposable(contribuable, forFiscalAutreElementImposable.getGenreImpot(),
																											date, forFiscalAutreElementImposable.getMotifRattachement(),
																											forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale(),
																											forFiscalAutreElementImposable.getTypeAutoriteFiscale(), motifOuverture);
				if (ffaei != null && dateFermeture != null && motifFermeture != null) {
					tiersService.closeForFiscalAutreElementImposable(contribuable, ffaei, dateFermeture, motifFermeture);
				}
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
		veuvageDeces(null, defunt, date, remarque, numeroEvenement);
	}

	private void addRemarque(Tiers tiers, String remarque) {

		final Remarque r = new Remarque();
		r.setTexte(remarque);
		r.setTiers(tiers);
		
		remarqueDAO.save(r);
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
				if (veuf.isHabitantVD()) {
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
					// (à condition qu'il soit bien survivant !)
					final RegDate dateDecesVeuf = tiersService.getDateDeces(veuf);
					if (dateDecesVeuf == null || dateDecesVeuf.isAfterOrEqual(lendemainDeces)) {
						final SituationFamille situationFamille = new SituationFamillePersonnePhysique();
						situationFamille.setDateDebut(lendemainDeces);
						situationFamille.setDateFin(dateDecesVeuf);
						situationFamille.setEtatCivil(ch.vd.uniregctb.type.EtatCivil.VEUF);
						situationFamille.setNombreEnfants(nombreEnfants);
						situationFamilleService.addSituationFamille(situationFamille, veuf);
					}
				}
			}
		}
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

		// [UNIREG-2653] en cas d'annulation de décès, l'ancien décédé peut
		// repasser habitant s'il n'est pas décédé dans le civil (et qu'il réside dans le canton, évidemment)
		if (tiers.isConnuAuCivil() && !tiers.isHabitantVD()) {
			final Individu individu = serviceCivilService.getIndividu(tiers.getNumeroIndividu(), date.year());
			if (individu.getDateDeces() == null && tiersService.isDomicileVaudois(tiers, null)) {
				// il n'est pas mort au civil et réside dans le canton -> retour à la case "habitant"
				tiersService.changeNHenHabitant(tiers, individu.getNoTechnique(), date);
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
			reopenRapportsSujetAt(tiers, date);
		}
	}

	/**
	 * Réouvre tous les rapports-sujet qui sont fermés à la date spécifiée.
	 *
	 * @param rapports
	 * @param date  la date de fermeture des rapports qu'il faut réouvrir.
	 */
	private void reopenRapportsAt(Set<RapportEntreTiers> rapports, RegDate date) {

		final List<RapportEntreTiers> rapportsAOuvrir = new ArrayList<RapportEntreTiers>();
		final List<RapportEntreTiers> rapportsAAnnuler = new ArrayList<RapportEntreTiers>();

		// On analyse les rapports en question
		for (RapportEntreTiers rapport : rapports) {
			if (!rapport.isAnnule() && rapport.getDateFin() == date) {
				// duplique le rapport et réouvre le nouveau
				RapportEntreTiers nouveauRapport = rapport.duplicate();
				nouveauRapport.setDateFin(null);
				// ajout à la liste des rapports à ajouter
				rapportsAOuvrir.add(nouveauRapport);
				rapportsAAnnuler.add(rapport);
			}
		}

		// On ajoute tous les nouveaux rapports
		for (RapportEntreTiers rapport : rapportsAOuvrir) {
			final Tiers sujet = tiersDAO.get(rapport.getSujetId());
			final Tiers objet = tiersDAO.get(rapport.getObjetId());
			tiersService.addRapport(rapport, sujet, objet);
		}

		// On annule tous les anciens rapports (maintenant remplacés par des nouveaux rapports réouverts)
		for (RapportEntreTiers rapport : rapportsAAnnuler) {
			rapport.setAnnule(true);
		}
	}

	/**
	 * Réouvre tous les rapports-objet qui sont fermés à la date spécifiée.
	 *
	 * @param tiers le tiers dont on veut réouvrir des rapports objet
	 * @param date  la date de fermeture des rapports qu'il faut réouvrir.
	 */
	private void reopenRapportsObjetAt(Tiers tiers, RegDate date) {
		reopenRapportsAt(tiers.getRapportsObjet(), date);
	}

	/**
	 * Réouvre tous les rapports-sujet qui sont fermés à la date spécifiée.
	 *
	 * @param tiers le tiers dont on veut réouvrir des rapports sujet
	 * @param date  la date de fermeture des rapports qu'il faut réouvrir.
	 */
	private void reopenRapportsSujetAt(Tiers tiers, RegDate date) {
		reopenRapportsAt(tiers.getRapportsSujet(), date);
	}

	public ValidationResults validateVeuvage(PersonnePhysique veuf, RegDate date) {
		final ValidationResults results = new ValidationResults();
		/*
		 * Récupération du ménage du veuf
		 */
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(veuf, date);
		if (couple != null && couple.getConjoint(veuf) != null && couple.getConjoint(veuf).isConnuAuCivil()) {
			/*
			 * Normalement le veuvage ne doit s'appliquer qu'aux personnes mariées seules.
			 */
			results.addError("Le veuvage ne peut s'appliquer qu'à une personne mariée seule dans le civil.");
		}

		if (!results.hasErrors())
		{
			if (couple != null) {

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
			else {
				// [UNIREG-1623] Il n'y a un problème que si le veuf n'a pas de for principal existant à la date de veuvage
				final ForFiscalPrincipal forVeuf = veuf.getForFiscalPrincipalAt(date);
				if (forVeuf == null) {
					results.addError("L'individu veuf n'a ni couple connu ni for valide à la date de veuvage : problème d'assujettissement ?");
				}
			}
		}

		return results;
	}

	/**
	 * Un veuvage et un décès sont conceptuellement la même chose, ce n'est juste pas le même conjoint que l'on met en avant :
	 * le veuf d'un côté, le défunt de l'autre...
	 * @param veuf (nullable) la personne physique survivante
	 * @param defunt (nullable) la personne physique décédée
	 * @param date date du décès / veuvage
	 * @param remarque (nullable) remarque à ajouter aux tiers couple et personnes physiques
	 * @param numeroEvenement (nullable) numéro d'événement si le traitement fait suite à l'arrivée d'un événement civil
	 */
	private void veuvageDeces(PersonnePhysique veuf, PersonnePhysique defunt, RegDate date, String remarque, Long numeroEvenement) {

		//
		// on récupère d'abord le ménage commun et ses éléments constitutifs qui nous manqueraient encore
		//
		final EnsembleTiersCouple menageComplet;
		if (defunt != null) {
			menageComplet = tiersService.getEnsembleTiersCouple(defunt, date);
			if (menageComplet != null) {
				if (veuf != null) {
					// on vérifie que c'est le bon couple...
					final PersonnePhysique conjointDuDefunt = menageComplet.getConjoint(defunt);
					if (veuf != conjointDuDefunt) {
						final String msg = String.format("Le ménage courant (%s) du défunt (%s) ne fait pas référence au veuf (%s)",
														FormatNumeroHelper.numeroCTBToDisplay(menageComplet.getMenage().getNumero()),
														FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero()),
														FormatNumeroHelper.numeroCTBToDisplay(veuf.getNumero()));
						throw new EvenementCivilHandlerException(msg);
					}
				}
				else {
					veuf = menageComplet.getConjoint(defunt);
				}
			}
		}
		else {
			menageComplet = tiersService.getEnsembleTiersCouple(veuf, date);
			if (menageComplet != null) {
				defunt = menageComplet.getConjoint(veuf);
			}
		}

		// si le défunt avait déjà un for qui s'ouvre au lendemain de sa date de décès pour le motif VEUVAGE,
		// alors c'est que son conjoint est également décédé le même jour que lui, mais on ne le savait pas
		// encore au moment où on a traité son décès à lui...
		if (defunt != null) {
			final RegDate lendemainDeces = date.getOneDayAfter();
			final ForFiscalPrincipal ffp = defunt.getForFiscalPrincipalAt(lendemainDeces);
			if (ffp != null && RegDateHelper.equals(lendemainDeces, ffp.getDateDebut()) && ffp.getMotifOuverture() == MotifFor.VEUVAGE_DECES) {

				// il faut alors simplement annuler le for fiscal principal
				Audit.info(numeroEvenement, "Les deux conjoints sont décédés le même jour : annulation du for fiscal du deuxième défunt");
				tiersService.annuleForFiscal(ffp, true);

				defunt.setBlocageRemboursementAutomatique(true);

				// le deuxième défunt doit avoir une situation de famille VEUF
				final SituationFamille sf = defunt.getSituationFamilleAt(lendemainDeces);
				if (sf != null) {
					situationFamilleService.annulerSituationFamilleSansRouvrirPrecedente(sf.getId());
				}
				return;
			}
		}

		final boolean wasDefuntHabitant = defunt != null && defunt.isHabitantVD();

		// [UNIREG-2653] un habitant décédé doit passer non-habitant
		if (wasDefuntHabitant) {
			tiersService.changeHabitantenNH(defunt);
		}
		if (defunt != null && (!wasDefuntHabitant || numeroEvenement == null)) {
			// si décès via IHM on surcharge la date de décès du civil
			defunt.setDateDeces(date);
		}

		// ajout des remarques
		if (!StringUtils.isBlank(remarque)) {
			if (defunt != null) {
				addRemarque(defunt, remarque);
			}
			if (veuf != null) {
				addRemarque(veuf, remarque);
			}
			if (menageComplet != null) {
				addRemarque(menageComplet.getMenage(), remarque);
			}
		}

		if (menageComplet != null) {
			final MenageCommun menage = menageComplet.getMenage();
			final ForFiscalPrincipal forMenage = menage.getForFiscalPrincipalAt(null);

			//
			// Sauvegarde des fors secondaires et autres éléments imposables du ménage
			// pour ré-ouverture potentielle sur le veuf
			//
			final ForsParType forsParType = menage.getForsParType(false);
			final List<ForFiscalSecondaire> forsSecondaires = forsParType.secondaires;
			final List<ForFiscalAutreElementImposable> forsAutreElement = forsParType.autreElementImpot;

			// fermeture des fors sur le ménage
			Audit.info(numeroEvenement, String.format("Fermeture des fors fiscaux du ménage commun (%d)", menage.getNumero()));
			tiersService.closeAllForsFiscaux(menage, date, MotifFor.VEUVAGE_DECES);

			// fermeture des liens d'appartenance ménage des personnes impliquées
			if (defunt != null) {
				tiersService.closeAllRapports(defunt, date);

				// le blocage des remboursements automatiques se fait normalement à la fermeture du For principal,
				// ce qui est correct en ce qui concerne le ménage, dont les remboursements automatiques doivent
				// être bloqués ; en ce qui concerne le défunt, qui ne possédait pas de For actif, aucun For
				// ne sera fermé ici et le blocage des remboursements automatiques n'aura pas été fait : il faut
				// donc le faire explicitement ici
				defunt.setBlocageRemboursementAutomatique(true);
			}
			if (veuf != null) {
				tiersService.closeAppartenanceMenage(veuf, menage, date);
			}

			// ouverture du for sur le survivant (= le veuf)
			if (veuf != null) {

				// ... sauf évidemment si le veuf est déjà décédé lui-aussi
				final RegDate dateDecesVeuf = tiersService.getDateDeces(veuf);
				if (dateDecesVeuf == null || dateDecesVeuf.isAfter(date)) {

					if (forMenage != null) {

						Audit.info(numeroEvenement, String.format("Ouverture du for fiscal sur le tiers survivant (%d)", veuf.getNumero()));

						final ModeImpositionResolver decesResolver = new DecesModeImpositionResolver(tiersService, numeroEvenement);

						// d'abord le for fiscal principal
						final ForFiscalPrincipal ffp = createForFiscalPrincipalApresFermetureMenage(date.getOneDayAfter(), veuf, forMenage, MotifFor.VEUVAGE_DECES, decesResolver, true, numeroEvenement, true);
						final MotifFor motifFermeture;
						if (dateDecesVeuf != null) {
							motifFermeture = MotifFor.VEUVAGE_DECES;
							tiersService.closeForFiscalPrincipal(ffp, dateDecesVeuf, motifFermeture);
						}
						else {
							motifFermeture = null;
						}

						// puis les fors secondaires et autres...
						if (ffp != null) {
							createForsSecondaires(date.getOneDayAfter(), veuf, forsSecondaires, MotifFor.VEUVAGE_DECES, dateDecesVeuf, motifFermeture);
							createForsAutreElementImpossable(date.getOneDayAfter(), veuf, forsAutreElement, MotifFor.VEUVAGE_DECES, dateDecesVeuf, motifFermeture);
						}
					}
					else {
						// ok, pas de for sur le ménage, mais peut-être faut-il quand-même en ouvrir un sur le veuf...
						Audit.info(numeroEvenement, String.format("Il n'y avait pas de for sur le ménage, aucun for ne sera donc ouvert sur le survivant (%d)", veuf.getNumero()));
					}
				}
			}
		}
		else {
			if (defunt != null) {
				Audit.info(numeroEvenement, "Fermeture des fors fiscaux du défunt");
				tiersService.closeAllForsFiscaux(defunt, date, MotifFor.VEUVAGE_DECES);
				tiersService.closeAllRapports(defunt, date);
			}
		}

		doUpdateSituationFamilleDeces(defunt, veuf, menageComplet != null ? menageComplet.getMenage() : null, date);
	}

	public void veuvage(PersonnePhysique veuf, RegDate date, String remarque, Long numeroEvenement) {
		veuvageDeces(veuf, null, date, remarque, numeroEvenement);
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

			// Réouverture du rapport tiers-ménage
			RapportEntreTiers nouveauRapport = dernierRapportMenage.duplicate();
			nouveauRapport.setDateFin(null);
			tiersService.addRapport(nouveauRapport, tiers, menageCommun);
			dernierRapportMenage.setAnnule(true);

			// [UNIREG-1422] Traitement du conjoint décédé inconnu du civil
			if (conjointDecede != null) {
				final RapportEntreTiers rapportDecede = conjointDecede.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				
				// Réouverture du rapport tiers-ménage
				final RapportEntreTiers nouveauRapportDecede = rapportDecede.duplicate();
				nouveauRapportDecede.setDateFin(null);
				tiersService.addRapport(nouveauRapportDecede, conjointDecede, menageCommun);
				rapportDecede.setAnnule(true);

				if (!conjointDecede.isHabitantVD() && conjointDecede.getDateDeces() != null) {
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
