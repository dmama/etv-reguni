package ch.vd.uniregctb.evenement.retourdi.pm;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.uniregctb.adresse.AdresseCivileAdapter;
import ch.vd.uniregctb.adresse.AdresseDataException;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseFiscale;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataireAdapter;
import ch.vd.uniregctb.adresse.AdresseMandataireEtrangere;
import ch.vd.uniregctb.adresse.AdresseMandataireSuisse;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.LignesAdresse;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.BouclementHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.LocalisationFiscale;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Composant de traitement métier du retour (= contenu) des données d'une DI PM
 */
public class RetourDIPMServiceImpl implements RetourDIPMService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RetourDIPMServiceImpl.class);

	private static abstract class Motifs {
		static final String ADRESSE_NON_TRAITEE = "Retour DI - Adresse non-traitée";
		static final String DI_ANNULEE = "Retour DI - Déclaration annulée";
		static final String DI_NON_QUITTANCEE = "Retour DI - Déclaration non-quittancée";
		static final String DATE_EXERCICE_COMMERCIAL_IGNOREE = "Retour DI - Date de fin de l'exercice commercial";
		static final String CHGT_PERIODE_FISCALE_DI_ULTERIEURE = "Retour DI - Changement de période fiscale avec déclaration retournée ultérieure";
		static final String CHGT_PERIODE_FISCALE = "Retour DI - Changement de période fiscale";
		static final String NON_ASSUJETTI_APRES_CHGT_EX_COMMERCIAL = "Retour DI - Aucun assujettissement à la nouvelle date de fin d'exercice commercial";
		static final String CHGT_RAISON_SOCIALE = "Retour DI - Changement de raison sociale";
		static final String CHGT_SIEGE = "Retour DI - Changement de siège";
		static final String CHGT_ADMINISTRATION_EFFECTIVE = "Retour DI - Changement d'administration effective";
		static final String CHGT_COMPTE_BANCAIRE = "Retour DI - Compte bancaire";
		static final String MANDATAIRE = "Retour DI - Mandataire";
	}

	private TiersService tiersService;
	private TacheService tacheService;
	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService infraService;
	private AdresseService adresseService;
	private ExerciceCommercialHelper exerciceCommercialHelper;
	private BouclementService bouclementService;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private AssujettissementService assujettissementService;
	private ValidationService validationService;
	private IbanValidator ibanValidator;
	private GlobalTiersSearcher tiersSearcher;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setExerciceCommercialHelper(ExerciceCommercialHelper exerciceCommercialHelper) {
		this.exerciceCommercialHelper = exerciceCommercialHelper;
	}

	public void setBouclementService(BouclementService bouclementService) {
		this.bouclementService = bouclementService;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}

	/**
	 * Point d'entrée principal du service
	 * @param retour les informations en question
	 * @param incomingHeaders les méta-données autour du message entrant
	 * @throws EsbBusinessException en cas de problème
	 */
	@Override
	public void traiterRetour(RetourDI retour, Map<String, String> incomingHeaders) throws EsbBusinessException {

		// connait-on ce contribuable entreprise ?
		final long noCtb = retour.getNoCtb();
		final Tiers tiers = tiersService.getTiers(noCtb);
		if (tiers == null || !(tiers instanceof Entreprise)) {
			throw new EsbBusinessException(EsbBusinessCode.CTB_INEXISTANT, "Le contribuable " + FormatNumeroHelper.numeroCTBToDisplay(noCtb) + " n'existe pas ou n'est pas une entreprise.", null);
		}

		// le contribuable entreprise a-t-il une déclaration sur la bonne PF avec le bon numéro de séquence ?
		final Entreprise entreprise = (Entreprise) tiers;
		final List<DeclarationImpotOrdinairePM> declarationsDansPeriode = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, retour.getPf(), true);
		final DeclarationImpotOrdinairePM declarationIdentifiee = findDeclaration(declarationsDansPeriode, retour.getNoSequence());
		if (declarationIdentifiee == null) {
			throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "L'entreprise " + FormatNumeroHelper.numeroCTBToDisplay(noCtb) + " ne possède pas de déclaration d'impôt " + retour.getPf() + " avec le numéro de séquence " + retour.getNoSequence() + '.', null);
		}

		// on a maintenant bien une DI identifiée... est-elle dans un état cohérent par rapport à un retour de scan ?
		if (declarationIdentifiee.isAnnule()) {
			tacheService.genereTacheControleDossier(entreprise, Motifs.DI_ANNULEE);
			addRemarqueDonneesCompletes(entreprise, "Données de DI reçues sur la déclaration annulée " + retour.getPf() + "/" + retour.getNoSequence(), retour);
			return;
		}

		// la DI n'est donc pas annulée... est-elle seulement retournée ?
		final EtatDeclaration dernierEtat = declarationIdentifiee.getDernierEtat();
		if (dernierEtat == null || dernierEtat.getEtat() != TypeEtatDeclaration.RETOURNEE) {
			tacheService.genereTacheControleDossier(entreprise, Motifs.DI_NON_QUITTANCEE);
			addRemarqueDonneesCompletes(entreprise, "Données de DI reçues sur la déclaration non-quittancée " + retour.getPf() + "/" + retour.getNoSequence(), retour);
			return;
		}

		// Y a-t-il des choses à faire au niveau de l'entreprise elle-même ?
		final RegDate dateReference = extractDateReference(retour);
		final InformationsEntreprise infosEntreprise = retour.getEntreprise();
		if (infosEntreprise != null) {
			traiterFinExerciceCommercial(entreprise, declarationIdentifiee, infosEntreprise.getDateFinExerciceCommercial());
			traiterAdresseCourrierEtRaisonSociale(entreprise, retour.getPf(), retour.getNoSequence(), dateReference, infosEntreprise.getAdresseCourrier(), infosEntreprise.getNoTelContact());
			traiterSiege(entreprise, retour.getPf(), retour.getNoSequence(), dateReference, infosEntreprise.getSiege());
			traiterAdministrationEffective(entreprise, retour.getPf(), retour.getNoSequence(), dateReference, infosEntreprise.getAdministrationEffective());
			traiterInformationsBancaires(entreprise, retour.getPf(), retour.getNoSequence(), infosEntreprise.getIban(), infosEntreprise.getTitulaireCompteBancaire());
		}

		// Y a-t-il des choses à faire au niveau du mandataire général de l'entreprise ?
		final InformationsMandataire infosMandataire = retour.getMandataire();
		if (infosMandataire != null && infosMandataire.isNotEmpty()) {
			// ah ah... il y a quelque chose...
			traiterInformationsMandataire(entreprise, retour.getPf(), retour.getNoSequence(), infosMandataire);
		}
		else {
			// pas d'information fournie -> on stoppe l'éventuel mandat général en cours
			fermerMandatGeneralActif(entreprise, retour.getPf(), retour.getNoSequence());
		}
   	}

   	private static String[] extractLignes(@Nullable AdresseEnvoiDetaillee adresse) {
	    return Optional.ofNullable(adresse)
			    .map(AdresseEnvoiDetaillee::getLignes)
			    .map(LignesAdresse::asTexte)
			    .orElse(null);
    }

	/**
	 * Traitement des informations de mandataire général présentes dans la déclaration retournée
	 * @param entreprise entreprise ciblée
	 * @param pf période fiscale initiale (= à l'envoi) de la DI
	 * @param noSequence numéro de séquence initial (= à l'envoi) de la DI dans sa période fiscale (initiale)
	 * @param infosMandataire informations extraites de la DI
	 */
	private void traiterInformationsMandataire(Entreprise entreprise, int pf, int noSequence, @NotNull InformationsMandataire infosMandataire) {

		final RegDate dateReference = getDateQuittancementDerniereDeclarationRetournee(entreprise);
		final RegDate dateClotureMandatPrecedent = findDateClotureMandat();
		if (dateClotureMandatPrecedent == null) {
			// on ne sait pas vraiment à partir de quand ouvrir le nouveau mandat
			tacheService.genereTacheControleDossier(entreprise, Motifs.MANDATAIRE);
			addRemarque(entreprise, String.format("Le système n'a pas pu déterminer la date de début de validité des informations de mandataire présentes dans la DI %d/%d (%s).",
			                                      pf, noSequence,
			                                      infosMandataire.toDisplayString(infraService, adresseService, dateReference)));
			return;
		}
		final RegDate dateDebutNouveauMandat = dateClotureMandatPrecedent.getOneDayAfter();

		//
		// données fournies
		//

		final Contribuable mandataireFourni;                // le mandataire reconnu depuis les données de la DI (IDE), optionnel
		final Pair<String, Adresse> raisonSocialeEtAdresseFournies;                  // la raison sociale fournie (optionnelle)
		final String[] lignesAdresseFournie;                // les 6 lignes de l'adresse telle que fournie sur la DI (retranscrites, quand-même...), optionelles
		final String[] lignesAdresseMandataireFourni;       // les 6 lignes de l'adresse de représentation du mandataire reconnu depuis les données de la DI (IDE), optionnelles
		final boolean avecCopieMandataireFourni;
		final String noTelContactMandataireFourni;
		final String contactMandataireFourni;

		// si on a un numéro IDE dans les données, on essaie de voir si on connait déjà le gulu
		if (infosMandataire.isNumeroIdeMandataireUtilisable()) {
			Contribuable mandataireIdentifie = null;
			try {
				mandataireIdentifie = findMandataireParNumeroIDE(infosMandataire.getIdeMandataire());
			}
			catch (TooManyResultsException e) {
				// trop de résultats... on ajoute une tâche avec une remarque et on continue
				tacheService.genereTacheControleDossier(entreprise, Motifs.MANDATAIRE);
				addRemarque(entreprise, String.format("Identification du mandataire pointé par le numéro IDE %s dans la DI %d/%d imprécise (%s).",
				                                      FormatNumeroHelper.formatNumIDE(infosMandataire.getIdeMandataire()),
				                                      pf, noSequence,
				                                      e.getMessage()));
			}
			mandataireFourni = mandataireIdentifie;
		}
		else {
			mandataireFourni = null;
		}

		// flag "sans copie mandataire" dans la DI traduit ici par un "avec copie mandataire" (true par défaut)
		avecCopieMandataireFourni = infosMandataire.getSansCopieMandataire() == null || !infosMandataire.getSansCopieMandataire();

		// extraction des informations du contact (numéro de téléphone et nom/prénom), si fournies
		noTelContactMandataireFourni = StringUtils.trimToNull(infosMandataire.getNoTelContact());
		contactMandataireFourni = StringUtils.trimToNull(infosMandataire.getContact());

		// reconstitution des lignes d'adresses fournies dans la déclaration
		if (infosMandataire.getAdresse() != null) {
			final AdresseRaisonSociale adresseFournie = infosMandataire.getAdresse();
			raisonSocialeEtAdresseFournies = adresseFournie.split(infraService, tiersService, dateReference);
			if (raisonSocialeEtAdresseFournies == null) {
				tacheService.genereTacheControleDossier(entreprise, Motifs.MANDATAIRE);

				// si un mandataire a été reconnu par son numéro IDE, on peut quand-même générer le lien même si aucune adresse supplémentaire n'a été donnée
				if (mandataireFourni == null) {
					addRemarque(entreprise, String.format("Les données d'adresse/raison sociale trouvées pour le mandataire dans la DI %d/%d n'ont pas pu être interprétées de manière concluante :\n%s.",
					                                      pf, noSequence,
					                                      infosMandataire.toDisplayString(infraService, adresseService, dateReference)));
					return;
				}
				else {
					addRemarque(entreprise, String.format("Les données d'adresse/raison sociale trouvées pour le mandataire dans la DI %d/%d n'ont pas pu être interprétées de manière concluante (%s).",
					                                      pf, noSequence,
					                                      adresseFournie.toDisplayString(infraService, adresseService, dateReference)));
				}

				lignesAdresseFournie = null;
			}
			else {
				final Tiers mandatairePourAdresse;
				if (mandataireFourni != null) {
					mandatairePourAdresse = mandataireFourni;
				}
				else {
					final DestinataireAdresse destinataire = infosMandataire.getAdresse().getDestinataire();
					if (destinataire != null) {
						mandatairePourAdresse = destinataire.buildDummyTiers();
					}
					else {
						final Entreprise dummyEntrepriseMandataire = new Entreprise();
						if (raisonSocialeEtAdresseFournies.getLeft() != null) {
							dummyEntrepriseMandataire.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(null, null, raisonSocialeEtAdresseFournies.getLeft()));
						}
						mandatairePourAdresse = dummyEntrepriseMandataire;
					}
				}
				try {
					lignesAdresseFournie = toLines(mandatairePourAdresse, raisonSocialeEtAdresseFournies.getRight(), dateReference);
				}
				catch (AdresseException e) {
					final String msg = String.format("Erreur à la résolution de l'adresse fournie dans la DI %d/%d du contribuable %s (%s)",
					                                 pf, noSequence,
					                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
					                                 adresseFournie.toDisplayString(infraService, adresseService, dateReference));
					LOGGER.error(msg, e);
					tacheService.genereTacheControleDossier(entreprise, Motifs.MANDATAIRE);
					addRemarque(entreprise, StringUtils.isNotBlank(e.getMessage()) ? String.format("%s : %s", msg, e.getMessage()) : msg);
					return;
				}
			}
		}
		else {
			raisonSocialeEtAdresseFournies = null;
			lignesAdresseFournie = null;
		}

		// reconstitution des lignes de l'adresse de représentation du mandataire fourni
		if (mandataireFourni != null) {
			AdresseEnvoiDetaillee representation = null;
			try {
				representation = adresseService.getAdresseEnvoi(mandataireFourni, dateReference, TypeAdresseFiscale.REPRESENTATION, false);
			}
			catch (AdresseException e) {
				// ok, on a un problème (on peut le logger...) mais ce n'est pas si grave si une adresse a été fournie de toute façon
				LOGGER.error("Impossible de calculer l'adresse de représentation du tiers " + FormatNumeroHelper.numeroCTBToDisplay(mandataireFourni.getNumero()), e);
				if (raisonSocialeEtAdresseFournies == null) {
					// aucune autre adresse fournie -> baboom !
					tacheService.genereTacheControleDossier(entreprise, Motifs.MANDATAIRE);
					addRemarque(entreprise, String.format("Problème lors du calcul de l'adresse de représentation du mandataire identifié dans la DI %d/%d (%s, %s).",
					                                      pf, noSequence,
					                                      FormatNumeroHelper.numeroCTBToDisplay(mandataireFourni.getNumero()),
					                                      tiersService.getNomRaisonSociale(mandataireFourni)));
					return;
				}
			}
			lignesAdresseMandataireFourni = extractLignes(representation);
		}
		else {
			lignesAdresseMandataireFourni = null;
		}
		final boolean hasDonneesFournies = mandataireFourni != null || lignesAdresseFournie != null;


		//
		// données connues
		//

		final Mandat mandatConnu;                           // le mandat connu (optionnel)
		final AdresseMandataire adresseMandataireConnue;    // l'adresse mandataire connue (optionnelle)
		final String[] lignesAdresseConnue;                 // les 6 lignes de l'adresse issue du mandat ou de l'adresse mandataire connue (optionnelles)
		final Boolean avecCopieMandataireConnu;
		final String noTelContactMandataireConnu;
		final String contactMandataireConnu;

		// y a-t-il déjà une information de mandat général présente ?
		mandatConnu = findMandatGeneralActif(entreprise, dateDebutNouveauMandat);
		adresseMandataireConnue = findAdresseMandataireGeneraleActive(entreprise, dateDebutNouveauMandat);
		try {
			if (mandatConnu != null) {
				final Tiers mandataireConnu = tiersService.getTiers(mandatConnu.getObjetId());
				avecCopieMandataireConnu = mandatConnu.getWithCopy() != null && mandatConnu.getWithCopy();
				final AdresseEnvoiDetaillee adresseMandataireActif = adresseService.getAdresseEnvoi(mandataireConnu, null, TypeAdresseFiscale.REPRESENTATION, false);
				lignesAdresseConnue = extractLignes(adresseMandataireActif);
				noTelContactMandataireConnu = mandatConnu.getNoTelephoneContact();
				contactMandataireConnu = mandatConnu.getNomPersonneContact();
			}
			else if (adresseMandataireConnue != null) {
				avecCopieMandataireConnu = adresseMandataireConnue.isWithCopy();
				final AdresseGenerique adresseGenerique = new AdresseMandataireAdapter(adresseMandataireConnue, infraService);
				final AdresseEnvoiDetaillee adresseDetaillee = adresseService.buildAdresseEnvoi(adresseGenerique.getSource().getTiers(), adresseGenerique, dateReference);
				lignesAdresseConnue = extractLignes(adresseDetaillee);
				noTelContactMandataireConnu = adresseMandataireConnue.getNoTelephoneContact();
				contactMandataireConnu = adresseMandataireConnue.getNomPersonneContact();
			}
			else {
				avecCopieMandataireConnu = null;
				lignesAdresseConnue = null;
				noTelContactMandataireConnu = null;
				contactMandataireConnu = null;
			}
		}
		catch (AdresseException e) {
			LOGGER.error(String.format("Erreur de résolution de l'adresse du mandat / de l'adresse mandataire de type 'général' du contribuable %s au %s",
			                           FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                           RegDateHelper.dateToDisplayString(dateDebutNouveauMandat)),
			             e);

			tacheService.genereTacheControleDossier(entreprise, Motifs.MANDATAIRE);
			addRemarque(entreprise, String.format("Informations de mandataire de la DI %d/%d non-traitée en raison d'une erreur à la résolution de l'adresse mandataire courante (%s) :\n%s.",
			                                      pf, noSequence,
			                                      e.getMessage(),
			                                      infosMandataire.toDisplayString(infraService, adresseService, dateReference)));
			return;
		}


		//
		// comparaison entre la consigne (= ce qu'il y a dans la déclaration) et l'état actuel (= ce que nous connaissons en base)
		//

		// la nouvelle adresse est l'adresse explicitement fournie ou, à défaut, l'adresse de représentation du mandataire fourni (pour peu qu'il soit effectivement fourni)
		final String[] lignesNouvelleAdresse = lignesAdresseFournie != null ? lignesAdresseFournie : lignesAdresseMandataireFourni;
		if (!areEquals(lignesAdresseConnue, lignesNouvelleAdresse)
				|| (hasDonneesFournies && (avecCopieMandataireConnu == null || avecCopieMandataireConnu != avecCopieMandataireFourni))
				|| (noTelContactMandataireFourni != null && !Objects.equals(noTelContactMandataireFourni, noTelContactMandataireConnu))
				|| (contactMandataireFourni != null && !Objects.equals(contactMandataireFourni, contactMandataireConnu))) {

			// il y a quelque chose à faire...

			// maintenant, on doit ouvrir la nouvelle adresse / le nouveau mandat général
			if (mandataireFourni != null && (lignesAdresseFournie == null || areEquals(lignesAdresseFournie, lignesAdresseMandataireFourni))) {
				// c'est bien un lien qu'il faut faire...
				final Mandat nouveauMandat = Mandat.general(dateDebutNouveauMandat, null, entreprise, mandataireFourni, avecCopieMandataireFourni);
				nouveauMandat.setNoTelephoneContact(noTelContactMandataireFourni);
				nouveauMandat.setNomPersonneContact(contactMandataireFourni);

				// s'il y avait une adresse/un mandat connu, il faut le fermer à la date de fin de l'exercice commercial qui précède celui de la DI retournée
				fermerLienAdresseMandataire(entreprise, mandatConnu, adresseMandataireConnue, dateClotureMandatPrecedent);

				// ajout du lien
				entreprise.addRapportSujet(hibernateTemplate.merge(nouveauMandat));
			}
			else if (raisonSocialeEtAdresseFournies != null) {
				final Adresse nouvelleAdresseFournie = raisonSocialeEtAdresseFournies.getRight();
				final String raisonSociale = mandataireFourni != null ? tiersService.getNomRaisonSociale(mandataireFourni) : raisonSocialeEtAdresseFournies.getLeft();
				final String complement = mandataireFourni != null ? raisonSocialeEtAdresseFournies.getLeft() : null;

				// c'est une "simple" adresse mandataire qu'il faut faire...
				final AdresseMandataire nouvelleAdresseMandataire;
				try {
					nouvelleAdresseMandataire = buildAdresseMandataire(dateDebutNouveauMandat, nouvelleAdresseFournie, raisonSociale, avecCopieMandataireFourni);
					if (complement != null && nouvelleAdresseMandataire.getComplement() == null) {
						nouvelleAdresseMandataire.setComplement(complement);
					}
					nouvelleAdresseMandataire.setNoTelephoneContact(noTelContactMandataireFourni);
					nouvelleAdresseMandataire.setNomPersonneContact(contactMandataireFourni);
				}
				catch (AdresseException e) {
					LOGGER.error(String.format("Erreur à la constitution de l'adresse mandataire de type 'général' du contribuable %s au %s",
					                           FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
					                           RegDateHelper.dateToDisplayString(dateDebutNouveauMandat)),
					             e);
					tacheService.genereTacheControleDossier(entreprise, Motifs.MANDATAIRE);
					addRemarque(entreprise, String.format("Impossible de créer une adresse mandataire à partir des données fournies dans la DI %d/%d : %s.",
					                                      pf, noSequence,
					                                      infosMandataire.toDisplayString(infraService, adresseService, dateReference)));
					return;
				}

				// s'il y avait une adresse/un mandat connu, il faut le fermer à la date de fin de l'exercice commercial qui précède celui de la DI retournée
				// (cette opération n'est faite qu'après qu'on se soit assuré que la nouvelle adresse mandataire pouvait être construite)
				fermerLienAdresseMandataire(entreprise, mandatConnu, adresseMandataireConnue, dateClotureMandatPrecedent);

				// ajout de l'adresse
				entreprise.addAdresseMandataire(nouvelleAdresseMandataire);
			}
			else {
				// Ce cas ici, qui correspond à
				// - un mandat / une adresse mandataire existante
				// - une donnée dans la DI sans identification de tiers (mandataireFourni == null)
				// - une adresse de mandataire non-reconnue
				// ce cas, donc, a normalement déjà été exclu plus haut (adresse fournie mais non-reconnue sans mandataire explicitement pointé par un numéro IDE)
				throw new RuntimeException("Cas non prévu...");
			}
		}
	}

	/**
	 * Exception lancée par la méthode {@link #findMandataireParNumeroIDE(String)} quand plusieurs candidats sont identifiés
	 */
	private static class TooManyResultsException extends Exception {

		private final Set<Long> foundIds;

		public TooManyResultsException(Throwable cause) {
			super(cause);
			this.foundIds = Collections.emptySet();
		}

		public TooManyResultsException(@NotNull Set<Long> foundIds) {
			this.foundIds = new TreeSet<>(foundIds);
		}

		@Override
		public String getMessage() {
			if (foundIds.isEmpty()) {
				return super.getMessage();
			}
			final StringRenderer<Long> renderer = FormatNumeroHelper::numeroCTBToDisplay;
			return String.format("%d tiers trouvés : %s",
			                     foundIds.size(),
			                     CollectionsUtils.toString(foundIds, renderer, ", "));
		}
	}

	/**
	 * @param ide numéro IDE (forme normalisée sans points ni tiret)
	 * @return un contribuable correspondant à ce numéro, s'il existe et est unique
	 * @throws TooManyResultsException en cas de résultats multiples
	 */
	@Nullable
	private Contribuable findMandataireParNumeroIDE(@NotNull String ide) throws TooManyResultsException {
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumeroIDE(ide);
		criteria.setTypesTiersImperatifs(EnumSet.of(TiersCriteria.TypeTiers.CONTRIBUABLE_PP,
		                                            TiersCriteria.TypeTiers.ENTREPRISE,
		                                            TiersCriteria.TypeTiers.ETABLISSEMENT_SECONDAIRE));
		try {
			final List<TiersIndexedData> searchResult = tiersSearcher.search(criteria);
			if (searchResult.isEmpty() || searchResult.size() > 1) {
				// aucun ou plusieurs -> personne
				if (searchResult.isEmpty()) {
					LOGGER.info("Aucun tiers trouvé avec le numéro IDE " + FormatNumeroHelper.formatNumIDE(ide));
					return null;
				}
				else {
					final Set<Long> foundIds = new HashSet<>(searchResult.size());
					for (TiersIndexedData found : searchResult) {
						foundIds.add(found.getNumero());
					}

					// petit blindage du cas où l'indexeur contiendrait des doublons du même tiers...
					if (foundIds.size() > 1) {
						throw new TooManyResultsException(foundIds);
					}
				}
			}

			// on en a trouvé un... on va dire que c'est lui
			return (Contribuable) tiersService.getTiers(searchResult.get(0).getNumero());
		}
		catch (TooManyResultsIndexerException e) {
			throw new TooManyResultsException(e);
		}
	}

	/**
	 * Ferme le mandat général (quelle que soit sa forme - lien ou simple adresse) actif à la veille
	 * de la date de début de la période d'imposition de la DI retournée
	 * @param entreprise entreprise concernée
	 * @param pf période fiscale de la déclaration
	 * @param noSequence numéro de séquence de la déclaration dans sa période fiscale
	 */
	private void fermerMandatGeneralActif(Entreprise entreprise, int pf, int noSequence) {
		final RegDate dateClotureSouhaitee = findDateClotureMandat();
		if (dateClotureSouhaitee == null) {
			// s'il y a un mandat actif maintenant (= un mandat dont on est certain qu'il devrait être fermé), on laisse un message
			if (findMandatGeneralActif(entreprise, null) != null || findAdresseMandataireGeneraleActive(entreprise, null) != null) {
				tacheService.genereTacheControleDossier(entreprise, Motifs.MANDATAIRE);
				addRemarque(entreprise, String.format("La déclaration %d/%d n'indique pas de mandataire, mais le système n'a pas pu déterminer quand clôturer le mandat actif.",
				                                      pf, noSequence));
			}
			return;
		}

		final Mandat mandatActif = findMandatGeneralActif(entreprise, dateClotureSouhaitee.getOneDayAfter());
		final AdresseMandataire adresseMandataireActive = findAdresseMandataireGeneraleActive(entreprise, dateClotureSouhaitee.getOneDayAfter());
		fermerLienAdresseMandataire(entreprise, mandatActif, adresseMandataireActive, dateClotureSouhaitee);
	}

	/**
	 * Fermeture d'un lien de mandat (ou, si absent, de l'adresse mandataire) - évidemment, rien n'est fait si les deux sont nuls
	 * @param entreprise entreprise ciblée
	 * @param mandat lien de mandat à fermer
	 * @param adresseMandataire en absence de mandat à fermer, adresse mandataire à fermer
	 * @param dateCloture date à utiliser pour clôturer les entités
	 */
	private void fermerLienAdresseMandataire(Entreprise entreprise, @Nullable Mandat mandat, @Nullable AdresseMandataire adresseMandataire, RegDate dateCloture) {
		if (mandat != null) {
			final RapportEntreTiers nouvelleInstance = this.fermerAt(mandat, dateCloture);
			if (nouvelleInstance != null) {
				entreprise.addRapportSujet(hibernateTemplate.merge(nouvelleInstance));
			}
		}
		else if (adresseMandataire != null) {
			final AdresseMandataire nouvelleInstance = fermerAt(adresseMandataire, dateCloture);
			if (nouvelleInstance != null) {
				entreprise.addAdresseMandataire(nouvelleInstance);
			}
		}
	}

	/**
	 * Ferme l'entité à la date donnée
	 * @param rangeEntity entité à fermer
	 * @param dateCloture date de clôture souhaitée
	 * @param <T> type de l'entité
	 * @return si la fermeture de l'entité aboutit à la création d'une nouvelle entité (= annulation de la précédente + création), la nouvelle entité créée (non-encore persistée...)
	 */
	@Nullable
	private <T extends HibernateDateRangeEntity & Duplicable<T>> T fermerAt(T rangeEntity, RegDate dateCloture) {
		// Plusieurs cas sont possibles (l'entité est connue pour être valide au lendemain de la date de clôture souhaitée)
		// - l'entité n'est pas valable à la date de clôture (= elle ne commence que le lendemain) -> on l'annule
		// - l'entité est déjà clôturée (à une date ultérieure, donc...) -> on annule et on la remplace par une copie fermée à la bonne date
		// - l'entité est valable et encore ouverte -> on la ferme
		if (!rangeEntity.isValidAt(dateCloture)) {
			rangeEntity.setAnnule(true);
		}
		else if (rangeEntity.getDateFin() != null) {
			final T copy = rangeEntity.duplicate();
			rangeEntity.setAnnule(true);
			copy.setDateFin(dateCloture);
			return copy;
		}
		else {
			rangeEntity.setDateFin(dateCloture);
		}
		return null;
	}

	/**
	 * @return la veille de la date du jour
	 */
	@Nullable
	private RegDate findDateClotureMandat() {
		// [SIFISC-21206] la date en question est _toujours_ la veille de la date de réception du message de retour de la DI
		return RegDate.get().getOneDayBefore();
	}

	/**
	 * @param entreprise l'entreprise cible
	 * @return l'éventuel mandat général actif à la date de référence
	 */
	@Nullable
	private static Mandat findMandatGeneralActif(Entreprise entreprise, RegDate dateReference) {
		// mandat général ?
		for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
			if (!ret.isAnnule() && ret.isValidAt(dateReference) && ret.getType() == TypeRapportEntreTiers.MANDAT) {
				final Mandat candidat = (Mandat) ret;
				if (candidat.getTypeMandat() == TypeMandat.GENERAL) {
					return candidat;
				}
			}
		}
		// rien trouvé...
		return null;
	}

	/**
	 * @param entreprise l'entreprise cible
	 * @return l'éventuelle adresse mandataire de type général active à la date de référence
	 */
	@Nullable
	private static AdresseMandataire findAdresseMandataireGeneraleActive(Entreprise entreprise, RegDate dateReference) {
		// adresse mandataire ?
		for (AdresseMandataire am : entreprise.getAdressesMandataires()) {
			if (!am.isAnnule() && am.isValidAt(dateReference) && am.getTypeMandat() == TypeMandat.GENERAL) {
				return am;
			}
		}
		// rien trouvé...
		return null;
	}

	/**
	 * Traitement des changements dans les coordonnées financières déclarées dans une DI
	 * @param entreprise entreprise concernée
	 * @param pf période fiscale de la déclaration
	 * @param noSequence numéro de séquence de la déclaration dans sa période fiscale
	 * @param iban éventuel IBAN fourni
	 * @param titulaireCompte éventuel titulaire du compte fourni
	 */
	private void traiterInformationsBancaires(Entreprise entreprise, int pf, int noSequence, @Nullable String iban, @Nullable String titulaireCompte) {

		// d'abord, on traite l'IBAN
		final String ibanNormalise = IbanHelper.normalize(iban);
		final boolean ibanModifie = ibanNormalise != null
				&& StringUtils.isNotBlank(ibanNormalise)
				&& !"CH".equalsIgnoreCase(ibanNormalise)
				&& traiterCompteBancaire(entreprise, pf, noSequence, ibanNormalise);

		// puis on traite le titulaire du compte
		if (ibanModifie) {
			if (titulaireCompte != null && StringUtils.isNotBlank(titulaireCompte)) {
				entreprise.setTitulaireCompteBancaire(titulaireCompte.trim());
			}
			else {
				final String raisonSociale = tiersService.getDerniereRaisonSociale(entreprise);
				entreprise.setTitulaireCompteBancaire(raisonSociale);
			}
		}
	}

	/**
	 * Traitement des changements dans le numéro de compte bancaire (IBAN)
	 * @param entreprise entreprise concernée
	 * @param pf période fiscale de la déclaration
	 * @param noSequence numéro de séquence de la déclaration dans sa période fiscale
	 * @param iban IBAN fourni (sous sa forme normalisée)
	 * @return <code>true</code> si l'IBAN a été mis à jour, <code>false</code> dans le cas contraire
	 */
	private boolean traiterCompteBancaire(Entreprise entreprise, int pf, int noSequence, @NotNull String iban) {
		final CoordonneesFinancieres coordonneesFinancieres = entreprise.getCoordonneesFinancieres();
		final String ibanConnu = coordonneesFinancieres != null ? IbanHelper.normalize(coordonneesFinancieres.getIban()) : null;
		if (ibanConnu == null || !ibanConnu.equalsIgnoreCase(iban)) {
			final String newIbanValidationError = ibanValidator.getIbanValidationError(iban);
			boolean accepterNouvelIban = false;
			if (newIbanValidationError == null) {
				accepterNouvelIban = true;
			}
			else {
				// ok, le nouveau numéro n'est pas valide...

				// si on avait un ancien IBAN valide, on laissera une trace, sinon on prend le nouveau quand-même
				final String oldIbanValidationError = ibanValidator.getIbanValidationError(ibanConnu);      // non-vide si iban vide
				if (oldIbanValidationError == null) {
					// on laisse un message
					tacheService.genereTacheControleDossier(entreprise, Motifs.CHGT_COMPTE_BANCAIRE);
					addRemarque(entreprise, String.format("Le numéro de compte bancaire (%s) déclaré dans la DI %d/%d est invalide, et n'a donc pas écrasé le numéro valide connu.",
					                                      iban, pf, noSequence));
				}
				else {
					accepterNouvelIban = true;
				}
			}

			if (accepterNouvelIban) {
				// pour rester cohérent avec ce qui est fait pour les PP, on ne touche pas à l'éventuel BIC/SWIFT présent
				if (coordonneesFinancieres != null) {
					coordonneesFinancieres.setIban(iban);
				}
				else {
					entreprise.setCoordonneesFinancieres(new CoordonneesFinancieres(iban, null));
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Traitement d'un éventuel nouveau siège déclaré dans une DI
	 * @param entreprise entreprise concernée
	 * @param pf période fiscale de la déclaration
	 * @param noSequence numéro de séquence de la déclaration dans sa période fiscale
	 * @param dateReference date de référence pour les éventuelle résolution de noms...
	 * @param siege éventuel siège fourni dans la déclaration
	 */
	private void traiterSiege(Entreprise entreprise, int pf, int noSequence, RegDate dateReference, @Nullable Localisation siege) {
		// aucune information...
		if (siege == null) {
			return;
		}

		final LocalisationFiscale lf = siege.transcriptionFiscale(infraService, dateReference);
		if (lf == null) {
			// pas trouvé...
			tacheService.genereTacheControleDossier(entreprise, Motifs.CHGT_SIEGE);
			addRemarque(entreprise, String.format("L'information de siège présente dans la DI %d/%d (%s) n'a pas pu être interprétée automatiquement.",
			                                      pf, noSequence,
			                                      siege.toDisplayString(infraService, dateReference)));
			return;
		}

		// comparaison avec le siège à la date de référence de l'existant
		final RegDate dateReferenceExistant = getDateQuittancementDerniereDeclarationRetournee(entreprise);
		final List<DomicileHisto> siegesConnus = tiersService.getSieges(entreprise, false);
		final DomicileHisto siegeCourant = DateRangeHelper.rangeAt(siegesConnus, dateReferenceExistant);
		if (siegeCourant != null && sameLocations(lf, siegeCourant)) {
			// rien ne change...
			return;
		}

		// tâche de contrôle de dossier et remarque explicative
		tacheService.genereTacheControleDossier(entreprise, Motifs.CHGT_SIEGE);
		addRemarque(entreprise, String.format("Nouveau siège déclaré dans la DI %d/%d : %s.",
		                                      pf, noSequence,
		                                      siege.toDisplayString(infraService, dateReference)));
	}

	/**
	 * Traitement d'une éventuelle nouvelle administation effective déclarée dans une DI
	 * @param entreprise entreprise concernée
	 * @param pf période fiscale de la déclaration
	 * @param noSequence numéro de séquence de la déclaration dans sa période fiscale
	 * @param dateReference date de référence pour les éventuelle résolution de noms...
	 * @param administrationEffective éventuelle administration effective fournie dans la déclaration
	 */
	private void traiterAdministrationEffective(Entreprise entreprise, int pf, int noSequence, RegDate dateReference, @Nullable Localisation administrationEffective) {
		// aucune information...
		if (administrationEffective == null) {
			return;
		}

		final LocalisationFiscale lf = administrationEffective.transcriptionFiscale(infraService, dateReference);
		if (lf == null) {
			// pas trouvé...
			tacheService.genereTacheControleDossier(entreprise, Motifs.CHGT_ADMINISTRATION_EFFECTIVE);
			addRemarque(entreprise, String.format("L'information d'administration effective présente dans la DI %d/%d (%s) n'a pas pu être interprétée automatiquement.",
			                                      pf, noSequence,
			                                      administrationEffective.toDisplayString(infraService, dateReference)));
			return;
		}

		// comparaison avec le for principal à la date de référence de l'existant
		final RegDate dateReferenceExistant = getDateQuittancementDerniereDeclarationRetournee(entreprise);
		final ForFiscalPrincipalPM ffp = entreprise.getForFiscalPrincipalAt(dateReferenceExistant);
		if (ffp != null && sameLocations(lf, ffp)) {
			// rien ne change...
			return;
		}

		// tâche de contrôle de dossier et remarque explicative
		tacheService.genereTacheControleDossier(entreprise, Motifs.CHGT_ADMINISTRATION_EFFECTIVE);
		addRemarque(entreprise, String.format("Nouvelle administration effective déclarée dans la DI %d/%d : %s.",
		                                      pf, noSequence,
		                                      administrationEffective.toDisplayString(infraService, dateReference)));
	}

	/**
	 * @param lf1 une localisation fiscale
	 * @param lf2 une autre localisation fiscale
	 * @return <code>true</code> si les deux localisation sont identiques
	 */
	private static boolean sameLocations(@NotNull LocalisationFiscale lf1, @NotNull LocalisationFiscale lf2) {
		return lf1.getTypeAutoriteFiscale() == lf2.getTypeAutoriteFiscale()
				&& lf1.getNumeroOfsAutoriteFiscale() != null
				&& lf2.getNumeroOfsAutoriteFiscale() != null
				&& lf1.getNumeroOfsAutoriteFiscale().equals(lf2.getNumeroOfsAutoriteFiscale());
	}

	/**
	 * Traitement d'une éventuelle nouvelle adresse courrier / raison sociale reçue dans une déclaration d'impôt
	 * @param entreprise entreprise concernée
	 * @param pf période fiscale de la DI initiale
	 * @param noSequence numéro de séquence de la DI initiale dans sa période fiscale
	 * @param dateReference date de référence pour les éventuelle résolution de noms...
	 * @param adresseCourrier l'éventuelle nouvelle adresse courrier fournie
	 * @param telProfessionnel le numéro de téléphone professionnel
	 */
	private void traiterAdresseCourrierEtRaisonSociale(Entreprise entreprise, int pf, int noSequence, RegDate dateReference, @Nullable AdresseRaisonSociale adresseCourrier, @Nullable String telProfessionnel) {

		// cas super simple : aucune donnée fournie
		if (adresseCourrier == null) {
			return;
		}

		// dans tous les cas le contact
		traiterContact(entreprise, adresseCourrier.getContact(), telProfessionnel);

		// [SIFISC-22080] si on n'a que le destinataire, c'est là que l'on s'arrête...
		if (!adresseCourrier.isDestinataireSeul()) {
			final Pair<String, Adresse> raisonSocialeEtAdresse = adresseCourrier.split(infraService, tiersService, dateReference);
			if (raisonSocialeEtAdresse == null) {
				tacheService.genereTacheControleDossier(entreprise, Motifs.ADRESSE_NON_TRAITEE);
				addRemarque(entreprise, String.format("Les données d'adresse/raison sociale trouvées dans la DI %d/%d n'ont pas pu être interprétées de manière concluante (%s).",
				                                      pf, noSequence,
				                                      adresseCourrier.toDisplayString(infraService, adresseService, dateReference)));
				return;
			}

			// il faut comparer avec l'adresse courrier "à la date de quittancement de la dernière DI retournée"... quelle est cette date ?
			final RegDate dateReferenceExistant = getDateQuittancementDerniereDeclarationRetournee(entreprise);

			// d'abord l'adresse courrier
			traiteAdresseCourrier(entreprise, pf, noSequence, dateReference, dateReferenceExistant, adresseCourrier, raisonSocialeEtAdresse.getRight());

			// puis la raison sociale
			traiteRaisonSociale(entreprise, pf, noSequence, dateReferenceExistant, raisonSocialeEtAdresse.getLeft());
		}
	}

	/**
	 * Prise en compte d'une éventuelle donnée de contact dans la DI
	 * @param entreprise entreprise concernée
	 * @param contact eventuel nom de la personne de contact
	 * @param telProfessionnel le numéro de téléphone professionnel
	 */
	private void traiterContact(Entreprise entreprise, @Nullable String contact, @Nullable String telProfessionnel) {
		// [SIFISC-21738] Dans le cas où une donnée de contact est fournie, et qu'elle est différente de la donnée
		// préalablement connue, il faut effacer les numéros de téléphone...
		if (StringUtils.isNotBlank(contact) && !Objects.equals(contact, entreprise.getPersonneContact())) {
			entreprise.setPersonneContact(contact);
			entreprise.setNumeroTelephonePortable(null);
			entreprise.setNumeroTelephonePrive(null);
			entreprise.setNumeroTelecopie(null);
		}

		// [SIFISC-21693] On met-à-jour le numéro de téléphone professionnel dans tous les cas
		entreprise.setNumeroTelephoneProfessionnel(telProfessionnel);
	}

	/**
	 * Traitement spécifique de la raison sociale reçue dans une déclaration d'impôt
	 * @param entreprise entreprise concernée
	 * @param pf période fiscale de la DI initiale
	 * @param noSequence numéro de séquence de la DI initiale dans sa période fiscale
	 * @param dateReferenceExistant date de référence pour les données déjà actuelles (= date de début d'une éventuelle nouvelle surcharge d'adresse)
	 * @param raisonSocialeTranscrite la nouvelle raison sociale extraite des données fournies
	 */
	private void traiteRaisonSociale(Entreprise entreprise, int pf, int noSequence, RegDate dateReferenceExistant, @Nullable String raisonSocialeTranscrite) {

		// rien de communiqué = rien de nouveau
		if (StringUtils.isBlank(raisonSocialeTranscrite)) {
			return;
		}

		// on ne fera quelque chose que si l'entreprise n'est pas sous le joug de RCEnt... autant vérifier ça tout de suite
		if (entreprise.isConnueAuCivil()) {
			return;
		}

		// quelle est la raison sociale connue de l'entreprise à la date de référence de l'existant ?
		final List<RaisonSocialeFiscaleEntreprise> raisonsSocialesConnues = entreprise.getRaisonsSocialesNonAnnuleesTriees();
		final RaisonSocialeFiscaleEntreprise raisonSocialeConnue = DateRangeHelper.rangeAt(raisonsSocialesConnues, dateReferenceExistant);
		if (raisonSocialeConnue != null && StringEqualityHelper.equals(raisonSocialeTranscrite, raisonSocialeConnue.getRaisonSociale())) {
			// c'est toujours la même
			return;
		}

		// on nous annonce donc une nouvelle raison sociale...
		// -> tâche de contrôle de dossier
		tacheService.genereTacheControleDossier(entreprise, Motifs.CHGT_RAISON_SOCIALE);
		addRemarque(entreprise, String.format("Nouvelle raison sociale annoncée (%s) dans la DI %d/%d.", raisonSocialeTranscrite, pf, noSequence));
	}

	@NotNull
	private String[] toLines(Tiers destinataire, @NotNull Adresse adresse, RegDate dateReference) throws AdresseException {
		try {
			final AdresseGenerique generique = new AdresseCivileAdapter(adresse, destinataire, false, infraService);
			final AdresseEnvoiDetaillee detaillee = adresseService.buildAdresseEnvoi(destinataire, generique, dateReference);
			return extractLignes(detaillee);
		}
		catch (DonneesCivilesException e) {
			// pas vraiment possible ici car les dates de début et de fin sont toujours nulles....
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Traitement spécifique de l'adresse courrier reçue dans une déclaration d'impôt
	 * @param entreprise entreprise concernée
	 * @param pf période fiscale de la DI initiale
	 * @param noSequence numéro de séquence de la DI initiale dans sa période fiscale
	 * @param dateReference date de référence pour les éventuelle résolution de noms...
	 * @param dateReferenceExistant date de référence pour les données déjà actuelles (= date de début d'une éventuelle nouvelle surcharge d'adresse)
	 * @param adresseFournie la nouvelle adresse courrier fournie
	 * @param adresseTranscrite la nouvelle adresse courrier fournie sous une forme transcrite
	 */
	private void traiteAdresseCourrier(Entreprise entreprise, int pf, int noSequence, RegDate dateReference, RegDate dateReferenceExistant, @NotNull AdresseRaisonSociale adresseFournie, @NotNull Adresse adresseTranscrite) {

		// le contribuable a déclaré une adresse... est-ce la même que celle que l'on connait déjà ?
		try {
			final String[] nouvelleAdresse = toLines(entreprise, adresseTranscrite, dateReference);
			final AdresseEnvoiDetaillee ancienneAdresse = adresseService.getAdresseEnvoi(entreprise, dateReferenceExistant, TypeAdresseFiscale.COURRIER, false);

			// on compare juste la représentation visuelle des adresses (= les 6 lignes)
			final boolean sameAddresses = areEquals(nouvelleAdresse, extractLignes(ancienneAdresse));
			if (sameAddresses) {
				// rien à faire, les adresses sont les mêmes
				return;
			}
		}
		catch (AdresseException e) {
			tacheService.genereTacheControleDossier(entreprise, Motifs.ADRESSE_NON_TRAITEE);
			addRemarque(entreprise, String.format("L'adresse récupérée dans la DI %d/%d (%s) n'a pas pu être comparée à l'adresse courrier connue au %s.",
			                                      pf, noSequence,
			                                      adresseFournie.toDisplayString(infraService, adresseService, dateReference),
			                                      RegDateHelper.dateToDisplayString(dateReferenceExistant)));
			return;
		}

		// adresses différentes... voyons voyons...

		// y a-t-il une surcharge d'adresse courrier à la date de référence ?
		final AdresseTiers surchargeExistante = entreprise.getAdresseActive(TypeAdresseTiers.COURRIER, dateReferenceExistant);
		if (surchargeExistante != null && surchargeExistante instanceof AdresseFiscale && ((AdresseFiscale) surchargeExistante).isPermanente()) {
			// non seulement il y a une surcharge, mais celle-ci est permanente... on ne touche à rien !
			tacheService.genereTacheControleDossier(entreprise, Motifs.ADRESSE_NON_TRAITEE);
			addRemarque(entreprise, String.format("L'adresse récupérée dans la DI %d/%d (%s) n'a pas été prise en compte automatiquement en raison de la présence au %s d'une surcharge permanente d'adresse courrier.",
			                                      pf, noSequence,
			                                      adresseFournie.toDisplayString(infraService, adresseService, dateReference),
			                                      RegDateHelper.dateToDisplayString(dateReferenceExistant)));
			return;
		}

		// on calcule la nouvelle adresse non-permanente avant de fermer l'ancienne des fois qu'il y aurait un souci, pour
		// ne rien toucher avant d'être sûr...
		final AdresseSupplementaire nouvelleAdresse;
		try {
			nouvelleAdresse = buildSurchargeCourrier(dateReferenceExistant, entreprise, adresseTranscrite);
		}
		catch (AdresseException e) {
			// on n'a pas réussi à générer une adresse utilisable...
			tacheService.genereTacheControleDossier(entreprise, Motifs.ADRESSE_NON_TRAITEE);
			addRemarque(entreprise, String.format("L'adresse récupérée dans la DI %d/%d (%s) n'a pas pu être transcrite en surcharge d'adresse courrier (%s).",
			                                      pf, noSequence,
			                                      adresseFournie.toDisplayString(infraService, adresseService, dateReference),
			                                      e.getMessage()));
			return;
		}

		// pas d'adresse surchargée, ou pas permanente...
		if (surchargeExistante != null) {
			// il faut neutraliser cette adresse
			if (surchargeExistante.getDateDebut() == dateReferenceExistant) {
				// la surcharge commençait justement à la date de référence, on ne peut pas la fermer à la veille, il faut donc l'annuler
				surchargeExistante.setAnnule(true);
			}
			else if (surchargeExistante.getDateFin() != null) {
				// la surcharge, bien que commençant avant la date de référence, est déjà fermée (peu importe quand... mais c'est à une
				// date postérieure ou égale à la date de référence, puisque l'adresse est valide à la date de référence)
				// --> on ne traite pas le changement d'adresse, charge à un opérateur de le faire à la main
				tacheService.genereTacheControleDossier(entreprise, Motifs.ADRESSE_NON_TRAITEE);
				addRemarque(entreprise, String.format("L'adresse récupérée dans la DI %d/%d (%s) n'a pas été prise en compte automatiquement en raison de la présence au %s d'une surcharge fermée d'adresse courrier.",
				                                      pf, noSequence,
				                                      adresseFournie.toDisplayString(infraService, adresseService, dateReference),
				                                      RegDateHelper.dateToDisplayString(dateReferenceExistant)));
				return;
			}
			else {
				// ok, on ferme la surcharge existante à la veille de la date de référence
				surchargeExistante.setDateFin(dateReferenceExistant.getOneDayBefore());
			}
		}

		// activer la surcharge non-permanente d'adresse courrier
		entreprise.addAdresseTiers(nouvelleAdresse);
	}

	/**
	 * @param lignes1 tableau de lignes
	 * @param lignes2 tableau de lignes
	 * @return <code>true</code> si les tableaux sont égaux (les lignes sont comparées de manière non-sensible à la casse)
	 */
	private static boolean areEquals(@Nullable String[] lignes1, @Nullable String[] lignes2) {
		final int length1 = lignes1 != null ? lignes1.length : 0;
		final int length2 = lignes2 != null ? lignes2.length : 0;
		if (length1 != length2) {
			return false;
		}
		for (int i = 0 ; i < length1 ; ++ i) {
			final String ligne1 = lignes1[i];
			final String ligne2 = lignes2[i];
			if (!StringEqualityHelper.equals(ligne1, ligne2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Création d'une surcharge d'adresse courrier depuis les données de l'adresse récupérée
	 * @param dateDebut date de début de la surcharge à générer
	 * @param source données récupérées depuis la déclaration
	 * @return une adresse pouvant être ajoutée au tiers comme surcharge
	 * @throws AdresseException en cas de souci à la transcription
	 */
	@NotNull
	private AdresseSupplementaire buildSurchargeCourrier(RegDate dateDebut, Entreprise entreprise, @NotNull Adresse source) throws AdresseException {
		// il y a deux type d'adresses supplémentaires : suisse ou étrangère...
		final AdresseSupplementaire result;
		final Integer ofsPays = source.getNoOfsPays();
		if (ofsPays == null || ofsPays == ServiceInfrastructureService.noOfsSuisse) {
			final AdresseSuisse adresse = new AdresseSuisse();
			adresse.setNumeroOrdrePoste(source.getNumeroOrdrePostal());
			adresse.setNumeroRue(source.getNumeroRue());
			result = adresse;
		}
		else {
			final AdresseEtrangere adresse = new AdresseEtrangere();
			adresse.setNumeroOfsPays(ofsPays);
			adresse.setNumeroPostalLocalite(StringUtils.trimToNull(StringUtils.trimToEmpty(source.getNumeroPostal()) + " " + StringUtils.trimToEmpty(source.getLocalite())));
			result = adresse;
		}

		result.setDateDebut(dateDebut);
		result.setUsage(TypeAdresseTiers.COURRIER);
		result.setPermanente(false);
		result.setComplement(source.getTitre());
		if (source.getCasePostale() != null) {
			result.setNumeroCasePostale(source.getCasePostale().getNumero());
			result.setTexteCasePostale(source.getCasePostale().getType());
		}
		result.setNumeroAppartement(source.getNumeroAppartement());
		result.setNumeroMaison(source.getNumero());
		result.setRue(source.getRue());
		result.setTiers(entreprise);

		// validation anticipée pour ne pas être surpris en fin de course...
		final ValidationResults vr = validationService.validate(result);
		if (vr.hasErrors()) {
			throw new AdresseDataException(vr);
		}
		return result;
	}

	/**
	 * Création d'une adresse mandataire depuis les données de l'adresse récupérée
	 * @param dateDebut date de début de l'adresse mandataire à générer
	 * @param source données récupérées depuis la déclaration
	 * @return une adresse pouvant être ajoutée au tiers comme surcharge
	 * @throws AdresseException en cas de souci à la transcription
	 */
	@NotNull
	private AdresseMandataire buildAdresseMandataire(RegDate dateDebut, @NotNull Adresse source, String raisonSociale, boolean withCopy) throws AdresseException {
		// il y a deux type d'adresses mandataires : suisse ou étrangère...
		final AdresseMandataire result;
		final Integer ofsPays = source.getNoOfsPays();
		if (ofsPays == null || ofsPays == ServiceInfrastructureService.noOfsSuisse) {
			final AdresseMandataireSuisse adresse = new AdresseMandataireSuisse();
			adresse.setNumeroOrdrePoste(source.getNumeroOrdrePostal());
			adresse.setNumeroRue(source.getNumeroRue());
			result = adresse;
		}
		else {
			final AdresseMandataireEtrangere adresse = new AdresseMandataireEtrangere();
			adresse.setNumeroOfsPays(ofsPays);
			adresse.setNumeroPostalLocalite(StringUtils.trimToNull(StringUtils.trimToEmpty(source.getNumeroPostal()) + " " + StringUtils.trimToEmpty(source.getLocalite())));
			result = adresse;
		}

		result.setDateDebut(dateDebut);
		result.setComplement(source.getTitre());
		if (source.getCasePostale() != null) {
			result.setNumeroCasePostale(source.getCasePostale().getNumero());
			result.setTexteCasePostale(source.getCasePostale().getType());
		}
		result.setNumeroMaison(source.getNumero());
		result.setRue(source.getRue());
		result.setNomDestinataire(raisonSociale);
		result.setTypeMandat(TypeMandat.GENERAL);
		result.setWithCopy(withCopy);

		// validation anticipée pour ne pas être surpris en fin de course...
		final ValidationResults vr = validationService.validate(result);
		if (vr.hasErrors()) {
			throw new AdresseDataException(vr);
		}
		return result;
	}

	/**
	 * @param entreprise une entreprise
	 * @return la dernière date (= la plus récente) de quittancement non-annulée d'une déclaration d'impôt PM non-annulée
	 */
	@NotNull
	private static RegDate getDateQuittancementDerniereDeclarationRetournee(Entreprise entreprise) {
		final List<DeclarationImpotOrdinairePM> all = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, false);
		final SortedSet<RegDate> datesQuittancement = new TreeSet<>();
		for (DeclarationImpotOrdinairePM di : all) {
			final EtatDeclaration etat = di.getDernierEtat();
			if (etat != null && etat.getEtat() == TypeEtatDeclaration.RETOURNEE) {
				datesQuittancement.add(etat.getDateObtention());
			}
		}
		// s'il n'y a rien dans cette collection, c'est que nous n'avons aucune DI non-annulée quittancée...
		// (mais le cas a déjà été traité en amont, non ?)
		return datesQuittancement.last();
	}

	/**
	 * Traitement d'une éventuelle nouvelle date d'exercice commercial
	 * @param entreprise entreprise concernée
	 * @param di déclaration matchée sur les données reçues
	 * @param dateFinExerciceCommercial date de fin d'exercice commercial renseignée dans la déclaration
	 */
	private void traiterFinExerciceCommercial(Entreprise entreprise, DeclarationImpotOrdinairePM di, @Nullable RegDate dateFinExerciceCommercial) throws EsbBusinessException {

		// détection du cas hyper simple où rien ne change
		final RegDate ancienneFinExerciceCommercial = di.getDateFinExerciceCommercial();
		if (dateFinExerciceCommercial == null || dateFinExerciceCommercial == ancienneFinExerciceCommercial) {
			// rien ne bouge...
			return;
		}

		// ok, la date a été modifiée...

		// récupération de la nouvelle période fiscale et association à la DI
		final int anneeNouvelleFinExercice = dateFinExerciceCommercial.year();
		final PeriodeFiscale nouvellePeriodeFiscale = periodeFiscaleDAO.getPeriodeFiscaleByYear(anneeNouvelleFinExercice);
		if (nouvellePeriodeFiscale == null) {
			throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "La période fiscale " + anneeNouvelleFinExercice + " n'existe pas dans Unireg.", null);
		}

		// [SIFISC-22459] si la nouvelle date de fin de l'exercice commercial est antérieure à la date de début de la DI, c'est qu'il y a souci, non ?
		if (dateFinExerciceCommercial.isBefore(di.getDateDebutExerciceCommercial())) {
			tacheService.genereTacheControleDossier(entreprise, Motifs.DATE_EXERCICE_COMMERCIAL_IGNOREE);
			addRemarque(entreprise,
			            String.format("Le retour de la DI %d/%d annonce une nouvelle fin d'exercice commercial au %s, mais celle-ci n'a pas été prise en compte automatiquement car elle est antérieure à la date de début de l'exercice commercial de la DI (%s).",
			                          di.getPeriode().getAnnee(),
			                          di.getNumero(),
			                          RegDateHelper.dateToDisplayString(dateFinExerciceCommercial),
			                          RegDateHelper.dateToDisplayString(di.getDateDebutExerciceCommercial())));

			// pas la peine d'aller plus loin...
			return;
		}

		// re-calcul des cycles de bouclement souhaités
		final List<ExerciceCommercial> anciensExercicesCommerciaux = exerciceCommercialHelper.getExercicesCommerciauxExposables(entreprise);
		final Set<RegDate> datesBouclement = anciensExercicesCommerciaux.stream()
				.map(ExerciceCommercial::getDateFin)
				.collect(Collectors.toSet());
		// remplacement de l'ancienne date par la nouvelle (c'est le re-calcul des tâches qui fera le reste - dates dans la DI)
		if (!datesBouclement.remove(ancienneFinExerciceCommercial)) {
			// il y a un problème, non ?
			// la date fournie comme ancien bouclement ne correspond pas à un bouclement existant d'après les calculs actuels...
			tacheService.genereTacheControleDossier(entreprise, Motifs.DATE_EXERCICE_COMMERCIAL_IGNOREE);
			addRemarque(entreprise, String.format(
					"Le retour de la DI %d/%d annonce une nouvelle fin d'exercice commercial au %s, mais celle-ci n'a pas été prise en compte automatiquement car la déclaration concernée n'est pas alignée avec les dates théoriques connues (tâches en instance ?).",
					di.getPeriode().getAnnee(),
					di.getNumero(),
					RegDateHelper.dateToDisplayString(dateFinExerciceCommercial)));

			// on arrête là...
			return;
		}

		// tous les bouclements entre les deux dates doivent être supprimés
		// (et on repart avec des cycles annuels tant qu'on ne se heurte pas à une DI déjà retournée)
		final NavigableSet<RegDate> datesBouclementTriees = new TreeSet<>(datesBouclement);
		final RegDate debutZone = RegDateHelper.minimum(ancienneFinExerciceCommercial, dateFinExerciceCommercial, NullDateBehavior.EARLIEST);

		// il faut trouver les DI retournées postérieures à la DI dont on traite actuellement le retour (toutes PF confondues)
		final DeclarationImpotOrdinairePM declarationRetourneePosterieure = findPremiereDeclarationImpotRetourneeApresDate(entreprise, di.getDateFin());
		final RegDate finDeclarationeRetournee = declarationRetourneePosterieure != null ? declarationRetourneePosterieure.getDateFinExerciceCommercial() : null;
		if (finDeclarationeRetournee == null) {
			datesBouclementTriees.tailSet(debutZone, false).clear();
		}
		else {
			datesBouclementTriees
					.tailSet(debutZone, false)
					.headSet(finDeclarationeRetournee, false)
					.clear();

			// cycles annuels temporaires (jusqu'à la DI retournée)
			// on ne rajoute des bouclements que sur les années où il n'y en a pas déjà un
			final Set<Integer> presentYears = datesBouclementTriees.stream()
					.map(RegDate::year)
					.collect(Collectors.toSet());
			for (RegDate date = dateFinExerciceCommercial.addYears(1) ; date.compareTo(finDeclarationeRetournee) < 0 ; date = date.addYears(1)) {
				if (!presentYears.contains(date.year())) {
					datesBouclementTriees.add(date);
				}
			}
		}
		datesBouclementTriees.add(dateFinExerciceCommercial);

		// [SIFISC-22254] vérification des périodes couvertes par des bouclements (il doit y en avoir un par an sauf exceptionnellement la première année)
		// (on ne vérifie que dans la période temporelle entre l'ancienne année de bouclement et la nouvelle)
		final Set<Integer> periodesAvecBouclement = datesBouclementTriees.stream()
				.map(RegDate::year)
				.collect(Collectors.toSet());
		if (anneeNouvelleFinExercice != ancienneFinExerciceCommercial.year()) {
			final int anneeFondation = anciensExercicesCommerciaux.get(0).getDateDebut().year();
			final int anneePremierBouclementAControler = Math.max(Math.min(ancienneFinExerciceCommercial.year(), anneeNouvelleFinExercice), anneeFondation + 1);
			final int anneeDernierBouclementAControler = Math.max(ancienneFinExerciceCommercial.year(), anneeNouvelleFinExercice);
			if (anneeDernierBouclementAControler - anneePremierBouclementAControler > 0) {
				for (int annee = anneePremierBouclementAControler ; annee <= anneeDernierBouclementAControler ; ++ annee) {
					if (!periodesAvecBouclement.contains(annee) && annee > anneeFondation) {
						tacheService.genereTacheControleDossier(entreprise, Motifs.DATE_EXERCICE_COMMERCIAL_IGNOREE);
						addRemarque(entreprise, String.format("Le retour de la DI %d/%d annonce une nouvelle fin d'exercice commercial au %s, mais l'année civile %d se retrouve alors sans bouclement, ce qui est interdit.",
						                                      di.getPeriode().getAnnee(),
						                                      di.getNumero(),
						                                      RegDateHelper.dateToDisplayString(dateFinExerciceCommercial),
						                                      annee));

						// on arrête là...
						return;
					}
				}
			}
		}

		// mise à jour des nouveaux cycles de bouclement dans l'entreprise
		final List<Bouclement> nouveauxCycles = bouclementService.extractBouclementsDepuisDates(datesBouclementTriees, 12);
		BouclementHelper.resetBouclements(entreprise, nouveauxCycles);

		if (ancienneFinExerciceCommercial.year() != anneeNouvelleFinExercice) {
			// années différentes...

			// commençons par voir s'il existe une DI retournée postérieure à la DI que l'on considère maintenant
			final DeclarationImpotOrdinairePM retourneeSuivante = findPremiereDeclarationImpotRetourneeApresDate(entreprise, di.getDateFin());
			if (retourneeSuivante != null) {
				tacheService.genereTacheControleDossier(entreprise, Motifs.CHGT_PERIODE_FISCALE_DI_ULTERIEURE);
			}
			else {
				tacheService.genereTacheControleDossier(entreprise, Motifs.CHGT_PERIODE_FISCALE);
			}

			// les dates à assigner à la DI qui change (la date de début sera immédiatement recalculée si nécessaire par la mécanique de synchronisation des tâches)
			final DateRange nouveauRange = new DateRangeHelper.Range(RegDateHelper.minimum(di.getDateDebut(), dateFinExerciceCommercial, NullDateBehavior.EARLIEST), dateFinExerciceCommercial);

			// les autres déclarations d'impôt de la nouvelle période doivent peut-être être reprises un peu...
			final List<DeclarationImpotOrdinairePM> declarationsSurNouvellePeriode = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, nouvellePeriodeFiscale.getAnnee(), false);
			if (!declarationsSurNouvellePeriode.isEmpty()) {
				for (DeclarationImpotOrdinairePM diNouvellePeriode : declarationsSurNouvellePeriode) {
					if (DateRangeHelper.intersect(diNouvellePeriode, nouveauRange)) {
						// ça va coincer dès qu'on va bouger la DI en retour...
						// en fait, ce cas n'est possible que si on repousse plus loin la date de fin de la DI existante, i.e. si on passe d'une PF n à une PF n+x
						// -> on va donc essayer de corriger le cas en rognant sur la date de début de la DI déjà présente sur la nouvelle PF
						if (nouveauRange.getDateFin().compareTo(diNouvellePeriode.getDateFin()) < 0) {
							// rognage possible, la DI ne disparaît pas complètement...
							diNouvellePeriode.setDateDebut(nouveauRange.getDateFin().getOneDayAfter());
							diNouvellePeriode.setDateDebutExerciceCommercial(diNouvellePeriode.getDateDebut());
						}
						else {
							// oulala, si on rogne la date de début, on dépasse la date de fin...
							// il faut donc annuler la DI, tout simplement
							diNouvellePeriode.setAnnule(true);
						}
					}
				}
			}

			// association de la période fiscale, du modèle de document (du même type), s'il existe, et des dates...
			di.setPeriode(nouvellePeriodeFiscale);
			di.setModeleDocument(nouvellePeriodeFiscale.get(di.getTypeDeclaration()));
			di.setDateDebut(nouveauRange.getDateDebut());
			di.setDateFin(nouveauRange.getDateFin());
			di.setDateDebutExerciceCommercial(nouveauRange.getDateDebut());
			di.setDateFinExerciceCommercial(nouveauRange.getDateFin());

			final int ancienNumeroSequence = di.getNumero();
			di.setNumero(null);                                                 // recalcul nécessaire suite au changement de période fiscale
			entreprise.getDeclarations().remove(di);
			entreprise.addDeclaration(di);

			// on ajoute une remarque pour le suivi
			addRemarque(entreprise, String.format("La déclaration %d/%d a été transformée en %d/%d suite au déplacement de la date de fin d'exercice commercial du %s au %s par retour de la DI.",
			                                      ancienneFinExerciceCommercial.year(),
			                                      ancienNumeroSequence,
			                                      anneeNouvelleFinExercice,
			                                      di.getNumero(),
			                                      RegDateHelper.dateToDisplayString(ancienneFinExerciceCommercial),
			                                      RegDateHelper.dateToDisplayString(dateFinExerciceCommercial)));
		}

		// Calcul de l'assujettissement... Si l'entreprise n'est pas assujettie à la nouvelle date de fin d'exercice commercial, il faut
		// créer une tâche de contrôle de dossier
		try {
			final List<Assujettissement> assujettissements = assujettissementService.determine(entreprise);
			final Assujettissement assujettissement = DateRangeHelper.rangeAt(assujettissements, dateFinExerciceCommercial);
			if (assujettissement == null) {
				// oups ! pas d'assujettissement à la nouvelle date...
				// -> tâche de contrôle de dossier
				tacheService.genereTacheControleDossier(entreprise, Motifs.NON_ASSUJETTI_APRES_CHGT_EX_COMMERCIAL);
			}
		}
		catch (AssujettissementException e) {
			// rien pu faire...
			LOGGER.warn(String.format("Impossible de calculer l'assujettissement de l'entreprise %s pour laquelle on vient de traiter le retour de la DI %d avec changement de fin d'exercice commercial.",
			                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                          ancienneFinExerciceCommercial.year()),
			            e);
		}
	}

	/**
	 * Récupération, dans une liste des déclarations d'une PF donnée, de celle qui a le bon numéro de séquence
	 * @param declarations les déclarations d'une PF
	 * @param noSequence le numéro de séquence recherché
	 * @return la DI (si elle existe) qui possède le bon numéro de séquence (on prend la première que l'on trouve, il ne devrait y en avoir qu'une par PF de toute façon)
	 */
	@Nullable
	private static DeclarationImpotOrdinairePM findDeclaration(List<DeclarationImpotOrdinairePM> declarations, int noSequence) {
		for (DeclarationImpotOrdinairePM di : declarations) {
			if (di.getNumero() != null && di.getNumero() == noSequence) {
				return di;
			}
		}
		return null;
	}

	/**
	 * @param entreprise une entreprise
	 * @param date une date seuil
	 * @return la première déclaration retournée dont la période est strictement postérieure à la date de référence donnée
	 */
	@Nullable
	private static DeclarationImpotOrdinairePM findPremiereDeclarationImpotRetourneeApresDate(Entreprise entreprise, RegDate date) {
		final List<DeclarationImpotOrdinairePM> all = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, false);
		for (DeclarationImpotOrdinairePM di : all) {
			if (di.getDateDebut().compareTo(date) > 0) {
				final EtatDeclaration dernierEtat = di.getDernierEtat();
				if (dernierEtat != null && dernierEtat.getEtat() == TypeEtatDeclaration.RETOURNEE) {
					return di;
				}
			}
		}
		return null;
	}

	/**
	 * Ajout d'une nouvelle remarque avec le texte donné
	 * @param entreprise entreprise concernée
	 * @param texte la remarque à ajouter
	 */
	private void addRemarque(Entreprise entreprise, String texte) {
		if (StringUtils.isNotBlank(texte)) {
			final Remarque remarque = new Remarque();
			remarque.setTiers(entreprise);
			remarque.setTexte(StringUtils.abbreviate(texte.trim(), LengthConstants.TIERS_REMARQUE));
			hibernateTemplate.merge(remarque);
		}
	}

	/**
	 * Ajout d'une nouvelle remarque sur le tiers entreprise, comprendant les données retournées et préfixée par l'entête donné
	 * @param entreprise l'entreprise concernée
	 * @param entete [optionnelle] l'entête à placer sur la première ligne de la remarque
	 * @param retour les données reçues
	 */
	private void addRemarqueDonneesCompletes(Entreprise entreprise, String entete, RetourDI retour) {
		addRemarque(entreprise, buildTexteRemarqueDonneesCompletes(entete, retour));
	}

	private String buildTexteRemarqueDonneesCompletes(String entete, RetourDI retour) {
		final StringBuilder b = new StringBuilder();
		if (StringUtils.isNotBlank(entete)) {
			b.append(entete).append(" :\n");
		}
		final InformationsEntreprise entreprise = retour.getEntreprise();
		final RegDate dateReference = extractDateReference(retour);
		if (entreprise != null) {
			if (entreprise.getAdministrationEffective() != null) {
				addLigne(b, String.format("- administration effective : %s", entreprise.getAdministrationEffective().toDisplayString(infraService, dateReference)));
			}
			if (entreprise.getSiege() != null) {
				addLigne(b, String.format("- siège : %s", entreprise.getSiege().toDisplayString(infraService, dateReference)));
			}
			if (entreprise.getDateFinExerciceCommercial() != null) {
				addLigne(b, String.format("- date de fin d'exercice commercial : %s", RegDateHelper.dateToDisplayString(entreprise.getDateFinExerciceCommercial())));
			}
			if (entreprise.getIban() != null) {
				addLigne(b, String.format("- IBAN : %s", IbanHelper.toDisplayString(entreprise.getIban())));
			}
			if (entreprise.getTitulaireCompteBancaire() != null) {
				addLigne(b, String.format("- titulaire compte bancaire : %s", entreprise.getTitulaireCompteBancaire()));
			}
			if (entreprise.getAdresseCourrier() != null) {
				addLigne(b, String.format("- adresse courrier : %s", entreprise.getAdresseCourrier().toDisplayString(infraService, adresseService, dateReference)));
			}
		}
		final InformationsMandataire mandataire = retour.getMandataire();
		if (mandataire != null) {
			if (mandataire.isNumeroIdeMandataireUtilisable()) {
				addLigne(b, String.format("- IDE mandataire : %s", FormatNumeroHelper.formatNumIDE(mandataire.getIdeMandataire())));
			}
			if (mandataire.getSansCopieMandataire() != null) {
				addLigne(b, String.format("- copie mandataire : %s", mandataire.getSansCopieMandataire() ? "sans" : "avec"));
			}
			if (mandataire.getNoTelContact() != null) {
				addLigne(b, String.format("- téléphone contact mandataire : %s", mandataire.getNoTelContact()));
			}
			if (mandataire.getAdresse() != null) {
				addLigne(b, String.format("- adresse mandataire : %s", mandataire.getAdresse().toDisplayString(infraService, adresseService, dateReference)));
			}
		}
		return StringUtils.trimToNull(b.toString());
	}

	private static void addLigne(StringBuilder b, String line) {
		b.append(line).append("\n");
	}

	private static RegDate extractDateReference(RetourDI retour) {
		final InformationsEntreprise entreprise = retour.getEntreprise();
		if (entreprise != null && entreprise.getDateFinExerciceCommercial() != null) {
			return entreprise.getDateFinExerciceCommercial();
		}
		final RegDate today = RegDate.get();
		if (retour.getPf() == today.year()) {
			return today;
		}
		return RegDate.get(retour.getPf(), 12, 31);
	}
}
