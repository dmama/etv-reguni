package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseGenerique.Source;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class AdresseServiceImpl implements AdresseService {

	// private final Logger LOGGER = Logger.getLogger(AdresseServiceImpl.class);

	private static final String SUFFIXE_DEFUNT_MASCULIN = ", défunt";
	private static final String SUFFIXE_DEFUNT_FEMININ = ", défunte";
	private static final String SUFFIXE_DEFUNT_NEUTRE = ", défunt(e)";

	public static final String POUR_ADRESSE = "p.a.";
	private static final Pattern POUR_ADRESSE_PATTERN = Pattern.compile("^(pa|p/a|pa\\.|p\\.a|p\\.a\\.|chez|c/|c/\\.|co|c/o|co\\.|c/ems)[ :\t]", Pattern.CASE_INSENSITIVE);

	/**
	 * Profondeur maximale d'appel récursive dans la résolution des adresses (pour détecter les boucles de résolutions d'adresses)
	 */
	private static final int MAX_CALL_DEPTH = 20;
	private static final int OPTIONALITE_CASE_POSTALE = 1;
	private static final int OPTIONALITE_COMPLEMENT = 2;


	private TiersService tiersService;
	private TiersDAO tiersDAO;
	private ServiceInfrastructureService serviceInfra;
	private ServicePersonneMoraleService servicePM;
	private ServiceCivilService serviceCivilService;
	private AdresseTiersDAO adresseTiersDAO;
	private PlatformTransactionManager transactionManager;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseTiersDAO(AdresseTiersDAO adresseTiersDAO) {
		this.adresseTiersDAO = adresseTiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public AdresseServiceImpl() {
	}

	protected AdresseServiceImpl(TiersService tiersService, TiersDAO tiersDAO, ServiceInfrastructureService serviceInfra,
	                             ServicePersonneMoraleService servicePM, ServiceCivilService serviceCivilService) {
		this.tiersService = tiersService;
		this.tiersDAO = tiersDAO;
		this.serviceInfra = serviceInfra;
		this.servicePM = servicePM;
		this.serviceCivilService = serviceCivilService;
	}

	/**
	 * {@inheritDoc}
	 */
	public AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers, RegDate date, TypeAdresseFiscale type, boolean strict) throws AdresseException {
		Assert.notNull(tiers);

		// Détermine les informations de l'adresse d'envoi
		final EnvoiInfo envoi = determineEnvoiInfo(tiers, date, type);

		// Récupère l'adresse fiscale du tiers chez qui l'envoi doit être adressé
		final AdresseGenerique adresseDestination = getAdresseFiscale(envoi.destination, envoi.typeAdresseDestination, date, strict);
		if (adresseDestination == null && type == TypeAdresseFiscale.POURSUITE_AUTRE_TIERS) {
			// [UNIREG-1808] l'adresse de poursuite autre tiers n'est renseignée que dans des cas bien précis, dans les autres cas elle est nulle
			return null;
		}

		final Source source;
		if (envoi.sourceOverride != null) {
			source = envoi.sourceOverride;
		}
		else if (adresseDestination != null) {
			source = adresseDestination.getSource();
		}
		else {
			source = null;
		}

		final AdresseEnvoiDetaillee adresseEnvoi = new AdresseEnvoiDetaillee(source);

		fillDestinataire(adresseEnvoi, envoi.destinataire, date);
		fillDestination(adresseEnvoi, adresseDestination, envoi.destination, envoi.avecPourAdresse, date);

		return adresseEnvoi;
	}

	/**
	 * Contient les informations nécessaires à la construction d'une adresse d'envoi correcte.
	 */
	private static class EnvoiInfo {

		/**
		 * Le tiers à qui l'envoi est destiné
		 */
		final Tiers destinataire;

		/**
		 * Le tiers chez qui l'envoi est adressé
		 */
		final Tiers destination;

		/**
		 * <b>vrai</b> s'il faut ajouter un préfixe "p.a." avant la destination
		 */
		final boolean avecPourAdresse;

		/**
		 * Le type d'adresse utilisé pour adresser l'envoi
		 */
		final TypeAdresseFiscale typeAdresseDestination;

		final Source sourceOverride;

		public EnvoiInfo(Tiers destinataire, TypeAdresseFiscale type) {
			this.destinataire = destinataire;
			this.destination = destinataire;
			this.avecPourAdresse = false;
			this.typeAdresseDestination = type;
			this.sourceOverride = null;
		}

		private EnvoiInfo(Tiers destinataire, Tiers destination, boolean avecPourAdresse, TypeAdresseFiscale typeAdresseDestination, Source sourceOverride) {
			this.destinataire = destinataire;
			this.destination = destination;
			this.avecPourAdresse = avecPourAdresse;
			this.typeAdresseDestination = typeAdresseDestination;
			this.sourceOverride = sourceOverride;
		}
	}

	private EnvoiInfo determineEnvoiInfo(Tiers tiers, RegDate date, TypeAdresseFiscale type) throws AdresseException {

		EnvoiInfo data = new EnvoiInfo(tiers, type); // par défaut, le destinataire est le tiers lui-même

		final Tiers autreTiers = getAutreTiers(tiers, date, type);
		if (autreTiers != null) {
			// Cas spécial d'un tiers ayant son adresse pointant sur celle d'un autre tiers
			data = new EnvoiInfo(tiers, autreTiers, true, type, null);
		}

		if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;

			// Dans le cas du débiteur, la destination est l'adresse du contribuable associé, sauf si le débiteur possède lui-même une adresse du type considéré
			final TypeAdresseTiers coreType = type.asCoreType();
			if (coreType != null && TiersHelper.getAdresseTiers(debiteur, coreType, date) != null) {
				// le débiteur possède lui-même une adresse : on est tout bon
			}
			else {
				// on prend le contribuable associé au débiteur comme pour adresse (enfin, s'il existe)
				final Contribuable ctb = tiersService.getContribuable(debiteur);
				if (ctb != null) {
					final AdresseGenerique adresseCtb = getAdresseFiscale(ctb, type, date, false);
					if (adresseCtb != null) { // on prend en compte le contribuable associé que s'il possède au moins une adresse valide [UNIREG-2896]
						data = new EnvoiInfo(debiteur, ctb, false, type, null);
					}
				}
			}
		}

		if (type == TypeAdresseFiscale.POURSUITE) {

			if (TiersHelper.estSousTutelle(tiers, date)) {
				// [UNIREG-1808] dans le cas de l'adresse de poursuite d'un contribuable sous tutelle, le destinaire de l'adresse de poursuite est l'autorité tutelaire.
				final Tiers autoriteTutelaire = getRepresentant(tiers, TypeAdresseRepresentant.AUTORITE_TUTELAIRE, date);
				if (autoriteTutelaire != null) {
					data = new EnvoiInfo(autoriteTutelaire, autoriteTutelaire, false, TypeAdresseFiscale.REPRESENTATION, Source.TUTELLE);
				}
				else {
					// (msi 10.03.2010) Précision de Thierry: dans le cas d'un contribuable sous tutuelle sans autorité tutelaire renseignée, l'adresse de poursuite est simplement
					// l'adresse de domicile du contribuable. L'office de poursuite sera donc déterminé par SIPF à partir de l'adresse de domicile, ce qui est le meilleure choix
					// possible sans information plus spécifique.
				}
			}

			if (TiersHelper.possedeRepresentantAvecExecutionForcee(tiers, date)) {
				// [UNIREG-1808] dans le cas de l'adresse de poursuite d'un contribuable qui possède un représentant avec exécution forcée, le destinaire de l'adresse de poursuite est le représentant.
				final Tiers representant = getRepresentant(tiers, TypeAdresseRepresentant.REPRESENTATION_AVEC_EXECUTION_FORCEE, date);
				if (representant != null) {
					data = new EnvoiInfo(representant, representant, false, TypeAdresseFiscale.REPRESENTATION, Source.REPRESENTATION);
				}
			}
		}

		if (type == TypeAdresseFiscale.POURSUITE_AUTRE_TIERS) {
			// [UNIREG-1808] dans le cas de l'adresse de poursuite autre tiers, le destinataire est toujours le tuteur/curateur/conseiller légal/repésesentant.

			final Tiers representant = getRepresentant(tiers, TypeAdresseRepresentant.REPRESENTATION_AVEC_EXECUTION_FORCEE, date);
			if (representant != null) {
				data = new EnvoiInfo(representant, representant, false, TypeAdresseFiscale.REPRESENTATION, Source.REPRESENTATION);
			}

			final Tiers conseiller = getRepresentant(tiers, TypeAdresseRepresentant.CONSEIL_LEGAL, date);
			if (conseiller != null) {
				data = new EnvoiInfo(conseiller, conseiller, false, TypeAdresseFiscale.REPRESENTATION, Source.CONSEIL_LEGAL);
			}

			final Tiers curateur = getRepresentant(tiers, TypeAdresseRepresentant.CURATELLE, date);
			if (curateur != null) {
				data = new EnvoiInfo(curateur, curateur, false, TypeAdresseFiscale.REPRESENTATION, Source.CURATELLE);
			}

			final Tiers tuteur = getRepresentant(tiers, TypeAdresseRepresentant.TUTELLE, date);
			if (tuteur != null) {
				data = new EnvoiInfo(tuteur, tuteur, false, TypeAdresseFiscale.REPRESENTATION, Source.TUTELLE);
			}
		}

		return data;
	}

	/**
	 * Recherche l'autre tiers dans le cas d'un tiers ayant son adresse qui pointe sur celle d'un autre tiers.
	 *
	 * @param tiers le tiers de base
	 * @param date  la date à laquelle l'adresse pointant vers l'autre tiers est valide.
	 * @param type  le type d'adresse considéré
	 * @return l'autre tiers
	 */
	private Tiers getAutreTiers(Tiers tiers, RegDate date, TypeAdresseFiscale type) {

		Tiers autreTiers = null;

		// Cas spécial d'un tiers ayant son adresse pointant sur celle d'un autre tiers
		final TypeAdresseTiers coreType = type.asCoreType();
		if (coreType != null) {
			final AdresseTiers adresseCourrier = tiers.getAdresseActive(coreType, date);
			if (adresseCourrier != null && adresseCourrier instanceof AdresseAutreTiers) {
				final AdresseAutreTiers adresseAutreTiers = (AdresseAutreTiers) adresseCourrier;
				final Long id = adresseAutreTiers.getAutreTiersId();
				autreTiers = tiersDAO.get(id, true);
			}
		}

		return autreTiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public AdresseEnvoiDetaillee getAdresseEnvoi(Individu individu, RegDate date, boolean strict) throws AdresseException {
		return createAdresseEnvoi(individu, null, strict);
	}

	public AdresseCourrierPourRF getAdressePourRF(Contribuable ctb, RegDate date) throws AdresseException {

		String nomPrenom1 = null;
		String nomPrenom2 = null;
		if (ctb instanceof PersonnePhysique) {
			nomPrenom1 = getNomPrenom((PersonnePhysique) ctb, date);
		}
		else if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;
			/* Récupère la vue historique complète du ménage (date = null) */
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, null);

			final PersonnePhysique principal = ensemble.getPrincipal();
			if (principal != null) {
				nomPrenom1 = getNomPrenom(principal, date);
			}

			final PersonnePhysique conjoint = ensemble.getConjoint();
			if (conjoint != null) {
				final String np = getNomPrenom(conjoint, date);
				if (nomPrenom1 == null) {
					nomPrenom1 = np;
				}
				else {
					nomPrenom2 = np;
				}
			}
		}
		else {
			Assert.fail("Le registre foncier ne s'intéresse qu'aux personnes physiques et ménages communs!");
		}

		final AdresseGenerique adresse = getAdresseFiscale(ctb, TypeAdresseFiscale.COURRIER, date, false);
		final String rueEtNumero = adresse != null ? buildRueEtNumero(adresse) : null;
		final String npa = adresse != null ? adresse.getNumeroPostal() : null;
		final String localite = adresse != null ? adresse.getLocalite() : null;
		final String pays = adresse != null ? buildPays(adresse, true) : null;

		return new AdresseCourrierPourRF(nomPrenom1, nomPrenom2, rueEtNumero, npa, localite, pays);
	}

	/**
	 * Remplis les lignes correspondant à l'identification de la personne destinataire.
	 *
	 * @param adresse l'adresse d'envoi détaillée à remplir
	 * @param tiers   le tiers destinataire
	 * @param date    la date de validité de l'adresse
	 */
	private void fillDestinataire(AdresseEnvoiDetaillee adresse, Tiers tiers, RegDate date) {

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique personne = (PersonnePhysique) tiers;
			adresse.addFormulePolitesse(getFormulePolitesse(personne, date));
			adresse.addNomPrenom(getNomPrenom(personne, date));
		}
		else if (tiers instanceof MenageCommun) {
			MenageCommun menageCommun = (MenageCommun) tiers;
			/* Récupère la vue historique complète du ménage (date = null) */
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, null);

			final PersonnePhysique principal = ensemble.getPrincipal();
			if (principal != null) {
				adresse.addFormulePolitesse(getFormulePolitesse(ensemble, date));
				adresse.addNomPrenom(getNomPrenom(principal, date));
			}

			final PersonnePhysique conjoint = ensemble.getConjoint();
			if (conjoint != null) {
				adresse.addNomPrenom(getNomPrenom(conjoint, date));
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			final List<String> raisonSociale = tiersService.getRaisonSociale(debiteur);
			for (String ligne : raisonSociale) {
				adresse.addNomPrenom(ligne);
			}
			if (debiteur.getComplementNom() != null) {
				adresse.addPourAdresse(debiteur.getComplementNom());
			}
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			final CollectiviteAdministrative collectivite = (CollectiviteAdministrative) tiers;
			final List<String> nomComplet = getRaisonSocialeLongue(collectivite);
			for (String ligne : nomComplet) {
				adresse.addNomPrenom(ligne);
			}
		}
		else if (tiers instanceof AutreCommunaute) {
			final AutreCommunaute autre = (AutreCommunaute) tiers;
			adresse.addNomPrenom(autre.getNom());
		}
		else if (tiers instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) tiers;
			final List<String> raisonComplete = getRaisonSocialeLongue(entreprise);
			for (String ligne : raisonComplete) {
				adresse.addNomPrenom(ligne);
			}
		}
		else {
			throw new NotImplementedException("Type de tiers [" + tiers.getNatureTiers() + "] inconnu");
		}
	}

	/**
	 * Remplis les lignes correspondant à la destination géographique d'une adresse d'envoi.
	 *
	 * @param adresse            l'adresse d'envoi détaillée à remplir
	 * @param adresseDestination l'adresse générique pré-calculée
	 * @param destination        le tiers chez qui l'envoi est adressé
	 * @param avecPourAdresse    <b>vrai</b> s'il faut ajouter un préfixe "p.a." avant la destination
	 * @param date               la date de validité de l'adresse
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, AdresseGenerique adresseDestination, Tiers destination, boolean avecPourAdresse, RegDate date) throws AdresseException {

		if (avecPourAdresse) {
			adresse.addPourAdresse(getPourAdresse(destination));
		}

		if (destination instanceof PersonnePhysique) {
			fillDestination(adresse, (PersonnePhysique) destination, date, adresseDestination);
		}
		else if (destination instanceof MenageCommun) {
			fillDestination(adresse, (MenageCommun) destination, date, adresseDestination);
		}
		else if (destination instanceof DebiteurPrestationImposable || destination instanceof CollectiviteAdministrative || destination instanceof AutreCommunaute ||
				destination instanceof Entreprise) {
			fillDestination(adresse, adresseDestination);
		}
		else {
			throw new NotImplementedException("Type de tiers [" + destination.getNatureTiers() + "] inconnu");
		}
	}

	/**
	 * Rempli l'adresse de destination associée à une personne physique.
	 *
	 * @param adresse            l'adresse d'envoi détaillée à remplir
	 * @param personne           une personne physique
	 * @param date               la date de validité de l'adresse
	 * @param adresseDestination l'adresse générique pré-calculée
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, PersonnePhysique personne, RegDate date, AdresseGenerique adresseDestination) throws AdresseException {

		if (adresseDestination != null) {
			fillRepresentantPourAdresse(adresse, date, adresseDestination, personne);
			fillAdresseEnvoi(adresse, adresseDestination);
		}
	}

	/**
	 * Crée et retourne l'adresse d'envoi pour une personne physique.
	 *
	 * @param individu l'individu dont on veut connaître les adresses
	 * @param date     la date de validité de l'adresse d'envoi
	 * @param strict   si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *                 (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return une adresse d'envoi détaillée
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private AdresseEnvoiDetaillee createAdresseEnvoi(Individu individu, RegDate date, boolean strict) throws AdresseException {

		AdresseEnvoiDetaillee adresse = new AdresseEnvoiDetaillee(Source.CIVILE);
		adresse.addFormulePolitesse(getFormulePolitesse(individu, date));
		adresse.addNomPrenom(tiersService.getNomPrenom(individu));

		final AdressesCiviles adressesCourantes;
		try {
			adressesCourantes = new AdressesCiviles(serviceCivilService.getAdresses(individu.getNoTechnique(), date, strict));
			final Adresse adresseCourrier = adressesCourantes.courrier;

			if (adresseCourrier != null) {
				fillAdresseEnvoi(adresse, new AdresseCivileAdapter(adresseCourrier, false, serviceInfra));
			}
		}
		catch (DonneesCivilesException e) {
			throw new AdresseDataException(e);
		}

		return adresse;
	}

	/**
	 * Rempli l'adresse de destination associée à un ménage commun.
	 *
	 * @param adresse            l'adresse d'envoi détaillée à remplir
	 * @param menageCommun       un ménage commun
	 * @param date               la date de validité de l'adresse
	 * @param adresseDestination l'adresse générique pré-calculée
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, MenageCommun menageCommun, RegDate date, AdresseGenerique adresseDestination) throws AdresseException {

		// Une adresse courrier n'existe pas forcément (exemple: couple en cours de création)
		if (adresseDestination != null) {

			fillRepresentantPourAdresse(adresse, date, adresseDestination, menageCommun);

			// Rue, numéro et ville
			fillAdresseEnvoi(adresse, adresseDestination);
		}
	}

	/**
	 * Remplit la champs 'pour adresse' si l'adresse fiscale spécifiée découle d'une représentation (tutelle, curatelle, ...)
	 *
	 * @param adresse        l'adresse d'envoi détaillée à compléter
	 * @param date           la date de validité
	 * @param adresseFiscale l'adresse fiscale considéré
	 * @param tiers          le tiers associée à l'adresse
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private void fillRepresentantPourAdresse(AdresseEnvoiDetaillee adresse, RegDate date, AdresseGenerique adresseFiscale, Tiers tiers) throws AdresseException {
		final TypeAdresseRepresentant type = TypeAdresseRepresentant.getTypeAdresseRepresentantFromSource(adresseFiscale.getSource());
		if (type != null) {
			final Tiers representant = getRepresentant(tiers, type, date);
			Assert.notNull(representant);
			adresse.addPourAdresse(getPourAdresse(representant));
		}
	}

	/**
	 * Rempli l'adresse de destination associée sur un tiers générique
	 *
	 * @param adresse            l'adresse d'envoi détaillée à remplir
	 * @param adresseDestination l'adresse générique pré-calculée
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, AdresseGenerique adresseDestination) {

		if (adresseDestination != null) {
			fillAdresseEnvoi(adresse, adresseDestination);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public FormulePolitesse getFormulePolitesse(Tiers tiers) {

		final FormulePolitesse salutations;

		if (tiers instanceof PersonnePhysique) {
			salutations = getFormulePolitesse((PersonnePhysique) tiers, null);
		}
		else if (tiers instanceof MenageCommun) {
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
			salutations = getFormulePolitesse(ensemble, null);
		}
		else {
			// pas de formule de politesse pour les autres types de tiers
			salutations = null;
		}

		return salutations;
	}

	/**
	 * @param personne une personne physique
	 * @param date     la date de validité de la formule de politesse
	 * @return la formule de politesse pour l'adressage d'une personne physique
	 */
	private FormulePolitesse getFormulePolitesse(PersonnePhysique personne, RegDate date) {

		FormulePolitesse salutations;

		final boolean estDecede = estDecedeAt(personne, date);

		if (personne.isHabitantVD()) {
			if (!estDecede) {
				if (tiersService.getSexe(personne) == Sexe.MASCULIN) {
					salutations = FormulePolitesse.MONSIEUR;
				}
				else {
					salutations = FormulePolitesse.MADAME;
				}
			}
			else {
				salutations = FormulePolitesse.HERITIERS;
			}
		}
		else {
			if (!estDecede) {
				if (personne.getSexe() != null) {
					if (personne.getSexe() == Sexe.MASCULIN) {
						salutations = FormulePolitesse.MONSIEUR;
					}
					else {
						salutations = FormulePolitesse.MADAME;
					}
				}
				else {
					salutations = FormulePolitesse.MADAME_MONSIEUR;
				}
			}
			else {
				salutations = FormulePolitesse.HERITIERS;
			}
		}

		return salutations;
	}

	/**
	 * Détermine si une personne est décédée à une date donnée.
	 *
	 * @param personne une personne
	 * @param date     une date
	 * @return <b>vrai</b> si la personne est décédée à la date donnée; <b>faux</b> autrement.
	 */
	private boolean estDecedeAt(PersonnePhysique personne, RegDate date) {
		final RegDate dateDeces = tiersService.getDateDeces(personne);
		return (dateDeces != null && (date == null || dateDeces.isBeforeOrEqual(date)));
	}

	/**
	 * @param individu un individu
	 * @param date     la date de validité de la formule de politesse
	 * @return la formule de politesse pour l'adressage d'un individu
	 */
	private FormulePolitesse getFormulePolitesse(Individu individu, RegDate date) {
		FormulePolitesse salutations;

		final RegDate dateDeces = individu.getDateDeces();
		final boolean estDecede = (dateDeces != null && (date == null || dateDeces.isBeforeOrEqual(date)));

		if (!estDecede) {
			if (individu.isSexeMasculin()) {
				salutations = FormulePolitesse.MONSIEUR;
			}
			else {
				salutations = FormulePolitesse.MADAME;
			}
		}
		else {
			salutations = FormulePolitesse.HERITIERS;
		}
		return salutations;
	}

	/**
	 * Voir le document 'ModeleDonnees.doc' v0.1, §4.2 Formats d'adresses
	 *
	 * @param ensemble un ensemble tiers-couple
	 * @param date     la date de validité de la formule de politesse
	 * @return la formule de politesse pour l'adressage des parties d'un ménage commun
	 */
	protected FormulePolitesse getFormulePolitesse(EnsembleTiersCouple ensemble, RegDate date) {

		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();

		final Sexe sexePrincipal = tiersService.getSexe(principal);
		final Sexe sexeConjoint = (conjoint == null ? null : tiersService.getSexe(conjoint));

		final boolean principalEstDecede = estDecedeAt(principal, date);
		final boolean secondaireEstDecede = estDecedeAt(conjoint, date);

		// [UNIREG-749] la formule de politesse 'aux héritiers de' s'applique dès qu'un des deux tiers est décédé.
		if (principalEstDecede || secondaireEstDecede) {
			return FormulePolitesse.HERITIERS;
		}

		if (conjoint == null) {
			if (sexePrincipal == null) {
				return FormulePolitesse.MADAME_MONSIEUR;
			}
			else {
				if (Sexe.MASCULIN.equals(sexePrincipal)) {
					return FormulePolitesse.MONSIEUR;
				}
				else {
					return FormulePolitesse.MADAME;
				}
			}
		}
		else {
			if (sexePrincipal == null || sexeConjoint == null) {
				return FormulePolitesse.MADAME_MONSIEUR;
			}
			else {
				boolean principalMasculin = Sexe.MASCULIN.equals(sexePrincipal);
				boolean conjointMasculin = Sexe.MASCULIN.equals(sexeConjoint);

				if (principalMasculin && conjointMasculin) {
					return FormulePolitesse.MESSIEURS;
				}
				else if (principalMasculin && !conjointMasculin) {
					return FormulePolitesse.MONSIEUR_ET_MADAME;
				}
				else if (!conjointMasculin) {
					return FormulePolitesse.MESDAMES;
				}
				else {
					Assert.fail("Il n'est pas possible d'avoir un principal féminin avec un conjoint masculin");
					return null;
				}
			}
		}
	}

	/**
	 * @param personne une personne physique
	 * @param date     la date de validité du nom et du prénom
	 * @return la ligne du prénom et du nom pour la personne physique spécifiée.
	 */
	private String getNomPrenom(PersonnePhysique personne, RegDate date) {

		String prenomNom = tiersService.getNomPrenom(personne);

		// [UNIREG-749] on applique un suffixe 'défunt' aux personnes décédées
		final boolean estDecede = estDecedeAt(personne, date);
		if (estDecede) {
			final Sexe sexe = tiersService.getSexe(personne);
			if (sexe == null) {
				prenomNom += SUFFIXE_DEFUNT_NEUTRE;
			}
			else {
				switch (sexe) {
				case MASCULIN:
					prenomNom += SUFFIXE_DEFUNT_MASCULIN;
					break;
				case FEMININ:
					prenomNom += SUFFIXE_DEFUNT_FEMININ;
					break;
				}
			}
		}

		return prenomNom;
	}

	/**
	 * @param tiers un tiers
	 * @return la ligne "pour adresse" correspondant au tiers spécifié
	 */
	private String getPourAdresse(final Tiers tiers) {

		String line;
		if (tiers instanceof PersonnePhysique) {
			line = POUR_ADRESSE + " " + tiersService.getNomPrenom((PersonnePhysique) tiers);
		}
		else if (tiers instanceof MenageCommun) {
			MenageCommun menageCommun = (MenageCommun) tiers;
			/* Récupère la vue historique complète du ménage (date = null) */
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, null);
			line = POUR_ADRESSE + " " + tiersService.getNomPrenom(ensemble.getPrincipal());
			final PersonnePhysique conjoint = ensemble.getConjoint();
			if (conjoint != null) {
				line += " et " + tiersService.getNomPrenom(conjoint);
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			line = POUR_ADRESSE + " " + debiteur.getComplementNom();
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			final CollectiviteAdministrative collectivite = (CollectiviteAdministrative) tiers;
			line = POUR_ADRESSE + " " + getRaisonSociale(collectivite);
		}
		else if (tiers instanceof AutreCommunaute) {
			final AutreCommunaute autre = (AutreCommunaute) tiers;
			line = POUR_ADRESSE + " " + autre.getNom();
		}
		else if (tiers instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) tiers;
			line = POUR_ADRESSE + " " + getRaisonSociale(entreprise);
		}
		else {
			throw new NotImplementedException("Type de tiers [" + tiers.getNatureTiers() + "] non-implémenté");
		}

		return line;
	}

	/**
	 * @param collectivite une collectivité administrative
	 * @return la raison sociale pour l'adressage de la collectivité administrative spécifiée.
	 */
	private String getRaisonSociale(CollectiviteAdministrative collectivite) {
		try {
			ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative c = serviceInfra.getCollectivite(collectivite.getNumeroCollectiviteAdministrative());
			return c.getNomCourt();
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Impossible de trouver la collectivite administrative avec le numéro = "
					+ collectivite.getNumeroCollectiviteAdministrative(), e);
		}
	}

	/**
	 * @param collectivite une collectivité administrative
	 * @return la raison sociale pour l'adressage de la collectivité administrative spécifiée.
	 */
	private List<String> getRaisonSocialeLongue(CollectiviteAdministrative collectivite) {
		try {
			final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative c = serviceInfra.getCollectivite(collectivite
					.getNumeroCollectiviteAdministrative());

			final List<String> nomsComplets = new ArrayList<String>(3);
			if (StringUtils.isNotBlank(c.getNomComplet1())) {
				nomsComplets.add(c.getNomComplet1());
			}
			if (StringUtils.isNotBlank(c.getNomComplet2())) {
				nomsComplets.add(c.getNomComplet2());
			}
			if (StringUtils.isNotBlank(c.getNomComplet3())) {
				nomsComplets.add(c.getNomComplet3());
			}
			return nomsComplets;
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Impossible de trouver la collectivite administrative avec le numéro = "
					+ collectivite.getNumeroCollectiviteAdministrative(), e);
		}
	}

	/**
	 * Retourne la raison sociale pour l'adressage de l'entreprise spécifiée.
	 *
	 * @param entreprise une entreprise
	 * @return la raison sociale de l'enteprise sur une seule ligne
	 */
	private String getRaisonSociale(Entreprise entreprise) {
		final Long numeroEntreprise = entreprise.getNumeroEntreprise();
		Assert.notNull(numeroEntreprise);
		final PersonneMorale pm = servicePM.getPersonneMorale(numeroEntreprise);
		return pm.getRaisonSociale();
	}

	/**
	 * Retourne la raison sociale pour l'adressage de l'entreprise spécifiée.
	 *
	 * @param entreprise une entreprise
	 * @return la raison sociale de l'entreprise sur une, deux ou trois lignes.
	 */
	private List<String> getRaisonSocialeLongue(Entreprise entreprise) {

		final Long numeroEntreprise = entreprise.getNumeroEntreprise();
		Assert.notNull(numeroEntreprise);
		final PersonneMorale pm = servicePM.getPersonneMorale(numeroEntreprise);

		final List<String> nomsComplets = new ArrayList<String>(3);
		if (StringUtils.isNotBlank(pm.getRaisonSociale1())) {
			nomsComplets.add(pm.getRaisonSociale1());
		}
		if (StringUtils.isNotBlank(pm.getRaisonSociale2())) {
			nomsComplets.add(pm.getRaisonSociale2());
		}
		if (StringUtils.isNotBlank(pm.getRaisonSociale3())) {
			nomsComplets.add(pm.getRaisonSociale3());
		}

		return nomsComplets;
	}

	private static String buildRueEtNumero(AdresseGenerique adresse) {
		final String rueEtNumero;
		final String rue = adresse.getRue();
		final String numeroRue = adresse.getNumero();
		if (notEmpty(rue)) {
			if (notEmpty(numeroRue)) {
				rueEtNumero = String.format("%s %s", rue, numeroRue);
			}
			else {
				rueEtNumero = rue;
			}
		}
		else {
			rueEtNumero = null;
		}
		return rueEtNumero;
	}

	private String buildPays(AdresseGenerique adresse, boolean aussiSuisse) {
		final Integer noOfsPays = adresse.getNoOfsPays();
		try {
			final Pays pays = (noOfsPays == null ? null : serviceInfra.getPays(noOfsPays));
			final String nomPays;
			if (pays != null && (aussiSuisse || !pays.isSuisse())) {
				nomPays = pays.getNomMinuscule();
			}
			else {
				nomPays = null;
			}
			return nomPays;
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Impossible de trouver le pays avec le numéro Ofs = " + noOfsPays);
		}
	}

	/**
	 * Rempli l'adresse d'envoi en fonction de l'adresse civile/fiscale spécifiée.
	 *
	 * @param adresseEnvoi l'adresse d'envoi détaillée à remplir
	 * @param adresse      une adresse générique à partir de laquelle l'adresse d'envoi sera remplie
	 */
	private void fillAdresseEnvoi(AdresseEnvoiDetaillee adresseEnvoi, final AdresseGenerique adresse) {
		Assert.notNull(adresse, "Une adresse doit être spécifiée.");

		final String complement = adresse.getComplement();
		if (notEmpty(complement)) {
			adresseEnvoi.addComplement(complement, OPTIONALITE_COMPLEMENT);
		}

		final String rueEtNumero = buildRueEtNumero(adresse);
		if (StringUtils.isNotBlank(rueEtNumero)) {
			adresseEnvoi.addRueEtNumero(rueEtNumero);
		}

		final String casePostale = adresse.getCasePostale();
		if (notEmpty(casePostale)) {
			adresseEnvoi.addCasePostale(casePostale, OPTIONALITE_CASE_POSTALE);
		}

		final String localite = adresse.getLocalite();
		if (notEmpty(localite)) {
			final String numeroPostal = adresse.getNumeroPostal();
			if (notEmpty(numeroPostal)) {
				adresseEnvoi.addNpaEtLocalite(numeroPostal + " " + localite);
			}
			else {
				adresseEnvoi.addNpaEtLocalite(localite);
			}
		}

		final String nomPays = buildPays(adresse, false);
		if (StringUtils.isNotBlank(nomPays)) {
			final TypeAffranchissement typeAffranchissement = getTypeAffranchissement(adresse);
			adresseEnvoi.addPays(nomPays, typeAffranchissement);
		}
	}

	/**
	 * @param complement un complément d'adresse
	 * @return <b>vrai</b> si le complément d'adresse spécifié commence avec un "p.a.", un "chez", un "c/o", ou tout autre variantes reconnues.
	 */
	public static boolean isPrefixedByPourAdresse(final String complement) {
		return POUR_ADRESSE_PATTERN.matcher(complement).find();
	}

	/**
	 * Ajoute un préfixe "p.a." au complément s'il n'en possède pas déjà un.
	 *
	 * @param complement le complément à préfixer
	 * @return le complément préfixé
	 */
	public static String prefixByPourAdresseIfNeeded(String complement) {
		if (AdresseServiceImpl.isPrefixedByPourAdresse(complement)) {
			return complement;
		}
		else {
			return POUR_ADRESSE + " " + complement;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public AdressesFiscalesHisto getAdressesFiscalHisto(Tiers tiers, boolean strict) throws AdresseException {
		return getAdressesFiscalHisto(tiers, 0, strict);
	}

	private AdressesFiscalesHisto getAdressesFiscalHisto(Tiers tiers, int callDepth, boolean strict) throws AdresseException {

		if (tiers == null) {
			return null;
		}

		AdressesFiscalesHisto adresses = new AdressesFiscalesHisto();

		/*
		 * Récolte des adresses en provenance du host
		 */
		if (tiers instanceof Entreprise) {
			final AdressesPMHisto adressesPM = getAdressesPMHisto((Entreprise) tiers);
			final RegDate debut = adressesPM.getVeryFirstDate();
			final RegDate fin = adressesPM.getVeryLastDate();

			adresses.courrier = initAdressesPMHisto(adressesPM.courriers, debut, fin, adressesPM.sieges);
			adresses.domicile = initAdressesPMHisto(adressesPM.sieges, debut, fin, adressesPM.courriers);

			adresses.representation = adresses.courrier;
			adresses.poursuite = adresses.domicile;
		}
		else {
			final AdressesCivilesHisto adressesCiviles = getAdressesCivilesHisto(tiers, strict);
			final RegDate debut = adressesCiviles.getVeryFirstDate();
			final RegDate fin = adressesCiviles.getVeryLastDate();

			adresses.courrier = initAdressesCivilesHisto(adressesCiviles.courriers, debut, fin, adressesCiviles.principales, strict);
			adresses.domicile = initAdressesCivilesHisto(adressesCiviles.principales, debut, fin, adressesCiviles.courriers, strict);

			adresses.representation = adresses.courrier;
			adresses.poursuite = adresses.domicile;
		}

		/*
		 * Surcharge avec les adresses fiscales
		 */
		if (tiers instanceof MenageCommun) {
			/* Pour le cas du ménage commun, les adresses du principal sont utilisées comme premier défaut */
			final MenageCommun menage = (MenageCommun) tiers;
			final PersonnePhysique principal = getPrincipalPourAdresse(menage);
			final AdressesTiersHisto adressesPrincipal = TiersHelper.getAdressesTiersHisto(principal);

			if (adressesPrincipal != null) {
				adresses.courrier = surchargeAdressesTiersHisto(tiers, adresses.courrier, adressesPrincipal.courrier, Source.PRINCIPAL, true, callDepth + 1, strict);
				adresses.representation = surchargeAdressesTiersHisto(tiers, adresses.representation, adressesPrincipal.representation, Source.PRINCIPAL, true, callDepth + 1, strict);
				adresses.poursuite = surchargeAdressesTiersHisto(tiers, adresses.poursuite, adressesPrincipal.poursuite, Source.PRINCIPAL, true, callDepth + 1, strict);
				adresses.domicile = surchargeAdressesTiersHisto(tiers, adresses.domicile, adressesPrincipal.domicile, Source.PRINCIPAL, true, callDepth + 1, strict);
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			// Pour le cas du débiteur, les adresses du contribuable associé sont utilisées comme premier défaut. Il peut cependant arriver
			// que le débiteur ne possède pas de contribuable associé, dans ce cas on continue
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			final Contribuable contribuable = tiersService.getContribuable(debiteur);
			if (contribuable != null) {
				final AdressesFiscalesHisto adressesContribuable = getAdressesFiscalHisto(contribuable, callDepth + 1, strict);

				adresses.courrier = surchargeAdressesHisto(adresses.courrier, adressesContribuable.courrier, Source.CONTRIBUABLE, true);
				adresses.representation = surchargeAdressesHisto(adresses.representation, adressesContribuable.representation,
						Source.CONTRIBUABLE, true);
				adresses.poursuite = surchargeAdressesHisto(adresses.poursuite, adressesContribuable.poursuite, Source.CONTRIBUABLE, true);
				adresses.domicile = surchargeAdressesHisto(adresses.domicile, adressesContribuable.domicile, Source.CONTRIBUABLE, true);
			}
		}

		final AdressesTiersHisto adressesTiers = TiersHelper.getAdressesTiersHisto(tiers);
		adresses.courrier = surchargeAdressesTiersHisto(tiers, adresses.courrier, adressesTiers.courrier, null, null, callDepth + 1, strict);
		adresses.representation = surchargeAdressesTiersHisto(tiers, adresses.representation, adressesTiers.representation, null, null, callDepth + 1, strict);
		adresses.domicile = surchargeAdressesTiersHisto(tiers, adresses.domicile, adressesTiers.domicile, null, null, callDepth + 1, strict);
		adresses.poursuite = surchargeAdressesTiersHisto(tiers, adresses.poursuite, adressesTiers.poursuite, null, null, callDepth + 1, strict);

		// Applique les défauts, de manière à avoir une adresse valide pour chaque type d'adresse
		appliqueDefautsAdressesFiscalesHisto(adresses);

		// Si le tiers concerné possède un representant, on surchage avec l'adresse du représentant
		final List<AdresseGenerique> adressesRepresentant = getAdressesRepresentantHisto(tiers, TypeAdresseRepresentant.REPRESENTATION, callDepth + 1, strict);
		adresses.courrier = AdresseMixer.override(adresses.courrier, adressesRepresentant, null, null);

		// Si le tiers concerné possède un conseil légal, on surchage avec l'adresse du représentant
		final List<AdresseGenerique> adressesConseil = getAdressesRepresentantHisto(tiers, TypeAdresseRepresentant.CONSEIL_LEGAL, callDepth + 1, strict);
		adresses.courrier = AdresseMixer.override(adresses.courrier, adressesConseil, null, null);

		// Si le tiers concerné est sous tutelle, on surchage les adresses courrier avec les adresses représentation du tuteur
		final List<AdresseGenerique> adressesTuteur = getAdressesRepresentantHisto(tiers, TypeAdresseRepresentant.TUTELLE, callDepth + 1, strict);
		adresses.courrier = AdresseMixer.override(adresses.courrier, adressesTuteur, null, null);

		// Si le tiers concerné est sous curatelle, on surchage les adresses courrier avec les adresses représentation du curateur
		final List<AdresseGenerique> adressesCuratelle = getAdressesRepresentantHisto(tiers, TypeAdresseRepresentant.CURATELLE, callDepth + 1, strict);
		adresses.courrier = AdresseMixer.override(adresses.courrier, adressesCuratelle, null, null);

		// [UNIREG-1808] Si le tiers concerné possède un représentant avec exécution forcée, on surcharge les adresses de poursuite avec les adresses du représentant
		final List<AdresseGenerique> adressesRepresentantExecutionForcee = getAdressesRepresentantHisto(tiers, TypeAdresseRepresentant.REPRESENTATION_AVEC_EXECUTION_FORCEE, callDepth + 1, strict);
		adresses.poursuite = AdresseMixer.override(adresses.poursuite, adressesRepresentantExecutionForcee, null, null);

		// [UNIREG-1808] Si le tiers concerné est sous tutelle, on surchage les adresses poursuite avec les adresses de l'autorité tutelaire
		final List<AdresseGenerique> adressesAutoriteTutelaire = getAdressesRepresentantHisto(tiers, TypeAdresseRepresentant.AUTORITE_TUTELAIRE, callDepth + 1, strict);
		adresses.poursuite = AdresseMixer.override(adresses.poursuite, adressesAutoriteTutelaire, null, null);

		// [UNIREG-1808]
		adresses.poursuiteAutreTiers = surchargeAdressesTiersHisto(tiers, adresses.poursuiteAutreTiers, adressesTiers.poursuite, null, null, callDepth + 1, strict);
		adresses.poursuiteAutreTiers = AdresseMixer.override(adresses.poursuiteAutreTiers, adressesRepresentantExecutionForcee, null, null);
		adresses.poursuiteAutreTiers = AdresseMixer.override(adresses.poursuiteAutreTiers, adressesConseil, null, null);
		adresses.poursuiteAutreTiers = AdresseMixer.override(adresses.poursuiteAutreTiers, removeSourceConjoint(adressesCuratelle), null, null);
		adresses.poursuiteAutreTiers = AdresseMixer.override(adresses.poursuiteAutreTiers, removeSourceConjoint(adressesTuteur), null, null);

		return adresses;
	}

	/**
	 * [UNIREG-2227] retourne une collection (nouvelle si nécessaire) sans aucune adresse de type = 'CONJOINT'.
	 *
	 * @param adresses une liste d'adresses
	 * @return la liste d'adresse d'entrée si aucune adresse n'a été supprimée, ou une nouvelle liste autrement.
	 */
	private List<AdresseGenerique> removeSourceConjoint(List<AdresseGenerique> adresses) {
		if (adresses == null || adresses.isEmpty()) {
			return adresses;
		}
		final List<AdresseGenerique> list = new ArrayList<AdresseGenerique>(adresses.size());
		for (AdresseGenerique a : adresses) {
			if (a.getSource() != Source.CONJOINT) {
				list.add(a);
			}
		}
		return list;
	}

	/**
	 * Applique les règles de gestion des adresses par défaut sur les adresses fiscales historiques spécifiées.
	 * <p>
	 * Les règles sont les suivantes:
	 * <ul>
	 * <li>Pour les  adresses <i>courrier</i>, les défauts sont : <i>domicile</i>, <i>représentation</i> et <i>poursuite</i> </li>
	 * <li>Pour les  adresses <i>domicile</i>, les défauts sont : <i>poursuite</i>, <i>courrier</i> et <i>représentation</i> </li>
	 * <li>Pour les  adresses <i>représentation</i>, les défauts sont : <i>courrier</i>, <i>domicile</i> et <i>poursuite</i> </li>
	 * <li>Pour les  adresses <i>poursuite</i>, les défauts sont : <i>domicile</i>, <i>courrier</i> et <i>représentation</i> </li>
	 * </ul>
	 *
	 * @param adresses les adresses fiscales
	 */
	private void appliqueDefautsAdressesFiscalesHisto(AdressesFiscalesHisto adresses) {

		adresses.courrier = appliqueDefautsAdressesFiscalesHisto(adresses.courrier, adresses.domicile);
		adresses.courrier = appliqueDefautsAdressesFiscalesHisto(adresses.courrier, adresses.representation);
		adresses.courrier = appliqueDefautsAdressesFiscalesHisto(adresses.courrier, adresses.poursuite);

		adresses.domicile = appliqueDefautsAdressesFiscalesHisto(adresses.domicile, adresses.poursuite);
		adresses.domicile = appliqueDefautsAdressesFiscalesHisto(adresses.domicile, adresses.courrier);
		adresses.domicile = appliqueDefautsAdressesFiscalesHisto(adresses.domicile, adresses.representation);

		adresses.representation = appliqueDefautsAdressesFiscalesHisto(adresses.representation, adresses.courrier);
		adresses.representation = appliqueDefautsAdressesFiscalesHisto(adresses.representation, adresses.domicile);
		adresses.representation = appliqueDefautsAdressesFiscalesHisto(adresses.representation, adresses.poursuite);

		adresses.poursuite = appliqueDefautsAdressesFiscalesHisto(adresses.poursuite, adresses.domicile);
		adresses.poursuite = appliqueDefautsAdressesFiscalesHisto(adresses.poursuite, adresses.courrier);
		adresses.poursuite = appliqueDefautsAdressesFiscalesHisto(adresses.poursuite, adresses.representation);
	}

	private List<AdresseGenerique> appliqueDefautsAdressesFiscalesHisto(List<AdresseGenerique> adresses, List<AdresseGenerique> defaults) {
		if (defaults != null && defaults.size() > 0) {
			List<AdresseGenerique> d = new ArrayList<AdresseGenerique>();
			for (AdresseGenerique a : defaults) {
				if (a.isAnnule()) {
					// on ne prend pas en compte les adresses annulées comme défaut
					continue;
				}
				// met le flag 'defaut' à vrai
				d.add(new AdresseGeneriqueAdapter(a, null, null, true));
			}
			return AdresseMixer.override(d, adresses, null, null);
		}
		else {
			return adresses;
		}
	}

	/**
	 * Retourne le représentant à la date donnée du tiers spécifié.
	 *
	 * @param tiers le tiers potentiellement mis sous tutelle ou possèdant un conseil légal.
	 * @param type  le type de représentant
	 * @param date  la date de référence, ou null pour obtenir le représentant courant.
	 * @return le représentant, ou null si le tiers ne possède pas de représentant à la date spécifiée.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private Tiers getRepresentant(Tiers tiers, TypeAdresseRepresentant type, RegDate date) throws AdresseException {

		final Tiers representant;

		if (tiers instanceof MenageCommun) {
			representant = getRepresentantPourMenage((MenageCommun) tiers, type, date);
		}
		else {
			representant = getRepresentantPourTiers(tiers, type, date);
		}

		return representant;
	}

	/**
	 * Retourne le représentant à la date donnée du tiers spécifié.
	 *
	 * @param tiers le tiers potentiellement mis sous tutelle ou possèdant un conseil légal.
	 * @param type  le type de représentant
	 * @param date  la date de référence, ou null pour obtenir le représentant courant.
	 * @return le représentant, ou null si le tiers ne possède pas de représentant à la date spécifiée.
	 */
	private Tiers getRepresentantPourTiers(Tiers tiers, TypeAdresseRepresentant type, RegDate date) {

		final RapportEntreTiers rapport = TiersHelper.getRapportSujetOfType(tiers, type.getTypeRapport(), date);
		if (rapport == null) {
			return null;
		}

		Assert.isEqual(tiers.getId(), rapport.getSujetId());

		// info de la tutelle
		final Long representantId = type.getRepresentantId(rapport);
		if (representantId == null) {
			return null;
		}

		return tiersDAO.get(representantId);
	}

	/**
	 * Retourne le représentant à la date donnée du ménage commun spécifié.
	 *
	 * @param menage le ménage commun possèdant un conseil légal ou dont un des membres est potentiellement sous tutelle.
	 * @param type   le type de représentant
	 * @param date   la date de référence, ou null pour obtenir le représentant courant.
	 * @return le représentant, ou null si le ménage ne possède pas de représentant à la date spécifiée.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private Tiers getRepresentantPourMenage(MenageCommun menage, TypeAdresseRepresentant type, RegDate date) throws AdresseException {

		final RapportEntreTiers rapport;

		//
		// Attention, le comportement de cette méthode doit être cohérent avec celui de la méthode getAdresseCourrierConjointPourRepresentationMenage()
		//

		if (type == TypeAdresseRepresentant.TUTELLE || type == TypeAdresseRepresentant.CURATELLE) {

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, date);
			final PersonnePhysique principal = getPrincipalPourAdresse(menage);
			final PersonnePhysique conjoint = ensemble.getConjoint(principal);

			if (principal == null) {
				// pas de principal, pas de tuteur
				return null;
			}

			final RapportEntreTiers rapportPrincipal = TiersHelper.getRapportSujetOfType(principal, type.getTypeRapport(), date);
			if (rapportPrincipal == null) {
				rapport = null; // pas de tuteur ni curateur
			}
			else if (conjoint == null) {
				rapport = rapportPrincipal; // le principal est marié seul
			}
			else {

				final RegDate dateDeces = tiersService.getDateDeces(conjoint);

				if (dateDeces != null && RegDateHelper.isBeforeOrEqual(dateDeces, date, NullDateBehavior.LATEST)) {
					rapport = rapportPrincipal; // [UNIREG-2915] le conjoint est décédé
				}
				else if (TiersHelper.getRapportSujetOfType(conjoint, TypeRapportEntreTiers.TUTELLE, date) != null ||
						TiersHelper.getRapportSujetOfType(conjoint, TypeRapportEntreTiers.CURATELLE, date) != null) {
					rapport = rapportPrincipal; // le conjoint est aussi sous tutelle (ou curatelle)
				}
				else {
					final AdresseGenerique adresseConjoint = getAdresseFiscalPropre(conjoint, TypeAdresseFiscale.COURRIER, date, 0, false);
					if (adresseConjoint == null || adresseConjoint.getNoOfsPays() != ServiceInfrastructureService.noOfsSuisse) {
						// [UNIREG-2676] le conjoint habite hors-Suisse, le représentant est donc le tuteur du principal
						rapport = rapportPrincipal;
					}
					else {
						rapport = null; // le conjoint n'est pas sous tutelle (ni curatelle), le ménage ne l'est donc pas non plus
					}

				}
			}
		}
		else {
			rapport = TiersHelper.getRapportSujetOfType(menage, type.getTypeRapport(), date);
		}

		if (rapport == null) {
			return null;
		}

		final Long representantId = type.getRepresentantId(rapport);
		if (representantId == null) {
			return null;
		}

		return tiersDAO.get(representantId);
	}

	/**
	 * Retourne l'adresse du représentant pour le tiers spécifié.
	 *
	 * @param tiers                   un tiers
	 * @param date                    la date à laquelle l'adresse retournée est valide
	 * @param typeAdresseRepresentant le type de représentant
	 * @param callDepth               paramètre technique pour éviter les récursions infinies
	 * @param strict                  si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return l'adresse demandée, ou <b>null</b> si le tiers n'est pas sous tutelle.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private AdresseGenerique getAdresseRepresentant(Tiers tiers, RegDate date, TypeAdresseRepresentant typeAdresseRepresentant, int callDepth, boolean strict) throws AdresseException {

		List<AdresseGenerique> adressesHisto = getAdressesRepresentantHisto(tiers, typeAdresseRepresentant, callDepth, strict);
		for (AdresseGenerique a : adressesHisto) {
			if (a.isValidAt(date)) {
				return a;
			}
		}

		return null;
	}

	/**
	 * Retourne l'adresse fiscale propre au tiers spécifié (= en ignorant les éventuels représentants, tuteurs et autres curateurs).
	 *
	 * @param tiers     un tiers
	 * @param type      le type d'adresse considéré
	 * @param date      une date de validité de l'adresse
	 * @param callDepth paramètre technique pour éviter les récursions infinies
	 * @param strict    si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return une adresse générique ou <b>null</b> si le tiers ne possède pas d'adresse fiscale propre à la date donnée
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private AdresseGenerique getAdresseFiscalPropre(Tiers tiers, TypeAdresseFiscale type, RegDate date, int callDepth, boolean strict) throws AdresseException {
		final AdressesFiscalesHisto adressesHisto = getAdressesFiscalesPropreHisto(tiers, callDepth, strict);
		final AdressesFiscales adresses = adressesHisto.at(date);
		return adresses.ofType(type);
	}

	/**
	 * Retourne l'historique des adresses fiscales propres au tiers spécifié (= en ignorant les éventuels représentants, tuteurs et autres curateurs).
	 *
	 * @param tiers     un tiers
	 * @param callDepth paramètre technique pour éviter les récursions infinies
	 * @param strict    si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return l'historique des adresses génériques ou <b>null</b> si le tiers ne possède aucune adresses fiscales propres.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private AdressesFiscalesHisto getAdressesFiscalesPropreHisto(Tiers tiers, int callDepth, boolean strict) throws AdresseException {

		if (tiers == null) {
			return null;
		}

		AdressesFiscalesHisto adresses = new AdressesFiscalesHisto();

		// TODO (msi) factoriser se code avec getAdressesFiscalesHisto

		/*
		 * Récolte des adresses en provenance du host
		 */
		if (tiers instanceof Entreprise) {
			final AdressesPMHisto adressesPM = getAdressesPMHisto((Entreprise) tiers);
			final RegDate debut = adressesPM.getVeryFirstDate();
			final RegDate fin = adressesPM.getVeryLastDate();

			adresses.courrier = initAdressesPMHisto(adressesPM.courriers, debut, fin, adressesPM.sieges);
			adresses.domicile = initAdressesPMHisto(adressesPM.sieges, debut, fin, adressesPM.courriers);

			adresses.representation = adresses.courrier;
			adresses.poursuite = adresses.domicile;
		}
		else {
			final AdressesCivilesHisto adressesCiviles = getAdressesCivilesHisto(tiers, strict);
			final RegDate debut = adressesCiviles.getVeryFirstDate();
			final RegDate fin = adressesCiviles.getVeryLastDate();

			adresses.courrier = initAdressesCivilesHisto(adressesCiviles.courriers, debut, fin, adressesCiviles.principales, strict);
			adresses.domicile = initAdressesCivilesHisto(adressesCiviles.principales, debut, fin, adressesCiviles.courriers, strict);

			adresses.representation = adresses.courrier;
			adresses.poursuite = adresses.domicile;
		}

		/*
		 * Surcharge avec les adresses fiscales
		 */
		if (tiers instanceof MenageCommun) {
			/* Pour le cas du ménage commun, les adresses du principal sont utilisées comme premier défaut */
			final MenageCommun menage = (MenageCommun) tiers;
			final PersonnePhysique principal = getPrincipalPourAdresse(menage);
			final AdressesTiersHisto adressesPrincipal = TiersHelper.getAdressesTiersHisto(principal);

			if (adressesPrincipal != null) {
				adresses.courrier = surchargeAdressesTiersHisto(tiers, adresses.courrier, adressesPrincipal.courrier, null, null, callDepth + 1, strict);
				adresses.representation = surchargeAdressesTiersHisto(tiers, adresses.representation, adressesPrincipal.representation, null, null, callDepth + 1, strict);
				adresses.poursuite = surchargeAdressesTiersHisto(tiers, adresses.poursuite, adressesPrincipal.poursuite, null, null, callDepth + 1, strict);
				adresses.domicile = surchargeAdressesTiersHisto(tiers, adresses.domicile, adressesPrincipal.domicile, null, null, callDepth + 1, strict);
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			// Pour le cas du débiteur, les adresses du contribuable associé sont utilisées comme premier défaut. Il peut cependant arriver
			// que le débiteur ne possède pas de contribuable associé, dans ce cas on continue
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			final Contribuable contribuable = tiersService.getContribuable(debiteur);
			if (contribuable != null) {
				final AdressesFiscalesHisto adressesContribuable = getAdressesFiscalHisto(contribuable, callDepth + 1, strict);

				adresses.courrier = surchargeAdressesHisto(adresses.courrier, adressesContribuable.courrier, Source.CONTRIBUABLE, true);
				adresses.representation = surchargeAdressesHisto(adresses.representation, adressesContribuable.representation,
						Source.CONTRIBUABLE, true);
				adresses.poursuite = surchargeAdressesHisto(adresses.poursuite, adressesContribuable.poursuite, Source.CONTRIBUABLE, true);
				adresses.domicile = surchargeAdressesHisto(adresses.domicile, adressesContribuable.domicile, Source.CONTRIBUABLE, true);
			}
		}

		final AdressesTiersHisto adressesTiers = TiersHelper.getAdressesTiersHisto(tiers);
		adresses.courrier = surchargeAdressesTiersHisto(tiers, adresses.courrier, adressesTiers.courrier, null, null, callDepth + 1, strict);
		adresses.representation = surchargeAdressesTiersHisto(tiers, adresses.representation, adressesTiers.representation, null, null, callDepth + 1, strict);
		adresses.domicile = surchargeAdressesTiersHisto(tiers, adresses.domicile, adressesTiers.domicile, null, null, callDepth + 1, strict);
		adresses.poursuite = surchargeAdressesTiersHisto(tiers, adresses.poursuite, adressesTiers.poursuite, null, null, callDepth + 1, strict);

		// Applique les défauts, de manière à avoir une adresse valide pour chaque type d'adresse
		appliqueDefautsAdressesFiscalesHisto(adresses);

		return adresses;
	}

	/**
	 * {@inheritDoc}
	 */
	public AdresseGenerique getAdresseRepresentant(Tiers tiers, TypeAdresseRepresentant type, RegDate date, boolean strict)
			throws AdresseException {
		return getAdresseRepresentant(tiers, date, type, 0, strict);
	}

	/**
	 * Retourne l'historique des adresses du representant pour le tiers spécifié.
	 *
	 * @param tiers                   le tiers potentiellement sous mis sous tutelle.
	 * @param typeAdresseRepresentant le type de représentant
	 * @param callDepth               paramètre technique pour éviter les récursions infinies
	 * @param strict                  si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return les adresses demandées, ou une liste vide si le tiers n'a jamais été sous tutelle.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private List<AdresseGenerique> getAdressesRepresentantHisto(Tiers tiers, TypeAdresseRepresentant typeAdresseRepresentant, int callDepth, boolean strict) throws AdresseException {

		final List<AdresseGenerique> adressesTuteur;

		if (tiers instanceof MenageCommun) {
			adressesTuteur = getAdressesRepresentantHistoPourMenage((MenageCommun) tiers, typeAdresseRepresentant, callDepth + 1, strict);
		}
		else {
			adressesTuteur = getAdressesRepresentantHistoPourTiers(tiers, typeAdresseRepresentant, callDepth + 1, strict);
		}
		return adressesTuteur;
	}

	/**
	 * Retourne l'historique des adresses du representant pour le ménage commun spécifié.
	 * <p/>
	 * Dans le cas d'un ménage commun, les adresses courrier du conjoint priment sur celles du tuteur (pour autant que le conjoint ne soit pas lui-même sous tutelle).
	 *
	 * @param menage    le ménage commun potentiellement sous mis sous tutelle.
	 * @param type      le type de représentant
	 * @param callDepth paramètre technique pour éviter les récursions infinies
	 * @param strict    si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return les adresses demandées, ou une liste vide si le principal du ménage n'a jamais été sous tutelle.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private List<AdresseGenerique> getAdressesRepresentantHistoPourMenage(final MenageCommun menage, TypeAdresseRepresentant type, int callDepth, boolean strict) throws
			AdresseException {

		final List<AdresseGenerique> adresses;

		if (type == TypeAdresseRepresentant.TUTELLE || type == TypeAdresseRepresentant.CURATELLE) {
			// Un ménage ne peut pas être mis sous tutelle/curatelle, seulement les personnes physiques qui le compose. On va donc chercher le tuteur/curateur sur ces derniers.

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, null);
			final PersonnePhysique principal = getPrincipalPourAdresse(ensemble);
			final PersonnePhysique conjoint = ensemble.getConjoint(principal);

			if (principal == null) {
				// pas de principal, par de tuteur
				return Collections.emptyList();
			}

			// On récupère l'historique des adresses du tuteur du principal
			final List<AdresseGenerique> adressesTuteur = getAdressesRepresentantHistoPourTiers(principal, type, callDepth + 1, strict);

			if (adressesTuteur.isEmpty()) {
				adresses = Collections.emptyList();
			}
			else if (conjoint == null) {
				// cas du marié seul
				adresses = adressesTuteur;
			}
			else {
				// On détermine les périodes durant lesquelles le principal est sous tutelle de manière continue
				final List<DateRange> periodesTutellesPrincipal = DateRangeHelper.collateRange(adressesTuteur);

				// On détermine les adresses courrier du conjoint pour représenter le ménage pendant les périodes de tutelle du principal
				final List<AdresseGenerique> adressesConjointSansTutelle = getAdresseCourrierConjointPourRepresentationMenage(conjoint, periodesTutellesPrincipal, callDepth, strict);
				adresses = AdresseMixer.override(adressesTuteur, adressesConjointSansTutelle, null, null);
			}
		}
		else {
			// On récupère l'historique des adresses du représentant du principal
			adresses = getAdressesRepresentantHistoPourTiers(menage, type, callDepth + 1, strict);
		}

		return adresses;
	}

	/**
	 * Cette méthode permet de déterminer les adresses courrier du conjoint pour représenter le ménage pendant les périodes de tutelle/curatelle du principal.
	 *
	 * @param conjoint                  le conjoint
	 * @param periodesTutellePrincipal les périodes pendant lesquelles le principal est sous tutelle/curatelle
	 * @param callDepth                 paramètre technique pour éviter les récursions infinies
	 * @param strict                    si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return une liste d'adresses à utiliser comme adresses courrier du ménage dont fait partie le conjoint
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private List<AdresseGenerique> getAdresseCourrierConjointPourRepresentationMenage(PersonnePhysique conjoint, List<DateRange> periodesTutellePrincipal, int callDepth,
	                                                                                  boolean strict) throws AdresseException {

		final RegDate dateDeces = tiersService.getDateDeces(conjoint);

		//
		// Attention, le comportement de cette méthode doit être cohérent avec celui de la méthode getRepresentantPourMenage()
		//

		// On détermine les périodes de validité des adresses du conjoint comme adresse de représentation du ménage
		final List<DateRange> periodesValiditeAdressesConjoint;
		if (dateDeces == null) {
			periodesValiditeAdressesConjoint = periodesTutellePrincipal;
		}
		else {
			// [UNIREG-2644] en case de décès du conjoint, on limite la validité des adresses à la période avant le décès
			periodesValiditeAdressesConjoint = truncate(periodesTutellePrincipal, dateDeces.getOneDayBefore());
		}

		final List<AdresseGenerique> adressesConjointSansTutelle = new ArrayList<AdresseGenerique>();

		final List<AdresseGenerique> adressesCourrierConjoint = getAdressesCourrierHistoInRanges(conjoint, periodesValiditeAdressesConjoint, callDepth + 1, strict);
		for (AdresseGenerique adresse : adressesCourrierConjoint) {

			final Source source = adresse.getSource();
			if (source == Source.TUTELLE || source == Source.CURATELLE) {
				// on ignore toutes les adresses où le conjoint est lui-même sous tutelle
				continue;
			}

			final AdresseGenerique adresseConjoint;
			if (source != Source.CIVILE && source != Source.FISCALE) {
				// [UNIREG-1341] on utilise l'adresse courrier *propre* du conjoint (hors représentation) comme adresse de représentation du ménage
				final AdresseGenerique adressePropre = getAdresseFiscalPropre(conjoint, TypeAdresseFiscale.COURRIER, adresse.getDateDebut(), callDepth + 1, strict);
				adresseConjoint = new AdresseGeneriqueAdapter(adressePropre, adresse.getDateDebut(), adresse.getDateFin(), Source.CONJOINT, false);
			}
			else {
				adresseConjoint = new AdresseGeneriqueAdapter(adresse, Source.CONJOINT, false);
			}

			// [UNIREG-2676] on ignore toutes les adresses où le conjoint est hors-Suisse
			if (adresseConjoint.getNoOfsPays() == ServiceInfrastructureService.noOfsSuisse) {
				adressesConjointSansTutelle.add(adresseConjoint);
			}
		}

		return adressesConjointSansTutelle;
	}

	/**
	 * Tronque une liste de ranges pour qu'ils ne dépassent pas une date maximum
	 *
	 * @param ranges      une liste de ranges, triés par ordre croissants et qui ne se chevauchent pas.
	 * @param dateMaximum une date utilisée comme limite supérieur (incluse)
	 * @return une liste contenant tous les ranges qui sont valides avant la date maximum (le dernier range est tronqué si nécessaire).
	 */
	private static List<DateRange> truncate(List<DateRange> ranges, RegDate dateMaximum) {
		final List<DateRange> truncated = new ArrayList<DateRange>(ranges.size());
		for (DateRange range : ranges) {

			if (range.getDateDebut().isAfterOrEqual(dateMaximum)) {
				break; // inutile de continuer
			}

			if (range.getDateFin() == null || dateMaximum.isBeforeOrEqual(range.getDateFin())) {
				final DateRange r = new DateRangeHelper.Range(range.getDateDebut(), dateMaximum);
				truncated.add(r);
				break; // inutile de continuer
			}

			truncated.add(range);
		}
		return truncated;
	}

	/**
	 * Détermine et retourne les adresses courrier d'un tiers pour plusieurs périodes données.
	 *
	 * @param tiers     un tiers
	 * @param ranges    les périodes pour lesquelles on veut extraire les adresses
	 * @param callDepth paramètre technique pour éviter les récursions infinies
	 * @param strict    si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return une liste d'adresses courrier valides pendant les périodes demandées.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private List<AdresseGenerique> getAdressesCourrierHistoInRanges(Tiers tiers, List<DateRange> ranges, int callDepth, boolean strict) throws AdresseException {

		// On récupère les adresses du conjoint et on les filtre pour ne garder que celles valides durant les périodes calculées plus haut
		final AdressesFiscalesHisto adressesConjoint = getAdressesFiscalHisto(tiers, callDepth + 1, strict);
		if (strict) {
			verifieCoherenceAdresses(adressesConjoint.courrier, "Adresse de courrier", tiers);
		}

		final List<AdresseGenerique> adressesInRange = new ArrayList<AdresseGenerique>();
		for (DateRange range : ranges) {
			final List<AdresseGenerique> adressesRange = AdresseMixer.extract(adressesConjoint.courrier, range.getDateDebut(), range.getDateFin());
			adressesInRange.addAll(adressesRange);
		}

		return adressesInRange;
	}

	/**
	 * Retourne l'historique des adresses 'représentation' (ajusté à la durée des mises-sous-tutelle) du représentant du tiers spécifié.
	 *
	 * @param tiers     le tiers potentiellement sous mis sous tutelle ou avec un conseil légal.
	 * @param type      le type de représentant
	 * @param callDepth paramètre technique pour éviter les récursions infinies
	 * @param strict    si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return les adresses demandées, ou une liste vide si le tiers n'a jamais été sous tutelle.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private List<AdresseGenerique> getAdressesRepresentantHistoPourTiers(Tiers tiers, TypeAdresseRepresentant type, int callDepth, boolean strict)
			throws AdresseException {

		List<AdresseGenerique> adresses = new ArrayList<AdresseGenerique>();

		final List<RapportEntreTiers> rapports = TiersHelper.getRapportSujetHistoOfType(tiers, type.getTypeRapport());
		if (rapports != null) {

			/* pour toutes les périodes de mise sous tutelles/conseil légal */
			for (RapportEntreTiers rapport : rapports) {

				if (rapport.isAnnule()) {
					continue;
				}

				final Long representantId = type.getRepresentantId(rapport);
				if (representantId == null) {
					continue;
				}
				final Tiers representant = tiersDAO.get(representantId);
				final RegDate debutRapport = rapport.getDateDebut();
				final RegDate finRapport = rapport.getDateFin();

				/*
				 * Extrait les adresses du représentant et ajuste-les pour qu'elles correspondent à la durée de la représentation
				 */
				final int nextDepth = oneLevelDeeper(callDepth, tiers, representant, null);
				final AdressesFiscalesHisto adressesRepresentant = getAdressesFiscalHisto(representant, nextDepth, strict);
				if (strict) {
					verifieCoherenceAdresses(adressesRepresentant.representation, "Adresses de représentation", representant);
				}
				final List<AdresseGenerique> adressesRepresentation = AdresseMixer.extract(adressesRepresentant.representation,
						debutRapport, finRapport, type.getTypeSource(), false);
				adresses.addAll(adressesRepresentation);
			}
		}

		return adresses;
	}

	/**
	 * Vérifie que toutes les adresses données ont au moins une date de début de validité (à l'exception de la première qui peut être nulle), et que les dates de validités (si début et fin sont
	 * présentes) sont dans le bon ordre
	 *
	 * @param adresses            les adresses à tester
	 * @param descriptionContexte le context des données
	 * @param tiers               le tiers auquel les adresses appartiennent
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private static void verifieCoherenceAdresses(List<AdresseGenerique> adresses, String descriptionContexte, Tiers tiers) throws AdresseException {
		final int size = adresses.size();
		for (int i = 0; i < size; ++i) {
			final AdresseGenerique adresse = adresses.get(i);
			// [UNIREG-1097] la première adresse peut avoir une date de début nulle, et la dernière peut avoir une date de fin nulle.
			final ValidationResults validationResult = DateRangeHelper.validate(adresse, (i == 0), (i == size - 1));
			if (validationResult.hasErrors()) {
				throw new AdresseDataException(descriptionContexte + " du tiers n°" + tiers.getNumero(), validationResult);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public AdressesFiscales getAdressesFiscales(Tiers tiers, RegDate date, boolean strict) throws AdresseException {

		if (tiers == null) {
			return null;
		}

		final AdressesFiscalesHisto adressesHisto = getAdressesFiscalHisto(tiers, strict);
		return adressesHisto.at(date);
	}

	/**
	 * {@inheritDoc}
	 */
	public AdresseGenerique getAdresseFiscale(Tiers tiers, TypeAdresseFiscale type, RegDate date, boolean strict) throws AdresseException {

		if (tiers == null) {
			return null;
		}

		return getAdresseFiscale(tiers, type, date, 0, strict);
	}

	private AdresseGenerique getAdresseFiscale(Tiers tiers, TypeAdresseFiscale type, RegDate date, int callDepth, boolean strict) throws AdresseException {

		if (tiers == null) {
			return null;
		}

		final AdressesFiscalesHisto adressesHisto = getAdressesFiscalHisto(tiers, callDepth, strict);
		final List<AdresseGenerique> adresses = adressesHisto.ofType(type);
		if (adresses != null) {
			for (AdresseGenerique a : adresses) {
				if (a.isValidAt(date)) {
					return a;
				}
			}
		}

		return null;
	}

	/**
	 * Détermine le tiers principal pour le calcul des adresses du ménage commun. Selon [UNIREG-771] et comme intégré plus tard dans la spécification, le principal du couple ne sera pas toujours
	 * considéré comme principal pour le calcul des adresses.
	 *
	 * @param menageCommun le ménage commun
	 * @return le principal trouvé, ou <b>null</b> si le ménage ne possède aucun membre à la date spécifiée
	 */
	private PersonnePhysique getPrincipalPourAdresse(final MenageCommun menageCommun) {
		// [UNIREG-2234] date=null -> on s'intéresse à la vue historique du couple dans tous les cas.
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, null);
		return getPrincipalPourAdresse(ensemble);
	}

	/**
	 * Détermine le tiers principal pour le calcul des adresses du ménage commun. Selon [UNIREG-771] et comme intégré plus tard dans la spécification, le principal du couple ne sera pas toujours
	 * considéré comme principal pour le calcul des adresses.
	 *
	 * @param ensemble l'ensemble tiers-couple
	 * @return le principal trouvé, ou <b>null</b> si le ménage ne possède aucun membre à la date spécifiée
	 */
	private PersonnePhysique getPrincipalPourAdresse(EnsembleTiersCouple ensemble) {
		final PersonnePhysique principal = ensemble.getPrincipal();
		PersonnePhysique principalOuVaudois = principal;
		/*
		 *  [UNIREG-771] : dans le cas d’un contribuable couple, l’adresse de domicile et l’adresse de courrier sont celles de l’individu principal
		 *  sauf si le contribuable principal quitte le canton ou la Suisse alors que le contribuable secondaire reste dans le canton.
		 */
		if (principal != null && !principal.isHabitantVD()) {
			final PersonnePhysique conjoint = ensemble.getConjoint(principal);
			if (conjoint != null && conjoint.isHabitantVD()) {
				principalOuVaudois = conjoint;
			}
		}
		return principalOuVaudois;
	}

	/**
	 * {@inheritDoc}
	 */
	public AdressesCiviles getAdressesCiviles(Tiers tiers, RegDate date, boolean strict) throws AdresseException {
		final AdressesCiviles adressesCiviles;
		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique personne = (PersonnePhysique) tiers;
			if (personne.getNumeroIndividu() != null && personne.getNumeroIndividu() != 0) {
				adressesCiviles = getAdressesCiviles(personne, date, strict);
			}
			else {
				adressesCiviles = null;
			}
		}
		else if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) tiers;
			final PersonnePhysique principal = getPrincipalPourAdresse(menage);

			if (principal != null && principal.getNumeroIndividu() != null && principal.getNumeroIndividu() != 0) { //le principal peut être null dans le cas d'un mariage annulé
				adressesCiviles = getAdressesCiviles(principal, date, strict);
			}
			else {
				adressesCiviles = null;
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable || tiers instanceof AutreCommunaute) {
			/* ok, ces tiers ne possèdent pas d'adresses civiles par définition */
			adressesCiviles = null;
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			adressesCiviles = getAdressesCiviles((CollectiviteAdministrative) tiers);
		}
		else if (tiers instanceof Entreprise) {
			throw new IllegalArgumentException("Les entreprises ne possèdent pas d'adresses civiles !");
		}
		else {
			throw new NotImplementedException("Type de tiers [" + tiers.getNatureTiers() + "] inconnu");
		}
		return adressesCiviles;
	}

	/**
	 * Retourne les adresses civiles valide à la date donnée.
	 *
	 * @param habitant l'habitant dont on recherche les adresses.
	 * @param date     la date de référence (attention, la précision est l'année !), ou null pour obtenir toutes les adresses existantes.
	 * @param strict   si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *                 (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses civiles de l'habitant spécifié.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private AdressesCiviles getAdressesCiviles(PersonnePhysique habitant, RegDate date, boolean strict) throws AdresseException {
		try {
			return new AdressesCiviles(serviceCivilService.getAdresses(habitant.getNumeroIndividu(), date, strict));
		}
		catch (DonneesCivilesException e) {
			throw new AdresseDataException(e);
		}
	}

	/**
	 * Retourne l'historique des adresses civiles du tiers spécifié. Ou <b>null</b> si le tiers n'en possède pas.
	 *
	 * @param tiers  un tiers dont on veut extraite l'historique des adresses civiles.
	 * @param strict si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return l'historique des adresses civiles du tiers spécifié.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	public AdressesCivilesHisto getAdressesCivilesHisto(Tiers tiers, boolean strict) throws AdresseException {

		final AdressesCivilesHisto adressesCiviles;

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique personne = (PersonnePhysique) tiers;
			if (personne.getNumeroIndividu() != null && personne.getNumeroIndividu() != 0) {
				adressesCiviles = getAdressesCivilesHisto(personne, strict);
			}
			else {
				adressesCiviles = new AdressesCivilesHisto();
			}
		}
		else if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) tiers;
			final PersonnePhysique principal = getPrincipalPourAdresse(menage);

			if (principal != null && principal.getNumeroIndividu() != null && principal.getNumeroIndividu() != 0) { //le principal peut être null dans le cas d'un couple annulé
				adressesCiviles = getAdressesCivilesHisto(principal, strict);
			}
			else {
				adressesCiviles = new AdressesCivilesHisto();
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable || tiers instanceof AutreCommunaute) {
			/* ok, ces tiers ne possèdent pas d'adresses civiles par définition */
			adressesCiviles = new AdressesCivilesHisto();
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			adressesCiviles = getAdressesCivilesHisto((CollectiviteAdministrative) tiers);
		}
		else if (tiers instanceof Entreprise) {
			throw new IllegalArgumentException("Les entreprises ne possèdent pas d'adresses civiles !");
		}
		else {
			throw new NotImplementedException("Type de tiers [" + tiers.getNatureTiers() + "] inconnu");
		}

		return adressesCiviles;
	}

	private AdressesCivilesHisto getAdressesCivilesHisto(PersonnePhysique habitant, boolean strict) throws AdresseException {
		try {
			return new AdressesCivilesHisto(serviceCivilService.getAdressesHisto(habitant.getNumeroIndividu(), strict), strict);
		}
		catch (DonneesCivilesException e) {
			throw new AdresseDataException(e);
		}
	}

	private AdressesCiviles getAdressesCiviles(CollectiviteAdministrative collectivite) {

		AdressesCiviles adresses = new AdressesCiviles();

		final Integer numero = collectivite.getNumeroCollectiviteAdministrative();
		if (numero != null) {
			ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative collectiviteCivil;
			try {
				collectiviteCivil = serviceInfra.getCollectivite(numero);
				Assert.notNull(collectiviteCivil);

				adresses.principale = collectiviteCivil.getAdresse();
				adresses.courrier = adresses.principale;
			}
			catch (InfrastructureException e) {
				throw new RuntimeException("Erreur dans la récupération des adresses", e);
			}
		}

		return adresses;
	}

	private AdressesCivilesHisto getAdressesCivilesHisto(CollectiviteAdministrative collectivite) {

		AdressesCivilesHisto adresses = new AdressesCivilesHisto();

		try {
			ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative collectiviteCivil = serviceInfra.getCollectivite(collectivite.getNumeroCollectiviteAdministrative());
			Assert.notNull(collectiviteCivil);

			adresses.principales.add(collectiviteCivil.getAdresse());
			adresses.courriers.add(collectiviteCivil.getAdresse());
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Erreur dans la récupération des adresses", e);
		}

		return adresses;
	}

	public AdressesPMHisto getAdressesPMHisto(Entreprise entreprise) {
		final Long numeroEntreprise = entreprise.getNumeroEntreprise();
		Assert.notNull(numeroEntreprise);
		return servicePM.getAdressesHisto(numeroEntreprise);
	}

	/**
	 * Applique les règles business pour transformer l'adresse surchargée spécifiée en une adresse générique.
	 *
	 * @param tiers             le tiers associé à l'adresse
	 * @param adresseSurchargee l'adresse de tiers à résoudre
	 * @param callDepth         profondeur d'appel (technique)
	 * @param strict            si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger
	 *                          les données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return une adresse générique
	 * @throws AdresseException en cas de dépendence circulaire
	 */
	private AdresseGenerique resolveAdresseSurchargee(final Tiers tiers, final AdresseTiers adresseSurchargee, int callDepth, boolean strict)
			throws AdresseException {

		AdresseGenerique surcharge = null;

		if (adresseSurchargee instanceof AdresseSupplementaire) {
			final AdresseSupplementaire a = (AdresseSupplementaire) adresseSurchargee;
			surcharge = new AdresseSupplementaireAdapter(a, false, serviceInfra);
		}
		else if (adresseSurchargee instanceof AdresseCivile) {

			final AdresseCivile a = (AdresseCivile) adresseSurchargee;
			final RegDate debut = a.getDateDebut();
			final RegDate fin = a.getDateFin();
			final EnumTypeAdresse type = a.getType();

			final PersonnePhysique habitant = (PersonnePhysique) tiers; /* par définition, seul un habitant peut posséder une adresse civile */
			final AdressesCiviles adressesCiviles = getAdressesCiviles(habitant, adresseSurchargee.getDateDebut(), strict);
			Assert.notNull(adressesCiviles);

			final Adresse adresseCivile = adressesCiviles.ofType(type);
			try {
				surcharge = new AdresseCivileAdapter(adresseCivile, debut, fin, Source.FISCALE, false, serviceInfra);
			}
			catch (DonneesCivilesException e) {
				throw new AdresseDataException(e);
			}
		}
		else if (adresseSurchargee instanceof AdresseAutreTiers) {

			final AdresseAutreTiers a = (AdresseAutreTiers) adresseSurchargee;
			final RegDate debut = a.getDateDebut();
			final RegDate fin = a.getDateFin();
			final Long id = a.getAutreTiersId();
			final Tiers autreTiers = tiersDAO.get(id, true);
			final TypeAdresseFiscale type = TypeAdresseFiscale.fromCore(a.getType());
			Assert.notNull(autreTiers);

			final int nextDepth = oneLevelDeeper(callDepth, tiers, autreTiers, adresseSurchargee);
			final AdresseGenerique autreAdresse = getAdresseFiscale(autreTiers, type, adresseSurchargee.getDateDebut(), nextDepth, strict);
			if (autreAdresse != null) {
				surcharge = new AdresseGeneriqueAdapter(autreAdresse, debut, fin, AdresseGenerique.Source.FISCALE, false);
			}
		}
		else {
			throw new NotImplementedException("Type d'adresse [" + adresseSurchargee.getClass().getSimpleName() + "] inconnu");
		}
		return surcharge;
	}

	/**
	 * Complète les adresses de base avec les adresses civiles spécifiées entre la date de début et de celle de fin. Cette méthode part du principe qu'il n'existe aucune adresse de base dans la plage
	 * [debut; fin].
	 *
	 * @param adressesBase   les adresses de base sur lesquelles seront ajoutées les adresses civiles
	 * @param debut          la date de début de la plage à compléter
	 * @param fin            la date de fin (comprise) de la plage à compléter
	 * @param adresseCiviles les adresses civiles utilisées pour compléter les adresses de base
	 * @param strict         si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private void fillAdressesCivilesSlice(List<AdresseGenerique> adressesBase, RegDate debut, RegDate fin, List<Adresse> adresseCiviles, boolean strict) throws AdresseException {
		for (Adresse adresse : adresseCiviles) {
			final RegDate adresseDebut = adresse.getDateDebut();
			final RegDate adresseFin = adresse.getDateFin();

			if ((adresseDebut == null || fin == null || adresseDebut.isBeforeOrEqual(fin))
					&& (adresseFin == null || debut == null || adresseFin.isAfterOrEqual(debut))) {
				RegDate debutValidite = RegDateHelper.maximum(adresseDebut, debut, NullDateBehavior.EARLIEST);
				RegDate finValidite = RegDateHelper.minimum(adresseFin, fin, NullDateBehavior.LATEST);
				try {
					final AdresseCivileAdapter a = new AdresseCivileAdapter(adresse, debutValidite, finValidite, true, serviceInfra);
					adressesBase.add(a);
				}
				catch (DonneesCivilesException e) {
					if (strict) {
						throw new AdresseDataException(e);
					}
					// en mode non-strict, on ignore simplement l'adresse en erreur
				}
			}
		}
	}

	/**
	 * Complète les adresses de base avec les adresses PM spécifiées entre la date de début et de celle de fin. Cette méthode part du principe qu'il n'existe aucune adresse de base dans la plage [debut;
	 * fin].
	 *
	 * @param adressesBase les adresses de base sur lesquelles seront ajoutées les adresses PM
	 * @param debut        la date de début de la plage à compléter
	 * @param fin          la date de fin (comprise) de la plage à compléter
	 * @param adressePM    les adresses PM utilisées pour compléter les adresses de base
	 */
	private void fillAdressesPMSlice(List<AdresseGenerique> adressesBase, RegDate debut, RegDate fin, List<AdresseEntreprise> adressePM) {
		for (AdresseEntreprise adresse : adressePM) {
			final RegDate adresseDebut = adresse.getDateDebutValidite();
			final RegDate adresseFin = adresse.getDateFinValidite();

			if ((adresseDebut == null || fin == null || adresseDebut.isBeforeOrEqual(fin))
					&& (adresseFin == null || debut == null || adresseFin.isAfterOrEqual(debut))) {
				RegDate debutValidite = RegDateHelper.maximum(adresseDebut, debut, NullDateBehavior.EARLIEST);
				RegDate finValidite = RegDateHelper.minimum(adresseFin, fin, NullDateBehavior.LATEST);
				adressesBase.add(new AdressePMAdapter(adresse, debutValidite, finValidite, Source.PM, true));
			}
		}
	}

	/**
	 * Converti les adresses civiles spécifiées en adresses fiscales.
	 * <p/>
	 * La régle de mapping entre les adresses civiles et fiscales est :
	 * <p/>
	 * <pre>
	 * Civil Fiscal
	 * ----- ------
	 * Courrier  ----- Courrier
	 *             `-- Représentation
	 * Principal ----- Domicile
	 *             `-- Poursuite
	 * Secondaire      (non-mappée)
	 * Tutelle         (non-mappée)
	 * </pre>
	 *
	 * @param adressesCiviles        les adresses civiles de base
	 * @param dateDebutHisto         la date de début de la plage à convertir
	 * @param dateFinHisto           la date de fin (comprise) de la plage à convertir
	 * @param adressesCivilesDefault les adresses civiles par défaut utilisées pour boucher les trous dans les adresses civiles de base
	 * @param strict                 si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return les adresses génériques qui représentent les adresses civiles.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private List<AdresseGenerique> initAdressesCivilesHisto(List<Adresse> adressesCiviles, RegDate dateDebutHisto, RegDate dateFinHisto,
	                                                        List<Adresse> adressesCivilesDefault, boolean strict) throws AdresseException {

		/*
		 * Adapte la liste des adresses civiles
		 */
		List<AdresseGenerique> adresses = new ArrayList<AdresseGenerique>();

		for (Adresse adresse : adressesCiviles) {
			try {
				adresses.add(new AdresseCivileAdapter(adresse, false, serviceInfra));
			}
			catch (DonneesCivilesException e) {
				if (strict) {
					throw new AdresseDataException(e);
				}
				// en mode non-strict, on ignore simplement l'adresse en erreur
			}
		}


		/*
		 * Bouche tous les éventuels trous avec les adresses par défaut
		 */
		List<AdresseGenerique> defaults = new ArrayList<AdresseGenerique>();

		if (adresses.size() > 0) {
			RegDate courante = dateDebutHisto;
			for (AdresseGenerique adresse : adresses) {

				final RegDate debut = adresse.getDateDebut();
				final boolean trouDetecte = (courante == null && debut != null)
						|| (courante != null && debut != null && courante.isBefore(debut));

				if (trouDetecte) {
					fillAdressesCivilesSlice(defaults, courante, debut, adressesCivilesDefault, strict);
				}

				final RegDate fin = adresse.getDateFin();
				courante = (fin == null ? null : fin.getOneDayAfter());
			}
			if ((dateFinHisto == null && courante != null) || (dateFinHisto != null && courante != null && courante.isBefore(dateFinHisto))) {
				fillAdressesCivilesSlice(defaults, courante, dateFinHisto, adressesCivilesDefault, strict);
			}
		}
		else {
			fillAdressesCivilesSlice(defaults, dateDebutHisto, dateFinHisto, adressesCivilesDefault, strict);
		}

		if (defaults.size() > 0) {
			adresses.addAll(defaults);
			Collections.sort(adresses, new DateRangeComparator<AdresseGenerique>());
		}

		return adresses;
	}

	/**
	 * Converti les adresses PM spécifiées en adresses fiscales.
	 * <p/>
	 * La régle de mapping entre les adresses PM et fiscales est :
	 * <p/>
	 * <pre>
	 * PM              Fiscal
	 * -----           ------
	 * Courrier  ----- Courrier
	 *             `-- Représentation
	 * Siège     ----- Domicile
	 *             `-- Poursuite
	 * Facturation     (non-mappée)
	 * </pre>
	 *
	 * @param adressesPM        les adresses PM de base
	 * @param dateDebutHisto    la date de début de la plage à convertir
	 * @param dateFinHisto      la date de fin (comprise) de la plage à convertir
	 * @param adressesPMDefault les adresses PM par défaut utilisées pour boucher les trous dans les adresses PM de base
	 * @return les adresses génériques qui représentent les adresses PM.
	 */
	private List<AdresseGenerique> initAdressesPMHisto(List<AdresseEntreprise> adressesPM, RegDate dateDebutHisto, RegDate dateFinHisto,
	                                                   List<AdresseEntreprise> adressesPMDefault) {

		/*
		 * Adapte la liste des adresses civiles
		 */
		List<AdresseGenerique> adresses = new ArrayList<AdresseGenerique>();
		for (AdresseEntreprise adresse : adressesPM) {
			adresses.add(new AdressePMAdapter(adresse, false));
		}

		/*
		 * Bouche tous les éventuels trous avec les adresses par défaut
		 */
		List<AdresseGenerique> defaults = new ArrayList<AdresseGenerique>();

		if (adresses.size() > 0) {
			RegDate courante = dateDebutHisto;
			for (AdresseGenerique adresse : adresses) {

				final RegDate debut = adresse.getDateDebut();
				final boolean trouDetecte = (courante == null && debut != null)
						|| (courante != null && debut != null && courante.isBefore(debut));

				if (trouDetecte) {
					fillAdressesPMSlice(defaults, courante, debut, adressesPMDefault);
				}

				final RegDate fin = adresse.getDateFin();
				courante = (fin == null ? null : fin.getOneDayAfter());
			}
			if ((dateFinHisto == null && courante != null) || (dateFinHisto != null && courante != null && courante.isBefore(dateFinHisto))) {
				fillAdressesPMSlice(defaults, courante, dateFinHisto, adressesPMDefault);
			}
		}
		else {
			fillAdressesPMSlice(defaults, dateDebutHisto, dateFinHisto, adressesPMDefault);
		}

		if (defaults.size() > 0) {
			adresses.addAll(defaults);
			Collections.sort(adresses, new DateRangeComparator<AdresseGenerique>());
		}

		return adresses;
	}

	/**
	 * Surcharge la liste d'adresses spécifiées avec une liste d'adresse de tiers.
	 *
	 * @param tiers               un tiers dont on veut calculer les adresses
	 * @param adresses            les adresses génériques de base
	 * @param adressesSurchargees une liste d'adresses tiers à utiliser comme surcharge sur les adresses de base
	 * @param sourceSurcharge     valeur de surcharge pour les adresses surchargées, ou <b>null</b> pour garder la source des adresses originelles.
	 * @param defaultSurcharge    valeur de surcharge pour les adresses surchargées, ou <b>null</b> pour garder le défaut des adresses originelles.
	 * @param callDepth           paramètre technique pour éviter les récursions infinies
	 * @param strict              si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return les adresses génériques résultant de la surcharge des adresses de base avec les adresses de surcharge.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	private List<AdresseGenerique> surchargeAdressesTiersHisto(Tiers tiers, List<AdresseGenerique> adresses, List<AdresseTiers> adressesSurchargees, Source sourceSurcharge, Boolean defaultSurcharge,
	                                                           int callDepth, boolean strict
	) throws AdresseException {

		if (adressesSurchargees == null || adressesSurchargees.size() == 0) {
			return adresses;
		}

		List<AdresseGenerique> adresseSurchargeesGeneriques = new ArrayList<AdresseGenerique>();
		for (AdresseTiers adresse : adressesSurchargees) {
			adresseSurchargeesGeneriques.add(resolveAdresseSurchargee(tiers, adresse, callDepth + 1, strict));
		}

		return AdresseMixer.override(adresses, adresseSurchargeesGeneriques, sourceSurcharge, defaultSurcharge);
	}

	/**
	 * Surcharge la liste d'adresses spécifiées avec une autre liste d'adresse.
	 *
	 * @param adresses            les adresses génériques de base
	 * @param adressesSurchargees une liste d'adresses génériques à utiliser comme surcharge sur les adresses de base
	 * @param source              la source des adresses de surcharge
	 * @param isDefault           <b>vrai</b> si les adresses de surcharge sont des adresses par défaut, <b>faux</b> si ce n'est pas le cas et <b>null</b> si cette information est inconnue.
	 * @return les adresses génériques résultant de la surcharge des adresses de base avec les adresses de surcharge.
	 */
	private List<AdresseGenerique> surchargeAdressesHisto(List<AdresseGenerique> adresses, List<AdresseGenerique> adressesSurchargees,
	                                                      Source source, Boolean isDefault) {
		return AdresseMixer.override(adresses, adressesSurchargees, source, isDefault);
	}

	/**
	 * {@inheritDoc}
	 */
	public Tiers addAdresse(Tiers tiers, AdresseTiers adresse) {

		final TypeAdresseTiers usage = adresse.getUsage();
		final RegDate date = adresse.getDateDebut().getOneDayBefore();

		/*
		 * On ferme l'adresse tiers courante à la veille, si elle existe. Pour les adresses en provenance du civil, il n'y a rien à faire
		 * car on ne peut pas changer les adresses dans le registre civil (elles sont automatiquement "fermée" par le service de résolution
		 * des adresses)
		 */
		AdresseTiers adresseCourante = tiers.getAdresseTiersAt(-1, usage); // = dernière adresse tiers
		if (adresseCourante != null && adresseCourante.getDateFin() == null) {
			adresseCourante.setDateFin(date);
		}

		// Et on ajoute la nouvelle adresse
		tiers.addAdresseTiers(adresse);

		return tiersDAO.save(tiers);
	}

	/**
	 * {@inheritDoc}
	 */
	public void annulerAdresse(AdresseTiers adresse) {

		final TypeAdresseTiers usage = adresse.getUsage();
		final Tiers tiers = adresse.getTiers();
		Assert.notNull(tiers);

		// On rouvre l'adresse fiscale précédente, si elle existe *et* qu'elle est accolée à l'adresse annulée
		final AdresseTiers adressePrecedente = tiers.getAdresseTiersAt(-2, usage); // = avant-dernière adresse tiers
		final RegDate dateFinAdressePrecedente = (adressePrecedente == null ? null : adressePrecedente.getDateFin()); // [UNIREG-1580]

		if (dateFinAdressePrecedente != null && dateFinAdressePrecedente.getOneDayAfter().equals(adresse.getDateDebut())) {
			adressePrecedente.setDateFin(null);
		}

		// On annule l'adresse spécifiée
		adresse.setAnnule(true);
	}

	/**
	 * Ferme une adresse fiscale a une date donnée
	 *
	 * @param adresse l'adresse à fermer
	 * @param dateFin date de fermeture de l'adresse.
	 */
	public void fermerAdresse(AdresseTiers adresse, RegDate dateFin) {
		adresse.setDateFin(dateFin);
	}

	private static boolean notEmpty(final String string) {
		return string != null && string.trim().length() > 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getNomCourrier(Tiers tiers, RegDate date, boolean strict) throws AdresseException {

		final AdresseEnvoiDetaillee adresse = getAdresseEnvoi(tiers, date, TypeAdresseFiscale.COURRIER, strict);

		List<String> list = adresse.getNomPrenom();

		/*
		 * Cas spécial du débiteur où il est important d'afficher le complément du nom (ligne pour adresse) en plus des noms et prénoms
		 */
		if (tiers instanceof DebiteurPrestationImposable && adresse.getPourAdresse() != null) {
			list = new ArrayList<String>(list);
			list.add(adresse.getPourAdresse());
		}

		return list;
	}

	/**
	 * Calcul le nom courrier
	 *
	 * @param numeroIndividu le numéro de l'individu dont on veut connaître le nom de courrier
	 * @return le nom courrier de l'individu spécifié
	 */
	public String getNomCourrier(long numeroIndividu) {
		final Individu individu = serviceCivilService.getIndividu(numeroIndividu, null);
		if (individu == null) {
			throw new IndividuNotFoundException(numeroIndividu);
		}
		return tiersService.getNomPrenom(individu);
	}

	public AdresseGenerique getDerniereAdresseVaudoise(Tiers tiers, TypeAdresseFiscale type) throws AdresseException {
		final AdressesFiscalesHisto adressesHistoriques = getAdressesFiscalHisto(tiers, false);
		final List<AdresseGenerique> listeAdresse = adressesHistoriques.ofType(type);
		if (listeAdresse != null) {

			// Tri des adresses
			Collections.sort(listeAdresse, new DateRangeComparator<AdresseGenerique>());

			final ListIterator<AdresseGenerique> iter = listeAdresse.listIterator(listeAdresse.size());
			while (iter.hasPrevious()) {
				final AdresseGenerique adresseGenerique = iter.previous();
				final CommuneSimple commune;
				try {
					commune = serviceInfra.getCommuneByAdresse(adresseGenerique);
				}
				catch (InfrastructureException e) {
					throw new AdresseDataException(e);
				}
				if (commune != null && commune.isVaudoise()) {
					return adresseGenerique;
				}
			}
		}
		return null;
	}


	private static int oneLevelDeeper(int callDepth, Tiers tiers, Tiers autreTiers, AdresseTiers adresseSurchargee) throws AdressesResolutionException {

		if (callDepth >= MAX_CALL_DEPTH) {
			AdressesResolutionException exception = new AdressesResolutionException(
					"Cycle infini détecté dans la résolution des adresses ! " + "Veuillez vérifier les adresses des tiers n°"
							+ tiers.getNumero() + " et n°" + autreTiers.getNumero() + ".");
			exception.addTiers(tiers);
			exception.addTiers(autreTiers);
			exception.addAdresse(adresseSurchargee);
			throw exception;
		}

		return callDepth + 1;
	}

	/**
	 * {@inheritDoc}
	 */
	public TypeAffranchissement getTypeAffranchissement(AdresseGenerique adresse) {
		Assert.notNull(adresse);

		final Integer noPays = adresse.getNoOfsPays();
		if (noPays == null) {
			// adresse suisse
			return TypeAffranchissement.SUISSE;
		}

		return serviceInfra.getTypeAffranchissement(noPays);
	}

	public ResolutionAdresseResults ResoudreAdresse(RegDate dateTraitement, int nbThreads, StatusManager status) {
		ResolutionAdresseProcessor processor = new ResolutionAdresseProcessor(this, adresseTiersDAO, serviceInfra, transactionManager);
		return processor.run(dateTraitement, nbThreads, status);
	}
}
