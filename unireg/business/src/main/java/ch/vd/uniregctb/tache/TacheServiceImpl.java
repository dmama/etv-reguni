package ch.vd.uniregctb.tache;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.type.*;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.Tiers.ForsParType;
import ch.vd.uniregctb.tiers.Tiers.ForsParTypeAt;

/**
 * Service permettant la génération de tâches à la suite
 * d'événements fiscaux
 *
 * @author xcifde
 *
 */
public class TacheServiceImpl implements TacheService {

	//private static final Logger LOGGER = Logger.getLogger(TacheServiceImpl.class);

	private TacheDAO tacheDAO;

	private DeclarationImpotOrdinaireDAO diDAO;

	private DeclarationImpotService diService;

	private ParametreAppService parametres;

	private HibernateTemplate hibernateTemplate;

	private ServiceInfrastructureService serviceInfra;

	private TiersService tiersService;



	private PlatformTransactionManager transactionManager;


	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}

	/**
	 * Genere une tache à partir de la fermeture d'un for principal
	 *
	 * @param contribuable
	 * @param forPrincipal
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisFermetureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forPrincipal) {

		final RegDate dateFermeture = forPrincipal.getDateFin();
		final MotifFor motifFermeture = forPrincipal.getMotifFermeture();

		if (motifFermeture == null) { // les for HC et HS peuvent ne pas avoir de motif de fermeture
			return;
		}

		final ModeImposition modeImposition = forPrincipal.getModeImposition();
		if (modeImposition == ModeImposition.SOURCE) {
			// [UNIREG-1888] Aucune tâche générée pour les contribuables dont le mode d'imposition est "SOURCE"
			return;
		}

		final List<ForFiscal> forsFiscaux = contribuable.getForsFiscauxValidAt(dateFermeture);
		final boolean dernierForFerme = forsFiscaux.size() < 2;

		switch (motifFermeture) {
		case DEPART_HS:
			if (TypeAutoriteFiscale.PAYS_HS.equals(forPrincipal.getTypeAutoriteFiscale())) {
				/*
				 * le for principal est déjà hors-Suisse ! Cela arrive sur certain contribuables (voir par exemple le ctb n°52108102) où le
				 * départ HS a été enregistré comme déménagement VD. A ce moment-là, si le for principal HS est fermé avec le motif départ
				 * HS, il faut ignorer cet événement qui ne correspond à rien puisque le contribuable est déjà HS.
				 */
				return;
			}
			if (dernierForFerme) {
				genereTacheControleDossier(contribuable);

				final Declaration declaration = contribuable.getDeclarationActive(dateFermeture);
				if (declaration == null) {
					genereTacheDepartHorsSuisse(contribuable, dateFermeture);
				}
				genereTachesAnnulationDI(contribuable, dateFermeture.year() + 1);
			}
			break;
		case DEPART_HC:
			/*
			 * Si ce départ a lieu lors de la période courante, une tâche de contrôle du dossier est engendrée,
			 * pour que l’utilisateur puisse vérifier si les fors secondaires éventuels ont bien été enregistrés.
			 */
			if (dateFermeture.year() == RegDate.get().year()) {
				genereTacheControleDossier(contribuable);
			}
			// [UNIREG-1262] la génération de tâches d'annulation de DI doit se faire aussi sur l'année du départ
			genereTachesAnnulationDIDepartHC(contribuable, dateFermeture.year());
			break;
		case VEUVAGE_DECES:
			generateTacheTransmissionDossier(contribuable);
			// [UNIREG-1112] Annule toutes les déclarations d'impôt à partir de l'année de décès (car elles n'ont pas lieu d'être)
			genereTachesAnnulationDI(contribuable, dateFermeture.year() + 1);
			break;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			if (!contribuable.getForsParTypeAt(dateFermeture, false).secondaires.isEmpty()) {
				// [UNIREG-1105] Une tâche de contrôle de dossier (pour répartir les fors secondaires) doit être ouverte sur le couple en cas de séparation
				genereTacheControleDossier(contribuable);
			}
			// [UNIREG-1112] Annule toutes les déclarations d'impôt à partir de l'année de séparation (car elles n'ont pas lieu d'être)
			genereTachesAnnulationDI(contribuable, dateFermeture.year());
			// [UNIREG-1111] Génère une tâche d'émission de DI
			genereTacheEnvoiDISuiteFinAssujettissement(contribuable, dateFermeture, dateFermeture);
			break;
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
			genereTachesAnnulationDI(contribuable, dateFermeture.year());
			break;
		}
	}

	private void generateTacheTransmissionDossier(Contribuable contribuable) {
		final TacheTransmissionDossier tache = new TacheTransmissionDossier(TypeEtatTache.EN_INSTANCE, null, contribuable);
		tacheDAO.save(tache);
	}

	/**
	 * Génère une tâche à partir de la fermeture d'un for secondaire
	 *
	 * @param contribuable
	 * @param forSecondaire
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisFermetureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forSecondaire) {

		final RegDate dateFermeture = forSecondaire.getDateFin();
		final ForsParTypeAt forsAt = contribuable.getForsParTypeAt(dateFermeture, false);

		if (forsAt.principal == null || TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(forsAt.principal.getTypeAutoriteFiscale())) {
			return;
		}

		final ModeImposition modeImposition = forsAt.principal.getModeImposition();
		if (modeImposition == ModeImposition.SOURCE) {
			// [UNIREG-1888] Aucune tâche générée pour les contribuables dont le mode d'imposition est "SOURCE"
			return;
		}

		// S'il s'agit du dernier for ouvert dans le canton, on génère les tâches qui vont bien
		if (forsAt.secondaires.size() == 1) {
			Assert.isEqual(forSecondaire, forsAt.secondaires.get(0));

			MotifFor motifFermeture = forSecondaire.getMotifFermeture();
			switch (motifFermeture) {
			case DEPART_HS:
			case DEPART_HC:
				genereTacheControleDossier(contribuable);
			case FIN_EXPLOITATION:
				if (MotifRattachement.ACTIVITE_INDEPENDANTE.equals(forSecondaire.getMotifRattachement())
						&& TypeAutoriteFiscale.PAYS_HS.equals(forsAt.principal.getTypeAutoriteFiscale())) {
					genereTacheFinActiviteIndependante(contribuable, dateFermeture);
				}
			}
			genereTachesAnnulationDI(contribuable, dateFermeture.year());
		}

	}

	/**
	 * Génère une tache d'envoi de DI suite à la fin de l'activité indépendante d'un contribuable hors-Suisse.
	 *
	 * @param contribuable
	 *            le contribuable concerné
	 * @param dateFinActivite
	 *            la date de fin de l'activité indépendante
	 */
	private void genereTacheFinActiviteIndependante(Contribuable contribuable, RegDate dateFinActivite) {
		genereTacheEnvoiDISuiteFinAssujettissement(contribuable, dateFinActivite, null);
	}

	/**
	 * Génère une tache d'envoi de DI suite à la fin de l'assujettissement d'un contribuable
	 *
	 * @param contribuable
	 *            le contribuable concerné
	 * @param dateFinAssujettissement
	 *            la date de fin d'assujettissement.
	 * @param dateEcheance
	 *            (optionel) la date d'échéance de la déclaration d'impôt à créer.
	 */
	private void genereTacheEnvoiDISuiteFinAssujettissement(Contribuable contribuable, RegDate dateFinAssujettissement, RegDate dateEcheance) {

		final int year = dateFinAssujettissement.year();
		if (year < getPremierePeriodeFiscale()) {
			return;
		}

		// [UNIREG-1102] Calcul de la période d'imposition précise
		RegDate dateDebut;
		RegDate dateFin;
		TypeContribuable typeContribuable;
		TypeDocument typeDocument;
		Qualification qualification;
		TypeAdresseRetour adresseRetour;

		try {
			final List<PeriodeImposition> periodes = PeriodeImposition.determine(contribuable, year);
			if (periodes == null || periodes.isEmpty()) {
				// pas de DI à envoyer
				return;
			}

			PeriodeImposition periode = null;
			for (PeriodeImposition p : periodes) {
				if (p.isValidAt(dateFinAssujettissement)) {
					periode = p;
					break;
				}
			}
			if (periode == null) {
				return;
			}

			dateDebut = periode.getDateDebut();
			dateFin = periode.getDateFin();
			typeContribuable = periode.getTypeContribuable();
			typeDocument = periode.getTypeDocument();
			qualification = periode.getQualification();
			adresseRetour = periode.getAdresseRetour();
		}
		catch (AssujettissementException e) {
			// impossible de calculer la période d'imposition théorique, on ne prend pas de risque et on prend la période maximale,
			// c'est-à-dire l'année complète.
			Audit.warn("Impossible de calculer la période d'imposition théorique du contribuable n°" + contribuable.getNumero()
					+ " pour l'année " + year + " lors de la création d'une tâche d'envoi de déclaration d'impôt:"
					+ " la période d'imposition de la DI s'étendra sur toute l'année.");
			dateDebut = RegDate.get(year, 1, 1);
			dateFin = RegDate.get(year, 12, 31);
			typeContribuable = TypeContribuable.VAUDOIS_ORDINAIRE;
			typeDocument = TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH;
			qualification = null;
			adresseRetour = TypeAdresseRetour.CEDI;
		}

		genereTacheEnvoiDeclarationImpot(contribuable, dateDebut, dateFin, typeContribuable, typeDocument, dateEcheance, qualification, adresseRetour);
	}

	/**
	 * @param dateEcheance
	 * @param adresseRetour
	 */
	private void genereTacheEnvoiDeclarationImpot(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable, TypeDocument typeDocument, RegDate dateEcheance,
	                                              Qualification qualification, TypeAdresseRetour adresseRetour) {

		// d'abord il faut vérifier qu'il n'y a pas de DI du même type déjà émise et non-annulée pour la période considérée
		boolean envoiDejaFait = false;
		boolean demandeControle = false;
		final DeclarationImpotCriteria criteria = new DeclarationImpotCriteria();
		criteria.setContribuable(contribuable.getId());
		criteria.setAnnee(dateDebut.year());

		List<DateRange> rangesEnConflit = null;
		final List<DeclarationImpotOrdinaire> diExistantes = diDAO.find(criteria, true); // pas de flush de la session automatique
		if (diExistantes != null && diExistantes.size() > 0) {

			final DateRangeHelper.Range range = new DateRangeHelper.Range(dateDebut, dateFin);

			// toutes les DI sont renvoyées, même les DI annulées...
			for (DeclarationImpotOrdinaire di : diExistantes) {
				if (!di.isAnnule()) {
					if (DateRangeHelper.equals(range, di)) {
						// les périodes sont les mêmes: la DI a déjà été émise
						envoiDejaFait = true;
					}
					else if (DateRangeHelper.intersect(range, di)) {
						// les périodes s'intersectent, mais ne sont pas égales
						if (tacheDAO.existsTacheAnnulationEnInstanceOuEnCours(contribuable.getNumero(), di.getId())) {
							// ... mais la DI en question doit être annulée -> ok
							continue;
						}
						demandeControle = true;
						if (rangesEnConflit == null) {
							rangesEnConflit = new ArrayList<DateRange>();
						}
						rangesEnConflit.add(di);

						// TODO (jde) point ouvert: faut-il prendre en compte la période globale couverte par les périodes des DI d'une année fiscale?
					}
				}
			}
		}

		if (demandeControle) {
			Assert.notNull(rangesEnConflit);
			Assert.notEmpty(rangesEnConflit);

			final StringBuilder builder = new StringBuilder("La DI émise pour le contribuable ");
			builder.append(contribuable.getNumero());
			builder.append(" pour chacune des périodes suivantes ne correspond pas à la période d'assujettissement attendue : ");
			for (int i = 0 ; i < rangesEnConflit.size() ; ++i) {
				final DateRange range = rangesEnConflit.get(i);
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(DateRangeHelper.toString(range));
			}
			Audit.warn(builder.toString());

			genereTacheControleDossier(contribuable);
		}
		else if (!envoiDejaFait) {
			// [UNIREG-1105] on évite de créer des tâches dupliquées
			if (!tacheDAO.existsTacheEnvoiEnInstanceOuEnCours(contribuable.getNumero(), dateDebut, dateFin)) {
				final TacheEnvoiDeclarationImpot tache = new TacheEnvoiDeclarationImpot(TypeEtatTache.EN_INSTANCE, dateEcheance,
						contribuable, dateDebut, dateFin, typeContribuable, typeDocument, qualification, adresseRetour);
				tacheDAO.save(tache);
			}
		}
	}

	/**
	 * Génère une tache de contrôle de dossier sur un contribuable, en prenant bien soin de vérifier qu'il n'y en a pas déjà une non-traitée
	 *
	 * @param contribuable
	 */
	private void genereTacheControleDossier(Contribuable contribuable) {
		genereTacheControleDossier(contribuable,null);
	}


	/**
	 * Génère une tache de contrôle de dossier sur un contribuable et la lie à une collectivitéAdministrative
	 * en prenant bien soin de vérifier qu'il n'y en a pas déjà une non-traitée
	 *
	 * @param contribuable
	 * @param collectivite
	 */
	private void genereTacheControleDossier(Contribuable contribuable,CollectiviteAdministrative collectivite) {

		if (!tacheDAO.existsTacheEnInstanceOuEnCours(contribuable.getNumero(), TypeTache.TacheControleDossier)) {
			final TacheControleDossier tache = new TacheControleDossier(TypeEtatTache.EN_INSTANCE, null, contribuable);
			//UNIREG-1024 "la tâche de contrôle du dossier doit être engendrée pour l'ancien office d'impôt"
			if (collectivite!=null) {
				tache.setCollectiviteAdministrativeAssignee(collectivite);
			}
			tacheDAO.save(tache);
		}
	}
	/**
	 * Génère une tache d'envoi de DI suite au départ hors-Suisse d'un contribuable.
	 *
	 * @param contribuable
	 *            le contribuable concerné
	 * @param dateDepart
	 *            la date de départ
	 */
	private void genereTacheDepartHorsSuisse(Contribuable contribuable, RegDate dateDepart) {
		genereTacheEnvoiDISuiteFinAssujettissement(contribuable, dateDepart, null);
	}

	/**
	 * Genere une tache à partir de l'ouverture d'un for principal
	 *
	 * @param contribuable
	 * @param forFiscal
	 * @param ancienModeImposition nécessaire en cas d'ouverture de for pour motif "CHGT_MODE_IMPOSITION"
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisOuvertureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal, ModeImposition ancienModeImposition) {

		final MotifFor motifOuverture = forFiscal.getMotifOuverture();
		if (motifOuverture == null) { // les for principaux HC ou HS peuvent ne pas avoir de motif
			return; // rien à faire
		}

		final ModeImposition modeImposition = forFiscal.getModeImposition();
		if (modeImposition == ModeImposition.SOURCE) {
			// les sourciers ne recoivent pas de DIs (et donc pas de dossier non plus)
			return; // pas de tâche sur les sourciers purs [UNIREG-1888]
		}

		switch (motifOuverture) {
		case ARRIVEE_HC:
		case MAJORITE:
		case PERMIS_C_SUISSE:
		case ARRIVEE_HS:
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			generateTacheNouveauDossier(contribuable);
			genereTachesEnvoiDI(contribuable, forFiscal.getDateDebut());
			break;
		case CHGT_MODE_IMPOSITION:
			if (!ancienModeImposition.isAuRole() && modeImposition.isAuRole()) {
				generateTacheNouveauDossier(contribuable);
				genereTachesEnvoiDI(contribuable, forFiscal.getDateDebut());
			}
			break;
		case VEUVAGE_DECES:
			generateTacheNouveauDossier(contribuable);
			// [UNIREG-1112] il faut générer les tâches d'envoi de DIs sur le tiers survivant
			// [UNIREG-1265] Plus de création de tâche de génération de DI pour les décès
			// FIXME (PBO) à modifier quand UNIREG-1198 sera mis en place
			//genereTachesEnvoiDI(contribuable, forFiscal.getDateDebut(), false);
			break;
		case DEMENAGEMENT_VD:
			// si le demenagement arrive dans une periode fiscale échue, une tâche de contrôle
			// du dossier est engendrée pour l’ancien office d’impôt gérant
			// (déterminé par l’ancien for principal) s’il a changé.
			ForFiscalPrincipal ffp = contribuable.getForFiscalPrincipalAt(forFiscal.getDateDebut().getOneDayBefore());
			boolean changementOfficeImpot = false;
			if (ffp != null && !ffp.getNumeroOfsAutoriteFiscale().equals(forFiscal.getNumeroOfsAutoriteFiscale())) {
				changementOfficeImpot = true;
			}

			if (FiscalDateHelper.isEnPeriodeEchue(forFiscal.getDateDebut()) && changementOfficeImpot) {
				OfficeImpot office;
				try {
					office = serviceInfra.getOfficeImpotDeCommune(ffp.getNumeroOfsAutoriteFiscale());
				}
				catch (InfrastructureException e) {
					throw new RuntimeException(e);
				}
				//UNIREG-1886 si l'office nest pas trouvé (cas hors canton, hors suisse) on ne génère pas de tâche
				if (office!=null) {
					CollectiviteAdministrative collectivite = tiersService.getCollectiviteAdministrative(office.getNoColAdm());
					genereTacheControleDossier(contribuable,collectivite);
				}

			}
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTachesDepuisAnnulationDeFor(Contribuable contribuable) {

		final DeclarationImpotCriteria criterion = new DeclarationImpotCriteria();
		criterion.setContribuable(contribuable.getNumero());

		final List<DeclarationImpotOrdinaire> declarations = diDAO.find(criterion, true); // pas de flush de la session automatique
		for (DeclarationImpotOrdinaire di : declarations) {
			try {
				final List<Assujettissement> assuj = Assujettissement.determine(contribuable, di, false);
				if (assuj == null || assuj.isEmpty()) {
					if (!tacheDAO.existsTacheAnnulationEnInstanceOuEnCours(contribuable.getNumero(), di.getId())) {
						final TacheAnnulationDeclarationImpot tacheAnnulationDI = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, null, contribuable, di);
						tacheDAO.save(tacheAnnulationDI);
					}
				}
			} catch (AssujettissementException e) {
				throw new RuntimeException(e);
			}

		}
	}

	private void generateTacheNouveauDossier(Contribuable contribuable) {
		final TacheNouveauDossier tacheNouveauDossier = new TacheNouveauDossier(TypeEtatTache.EN_INSTANCE, null, contribuable);
		tacheDAO.save(tacheNouveauDossier);
	}

	/**
	 * Génère les tâches d'envoi DI de rattrapage dans le cas où un événement d'ouverture de for fiscal arrive tardivement (ex: en 2008,
	 * alors que le for s'est ouvert en 2005).
	 *
	 * @param contribuable
	 *            le contribuable concerné
	 * @param dateDebut
	 *            la date d'ouverture du for fiscal
	 */
	private void genereTachesEnvoiDI(Contribuable contribuable, RegDate dateDebut) {

		final RegDate dateCourante = RegDate.get();
		final int anneeCourante = dateCourante.year();
		final int anneeDebut = Math.max(dateDebut.year(), getPremierePeriodeFiscale());

		for (int annee = anneeDebut; annee < anneeCourante; ++annee) {
			try {
				final List<PeriodeImposition> periodes = PeriodeImposition.determine(contribuable, annee);
				if (periodes != null) {
					for (PeriodeImposition p : periodes) {
						genereTacheEnvoiDeclarationImpot(contribuable, p.getDateDebut(), p.getDateFin(), p.getTypeContribuable(), p
								.getTypeDocument(), null, p.getQualification(), p.getAdresseRetour());

					}
				}
			}
			catch (AssujettissementException e) {
				// impossible de calculer la période d'imposition théorique, on ne prend pas de risque et on prend la période maximale,
				// c'est-à-dire l'année complète.
				Audit.warn("Impossible de calculer la période d'imposition théorique du contribuable n°" + contribuable.getNumero()
						+ " pour l'année " + annee + " lors de la création d'une tâche d'envoi de déclaration d'impôt:"
						+ " la période d'imposition de la DI s'étendra sur toute l'année.");
				genereTacheEnvoiDeclarationImpot(contribuable, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, null, null, TypeAdresseRetour.CEDI);
			}
		}
	}

	/**
	 * Génère les tâches d'annulation DI pour les DIs comprises entre l'année spécifiée et l'année courante (non-comprise)
	 *
	 * @param contribuable
	 * @param forFiscal
	 */
	private void genereTachesAnnulationDI(Contribuable contribuable, final int anneeDebut) {

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		final DeclarationImpotCriteria criterion = new DeclarationImpotCriteria();
		criterion.setContribuable(contribuable.getNumero());
		criterion.setAnneeRange(new Pair<Integer, Integer>(anneeDebut, anneeCourante - 1));

		final int premierePeriodeFiscale = getPremierePeriodeFiscale();
		final List<DeclarationImpotOrdinaire> declarations = diDAO.find(criterion, true); // pas de flush de la session automatique
		for (DeclarationImpotOrdinaire di : declarations) {
			if (di.getPeriode().getAnnee() >= premierePeriodeFiscale) {
				// [UNIREG-1105] on évite de créer des tâches dupliquées
				if (!tacheDAO.existsTacheAnnulationEnInstanceOuEnCours(contribuable.getNumero(), di.getId())) {
					final TacheAnnulationDeclarationImpot tacheAnnulationDI = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, null, contribuable, di);
					tacheDAO.save(tacheAnnulationDI);
				}
			}
		}
	}


	/**
	 * Pour un départ HC, génère les tâches d'annulation DI pour les DIs déposées ou échues comprises entre l'année spécifiée et l'année courante (non-comprise)
	 *
	 * @param contribuable
	 * @param anneeDebut
	 * @param etatDeclaration
	 */
	private void genereTachesAnnulationDIDepartHC(Contribuable contribuable, final int anneeDebut) {

		final RegDate aujourdhui = RegDate.get();
		final int anneeCourante = aujourdhui.year();

		final DeclarationImpotCriteria criterion = new DeclarationImpotCriteria();
		criterion.setContribuable(contribuable.getNumero());
		criterion.setAnneeRange(new Pair<Integer, Integer>(anneeDebut, anneeCourante - 1));

		final int premierePeriodeFiscale = getPremierePeriodeFiscale();
		final List<DeclarationImpotOrdinaire> declarations = diDAO.find(criterion, true); // pas de flush de la session automatique

		final ForsParType forsParType = contribuable.getForsParType(false);

		for (DeclarationImpotOrdinaire di : declarations) {
			boolean diEchue = false;
			final EtatDeclaration dernierEtat = di.getDernierEtat();
			if (dernierEtat != null) {
				// génération de tache d'annulation si la DI est déposée ou échue
				if (TypeEtatDeclaration.ECHUE.equals(dernierEtat.getEtat()) ||
						TypeEtatDeclaration.RETOURNEE.equals(dernierEtat.getEtat()) ||
						TypeEtatDeclaration.SOMMEE.equals(dernierEtat.getEtat())) {
					diEchue = true;
				}
			}

			// S'il ne subsiste pas de for secondaire ouvert:
			if (di.getPeriode().getAnnee() >= premierePeriodeFiscale && !DateRangeHelper.intersect(di, forsParType.secondaires)) {
				// Si la déclaration d’impôt est déposée ou échue, une tâche d’annulation de la déclaration
				// d’impôt est engendrée pour que l’utilisateur puisse traiter le cas manuellement.
				if (diEchue) {
					// [UNIREG-1105] on évite de créer des tâches dupliquées
					if (!tacheDAO.existsTacheAnnulationEnInstanceOuEnCours(contribuable.getNumero(), di.getId())) {
						final TacheAnnulationDeclarationImpot tacheAnnulationDI = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, null, contribuable, di);
						tacheDAO.save(tacheAnnulationDI);
					}
				}
				else {
					// Sinon, la déclaration d’impôt est annulée automatiquement
					diService.annulationDI(contribuable, di, RegDate.get());
				}
			}
		}
	}

	/**
	 * Genere une tache à partir de la overture d'un for secondaire
	 *
	 * @param contribuable
	 * @param forFiscal
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisOuvertureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal) {
		ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(null);

		// [UNIREG-1888] Aucune tâche générée pour les contribuables dont le mode d'imposition est "SOURCE"
		if (forPrincipal != null && forPrincipal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && forPrincipal.getModeImposition() != ModeImposition.SOURCE) {

			final MotifRattachement motifRattachement = forFiscal.getMotifRattachement();
			if (motifRattachement == MotifRattachement.ACTIVITE_INDEPENDANTE || motifRattachement == MotifRattachement.IMMEUBLE_PRIVE) {

				generateTacheNouveauDossier(contribuable);
				if (forFiscal.getMotifRattachement().equals(MotifRattachement.ACTIVITE_INDEPENDANTE) ) {
					genereTachesEnvoiDI(contribuable, forFiscal.getDateDebut());
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTachesEnCoursCount(String username, Integer oid) {
		TacheCriteria criteria = new TacheCriteria();
		criteria.setEtatTache(TypeEtatTache.EN_COURS);
		criteria.setOid(oid);
		// TODO (msi) tenir compte du username
		return tacheDAO.count(criteria);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTachesEnInstanceCount(Integer oid) {
		TacheCriteria criteria = new TacheCriteria();
		criteria.setTypeTache(TypeTache.TacheNouveauDossier);
		criteria.setInvertTypeTache(true); // on veut tout sauf les nouveau dossiers
		criteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		criteria.setDateEcheanceJusqua(RegDate.get());
		criteria.setOid(oid);
		return tacheDAO.count(criteria);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDossiersEnInstanceCount(Integer oid) {
		TacheCriteria criteria = new TacheCriteria();
		criteria.setTypeTache(TypeTache.TacheNouveauDossier);
		criteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		criteria.setDateEcheanceJusqua(RegDate.get());
		criteria.setOid(oid);
		return tacheDAO.count(criteria);
	}

	private int getPremierePeriodeFiscale() {
		return parametres.getPremierePeriodeFiscale();
	}

	/**
	 * {@inheritDoc}
	 */
	public void onAnnulationContribuable(Contribuable contribuable) {

		TacheCriteria criteria = new TacheCriteria();
		criteria.setContribuable(contribuable);

		final List<Tache> taches = tacheDAO.find(criteria);
		for (Tache t : taches) {
			if (TypeEtatTache.TRAITE.equals(t.getEtat())) {
				// inutile d'annuler les tâches traitées
				continue;
			}
			if (t instanceof TacheAnnulationDeclarationImpot) {
				// rien à faire, il reste nécessaire d'annuler les déclarations d'impôt
			}
			else {
				// dans tous les autres cas, on annule la tâche qui est devenue caduque
				t.setAnnule(true);
			}
		}
	}

	public ListeTachesEnIsntanceParOID produireListeTachesEnIstanceParOID(RegDate dateTraitement,StatusManager status) throws Exception {
		final ProduireListeTachesEnInstanceParOIDProcessor processor = new ProduireListeTachesEnInstanceParOIDProcessor(hibernateTemplate, serviceInfra, transactionManager);
		return processor.run(dateTraitement, status);
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfra = serviceInfrastructureService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

}
