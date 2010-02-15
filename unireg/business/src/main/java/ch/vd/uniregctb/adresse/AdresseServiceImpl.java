package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateHelper;
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
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
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
import ch.vd.uniregctb.tiers.TiersHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;

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
	private ServiceInfrastructureService serviceInfra;
	private ServicePersonneMoraleService servicePM;
	private ServiceCivilService serviceCivilService;

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public ServiceInfrastructureService getServiceInfra() {
		return serviceInfra;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public ServicePersonneMoraleService getServicePM() {
		return servicePM;
	}

	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
	}

	public ServiceCivilService getServiceCivilService() {
		return serviceCivilService;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public AdresseServiceImpl() {
	}

	protected AdresseServiceImpl(TiersService tiersService, ServiceInfrastructureService serviceInfra,
			ServicePersonneMoraleService servicePM, ServiceCivilService serviceCivilService) {
		this.tiersService = tiersService;
		this.serviceInfra = serviceInfra;
		this.servicePM = servicePM;
		this.serviceCivilService = serviceCivilService;
	}

	/**
	 * {@inheritDoc}
	 */
	public AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers, RegDate date, boolean strict) throws AdresseException {
		Assert.notNull(tiers);

		final AdresseEnvoiDetaillee adresseEnvoi = new AdresseEnvoiDetaillee();

		fillDestinataire(adresseEnvoi, tiers);

		final Tiers autreTiers = getAutreTiers(tiers, date);
		if (autreTiers != null) {
			// Cas spécial d'un tiers ayant son adresse pointant sur celle d'un autre tiers
			adresseEnvoi.addPourAdresse(getPourAdresse(autreTiers));
			fillDestination(adresseEnvoi, autreTiers, date, strict);
		}
		else {
			fillDestination(adresseEnvoi, tiers, date, strict);
		}

		return adresseEnvoi;
	}

	/**
	 * Recherche l'autre tiers dans le cas d'un tiers ayant son adresse qui pointe sur celle d'un autre tiers.
	 *
	 * @return l'autre tiers
	 */
	private Tiers getAutreTiers(Tiers tiers, RegDate date) {

		Tiers autreTiers = null;

		// Cas spécial d'un tiers ayant son adresse pointant sur celle d'un autre tiers
		final AdresseTiers adresseCourrier = tiers.getAdresseActive(TypeAdresseTiers.COURRIER, date);
		if (adresseCourrier != null && adresseCourrier instanceof AdresseAutreTiers) {

			final AdresseAutreTiers adresseAutreTiers = (AdresseAutreTiers) adresseCourrier;
			autreTiers = adresseAutreTiers.getAutreTiers();
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
			nomPrenom1 = getNomPrenom((PersonnePhysique) ctb);
		}
		else if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;
			/* Récupère la vue historique complète du ménage (date = null) */
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, null);

			final PersonnePhysique principal = ensemble.getPrincipal();
			if (principal != null) {
				nomPrenom1 = getNomPrenom(principal);
			}

			final PersonnePhysique conjoint = ensemble.getConjoint();
			if (conjoint != null) {
				final String np = getNomPrenom(conjoint);
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

		final AdresseGenerique adresse = getAdresseFiscale(ctb, TypeAdresseTiers.COURRIER, date, false);
		final String rueEtNumero = adresse != null ? buildRueEtNumero(adresse) : null;
		final String npa = adresse != null ? adresse.getNumeroPostal() : null;
		final String localite = adresse != null ? adresse.getLocalite() : null;
		final String pays = adresse != null ? buildPays(adresse, true) : null;

		return new AdresseCourrierPourRF(nomPrenom1, nomPrenom2, rueEtNumero, npa, localite, pays);
	}

	/**
	 * Remplis les lignes correspondant à l'identification de la personne destinataire.
	 */
	private void fillDestinataire(AdresseEnvoiDetaillee adresse, Tiers tiers) {

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique personne = (PersonnePhysique) tiers;
			adresse.addFormulePolitesse(getFormulePolitesse(personne));
			adresse.addNomPrenom(getNomPrenom(personne));
		}
		else if (tiers instanceof MenageCommun) {
			MenageCommun menageCommun = (MenageCommun) tiers;
			/* Récupère la vue historique complète du ménage (date = null) */
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, null);

			final PersonnePhysique principal = ensemble.getPrincipal();
			if (principal != null) {
				adresse.addFormulePolitesse(getFormulePolitesse(ensemble));
				adresse.addNomPrenom(getNomPrenom(principal));
			}

			final PersonnePhysique conjoint = ensemble.getConjoint();
			if (conjoint != null) {
				adresse.addNomPrenom(getNomPrenom(conjoint));
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			final String nom1 = debiteur.getNom1();
			if (nom1 != null) {
				adresse.addNomPrenom(nom1);
			}
			final String nom2 = debiteur.getNom2();
			if (nom2 != null) {
				adresse.addNomPrenom(nom2);
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
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, Tiers tiers, RegDate date, boolean strict) throws AdresseException {

		if (tiers instanceof PersonnePhysique) {
			fillDestination(adresse, (PersonnePhysique) tiers, date, strict);
		}
		else if (tiers instanceof MenageCommun) {
			fillDestination(adresse, (MenageCommun) tiers, date, strict);
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			fillDestination(adresse, (DebiteurPrestationImposable) tiers, date, strict);
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			fillDestination(adresse, (CollectiviteAdministrative) tiers);
		}
		else if (tiers instanceof AutreCommunaute) {
			fillDestination(adresse, (AutreCommunaute) tiers, date, strict);
		}
		else if (tiers instanceof Entreprise) {
			fillDestination(adresse, (Entreprise) tiers, date, strict);
		}
		else {
			throw new NotImplementedException("Type de tiers [" + tiers.getNatureTiers() + "] inconnu");
		}
	}

	/**
	 * Rempli l'adresse de destination associée à une personne physique.
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, PersonnePhysique personne, RegDate date, boolean strict) throws AdresseException {

		AdresseGenerique adresseCourrier = getAdresseFiscale(personne, TypeAdresseTiers.COURRIER, date, strict);
		if (adresseCourrier != null) {

			switch (adresseCourrier.getSource()) {
			case REPRESENTATION:
				final Tiers representant = getRepresentant(personne, TypeAdresseRepresentant.REPRESENTATION, date);
				if (representant != null) {
					/* la personne possède un representant : on envoie le courrier chez son conseiller */
					adresse.addPourAdresse(getPourAdresse(representant));
				}
				break;
			case CONSEIL_LEGAL:
				final Tiers conseil = getRepresentant(personne, TypeAdresseRepresentant.CONSEIL_LEGAL, date);
				if (conseil != null) {
					/* la personne possède un conseil légal : on envoie le courrier chez son conseiller */
					adresse.addPourAdresse(getPourAdresse(conseil));
				}
				break;
			case TUTELLE:
				final Tiers tuteur = getRepresentant(personne, TypeAdresseRepresentant.TUTELLE, date);
				if (tuteur != null) {
					/* la personne est sous tutelle : on envoie le courrier chez son tuteur */
					adresse.addPourAdresse(getPourAdresse(tuteur));
				}
				break;
			case CURATELLE:
				final Tiers curateur = getRepresentant(personne, TypeAdresseRepresentant.CURATELLE, date);
				if (curateur != null) {
					/* la personne est sous curatelle : on envoie le courrier chez son curateur */
					adresse.addPourAdresse(getPourAdresse(curateur));
				}
				break;
			}
			fillAdresseEnvoi(adresse, adresseCourrier);
		}
	}

	/**
	 * Rempli l'adresse de destination associée à une autre communauté
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, AutreCommunaute autre, RegDate date, boolean strict) throws AdresseException {

		final AdresseGenerique adresseCourrier = getAdresseFiscale(autre, TypeAdresseTiers.COURRIER, date, strict);
		if (adresseCourrier != null) {
			fillAdresseEnvoi(adresse, adresseCourrier);
		}
	}

	/**
	 * Rempli l'adresse de destination associée à une entreprise
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, Entreprise entreprise, RegDate date, boolean strict) throws AdresseException {

		final AdresseGenerique adresseCourrier = getAdresseFiscale(entreprise, TypeAdresseTiers.COURRIER, date, strict);
		if (adresseCourrier != null) {
			fillAdresseEnvoi(adresse, adresseCourrier);
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
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException en cas d'incohérence des données civiles
	 */
	private AdresseEnvoiDetaillee createAdresseEnvoi(Individu individu, RegDate date, boolean strict) throws AdresseException {

		AdresseEnvoiDetaillee adresse = new AdresseEnvoiDetaillee();
		adresse.addFormulePolitesse(getFormulePolitesse(individu));
		adresse.addNomPrenom(tiersService.getNomPrenom(individu));

		final AdressesCiviles adressesCourantes;
		try {
			adressesCourantes = serviceCivilService.getAdresses(individu.getNoTechnique(), date, strict);
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
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, MenageCommun menageCommun, RegDate date, boolean strict) throws AdresseException {

		final AdresseGenerique adresseCourrier = getAdresseFiscale(menageCommun, TypeAdresseTiers.COURRIER, date, strict);

		// Une adresse courrier n'existe pas forcément (exemple: couple en cours de création)
		if (adresseCourrier != null) {

			/* Récupère la vue historique complète du ménage (date = null) */
			final PersonnePhysique principal = getPrincipalPourAdresse(menageCommun, null);

			if (principal != null) { //cas des couple annulé
				// Cas de la tutelle, curatelle et du conseil légal
				final Source typeSource = adresseCourrier.getSource();
				final TypeAdresseRepresentant typeAdresseRepresentant = TypeAdresseRepresentant.getTypeAdresseRepresentantFromSource(typeSource);
				if (typeAdresseRepresentant != null) {
					final Tiers representant = getRepresentant(principal, typeAdresseRepresentant, date);
					Assert.notNull(representant);
					adresse.addPourAdresse(getPourAdresse(representant));
				}
			}

			// Rue, numéro et ville
			fillAdresseEnvoi(adresse, adresseCourrier);
		}
	}

	/**
	 * Rempli l'adresse de destination associée à un debiteur
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, DebiteurPrestationImposable debiteur, RegDate date, boolean strict)
			throws AdresseException {

		// Dans le cas normal d'un débiteur, le destinataire est le contribuable associé
		Tiers destinataire = debiteur.getContribuable();
		if (destinataire == null) {
			// Il peut cependant arrive que le débiteur ne possède pas de contribuable associé,
			// dans ce que le destinataire est le débiteur lui-même
			destinataire = debiteur;
		}
		Assert.notNull(destinataire);

		/*
		 * Dans le cas du débiteur, la destination est l'adresse du contribuable associé, sauf si le débiteur possède lui-même une adresse
		 * courrier
		 */

		AdresseGenerique adresseCourrier = getAdresseFiscale(debiteur, TypeAdresseTiers.COURRIER, date, strict);
		if (adresseCourrier == null) {
			adresseCourrier = getAdresseFiscale(destinataire, TypeAdresseTiers.COURRIER, date, strict);
		}

		// Une adresse courrier n'existe pas forcément (exemple: débiteur en cours de création)
		if (adresseCourrier != null) {
			fillAdresseEnvoi(adresse, adresseCourrier);
		}
	}

	/**
	 * Rempli l'adresse de destination associée à une collectivité administrative
	 */
	private void fillDestination(AdresseEnvoiDetaillee adresse, CollectiviteAdministrative collectivite) {

		// récupère la collectivité
		final int noColAdm = collectivite.getNumeroCollectiviteAdministrative().intValue();
		final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative collectiviteCivil;
		try {
			collectiviteCivil = serviceInfra.getCollectivite(noColAdm);
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Erreur en essayant de récupérer les informations de collectivité administrative", e);
		}
		Assert.notNull(collectiviteCivil, "Impossible de récupérer la collectivité administrative dans le civil " + noColAdm);

		final Adresse adresseCivile = collectiviteCivil.getAdresse();

		// rue + numéro
		final String rue = adresseCivile.getRue();
		final String numeroRue = adresseCivile.getNumero();
		if (notEmpty(rue)) {
			if (notEmpty(numeroRue)) {
				adresse.addRueEtNumero(rue + " " + numeroRue);
			}
			else {
				adresse.addRueEtNumero(rue);
			}
		}

		// case postale
		final String casePostale = adresseCivile.getCasePostale();
		if (casePostale != null) {
			adresse.addCasePostale(casePostale);
		}

		// npa + localité
		adresse.addNpaEtLocalite(adresseCivile.getNumeroPostal() + " " + adresseCivile.getLocalite());

		// pays
		final Integer noOfsPays = adresseCivile.getNoOfsPays();
		try {
			Pays pays = (noOfsPays == null ? null : serviceInfra.getPays(noOfsPays));
			if (pays != null && !pays.isSuisse()) {
				adresse.addPays(pays.getNomMinuscule());
			}
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Impossible de trouver le pays avec le numéro Ofs = " + noOfsPays);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public FormulePolitesse getFormulePolitesse(Tiers tiers) {

		final FormulePolitesse salutations;

		if (tiers instanceof PersonnePhysique) {
			salutations = getFormulePolitesse((PersonnePhysique) tiers);
		}
		else if (tiers instanceof MenageCommun) {
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
			salutations = getFormulePolitesse(ensemble);
		}
		else {
			// pas de formule de politesse pour les autres types de tiers
			salutations = null;
		}

		return salutations;
	}

	/**
	 * @return la formule de politesse pour l'adressage d'une personne physique
	 */
	private FormulePolitesse getFormulePolitesse(PersonnePhysique personne) {

		FormulePolitesse salutations;

		if (personne.isHabitant()) {
			if (!tiersService.isDecede(personne)) {
				if (tiersService.getSexe(personne).equals(Sexe.MASCULIN))
					salutations = FormulePolitesse.MONSIEUR;
				else
					salutations = FormulePolitesse.MADAME;
			}
			else {
				salutations = FormulePolitesse.HERITIERS;
			}
		}
		else {
			if (!tiersService.isDecede(personne)) {
				if (personne.getSexe() != null) {
					if (personne.getSexe().equals(Sexe.MASCULIN))
						salutations = FormulePolitesse.MONSIEUR;
					else
						salutations = FormulePolitesse.MADAME;
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
	 * Retourne la formule de politesse pour l'adressage d'un individu
	 */
	private FormulePolitesse getFormulePolitesse(Individu individu) {
		FormulePolitesse salutations;
		if (individu.getDateDeces() == null) {
			if (individu.isSexeMasculin())
				salutations = FormulePolitesse.MONSIEUR;
			else
				salutations = FormulePolitesse.MADAME;
		}
		else {
			salutations = FormulePolitesse.HERITIERS;
		}
		return salutations;
	}

	/**
	 * Retourne la formule de politesse pour l'adressage des parties d'un ménage commun
	 * <p>
	 * Voir le document 'ModeleDonnees.doc' v0.1, §4.2 Formats d'adresses
	 */
	protected FormulePolitesse getFormulePolitesse(EnsembleTiersCouple ensemble) {

		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();

		final Sexe sexePrincipal = tiersService.getSexe(principal);
		final Sexe sexeConjoint = (conjoint == null ? null : tiersService.getSexe(conjoint));

		final RegDate dateDecesPrincipal = tiersService.getDateDeces(principal);
		final RegDate dateDecesConjoint = (conjoint == null ? null : tiersService.getDateDeces(conjoint));

		// [UNIREG-749] la formule de politesse 'aux héritiers de' s'applique dès qu'un des deux tiers est décédé.
		if (dateDecesPrincipal != null || dateDecesConjoint != null) {
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
				else if (!principalMasculin && !conjointMasculin) {
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
	 * @return la ligne du prénom et du nom pour la personne physique spécifiée.
	 */
	private String getNomPrenom(PersonnePhysique personne) {

		String prenomNom = tiersService.getNomPrenom(personne);

		// [UNIREG-749] on applique un suffixe 'défunt' aux personnes décédées
		final RegDate dateDeces = tiersService.getDateDeces(personne);
		if (dateDeces != null) {
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
	 * Retourne la ligne "pour adresse" dans le cas d'un tiers
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
			line = POUR_ADRESSE + " " +getRaisonSociale(entreprise);
		}
		else {
			throw new NotImplementedException("Type de tiers [" + tiers.getNatureTiers() + "] non-implémenté");
		}

		return line;
	}

	/**
	 * Retourne la raison sociale pour l'adressage de la collectivité administrative spécifiée.
	 */
	private String getRaisonSociale(CollectiviteAdministrative collectivite) {
		try {
			ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative c = serviceInfra.getCollectivite(collectivite
					.getNumeroCollectiviteAdministrative().intValue());
			return c.getNomCourt();
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Impossible de trouver la collectivite administrative avec le numéro = "
					+ collectivite.getNumeroCollectiviteAdministrative(), e);
		}
	}

	/**
	 * Retourne la raison sociale pour l'adressage de la collectivité administrative spécifiée.
	 */
	private List<String> getRaisonSocialeLongue(CollectiviteAdministrative collectivite) {
		try {
			final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative c = serviceInfra.getCollectivite(collectivite
					.getNumeroCollectiviteAdministrative());

			final List<String> nomsComplets = new ArrayList<String>(3);
			if (!StringUtils.isEmpty(c.getNomComplet1())) {
				nomsComplets.add(c.getNomComplet1());
			}
			if (!StringUtils.isEmpty(c.getNomComplet2())) {
				nomsComplets.add(c.getNomComplet2());
			}
			if (!StringUtils.isEmpty(c.getNomComplet3())) {
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
		if (!StringUtils.isEmpty(pm.getRaisonSociale1())) {
			nomsComplets.add(pm.getRaisonSociale1());
		}
		if (!StringUtils.isEmpty(pm.getRaisonSociale2())) {
			nomsComplets.add(pm.getRaisonSociale2());
		}
		if (!StringUtils.isEmpty(pm.getRaisonSociale3())) {
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
			throw new RuntimeException("Impossible de trouver le pays avec le numéro Ofs = "+noOfsPays);
		}
	}

	/**
	 * Rempli l'adresse d'envoi en fonction de l'adresse civile/fiscale spécifiée.
	 */
	private void fillAdresseEnvoi(AdresseEnvoiDetaillee adresseEnvoi, final AdresseGenerique adresse) {
		Assert.notNull(adresse, "Une adresse doit être spécifiée.");

		final String complement = adresse.getComplement();
		if (notEmpty(complement)) {
			adresseEnvoi.addComplement(complement, OPTIONALITE_COMPLEMENT);
		}

		final String rueEtNumero = buildRueEtNumero(adresse);
		if (!StringUtils.isEmpty(rueEtNumero)) {
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
		if (!StringUtils.isEmpty(nomPays)) {
			adresseEnvoi.addPays(nomPays);
		}
	}

	/**
	 * @return <b>vrai</b> si le complément d'adresse spécifiée commence avec un "p.a.", un "chez", un "c/o", ou tout autre variantes reconnues.
	 */
	public static boolean isPrefixedByPourAdresse(final String complement) {
		return POUR_ADRESSE_PATTERN.matcher(complement).find();
	}

	/**
	 * Ajoute un préfixe "p.a." au complément s'il n'en possède pas déjà un.
	 *
	 * @param complement
	 *            le complément à préfixer
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
			adresses.poursuite = initAdressesPMHisto(adressesPM.sieges, debut, fin, adressesPM.courriers);

			adresses.representation = adresses.courrier;
			adresses.domicile = adresses.poursuite;
		}
		else {
			final AdressesCivilesHisto adressesCiviles = getAdressesCivilesHisto(tiers, strict);
			final RegDate debut = adressesCiviles.getVeryFirstDate();
			final RegDate fin = adressesCiviles.getVeryLastDate();

			adresses.courrier = initAdressesCivilesHisto(adressesCiviles.courriers, debut, fin, adressesCiviles.principales, strict);
			adresses.poursuite = initAdressesCivilesHisto(adressesCiviles.principales, debut, fin, adressesCiviles.courriers, strict);

			adresses.representation = adresses.courrier;
			adresses.domicile = adresses.poursuite;
		}

		/*
		 * Surcharge avec les adresses fiscales
		 */
		if (tiers instanceof MenageCommun) {
			/* Pour le cas du ménage commun, les adresses du principal sont utilisées comme premier défaut */
			final MenageCommun menage = (MenageCommun) tiers;
			final PersonnePhysique principal = getPrincipalPourAdresse(menage, null);
			final AdressesTiersHisto adressesPrincipal = TiersHelper.getAdressesTiersHisto(principal);

			if (adressesPrincipal != null) {
				adresses.courrier = surchargeAdressesTiersHisto(tiers, adresses.courrier, adressesPrincipal.courrier, callDepth + 1, strict);
				adresses.representation = surchargeAdressesTiersHisto(tiers, adresses.representation, adressesPrincipal.representation,
						callDepth + 1, strict);
				adresses.poursuite = surchargeAdressesTiersHisto(tiers, adresses.poursuite, adressesPrincipal.poursuite, callDepth + 1, strict);
				adresses.domicile = surchargeAdressesTiersHisto(tiers, adresses.domicile, adressesPrincipal.domicile, callDepth + 1, strict);
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			// Pour le cas du débiteur, les adresses du contribuable associé sont utilisées comme premier défaut. Il peut cependant arriver
			// que le débiteur ne possède pas de contribuable associé, dans ce cas on continue
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
			final Contribuable contribuable = debiteur.getContribuable();
			if (contribuable != null) {
				final AdressesFiscalesHisto adressesContribuable = getAdressesFiscalHisto(contribuable, callDepth + 1, strict);

				adresses.courrier = surchargeAdressesHisto(adresses.courrier, adressesContribuable.courrier, Source.CONTRIBUABLE, true, strict);
				adresses.representation = surchargeAdressesHisto(adresses.representation, adressesContribuable.representation,
						Source.CONTRIBUABLE, true, strict);
				adresses.poursuite = surchargeAdressesHisto(adresses.poursuite, adressesContribuable.poursuite, Source.CONTRIBUABLE, true, strict);
				adresses.domicile = surchargeAdressesHisto(adresses.domicile, adressesContribuable.domicile, Source.CONTRIBUABLE, true, strict);
			}
		}

		final AdressesTiersHisto adressesTiers = TiersHelper.getAdressesTiersHisto(tiers);
		adresses.courrier = surchargeAdressesTiersHisto(tiers, adresses.courrier, adressesTiers.courrier, callDepth + 1, strict);
		adresses.representation = surchargeAdressesTiersHisto(tiers, adresses.representation, adressesTiers.representation, callDepth + 1, strict);
		adresses.poursuite = surchargeAdressesTiersHisto(tiers, adresses.poursuite, adressesTiers.poursuite, callDepth + 1, strict);
		adresses.domicile = surchargeAdressesTiersHisto(tiers, adresses.domicile, adressesTiers.domicile, callDepth + 1, strict);

		/*
		 * Applique les défauts, de manière à avoir une adresse valide pour chaque type d'adresse
		 */
		appliqueDefautsAdressesFiscalesHisto(adresses);

		/*
		 * Si le tiers concerné possède un representant, on surchage avec l'adresse du représentant
		 */
		final List<AdresseGenerique> adressesRepresentant = getAdressesRepresentantHisto(tiers, TypeAdresseRepresentant.REPRESENTATION,	callDepth + 1, strict);
		adresses.courrier = AdresseMixer.override(adresses.courrier, adressesRepresentant, null, null);

		/*
		 * Si le tiers concerné possède un conseil légal, on surchage avec l'adresse du représentant
		 */
		final List<AdresseGenerique> adressesConseil = getAdressesRepresentantHisto(tiers, TypeAdresseRepresentant.CONSEIL_LEGAL,
				callDepth + 1, strict);
		adresses.courrier = AdresseMixer.override(adresses.courrier, adressesConseil, null, null);

		/*
		 * Si le tiers concerné est sous tutelle, on surchage les adresses courrier avec les adresses représentation du tuteur
		 */
		final List<AdresseGenerique> adressesTuteur = getAdressesTuteurOuCurateurHisto(tiers, TypeAdresseRepresentant.TUTELLE, callDepth + 1, strict);
		adresses.courrier = AdresseMixer.override(adresses.courrier, adressesTuteur, null, null);

		/*
		 * Si le tiers concerné est sous curatelle, on surchage les adresses courrier avec les adresses représentation du curateur
		 */

		final List<AdresseGenerique> adressesCuratelle = getAdressesTuteurOuCurateurHisto(tiers, TypeAdresseRepresentant.CURATELLE, callDepth + 1, strict);
		adresses.courrier = AdresseMixer.override(adresses.courrier, adressesCuratelle, null, null);

		return adresses;
	}

	/**
	 * Applique les règles de gestion des adresses par défaut sur les adresses fiscales historiques spécifiées.
	 * <p>
	 * Les règles sont les suivantes:
	 * <ul>
	 * <li>Les adresses 'courrier' et 'poursuite' peuvent être utilisées mutuellement comme défauts</li>
	 * <li>Les adresses 'representation' ont pour défauts les adresses 'courrier'</li>
	 * <li>Les adresses 'domicile' ont pour défauts les adresses 'poursuite'</li>
	 * </ul>
	 *
	 * @param adresses
	 *            les adresses fiscales
	 */
	private void appliqueDefautsAdressesFiscalesHisto(AdressesFiscalesHisto adresses) {

		adresses.courrier = appliqueDefautsAdressesFiscalesHisto(adresses.courrier, adresses.poursuite);
		adresses.courrier = appliqueDefautsAdressesFiscalesHisto(adresses.courrier, adresses.domicile);
		adresses.courrier = appliqueDefautsAdressesFiscalesHisto(adresses.courrier, adresses.representation);

		adresses.poursuite = appliqueDefautsAdressesFiscalesHisto(adresses.poursuite, adresses.courrier);
		adresses.representation = appliqueDefautsAdressesFiscalesHisto(adresses.representation, adresses.courrier);
		adresses.domicile = appliqueDefautsAdressesFiscalesHisto(adresses.domicile, adresses.poursuite);
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
	 * Applique les règles de gestion des adresses par défaut sur les adresses fiscales spécifiées.
	 * <p>
	 * Les règles sont les suivantes:
	 * <ul>
	 * <li>Les adresses 'courrier' et 'poursuite' peuvent être utilisées mutuellement comme défauts</li>
	 * <li>Les adresses 'representation' ont pour défauts les adresses 'courrier'</li>
	 * <li>Les adresses 'domicile' ont pour défauts les adresses 'poursuite'</li>
	 * </ul>
	 *
	 * @param adresse
	 * @param strict
	 */
	private AdresseGenerique appliqueDefautsAdressesFiscales(Tiers tiers, AdresseGenerique adresse, TypeAdresseTiers type, RegDate date,
	                                                         int callDepth, boolean strict) throws AdresseException {

		if (adresse == null) {
			switch (type) {
			case COURRIER:
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.POURSUITE, date, callDepth + 1, strict) : adresse);
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.DOMICILE, date, callDepth + 1, strict) : adresse);
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.REPRESENTATION, date, callDepth + 1, strict) : adresse);
				break;
			case POURSUITE:
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.COURRIER, date, callDepth + 1, strict) : adresse);
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.DOMICILE, date, callDepth + 1, strict) : adresse);
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.REPRESENTATION, date, callDepth + 1, strict) : adresse);
				break;
			case REPRESENTATION:
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.COURRIER, date, callDepth + 1, strict) : adresse);
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.POURSUITE, date, callDepth + 1, strict) : adresse);
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.DOMICILE, date, callDepth + 1, strict) : adresse);
				break;
			case DOMICILE:
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.POURSUITE, date, callDepth + 1, strict) : adresse);
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.COURRIER, date, callDepth + 1, strict) : adresse);
				adresse = (adresse == null ? getDefault(tiers, TypeAdresseTiers.REPRESENTATION, date, callDepth + 1, strict) : adresse);
				break;
			default:
				throw new IllegalArgumentException("Type d'adresse tiers inconnu = [" + type + "]");
			}
		}

		return adresse;
	}

	private AdresseGenerique getDefault(Tiers tiers, TypeAdresseTiers type, RegDate date, int callDepth, boolean strict) throws AdresseException {
		AdresseGenerique a = getAdresseFiscale(tiers, type, date, false, callDepth + 1, strict);
		return a == null ? null : new AdresseGeneriqueAdapter(a, null, true);
	}

	/**
	 * Applique les règles de gestion des adresses par défaut sur les adresses fiscales spécifiées.
	 * <p>
	 * Les règles sont les suivantes:
	 * <ul>
	 * <li>Les adresses 'courrier' et 'poursuite' peuvent être utilisées mutuellement comme défauts</li>
	 * <li>Les adresses 'representation' ont pour défauts les adresses 'courrier'</li>
	 * <li>Les adresses 'domicile' ont pour défauts les adresses 'poursuite'</li>
	 * </ul>
	 *
	 * @param adresses
	 *            les adresses fiscales
	 */
	private void appliqueDefautsAdressesFiscales(AdressesFiscales adresses) {

		// mapping par défaut
		if (adresses.courrier == null && adresses.poursuite != null) {
			adresses.courrier = new AdresseGeneriqueAdapter(adresses.poursuite, null, true);
		}
		if (adresses.courrier == null && adresses.domicile != null) {
			adresses.courrier = new AdresseGeneriqueAdapter(adresses.domicile, null, true);
		}
		if (adresses.courrier == null && adresses.representation != null) {
			adresses.courrier = new AdresseGeneriqueAdapter(adresses.representation, null, true);
		}

		if (adresses.poursuite == null && adresses.courrier != null) {
			adresses.poursuite = new AdresseGeneriqueAdapter(adresses.courrier, null, true);
		}

		if (adresses.representation == null && adresses.courrier != null) {
			adresses.representation = new AdresseGeneriqueAdapter(adresses.courrier, null, true);
		}

		if (adresses.domicile == null && adresses.poursuite != null) {
			adresses.domicile = new AdresseGeneriqueAdapter(adresses.poursuite, null, true);
		}
	}

	/**
	 * Retourne le représentant à la date donnée du tiers spécifié.
	 *
	 * @param tiers
	 *            le tiers potentiellement mis sous tutelle ou possèdant un conseil légal.
	 * @param date
	 *            la date de référence, ou null pour obtenir le représentant courant.
	 * @return le représentant, ou null si le tiers ne possède pas de représentant à la date spécifiée.
	 */
	private Tiers getRepresentant(Tiers tiers, TypeAdresseRepresentant type, RegDate date) {

		final RapportEntreTiers rapport = TiersHelper.getRapportSujetOfType(tiers, type.getTypeRapport(), date);
		if (rapport == null) {
			return null;
		}

		// info de la tutelle
		final Tiers representant = rapport.getObjet();
		Assert.isTrue(tiers == rapport.getSujet());

		return representant;
	}

	/**
	 * Retourne l'adresse du representant pour le tiers spécifié.
	 *
	 * @return l'adresse demandée, ou <b>null</b> si le tiers n'est pas sous tutelle.
	 */
	private AdresseGenerique getAdresseRepresentant(Tiers tiers, RegDate date, TypeAdresseRepresentant typeAdresseRepresentant, int callDepth, boolean strict) throws AdresseException {
		final AdresseGenerique adresseTuteur;

		if (tiers instanceof MenageCommun) {
			adresseTuteur = getAdresseRepresentantPourMenage((MenageCommun) tiers, date, typeAdresseRepresentant, callDepth + 1, strict);
		}
		else {
			adresseTuteur = getAdresseRepresentant(tiers, typeAdresseRepresentant, date, callDepth + 1, strict);
		}
		return adresseTuteur;
	}

	/**
	 * Retourne l'adresse du representant pour le ménage commun spécifié.
	 * <p>
	 * Dans le cas d'un ménage commun, l'adresse courrier du conjoint prime sur celle du tuteur (pour autant que le conjoint ne soit pas
	 * lui-même sous tutelle).
	 *
	 * @return l'adresse demandée, ou <b>null</b> si le principal du ménage n'est pas sous tutelle.
	 */
	private AdresseGenerique getAdresseRepresentantPourMenage(MenageCommun menage, RegDate date, TypeAdresseRepresentant typeAdresseRepresentant, int callDepth, boolean strict) throws AdresseException {

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, date);
		final PersonnePhysique principal = getPrincipalPourAdresse(menage, date);
		final PersonnePhysique conjoint = ensemble.getConjoint(principal);

		if (principal == null) {
			// pas de principal -> pas de tuteur non plus
			Assert.isNull(conjoint);
			return null;
		}

		/*
		 * On récupère l'adresse du tuteur du principal
		 */
		final AdresseGenerique adressesTuteur = getAdresseRepresentant(principal, typeAdresseRepresentant, date, callDepth + 1, strict);
		if (adressesTuteur == null) {
			// pas de tutelle
			return null;
		}

		if (conjoint == null) {
			// cas du marié seul
			return adressesTuteur;
		}

		AdresseGenerique adresse = adressesTuteur;

		/*
		 * On récupère l'adresses courrier du conjoint
		 */
		final AdresseGenerique courrierConjoint = getAdresseFiscale(conjoint, TypeAdresseTiers.COURRIER, date, true, callDepth + 1, strict);

		// On ignore le conjoint s'il est lui-même sous tutelle
		if (!Source.TUTELLE.equals(courrierConjoint.getSource()) && !Source.CURATELLE.equals(courrierConjoint.getSource()) ) {
			adresse = new AdresseGeneriqueAdapter(courrierConjoint, Source.CONJOINT, false);
		}

		return adresse;
	}

	/**
	 * {@inheritDoc}
	 */
	public AdresseGenerique getAdresseRepresentant(Tiers tiers, TypeAdresseRepresentant type, RegDate date, boolean strict)
			throws AdresseException {
		return getAdresseRepresentant(tiers, type, date, 0, strict);
	}

	private AdresseGenerique getAdresseRepresentant(Tiers tiers, TypeAdresseRepresentant type, RegDate date, int callDepth, boolean strict)
			throws AdresseException {

		final RapportEntreTiers rapport = TiersHelper.getRapportSujetOfType(tiers, type.getTypeRapport(), date);
		if (rapport == null) {
			return null;
		}

		// info de la représentation
		final Tiers representant = rapport.getObjet();
		Assert.isSame(tiers, rapport.getSujet());

		final RegDate debut = rapport.getDateDebut();
		final RegDate fin = rapport.getDateFin();

		// ajustement de la validité de l'adresse à la durée de la représentation
		final AdresseGenerique adressesRepresentant = getAdresseFiscale(representant, TypeAdresseTiers.REPRESENTATION, date, true,
				callDepth + 1, strict);
		if (adressesRepresentant == null) {
			Audit.warn("Le tiers n°" + representant.getNumero() + " est le représentant du tiers n°" + tiers.getNumero()
					+ ", mais il ne possède aucune adresse !");
			return null;
		}

		final AdresseGeneriqueAdapter adresse = new AdresseGeneriqueAdapter(adressesRepresentant, debut, fin, type.getTypeSource(), false);
		return adresse;
	}

	/**
	 * Retourne l'historique des adresses du representant pour le tiers spécifié.
	 *
	 * @param tiers
	 *            le tiers potentiellement sous mis sous tutelle.
	 * @param strict
	 * @return les adresses demandées, ou une liste vide si le tiers n'a jamais été sous tutelle.
	 */
	private List<AdresseGenerique> getAdressesTuteurOuCurateurHisto(Tiers tiers, TypeAdresseRepresentant typeAdresseRepresentant, int callDepth, boolean strict) throws AdresseException {

		Assert.isTrue(TypeAdresseRepresentant.TUTELLE.equals(typeAdresseRepresentant) || TypeAdresseRepresentant.CURATELLE.equals(typeAdresseRepresentant));

		final List<AdresseGenerique> adressesTuteur;

		if (tiers instanceof MenageCommun) {
			adressesTuteur = getAdressesTuteurOuCurateurOuRepresentantHisto((MenageCommun) tiers, typeAdresseRepresentant,  callDepth + 1, strict);
		}
		else {
			adressesTuteur = getAdressesRepresentantHisto(tiers, typeAdresseRepresentant, callDepth + 1, strict);
		}
		return adressesTuteur;
	}

	/**
	 * Retourne l'historique des adresses du representant pour le ménage commun spécifié.
	 * <p>
	 * Dans le cas d'un ménage commun, les adresses courrier du conjoint priment sur celles du tuteur (pour autant que le conjoint ne soit
	 * pas lui-même sous tutelle).
	 *
	 * @return les adresses demandées, ou une liste vide si le principal du ménage n'a jamais été sous tutelle.
	 */
	private List<AdresseGenerique> getAdressesTuteurOuCurateurOuRepresentantHisto(final MenageCommun menage, TypeAdresseRepresentant typeAdresseRepresentant, int callDepth, boolean strict) throws AdresseException {

		Assert.isTrue(TypeAdresseRepresentant.TUTELLE.equals(typeAdresseRepresentant) || TypeAdresseRepresentant.CURATELLE.equals(typeAdresseRepresentant));

		/* Récupère la vue historique complète du ménage (date = null) */
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, null);
		final PersonnePhysique principal = getPrincipalPourAdresse(menage, null);
		final PersonnePhysique conjoint = ensemble.getConjoint(principal);
		if (principal == null) {
			// ménage commun sans principal (tous les rapports sont annulés)
			return null;
		}

		/*
		 * On récupère l'historique des adresses 'tutelle' du principal
		 */
		final List<AdresseGenerique> adressesTuteur = getAdressesRepresentantHisto(principal, typeAdresseRepresentant,
				callDepth + 1, strict);
		if (adressesTuteur.isEmpty()) {
			// pas de tutelle
			return adressesTuteur;
		}

		if (conjoint == null) {
			// cas du marié seul
			return adressesTuteur;
		}

		/*
		 * On détermine les périodes durant lesquelles le principal est sous tutelle de manière continue
		 */
		final List<DateRange> ranges = DateRangeHelper.collateRange(adressesTuteur);

		/*
		 * On récupère les adresses du conjoint et on les filtre pour ne garder que celles valides durant les périodes calculées plus haut.
		 */
		final AdressesFiscalesHisto adressesConjoint = getAdressesFiscalHisto(conjoint, callDepth + 1, strict);
		if (strict) {
			verifieCoherenceAdresses(adressesConjoint.courrier, "Adresse de courrier", conjoint);
		}

		final List<AdresseGenerique> surcharge = new ArrayList<AdresseGenerique>();

		for (DateRange range : ranges) {
			final List<AdresseGenerique> adressesRange = AdresseMixer.extract(adressesConjoint.courrier, range.getDateDebut(), range
					.getDateFin());
			for (AdresseGenerique adresse : adressesRange) {
				// on ignore toutes les adresses où le conjoint est lui-même sous tutelle
				if (!Source.getSourceFormTypeAdresseRepresentant(typeAdresseRepresentant).equals(adresse.getSource())) {
					surcharge.add(new AdresseGeneriqueAdapter(adresse, Source.CONJOINT, false));
				}
			}
		}

		return AdresseMixer.override(adressesTuteur, surcharge, null, null);
	}

	/**
	 * Retourne l'historique des adresses 'représentation' (ajusté à la durée des mises-sous-tutelle) du représentant du tiers spécifié.
	 *
	 * @param tiers
	 *            le tiers potentiellement sous mis sous tutelle ou avec un conseil légal.
	 * @param strict
	 * @return les adresses demandées, ou une liste vide si le tiers n'a jamais été sous tutelle.
	 */
	private List<AdresseGenerique> getAdressesRepresentantHisto(Tiers tiers, TypeAdresseRepresentant type, int callDepth, boolean strict)
			throws AdresseException {

		List<AdresseGenerique> adresses = new ArrayList<AdresseGenerique>();

		final List<RapportEntreTiers> rapports = TiersHelper.getRapportSujetHistoOfType(tiers, type.getTypeRapport());
		if (rapports != null) {

			/* pour toutes les périodes de mise sous tutelles/conseil légal */
			for (RapportEntreTiers rapport : rapports) {

				final Tiers representant = rapport.getObjet();
				final RegDate debutRapport = rapport.getDateDebut();
				final RegDate finRapport = rapport.getDateFin();

				/*
				 * Extrait les adresses du représentant et ajuste-les pour qu'elles correspondent à la durée de la représentation
				 */
				final AdressesFiscalesHisto adressesRepresentant = getAdressesFiscalHisto(representant, callDepth + 1, strict);
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
	 * Vérifie que toutes les adresses données ont au moins une date de début de validité (à l'exception de la première qui peut être
	 * nulle), et que les dates de validités (si début et fin sont présentes) sont dans le bon ordre
	 *
	 * @param adresses
	 *            les adresses à tester
	 * @param descriptionContexte le context des données
	 * @param tiers le tiers auquel les adresses appartiennent
	 * @throws DonneesCivilesException en cas d'incohérence détectée des données
	 */
	private static void verifieCoherenceAdresses(List<AdresseGenerique> adresses, String descriptionContexte, Tiers tiers) throws AdresseException {
		final int size = adresses.size();
		for (int i = 0; i < size; ++i) {
			final AdresseGenerique adresse = adresses.get(i);
			// [UNIREG-1097] la première adresse peut avoir une date de début nulle, et la dernière peut avoir une date de fin nulle.
			final ValidationResults validationResult = DateRangeHelper.validate(adresse, (i == 0), (i == size - 1));
			if (validationResult.hasErrors()) {
				throw new AdresseDataException(validationResult);
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

		AdressesFiscales adresses = new AdressesFiscales();
		adresses.courrier = getAdresseFiscale(tiers, TypeAdresseTiers.COURRIER, date, true, 0, strict);
		adresses.representation = getAdresseFiscale(tiers, TypeAdresseTiers.REPRESENTATION, date, true, 0, strict);
		adresses.poursuite = getAdresseFiscale(tiers, TypeAdresseTiers.POURSUITE, date, true, 0, strict);
		adresses.domicile = getAdresseFiscale(tiers, TypeAdresseTiers.DOMICILE, date, true, 0, strict);
		return adresses;
	}

	/**
	 * {@inheritDoc}
	 */
	public AdresseGenerique getAdresseFiscale(Tiers tiers, TypeAdresseTiers type, RegDate date, boolean strict) throws AdresseException {

		if (tiers == null) {
			return null;
		}

		return getAdresseFiscale(tiers, type, date, true, 0, strict);
	}

	private AdresseGenerique getAdresseFiscale(Tiers tiers, TypeAdresseTiers type, RegDate date, boolean appliqueDefauts, int callDepth, boolean strict)
			throws AdresseException {

		if (tiers == null) {
			return null;
		}

		AdresseGenerique adresse = null;

		// 1ère priorité : l'adresse du tuteur
		if (adresse == null && TypeAdresseTiers.COURRIER.equals(type)) {
			// Note : seule la tutelle provoque une substitution de l'adresse, la curatelle n'est pas concernée.
			// TODO (fnr) note ci-dessus en contradiction avec le cas JIRA  UNIREG-1329
			final AdresseGenerique adresseTuteur = getAdresseRepresentant(tiers, date, TypeAdresseRepresentant.TUTELLE, callDepth + 1, strict);
			if (adresseTuteur != null) {
				adresse = adresseTuteur;
			}
		}

		// [UNIREG-1329] 1.5 eme priorité : l'adresse du curateur
		if (adresse == null && TypeAdresseTiers.COURRIER.equals(type)) {
			final AdresseGenerique adresseCurateur = getAdresseRepresentant(tiers, date, TypeAdresseRepresentant.CURATELLE, callDepth + 1, strict);
			if (adresseCurateur != null) {
				adresse = adresseCurateur;
			}
		}

		// [UNIREG-1329] 1.6 eme priorité : l'adresse du curateur
		if (adresse == null && TypeAdresseTiers.REPRESENTATION.equals(type)) {
			final AdresseGenerique adresseRepresentant = getAdresseRepresentant(tiers, date, TypeAdresseRepresentant.REPRESENTATION, callDepth + 1, strict);
			if (adresseRepresentant != null) {
				adresse = adresseRepresentant;
			}
		}

		// 2ème priorité : l'adresse du conseil légal
		if (adresse == null && TypeAdresseTiers.COURRIER.equals(type)) {
			/*
			 * Si le tiers concerné possède un conseil légal, on surchage avec l'adresse du représentant
			 */
			final AdresseGenerique adresseConseil = getAdresseRepresentant(tiers, TypeAdresseRepresentant.CONSEIL_LEGAL, date,
					callDepth + 1, strict);
			if (adresseConseil != null) {
				adresse = adresseConseil;
			}
		}

		// 3ème priorité : l'adresse définie au niveau fiscal sur le tiers lui-même
		if (adresse == null) {
			// Récupère l'adresse directement définie sur le tiers au niveau fiscal.
			final AdresseTiers adresseSurchargee = TiersHelper.getAdresseTiers(tiers, type, date);
			adresse = surchargeAdresseTiers(tiers, adresse, adresseSurchargee, callDepth + 1, strict);
		}

		// 4ème priorité : les adresses des éventuels tiers liés par un rapport
		if (adresse == null) {
			if (tiers instanceof MenageCommun) {
				// Pour le cas du ménage commun, les adresses du principal sont utilisées comme premier défaut
				final MenageCommun menage = (MenageCommun) tiers;
				final PersonnePhysique principal = getPrincipalPourAdresse(menage, null); // date nulle -> on s'intéresse à la vue historique du couple
				AdresseTiers adressePrincipal = TiersHelper.getAdresseTiers(principal, type, date);
				adresse = surchargeAdresseTiers(tiers, adresse, adressePrincipal, callDepth + 1, strict);
			}
			else if (tiers instanceof DebiteurPrestationImposable) {
				// Pour le cas du débiteur, les adresses du contribuable associé sont utilisées comme premier défaut
				final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
				final AdresseGenerique adresseContribuable = getAdresseFiscale(debiteur.getContribuable(), type, date, true, callDepth + 1, strict);
				adresse = surchargeAdresses(adresse, adresseContribuable, Source.CONTRIBUABLE, true);
			}
		}

		// 5ème priorité : les adresses du tiers en provenance du host
		if (adresse == null) {
			if (tiers instanceof Entreprise) {
				final AdressesPM adressePM = getAdressesPM((Entreprise) tiers, date);
				adresse = initAdressesPM(adressePM).ofType(type);
			}
			else {
				final AdressesCiviles adressesCiviles = getAdressesCiviles(tiers, date, strict);
				adresse = initAdressesCiviles(adressesCiviles).ofType(type);
			}
		}

		// 6ème et dernière priorité : les adresses par défaut
		if (adresse == null && appliqueDefauts) {
			adresse = appliqueDefautsAdressesFiscales(tiers, adresse, type, date, callDepth, strict);
		}

		return adresse;
	}

	/**
	 * Détermine le tiers principal pour le calcul des adresses du ménage commun.
	 * Selon [UNIREG-771] et comme intégré plus tard dans la spécification, le principal du
	 * couple ne sera pas toujours considéré comme principal pour le calcul des adresses.
	 *
	 * @param menageCommun le ménage commun
	 * @param date
	 * @return
	 */
	private PersonnePhysique getPrincipalPourAdresse(final MenageCommun menageCommun, RegDate date) {
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, date);
		final PersonnePhysique principal = ensemble.getPrincipal();
		PersonnePhysique principalOuVaudois = principal;
		/*
		 *  [UNIREG-771] : dans le cas d’un contribuable couple, l’adresse de domicile et l’adresse de courrier sont celles de l’individu principal
		 *  sauf si le contribuable principal quitte le canton ou la Suisse alors que le contribuable secondaire reste dans le canton.
		 */
		if (principal != null && !principal.isHabitant()) {
			final PersonnePhysique conjoint = ensemble.getConjoint(principal);
			if (conjoint != null  && conjoint.isHabitant()) {
				principalOuVaudois = conjoint;
			}
		}
		return principalOuVaudois;
	}

	/**
	 * Surcharge l'adresse spécifiée avec une adresse tiers.
	 */
	private AdresseGenerique surchargeAdresseTiers(Tiers tiers, AdresseGenerique adresse, AdresseTiers adresseSurchargee, int callDepth, boolean strict)
			throws AdresseException {

		if (adresseSurchargee != null) {
			return resolveAdresseSurchargee(tiers, adresseSurchargee, callDepth + 1, strict);
		}
		else {
			return adresse;
		}
	}

	/**
	 * Surcharge les adresses spécifiées avec d'autres adresses.
	 */
	private AdresseGenerique surchargeAdresses(AdresseGenerique adresse, AdresseGenerique adresseSurchargee, Source source,
			Boolean isDefault) {
		if (adresseSurchargee != null) {
			return new AdresseGeneriqueAdapter(adresseSurchargee, source, isDefault);
		}
		else {
			return adresse;
		}
	}

	/**
	 * Converti les adresses civiles spécifiées en adresses fiscales.
	 * <p>
	 * La régle de mapping entre les adresses civiles et fiscales est :
	 *
	 * <pre>
	 * Civil Fiscal
	 * ----- ------
	 * Courrier  ----- Courrier
	 *             `-- Représentation
	 * Principal ----- Poursuite
	 *             `-- Domicile
	 * Secondaire      (non-mappée)
	 * Tutelle         (non-mappée)
	 * </pre>
	 */
	private AdressesFiscales initAdressesCiviles(AdressesCiviles adressesCiviles) throws AdresseException {

		AdressesFiscales adresses = new AdressesFiscales();

		if (adressesCiviles != null) {
			// mapping standard
			try {
				if (adressesCiviles.courrier != null) {
					adresses.courrier = new AdresseCivileAdapter(adressesCiviles.courrier, false, serviceInfra);
					adresses.representation = adresses.courrier;
				}
				if (adressesCiviles.principale != null) {
					adresses.poursuite = new AdresseCivileAdapter(adressesCiviles.principale, false, serviceInfra);
					adresses.domicile = adresses.poursuite;
				}
			}
			catch (DonneesCivilesException e) {
				throw new AdresseDataException(e);
			}

			appliqueDefautsAdressesFiscales(adresses);
		}

		return adresses;
	}

	/**
	 * Converti les adresses PM spécifiées en adresses fiscales.
	 * <p>
	 * La régle de mapping entre les adresses PM et fiscales est :
	 *
	 * <pre>
	 * PM              Fiscal
	 * -----           ------
	 * Courrier  ----- Courrier
	 *             `-- Représentation
	 * Siège     ----- Poursuite
	 *             `-- Domicile
	 * Facturation     (non-mappée)
	 * </pre>
	 *
	 * TODO (msi) Demandez à Thierry la confirmation du mapping ci-dessous !
	 */
	private AdressesFiscales initAdressesPM(AdressesPM adressePM) {
		AdressesFiscales adresses = new AdressesFiscales();

		if (adressePM != null) {
			// mapping standard
			if (adressePM.courrier != null) {
				adresses.courrier = new AdressePMAdapter(adressePM.courrier, false);
				adresses.representation = adresses.courrier;
			}
			if (adressePM.siege != null) {
				adresses.poursuite = new AdressePMAdapter(adressePM.siege, false);
				adresses.domicile = adresses.poursuite;
			}

			appliqueDefautsAdressesFiscales(adresses);
		}

		return adresses;
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
			final PersonnePhysique principal = getPrincipalPourAdresse(menage, date);

			if (principal != null && principal.getNumeroIndividu() != null && principal.getNumeroIndividu() != 0) { //le principal peut être null dans le cas d'un mariage annulé
				adressesCiviles = getAdressesCiviles(principal, date, strict);
			}
			else {
				adressesCiviles = null;
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable || tiers instanceof AutreCommunaute) {
			/* ok, ces tiers ne possèdent pas d'adresse civiles par définition */
			adressesCiviles = null;
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			adressesCiviles = getAdressesCiviles((CollectiviteAdministrative) tiers);
		}
		else if (tiers instanceof Entreprise) {
			throw new IllegalArgumentException("Les entreprises ne possèdent pas d'adresse civiles !");
		}
		else {
			throw new NotImplementedException("Type de tiers [" + tiers.getNatureTiers() + "] inconnu");
		}
		return adressesCiviles;
	}

	/**
	 * Retourne les adresses civiles valide à la date donnée.
	 *
	 * @param habitant
	 *            l'habitant dont on recherche les adresses.
	 *
	 * @param date
	 *            la date de référence (attention, la précision est l'année !), ou null pour obtenir toutes les adresses existantes.
	 * @param strict	 si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses civiles de l'habitant spécifié.
	 */
	private AdressesCiviles getAdressesCiviles(PersonnePhysique habitant, RegDate date, boolean strict) throws AdresseException {
		try {
			return serviceCivilService.getAdresses(habitant.getNumeroIndividu(), date, strict);
		}
		catch (DonneesCivilesException e) {
			throw new AdresseDataException(e);
		}
	}

	/**
	 * Retourne l'historique des adresses civiles du tiers spécifié. Ou <b>null</b> si le tiers n'en possède pas.
	 */
	private AdressesCivilesHisto getAdressesCivilesHisto(Tiers tiers, boolean strict) throws AdresseException {

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
			/* Récupère la vue historique complète du ménage (date = null) */
			final PersonnePhysique principal = getPrincipalPourAdresse(menage, null);

			if (principal != null && principal.getNumeroIndividu() != null && principal.getNumeroIndividu() != 0) { //le principal peut être null dans le cas d'un couple annulé
				adressesCiviles = getAdressesCivilesHisto(principal, strict);
			}
			else {
				adressesCiviles = new AdressesCivilesHisto();
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable || tiers instanceof AutreCommunaute) {
			/* ok, ces tiers ne possèdent pas d'adresse civiles par définition */
			adressesCiviles = new AdressesCivilesHisto();
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			adressesCiviles = getAdressesCivilesHisto((CollectiviteAdministrative) tiers);
		}
		else if (tiers instanceof Entreprise) {
			throw new IllegalArgumentException("Les entreprises ne possèdent pas d'adresse civiles !");
		}
		else {
			throw new NotImplementedException("Type de tiers [" + tiers.getNatureTiers() + "] inconnu");
		}

		return adressesCiviles;
	}

	private AdressesCivilesHisto getAdressesCivilesHisto(PersonnePhysique habitant, boolean strict) throws AdresseException {
		try {
			return serviceCivilService.getAdressesHisto(habitant.getNumeroIndividu(), strict);
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
				collectiviteCivil = serviceInfra.getCollectivite(numero.intValue());
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
			ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative collectiviteCivil = serviceInfra.getCollectivite(collectivite
					.getNumeroCollectiviteAdministrative().intValue());
			Assert.notNull(collectiviteCivil);

			adresses.principales.add(collectiviteCivil.getAdresse());
			adresses.courriers.add(collectiviteCivil.getAdresse());
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Erreur dans la récupération des adresses", e);
		}

		return adresses;
	}

	public AdressesPM getAdressesPM(Entreprise entreprise, RegDate date) {
		final Long numeroEntreprise = entreprise.getNumeroEntreprise();
		Assert.notNull(numeroEntreprise);
		return servicePM.getAdresses(numeroEntreprise.longValue(), date);
	}

	public AdressesPMHisto getAdressesPMHisto(Entreprise entreprise) {
		final Long numeroEntreprise = entreprise.getNumeroEntreprise();
		Assert.notNull(numeroEntreprise);
		return servicePM.getAdressesHisto(numeroEntreprise.longValue());
	}

	/**
	 * Applique les règles business pour transformer l'adresse surchargée spécifiée en une adresse générique.
	 *
	 * @param tiers le tiers associé à l'adresse
	 * @param adresseSurchargee l'adresse de tiers à résoudre
	 * @param callDepth profondeur d'appel (technique)
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
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
				surcharge = new AdresseCivileAdapter(adresseCivile, debut, fin, Source.FISCALE, false,serviceInfra);
			}
			catch (DonneesCivilesException e) {
				throw new AdresseDataException(e);
			}
		}
		else if (adresseSurchargee instanceof AdresseAutreTiers) {

			final AdresseAutreTiers a = (AdresseAutreTiers) adresseSurchargee;
			final RegDate debut = a.getDateDebut();
			final RegDate fin = a.getDateFin();
			final Tiers autreTiers = a.getAutreTiers();
			final TypeAdresseTiers type = a.getType();
			Assert.notNull(autreTiers);

			if (callDepth >= MAX_CALL_DEPTH) {
				AdressesResolutionException exception = new AdressesResolutionException(
						"Cycle infini détecté dans la résolution des adresses ! " + "Veuillez vérifier les adresses des tiers n°"
								+ tiers.getNumero() + " et n°" + autreTiers.getNumero() + ".");
				exception.addTiers(tiers);
				exception.addTiers(autreTiers);
				exception.addAdresse(adresseSurchargee);
				throw exception;
			}

			final AdresseGenerique autreAdresse = getAdresseFiscale(autreTiers, type, adresseSurchargee.getDateDebut(), true, callDepth + 1, strict);
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
	 * Ajoute toutes les adresses civiles existantes entre la date de début et de celle de fin à la liste des adresses par défaut.
	 */
	private void fillAdressesCivilesSlice(List<AdresseGenerique> adressesDefault, RegDate debut, RegDate fin, List<Adresse> adresseCiviles, boolean strict) throws AdresseException {
		for (Adresse adresse : adresseCiviles) {
			final RegDate adresseDebut = adresse.getDateDebut();
			final RegDate adresseFin = adresse.getDateFin();

			if ((adresseDebut == null || fin == null || adresseDebut.isBeforeOrEqual(fin))
					&& (adresseFin == null || debut == null || adresseFin.isAfterOrEqual(debut))) {
				RegDate debutValidite = RegDateHelper.maximum(adresseDebut, debut, NullDateBehavior.EARLIEST);
				RegDate finValidite = RegDateHelper.minimum(adresseFin, fin, NullDateBehavior.LATEST);
				try {
					final AdresseCivileAdapter a = new AdresseCivileAdapter(adresse, debutValidite, finValidite, true, serviceInfra);
					adressesDefault.add(a);
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
	 * Ajoute toutes les adresses PM existantes entre la date de début et de celle de fin à la liste des adresses par défaut.
	 */
	private void fillAdressesPMSlice(List<AdresseGenerique> adressesDefault, RegDate debut, RegDate fin, List<AdresseEntreprise> adressePM) {
		for (AdresseEntreprise adresse : adressePM) {
			final RegDate adresseDebut = adresse.getDateDebutValidite();
			final RegDate adresseFin = adresse.getDateFinValidite();

			if ((adresseDebut == null || fin == null || adresseDebut.isBeforeOrEqual(fin))
					&& (adresseFin == null || debut == null || adresseFin.isAfterOrEqual(debut))) {
				RegDate debutValidite = RegDateHelper.maximum(adresseDebut, debut, NullDateBehavior.EARLIEST);
				RegDate finValidite = RegDateHelper.minimum(adresseFin, fin, NullDateBehavior.LATEST);
				adressesDefault.add(new AdressePMAdapter(adresse, debutValidite, finValidite, Source.PM, true));
			}
		}
	}

	/**
	 * Converti les adresses civiles spécifiées en adresses fiscales.
	 * <p>
	 * La régle de mapping entre les adresses civiles et fiscales est :
	 *
	 * <pre>
	 * Civil Fiscal
	 * ----- ------
	 * Courrier  ----- Courrier
	 *             `-- Représentation
	 * Principal ----- Poursuite
	 *             `-- Domicile
	 * Secondaire      (non-mappée)
	 * Tutelle         (non-mappée)
	 * </pre>
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
	 * <p>
	 * La régle de mapping entre les adresses PM et fiscales est :
	 *
	 * <pre>
	 * PM              Fiscal
	 * -----           ------
	 * Courrier  ----- Courrier
	 *             `-- Représentation
	 * Siège     ----- Poursuite
	 *             `-- Domicile
	 * Facturation     (non-mappée)
	 * </pre>
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
	 */
	private List<AdresseGenerique> surchargeAdressesTiersHisto(Tiers tiers, List<AdresseGenerique> adresses,
	                                                           List<AdresseTiers> adressesSurchargees, int callDepth, boolean strict) throws AdresseException {

		if (adressesSurchargees == null || adressesSurchargees.size() == 0) {
			return adresses;
		}

		List<AdresseGenerique> adresseSurchargeesGeneriques = new ArrayList<AdresseGenerique>();
		for (AdresseTiers adresse : adressesSurchargees) {
			adresseSurchargeesGeneriques.add(resolveAdresseSurchargee(tiers, adresse, callDepth + 1, strict));
		}

		return AdresseMixer.override(adresses, adresseSurchargeesGeneriques, null, null);
	}

	/**
	 * Surcharge la liste d'adresses spécifiées avec une autre liste d'adresse.
	 */
	private List<AdresseGenerique> surchargeAdressesHisto(List<AdresseGenerique> adresses, List<AdresseGenerique> adressesSurchargees,
	                                                      Source source, Boolean isDefault, boolean strict) {
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

		return tiersService.getTiersDAO().save(tiers);
	}

	/**
	 * {@inheritDoc}
	 */
	public void annulerAdresse(AdresseTiers adresse) {

		final TypeAdresseTiers usage = adresse.getUsage();
		final Tiers tiers = adresse.getTiers();
		Assert.notNull(tiers);

		// On rouvre l'adresse fiscale précédente, si elle existe *et* qu'elle est accolée à l'adresse annulée
		AdresseTiers adressePrecedente = tiers.getAdresseTiersAt(-2, usage); // = avant-dernière adresse tiers
		if (adressePrecedente != null && adressePrecedente.getDateFin().getOneDayAfter().equals(adresse.getDateDebut())) {
			adressePrecedente.setDateFin(null);
		}

		// On annule l'adresse spécifiée
		adresse.setAnnule(true);
	}

	private static boolean notEmpty(final String string) {
		return string != null && string.trim().length() > 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getNomCourrier(Tiers tiers, RegDate date, boolean strict) throws AdresseException {

		final AdresseEnvoiDetaillee adresse = getAdresseEnvoi(tiers, date, strict);

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
	 * @param numeroIndividu
	 * @return
	 * @throws AdresseException
	 */
	public String getNomCourrier(Long numeroIndividu) throws AdresseException {

		String prenomNom = null;
		Individu individu = getServiceCivilService().getIndividu(numeroIndividu.longValue(), DateHelper.getCurrentYear());
		if (individu == null) {
			throw new IndividuNotFoundException(numeroIndividu);
		}
		String prenom = individu.getDernierHistoriqueIndividu().getPrenom();
		if (prenom != null) {
			prenomNom = prenom;
		}
		String nom = individu.getDernierHistoriqueIndividu().getNom();
		if (prenom != null) {
			prenomNom = prenomNom + " "  + nom;
		}

		return prenomNom;
	}


	public AdresseGenerique getDerniereAdresseVaudoise(Tiers tiers, TypeAdresseTiers type) throws InfrastructureException, AdresseException {
		AdressesFiscalesHisto adressesHistoriques = getAdressesFiscalHisto(tiers,false);
		List<AdresseGenerique> listeAdresse = adressesHistoriques.ofType(type);
		for (AdresseGenerique adresseGenerique : listeAdresse) {
			Commune commune = serviceInfra.getCommuneByAdresse(adresseGenerique);
			if (commune.isVaudoise()) {
				return adresseGenerique;
			}
		}
		return null;
	}



}
