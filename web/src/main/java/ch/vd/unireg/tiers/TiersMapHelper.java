package ch.vd.unireg.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.ApplicationConfig;
import ch.vd.unireg.common.CommonMapHelper;
import ch.vd.unireg.documentfiscal.TypeAutreDocumentFiscalEmettableManuellement;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.LogicielMetier;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM;
import ch.vd.unireg.reqdes.EtatTraitement;
import ch.vd.unireg.tiers.TiersCriteria.TypeRecherche;
import ch.vd.unireg.tiers.TiersCriteria.TypeRechercheForFiscal;
import ch.vd.unireg.tiers.TiersCriteria.TypeRechercheLocalitePays;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridique;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.GroupeFlagsEntreprise;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.NatureJuridique;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TarifImpotSource;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseRetour;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeDroitAcces;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeFlagEntreprise;
import ch.vd.unireg.type.TypeLettreBienvenue;
import ch.vd.unireg.type.TypeMandat;
import ch.vd.unireg.type.TypeOperation;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Cette classe expose les différents enums sours forme de map enum->description.
 */
public class TiersMapHelper extends CommonMapHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersMapHelper.class);

	private Map<FormeJuridique, String> mapFormeJuridique;
	private Map<FormeJuridiqueEntreprise, String> mapFormesJuridiquesEntreprise;
	private Map<CategorieEntreprise, String> mapCategorieEntreprise;
	private Map<NatureJuridique, String> mapNatureJuridique;
	private Map<TypeRecherche, String> mapTypeRechercheNom;
	private Map<TypeRechercheLocalitePays, String> mapTypeRechercheLocalitePays;
	private Map<TypeRechercheForFiscal, String> mapTypeRechercheForFiscal;
	private Map<CategorieEtranger, String> mapCategorieEtranger;
	private Map<Sexe, String> mapSexe;
	private Map<MotifRattachement, String> mapRattachement;
	private Map<GenreImpot, String> mapGenreImpot;
	private Map<TypeAutoriteFiscale, String> mapTypeAutoriteFiscale;
	private Map<TypeAutoriteFiscale, String> mapTypeAutoriteFiscaleDPI;
	private Map<TypeAutoriteFiscale, String> mapTypeAutoriteFiscaleEntreprise;
	private Map<ModeImposition, String> mapModeImposition;
	private Map<CategorieImpotSource, String> mapCategorieImpotSource;
	private Map<PeriodiciteDecompte, String> mapPeriodiciteDecompte;
	private Map<ModeCommunication, String> mapModeCommunication;
	private Map<TexteCasePostale, String> mapTexteCasePostale;
	private Map<TypeEvenementCivil, String> mapTypeEvenementCivil;
	private Map<TypeEvenementCivilEch, String> mapTypeEvenementCivilEch;
	private Map<ActionEvenementCivilEch, String> mapActionEvenementCivilEch;
	private Map<EtatEvenementCivil, String> mapStatusEvenementCivil;
	private Map<TypeEvenementEntreprise, String> mapTypeEvenementEntreprise;
	private Map<EtatEvenementEntreprise, String> mapEtatEvenementEntreprise;
	private Map<TypeRapportEntreTiers, String> mapTypeRapportEntreTiers;
	private Map<EtatEvenementCivil, String> mapEtatsEvenementCivil;
	private Map<TypeEtatDocumentFiscal, String> mapTypeEtatListeRecapitulative;
	private Map<EtatCivil, String> mapEtatsCivil;
	private Map<TypeAdresseTiers, String> mapTypeAdresse;
	private Map<TypeAdresseTiers, String> mapTypeAdresseFiscale;
	private Map<TypeDocument, String> mapTypesDeclarationImpotPP;
	private Map<TypeDocument, String> mapTypesDeclarationImpotPM;
	private Map<TypeDocument, String> mapTypesDeclarationImpotOrdinaire;
	private Map<TypeDocument, String> mapTypesDeclarationImpotPourParam;
	private Map<TypeAdresseRetour, String> mapTypesAdresseRetour;
	private Map<TarifImpotSource, String> mapTarifsImpotSource;
	private Map<PeriodeDecompte, String> mapPeriodeDecompte;
	private Map<TypeDroitAcces, String> mapDroitAcces;
	private Map<TypeOperation, String> mapTypeOperation;
	private Map<EtatTraitement, String> mapEtatTraitementReqDes;
	private Map<EtatDelaiDocumentFiscal, String> mapEtatDelaiDeclaration;
	private Map<AllegementFiscal.TypeCollectivite, String> mapTypeCollectiviteAllegement;
	private Map<AllegementFiscal.TypeImpot, String> mapTypeImpotAllegement;
	private Map<AllegementFiscalConfederation.Type, String> mapTypesIFDAllegement;
	private Map<AllegementFiscalCantonCommune.Type, String> mapTypesICCAllegement;
	private Map<TypeFlagEntreprise, String> mapTypesFlagEntreprise;
	private Map<ParametrePeriodeFiscalePM.ReferencePourDelai, String> mapReferencesPourDelai;
	private Map<TypeMandat, String> mapTypesMandat;
	private Map<TypeAutreDocumentFiscalEmettableManuellement, String> mapTypesAutreDocumentFiscalEmettableManuellement;
	private Map<TypeLettreBienvenue, String> mapTypesLettreBienvenue;
	private Map<TypeAnnonce, String> mapTypeAnnonce;
	private Map<StatutAnnonce, String> mapStatutAnnonce;
	private Map<EtatEvenementRF, String> mapEtatEvenementRF;

	private ServiceInfrastructureService infraService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * Initialise la map des formes juridiques
	 *
	 * @return une map
	 */
	public Map<FormeJuridique, String> getMapFormeJuridique() {
		if (mapFormeJuridique == null) {
			mapFormeJuridique = initMapEnum(ApplicationConfig.masterKeyFormeJuridique, FormeJuridique.class);
		}
		return mapFormeJuridique;
	}

	/**
	 * Initialise la map des natures juridiques
	 *
	 * @return une map
	 */
	public Map<NatureJuridique, String> getMapNatureJuridique() {
		if (mapNatureJuridique == null) {
			mapNatureJuridique = initMapEnum(ApplicationConfig.masterKeyNatureJuridique, NatureJuridique.class);
		}
		return mapNatureJuridique;
	}

	/**
	 * Initialise la map des types de recherche par nom
	 *
	 * @return une map
	 */
	public Map<TiersCriteria.TypeRecherche, String> getMapTypeRechercheNom() {
		if (mapTypeRechercheNom == null) {
			mapTypeRechercheNom = initMapEnum(ApplicationConfig.masterKeyTypeRechercheNom, TiersCriteria.TypeRecherche.class);
		}
		return mapTypeRechercheNom;
	}

	/**
	 * Initialise la map des types de recherche par localite / pays
	 *
	 * @return une map
	 */
	public Map<TiersCriteria.TypeRechercheLocalitePays, String> getMapTypeRechercheLocalitePays() {
		if (mapTypeRechercheLocalitePays == null) {
			mapTypeRechercheLocalitePays = initMapEnum(ApplicationConfig.masterKeyTypeRechercheLocalitePays,
					TiersCriteria.TypeRechercheLocalitePays.class);
		}
		return mapTypeRechercheLocalitePays;
	}

	/**
	 * Initialise la map des types de recherche par for
	 *
	 * @return une map
	 */
	public Map<TiersCriteria.TypeRechercheForFiscal, String> getMapTypeRechercheForFiscal() {
		if (mapTypeRechercheForFiscal == null) {
			mapTypeRechercheForFiscal = initMapEnum(ApplicationConfig.masterKeyTypeRechercheForFiscal,
					TiersCriteria.TypeRechercheForFiscal.class);
		}
		return mapTypeRechercheForFiscal;
	}

	/**
	 * Initialise la map des categories etranger
	 *
	 * @return une map
	 */
	public Map<CategorieEtranger, String> getMapCategorieEtranger() {
		if (mapCategorieEtranger == null) {
			mapCategorieEtranger = initMapEnum(ApplicationConfig.masterKeyCategorieEtranger, CategorieEtranger.class,
					CategorieEtranger._01_SAISONNIER_A);
		}
		return mapCategorieEtranger;
	}

	/**
	 * Initialise la map des sexes
	 *
	 * @return une map
	 */
	public Map<Sexe, String> getMapSexe() {
		if (mapSexe == null) {
			mapSexe = initMapEnum(ApplicationConfig.masterKeySexe, Sexe.class);
		}
		return mapSexe;
	}

	/**
	 * Initialise la map des rattachements
	 *
	 * @return une map
	 */
	public Map<MotifRattachement, String> getMapRattachement() {
		if (mapRattachement == null) {
			mapRattachement = initMapEnum(ApplicationConfig.masterKeyRattachement, MotifRattachement.class, MotifRattachement.ETABLISSEMENT_STABLE);
		}
		return mapRattachement;
	}

	public Map<MotifRattachement, String> getMapRattachement(MotifRattachement... motifs) {
		return initMapEnum(ApplicationConfig.masterKeyRattachement, motifs);
	}

	/**
	 * Initialise la map des genres d'impot
	 *
	 * @return une map
	 */
	public Map<GenreImpot, String> getMapGenreImpot() {
		if (mapGenreImpot == null) {
			mapGenreImpot = initMapEnum(ApplicationConfig.masterKeyGenreImpot, GenreImpot.class, GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		}
		return mapGenreImpot;
	}

	public Map<GenreImpot, String> getMapGenreImpot(GenreImpot... genres) {
		return initMapEnum(ApplicationConfig.masterKeyGenreImpot, genres);
	}

	/**
	 * Initialise la map des types de fors fiscaux
	 *
	 * @return une map
	 */
	public Map<TypeAutoriteFiscale, String> getMapTypeAutoriteFiscale() {
		if (mapTypeAutoriteFiscale == null) {
			mapTypeAutoriteFiscale = initMapEnum(ApplicationConfig.masterKeyTypeAutoriteFiscale, TypeAutoriteFiscale.class);
		}
		return mapTypeAutoriteFiscale;
	}

	/**
	 * Initialise la map des types de fors fiscaux pour les DPI
	 *
	 * @return une map
	 */
	public Map<TypeAutoriteFiscale, String> getMapTypeAutoriteFiscaleDPI() {
		if (mapTypeAutoriteFiscaleDPI == null) {
			mapTypeAutoriteFiscaleDPI = initMapEnum(ApplicationConfig.masterKeyTypeAutoriteFiscale, TypeAutoriteFiscale.class, TypeAutoriteFiscale.PAYS_HS);
		}
		return mapTypeAutoriteFiscaleDPI;
	}

	/**
	 * Initialise la map des types de fors fiscaux pour les DPI
	 *
	 * @return une map
	 */
	public Map<TypeAutoriteFiscale, String> getMapTypeAutoriteFiscaleEntreprise() {
		if (mapTypeAutoriteFiscaleEntreprise == null) {
			mapTypeAutoriteFiscaleEntreprise = initMapEnum(ApplicationConfig.masterKeyTypeAutoriteFiscale, TypeAutoriteFiscale.class, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		}
		return mapTypeAutoriteFiscaleEntreprise;
	}

	/**
	 * Initialise la map des modes d'imposition
	 *
	 * @return une map
	 */
	public Map<ModeImposition, String> getMapModeImposition() {

		if (mapModeImposition == null) {
			mapModeImposition = initMapEnum(ApplicationConfig.masterKeyModeImposition, ModeImposition.class);
		}
		return mapModeImposition;
	}

	/**
	 * Initialise la map des categories d'impot a la source
	 * @return une map
	 */
	public Map<CategorieImpotSource, String> getMapCategorieImpotSource() {
		if (mapCategorieImpotSource == null) {
			mapCategorieImpotSource = initMapEnum(ApplicationConfig.masterKeyCategorieImpotSource, CategorieImpotSource.class);
		}
		return mapCategorieImpotSource;
	}

	/**
	 * Initialise la map des periodicites decompte
	 *
	 * @return une map
	 */
	public Map<PeriodiciteDecompte, String> getMapPeriodiciteDecompte() {
		if (mapPeriodiciteDecompte == null) {
			mapPeriodiciteDecompte = initMapEnum(ApplicationConfig.masterKeyPeriodiciteDecompte, PeriodiciteDecompte.class);
		}
		return mapPeriodiciteDecompte;
	}

	/**
	 * Renvoie une map non-modifiable des périodicités de décomptes assignables au tout venant sur un débiteur
	 * @param current la périodicité actuelle du débiteur (qui doit toujours faire partie des choix sélectionnables)
	 * @return une map des valeurs sélectionables
	 */
	public Map<PeriodiciteDecompte, String> getMapLimiteePeriodiciteDecompte(PeriodiciteDecompte current) {
		final Set<PeriodiciteDecompte> allowedValues = EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.UNIQUE);
		if (current != null) {
			// on doit toujours pouvoir "descendre" en périodicité
			allowedValues.add(current);
			allowedValues.addAll(current.getShorterPeriodicities());
		}
		return getSpecificMapEnum(ApplicationConfig.masterKeyPeriodiciteDecompte, allowedValues);
	}

	/**
	 * Initialise la map des modes de communication
	 *
	 * @return une map
	 */
	public Map<ModeCommunication, String> getMapModeCommunication() {
		if (mapModeCommunication == null) {
			mapModeCommunication = initMapEnum(ApplicationConfig.masterKeyModeCommunication, ModeCommunication.class);
		}
		return mapModeCommunication;
	}

	/**
	 * Renvoie une map non-modifiable des modes de communication assignables au tout venant sur un débiteur
	 * @param modeCourant le mode de communication actuel du débiteur (qui doit toujours faire partie des choix sélectionnables)
	 * @param periodicite périodicité du débiteur
	 * @return une map des valeurs sélectionables
	 */
	public Map<ModeCommunication, String> getMapLimiteeModeCommunication(ModeCommunication modeCourant, PeriodiciteDecompte periodicite) {
		final Set<ModeCommunication> allowedValues;
		if (periodicite == PeriodiciteDecompte.UNIQUE || periodicite == PeriodiciteDecompte.MENSUEL) {
			allowedValues = EnumSet.allOf(ModeCommunication.class);
		}
		else {
			allowedValues = EnumSet.of(ModeCommunication.PAPIER);
		}
		if (modeCourant != null) {
			allowedValues.add(modeCourant);
		}
		return getSpecificMapEnum(ApplicationConfig.masterKeyModeCommunication, allowedValues);
	}

	/**
	 * Initialise la map des textes Case Postale
	 *
	 * @return une map
	 */
	public Map<TexteCasePostale, String> getMapTexteCasePostale() {
		if (mapTexteCasePostale == null) {
			mapTexteCasePostale = initMapEnum(ApplicationConfig.masterKeyTexteCasePostale, TexteCasePostale.class);
		}
		return mapTexteCasePostale;
	}

	/**
	 * Initialise la map des types d'evenements
	 *
	 * @return une map
	 */
	public Map<TypeEvenementCivil, String> getMapTypeEvenementCivil() {
		if (mapTypeEvenementCivil == null) {
			mapTypeEvenementCivil = initMapEnum(ApplicationConfig.masterKeyTypeEvenement, TypeEvenementCivil.class);
		}
		return mapTypeEvenementCivil;
	}

	/**
	 * Initialise la map des types d'evenements civils ech
	 *
	 * @return une map
	 */
	public Map<TypeEvenementCivilEch, String> getMapTypeEvenementCivilEch() {
		if (mapTypeEvenementCivilEch == null) {
			mapTypeEvenementCivilEch = initMapEnum(ApplicationConfig.masterKeyTypeEvenementEch, TypeEvenementCivilEch.class);
		}
		return mapTypeEvenementCivilEch;
	}

	/**
	 * Initialise la map des actions d'evenements civils ech
	 *
	 * @return une map
	 */
	public Map<ActionEvenementCivilEch, String> getMapActionEvenementCivilEch() {
		if (mapActionEvenementCivilEch == null) {
			mapActionEvenementCivilEch = initMapEnum(ApplicationConfig.masterKeyActionEvenementEch, ActionEvenementCivilEch.class);
		}
		return mapActionEvenementCivilEch;
	}

	/**
	 * Initialise la map des status d'evenements
	 *
	 * @return une map
	 */
	public Map<EtatEvenementCivil, String> getMapStatusEvenementCivil() {
		if (mapStatusEvenementCivil == null) {
			mapStatusEvenementCivil = initMapEnum(ApplicationConfig.masterKeyTypeEvenement, EtatEvenementCivil.class);
		}
		return mapStatusEvenementCivil;
	}

	/**
	 * Initialise la map des types de rapports entre tiers
	 *
	 * @return une map
	 */
	public Map<TypeRapportEntreTiers, String> getMapTypeRapportEntreTiers() {
		if (mapTypeRapportEntreTiers == null) {
			mapTypeRapportEntreTiers = initMapEnum(ApplicationConfig.masterKeyTypeRapportEntreTiers, TypeRapportEntreTiers.class);
		}
		return mapTypeRapportEntreTiers;
	}

	/**
	 * Initialise la map de etats des evts civils
	 *
	 * @return une map
	 */
	public Map<EtatEvenementCivil, String> getMapEtatsEvenementCivil() {
		if (mapEtatsEvenementCivil == null) {
			mapEtatsEvenementCivil = initMapEnum(ApplicationConfig.masterKeyEtatEvenementCivil, EtatEvenementCivil.class);
		}
		return mapEtatsEvenementCivil;
	}

	/**
	 * Initialise la map des types d'evenements civils entreprise
	 *
	 * @return une map
	 */
	public Map<TypeEvenementEntreprise, String> getMapTypeEvenementEntreprise() {
		if (mapTypeEvenementEntreprise == null) {
			mapTypeEvenementEntreprise = initMapEnum(ApplicationConfig.masterKeyTypeEvenementEntreprise, TypeEvenementEntreprise.class);
		}
		return mapTypeEvenementEntreprise;
	}

	/**
	 * Initialise la map de etats des evts entreprise
	 *
	 * @return une map
	 */
	public Map<EtatEvenementEntreprise, String> getMapEtatsEvenementEntreprise() {
		if (mapEtatEvenementEntreprise == null) {
			mapEtatEvenementEntreprise = initMapEnum(ApplicationConfig.masterKeyEtatEvenementEntreprise, EtatEvenementEntreprise.class);
		}
		return mapEtatEvenementEntreprise;
	}

	/**
	 * Initialise la map des états des unités de traitement ReqDes
	 * @return la map
	 */
	public Map<EtatTraitement, String> getMapEtatsUniteTraitementReqDes() {
		if (mapEtatTraitementReqDes == null) {
			mapEtatTraitementReqDes = initMapEnum(ApplicationConfig.masterKeyEtatTraitementReqDes, EtatTraitement.class);
		}
		return mapEtatTraitementReqDes;
	}

	/**
	 * Initialise la map de types d'etat de document utilisables pour les listes récapitulatives
	 *
	 * @return une map
	 */
	public Map<TypeEtatDocumentFiscal, String> getMapTypeEtatListeRecapitulative() {
		if (mapTypeEtatListeRecapitulative == null) {
			mapTypeEtatListeRecapitulative = initMapEnum(ApplicationConfig.masterKeyTypeEtatDocumentF, TypeEtatDocumentFiscal.class, TypeEtatDocumentFiscal.RAPPELE, TypeEtatDocumentFiscal.SUSPENDU);
		}
		return mapTypeEtatListeRecapitulative;
	}

	/**
	 * Initialise la map de etats des etats civils
	 *
	 * @return une map
	 */
	public Map<EtatCivil, String> getMapEtatsCivil() {
		if (mapEtatsCivil == null) {
			mapEtatsCivil = initMapEnum(ApplicationConfig.masterKeyEtatCivil, EtatCivil.class);
		}
		return mapEtatsCivil;
	}

	/**
	 * Initialise la map de etats des types adresse
	 *
	 * @return une map
	 */
	public Map<TypeAdresseTiers, String> getMapTypeAdresse() {
		if (mapTypeAdresse == null) {
			mapTypeAdresse = initMapEnum(ApplicationConfig.masterKeyTypeAdresseTiers, TypeAdresseTiers.class);
		}
		return mapTypeAdresse;
	}

	/**
	 * Initialise la map de etats des types autorisés pour une adresse fiscale
	 *
	 * @return une map
	 */
	public Map<TypeAdresseTiers, String> getMapTypeAdresseFiscale() {
		if (mapTypeAdresseFiscale == null) {
			mapTypeAdresseFiscale = initMapEnum(ApplicationConfig.masterKeyTypeAdresseTiers, TypeAdresseTiers.class, TypeAdresseTiers.DOMICILE);
		}
		return mapTypeAdresseFiscale;
	}



	/**
	 * Initialise la map des types de declaration d'impot pour l'écran de paramétrage
	 *
	 * @return une map
	 */
	public Map<TypeDocument, String> getTypesDeclarationImpotPourParam() {

		if (mapTypesDeclarationImpotPourParam == null) {
			mapTypesDeclarationImpotPourParam = initMapEnum(ApplicationConfig.masterKeyTypeDeclarationImpot, TypeDocument.class,TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE);
		}
		return mapTypesDeclarationImpotPourParam;
	}

	/**
	 * Initialise la map des types de declaration d'impot pour l'écran d'édition de la DI PP
	 *
	 * @return une map
	 */
	public Map<TypeDocument, String> getTypesDeclarationImpotPP() {
		if (mapTypesDeclarationImpotPP == null) {
			mapTypesDeclarationImpotPP = initMapEnum(ApplicationConfig.masterKeyTypeDeclarationImpot, TypeDocument.class,
			                                         TypeDocument.LISTE_RECAPITULATIVE,
			                                         TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
			                                         TypeDocument.E_FACTURE_ATTENTE_CONTACT,
			                                         TypeDocument.E_FACTURE_ATTENTE_SIGNATURE,
			                                         TypeDocument.DECLARATION_IMPOT_APM_BATCH,
			                                         TypeDocument.DECLARATION_IMPOT_APM_LOCAL,
			                                         TypeDocument.DECLARATION_IMPOT_PM_BATCH,
			                                         TypeDocument.DECLARATION_IMPOT_PM_LOCAL,
			                                         TypeDocument.QUESTIONNAIRE_SNC);
		}
		return mapTypesDeclarationImpotPP;
	}

	/**
	 * Initialise la map des types de declaration d'impot pour l'écran d'édition de la DI PP
	 *
	 * @return une map
	 */
	public Map<TypeDocument, String> getTypesDeclarationImpotPM() {
		if (mapTypesDeclarationImpotPM == null) {
			mapTypesDeclarationImpotPM = initMapEnum(ApplicationConfig.masterKeyTypeDeclarationImpot, TypeDocument.class,
			                                         TypeDocument.LISTE_RECAPITULATIVE,
			                                         TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
			                                         TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL,
			                                         TypeDocument.DECLARATION_IMPOT_DEPENSE,
			                                         TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE,
			                                         TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			                                         TypeDocument.DECLARATION_IMPOT_APM_BATCH,
			                                         TypeDocument.DECLARATION_IMPOT_PM_BATCH,
			                                         TypeDocument.QUESTIONNAIRE_SNC,
			                                         TypeDocument.E_FACTURE_ATTENTE_CONTACT,
			                                         TypeDocument.E_FACTURE_ATTENTE_SIGNATURE);
		}
		return mapTypesDeclarationImpotPM;
	}

	private static final Set<TypeDocument> DI_ORDINAIRE = EnumSet.of(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
	                                                                 TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL,
	                                                                 TypeDocument.DECLARATION_IMPOT_VAUDTAX,
	                                                                 TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE,
	                                                                 TypeDocument.DECLARATION_IMPOT_DEPENSE);

	/**
	 * Initialise la map des types de declarations d'impôt ordinaires pour l'écran d'édition de la DI (quittancement)
	 *
	 * @return une map
	 */
	public Map<TypeDocument, String> getTypesDeclarationsImpotOrdinaires() {

		if (mapTypesDeclarationImpotOrdinaire == null) {
			final List<TypeDocument> typesIgnores = new ArrayList<>();
			for (TypeDocument type : TypeDocument.values()) {
				// doivent être ignorées la version batch de la déclaration complète et toutes les déclarations non-ordinaires
				if (type == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH || !DI_ORDINAIRE.contains(type)) {
					typesIgnores.add(type);
				}
			}
			mapTypesDeclarationImpotOrdinaire = initMapEnum(ApplicationConfig.masterKeyTypeDeclarationImpot, TypeDocument.class, typesIgnores.toArray(new TypeDocument[0]));
		}
		return mapTypesDeclarationImpotOrdinaire;
	}


	/**
	 * Initialise la map des types d'adresse de retour
	 *
	 * @return une map
	 */
	public Map<TypeAdresseRetour, String> getTypesAdresseRetour() {
		if (mapTypesAdresseRetour == null) {
			mapTypesAdresseRetour = initMapEnum(ApplicationConfig.masterKeyTypeAdresseRetour, TypeAdresseRetour.class);
		}
		return mapTypesAdresseRetour;
	}


	/**
	 * Initialise la map des tarifs impot source
	 *
	 * @return une map
	 */
	public Map<TarifImpotSource, String> getTarifsImpotSource() {
		if (mapTarifsImpotSource == null) {
			mapTarifsImpotSource = initMapEnum(ApplicationConfig.masterKeyTarifImpotSource, TarifImpotSource.class);
		}
		return mapTarifsImpotSource;
	}

	/**
	 * Initialise la map des periode de décompte Elles sont triées par clef
	 *
	 * @return une map
	 */
	public Map<PeriodeDecompte, String> getPeriodeDecomptes() {
		if (mapPeriodeDecompte == null) {
			mapPeriodeDecompte = new EnumMap<>(PeriodeDecompte.class);
			PeriodeDecompte[] periodesDecompte = PeriodeDecompte.values();
			for (PeriodeDecompte aPeriodesDecompte : periodesDecompte) {
				String periodeDecompteNom = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyPeriodeDecompte + aPeriodesDecompte);
				mapPeriodeDecompte.put(aPeriodesDecompte, periodeDecompteNom);
			}
		}
		return mapPeriodeDecompte;
	}

	/**
	 * Initialise la map des droits d'acces
	 *
	 * @return une map
	 */
	public Map<TypeDroitAcces, String> getDroitAcces() {
		if (mapDroitAcces == null) {
			mapDroitAcces = initMapEnum(ApplicationConfig.masterKeyDroitAcces, TypeDroitAcces.class);
		}
		return mapDroitAcces;
	}

	/**
	 * Initialise la map des types d'opération
	 *
	 * @return une map
	 */
	public Map<TypeOperation, String> getTypeOperation() {
		if (mapTypeOperation == null) {
			mapTypeOperation = initMapEnum(ApplicationConfig.masterKeyTypeOperation, TypeOperation.class);
		}
		return mapTypeOperation;
	}

	/**
	 * Initialise la map avec touts les fournisseurs de tous les logiciels presents dans FiDoR
	 *
	 * @return une map
	 */
	public Map<Long, String> getAllLibellesLogiciels() {
		try {
			final List<Logiciel> listeLogiciels = infraService.getLogicielsCertifiesPour(LogicielMetier.EMPACI);
			final Map<Long, String> map = new HashMap<>();
			if (listeLogiciels != null && !listeLogiciels.isEmpty()) {
				for (Logiciel logiciel : listeLogiciels) {
					map.put(logiciel.getId(), logiciel.getLibelleComplet());
				}
			}
			return map;
		}
		catch (Exception e) {
			LOGGER.error("Impossible de récupérer la liste des logiciels", e);
			return Collections.emptyMap();
		}
	}

	/**
	 * Initialise la map des différents états que peut prendre une demande de délai de déclaration
	 * @return une map
	 */
	public Map<EtatDelaiDocumentFiscal, String> getTypesEtatsDelaiDeclaration() {
		if (mapEtatDelaiDeclaration == null) {
			mapEtatDelaiDeclaration = initMapEnum(ApplicationConfig.masterKeyEtatDelaiDeclaration, EtatDelaiDocumentFiscal.class);
		}
		return mapEtatDelaiDeclaration;
	}

	/**
	 * Initialise la map des formes juridiques
	 *
	 * @return une map
	 */
	public Map<FormeJuridiqueEntreprise, String> getMapFormesJuridiquesEntreprise() {
		if (mapFormesJuridiquesEntreprise == null) {
			mapFormesJuridiquesEntreprise = initMapEnum(ApplicationConfig.masterKeyFormeJuridiqueEntreprise, FormeJuridiqueEntreprise.class);
		}
		return mapFormesJuridiquesEntreprise;
	}

	/**
	 * Initialise la map des formes juridiques qui correpondent aux modalités données
	 *
	 * @return une map
	 */
	public Map<FormeJuridiqueEntreprise, String> getMapFormesJuridiquesEntrepriseChoisies(Set<FormeJuridiqueEntreprise> formesJuridiques) {
		return getSpecificMapEnum(ApplicationConfig.masterKeyFormeJuridiqueEntreprise, formesJuridiques);
	}

	/**
	 * Initialise la map des catégories d'entreprises
	 *
	 * @return une map
	 */
	public Map<CategorieEntreprise, String> getMapCategoriesEntreprise() {
		if (mapCategorieEntreprise == null) {
			final Map<CategorieEntreprise, String> bruttoMap = initMapEnum(ApplicationConfig.masterKeyCategorieEntreprise, CategorieEntreprise.class, CategorieEntreprise.AUTRE);
			final Map<CategorieEntreprise, String> map = new LinkedHashMap<>(bruttoMap);
			final String enAttente = map.remove(CategorieEntreprise.INDET);
			map.put(null, "-----");                         // sera mappé comme un séparateur dans la liste déroulante (à cause de la clé "null", la valeur est ignorée...)
			map.put(CategorieEntreprise.INDET, enAttente);  // "en attente de détermination" en fin de liste après un séparateur, donc
			mapCategorieEntreprise = Collections.unmodifiableMap(map);
		}
		return mapCategorieEntreprise;
	}

	/**
	 * Renvoie une map d'états entreprise en fonction de la list de type d'états fournie.
	 * @param transitions Une liste de type d'état d'entreprise
	 * @return la map correspondante
	 */
	public Map<TypeEtatEntreprise, String> getMapForTypeEtatEntreprise(List<TypeEtatEntreprise> transitions) {
		return initMapEnum(ApplicationConfig.masterKeyEtatEntreprise, transitions.toArray(new TypeEtatEntreprise[0]));
	}

	/**
	 * @return la map des types de collectivité disponibles dans un allègement fiscal
	 */
	public Map<AllegementFiscal.TypeCollectivite, String> getTypesCollectiviteAllegement() {
		if (mapTypeCollectiviteAllegement == null) {
			mapTypeCollectiviteAllegement = initMapEnum(ApplicationConfig.masterKeyTypeCollectiviteAllegement, AllegementFiscal.TypeCollectivite.class);
		}
		return mapTypeCollectiviteAllegement;
	}

	/**
	 * @return la map des types d'impôt disponibles dans un allègement fiscal
	 */
	public Map<AllegementFiscal.TypeImpot, String> getTypesImpotAllegement() {
		if (mapTypeImpotAllegement == null) {
			mapTypeImpotAllegement = initMapEnum(ApplicationConfig.masterKeyTypeImpotAllegement, AllegementFiscal.TypeImpot.class);
		}
		return mapTypeImpotAllegement;
	}

	/**
	 * @return la map des types disponibles dans un allègement fiscal fédéral (IFD)
	 */
	public Map<AllegementFiscalConfederation.Type, String> getTypesIFDAllegement() {
		if (mapTypesIFDAllegement == null) {
			mapTypesIFDAllegement = initMapEnum(ApplicationConfig.masterKeyTypeIFDAllegement, AllegementFiscalConfederation.Type.class);
		}
		return mapTypesIFDAllegement;
	}

	/**
	 * @return la map des types disponibles dans un allègement fiscal communal ou cantonal (ICC)
	 */
	public Map<AllegementFiscalCantonCommune.Type, String> getTypesICCAllegement() {
		if (mapTypesICCAllegement == null) {
			mapTypesICCAllegement = initMapEnum(ApplicationConfig.masterKeyTypeICCAllegement, AllegementFiscalCantonCommune.Type.class);
		}
		return mapTypesICCAllegement;
	}

	/**
	 * @return la map des types disponibles pour les flags d'entreprise
	 */
	public Map<TypeFlagEntreprise, String> getTypesFlagEntreprise(GroupeFlagsEntreprise groupe) {
		if (mapTypesFlagEntreprise == null) {
			mapTypesFlagEntreprise = initMapEnum(ApplicationConfig.masterKeyTypeFlagEntreprise, TypeFlagEntreprise.class);
		}

		// il reste un petit filtre à faire, selon le groupe
		final Map<TypeFlagEntreprise, String> typesDuGroupe = new LinkedHashMap<>(mapTypesFlagEntreprise);
		typesDuGroupe.keySet().retainAll(TypeFlagEntreprise.ofGroupe(groupe));
		return typesDuGroupe;
	}

	/**
	 * @return la map des types disponibles pour les références pour délai
	 */
	public Map<ParametrePeriodeFiscalePM.ReferencePourDelai, String> getMapReferencesPourDelai() {
		if (mapReferencesPourDelai == null) {
			mapReferencesPourDelai = initMapEnum(ApplicationConfig.masterKeyReferencePourDelai, ParametrePeriodeFiscalePM.ReferencePourDelai.class);
		}
		return mapReferencesPourDelai;
	}

	/**
	 * @return la map des types disponibles pour les mandats
	 */
	public Map<TypeMandat, String> getTypesMandat() {
		if (mapTypesMandat == null) {
			mapTypesMandat = initMapEnum(ApplicationConfig.masterKeyTypeMandat, TypeMandat.class);
		}
		return mapTypesMandat;
	}

	/**
	 * @return la map des types de document fiscal émettables manuellement
	 */
	public Map<TypeAutreDocumentFiscalEmettableManuellement, String> getTypesAutreDocumentFiscalEmettableManuellement() {
		if (mapTypesAutreDocumentFiscalEmettableManuellement == null) {
			mapTypesAutreDocumentFiscalEmettableManuellement = initMapEnum(ApplicationConfig.maskerKeyTypeAutreDocumentFiscalEmettableManuellement, TypeAutreDocumentFiscalEmettableManuellement.class);
		}
		return mapTypesAutreDocumentFiscalEmettableManuellement;
	}

	/**
	 * @return la map des types de lettres de bienvenue
	 */
	public Map<TypeLettreBienvenue, String> getTypesLettreBienvenue() {
		if (mapTypesLettreBienvenue == null) {
			mapTypesLettreBienvenue = initMapEnum(ApplicationConfig.maskerKeyTypeLettreBienvenue, TypeLettreBienvenue.class);
		}
		return mapTypesLettreBienvenue;
	}

	public Map<TypeAnnonce, String> getTypeAnnonce() {
		if (mapTypeAnnonce == null) {
			mapTypeAnnonce = initMapEnum("option.type.annonce.", TypeAnnonce.class);
		}
		return mapTypeAnnonce;
	}

	public Map<StatutAnnonce, String> getStatutAnnonce() {
		if (mapStatutAnnonce == null) {
			mapStatutAnnonce = initMapEnum("option.statut.annonce.", StatutAnnonce.class);
		}
		return mapStatutAnnonce;
	}

	public Map<EtatEvenementRF, String> getEtatEvenementRF() {
		if (mapEtatEvenementRF == null) {
			mapEtatEvenementRF = initMapEnum("option.rf.etat.evenement.", EtatEvenementRF.class);
		}
		return mapEtatEvenementRF;
	}
}
