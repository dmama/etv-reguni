package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.LogicielMetier;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRechercheForFiscal;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRechercheLocalitePays;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.FormeJuridique;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.NatureJuridique;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeDroitAcces;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeOperation;

/**
 * Cette classe expose les différents enums sours forme de map enum->description.
 */
public class TiersMapHelper extends CommonMapHelper {

	private static final Logger LOGGER = Logger.getLogger(TiersMapHelper.class);

	private Map<FormeJuridique, String> mapFormeJuridique;
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
	private Map<ModeImposition, String> mapModeImposition;
	private Map<CategorieImpotSource, String> mapCategorieImpotSource;
	private Map<PeriodiciteDecompte, String> mapPeriodiciteDecompte;
	private Map<ModeCommunication, String> mapModeCommunication;
	private Map<TexteCasePostale, String> mapTexteCasePostale;
	private Map<TypeEvenementCivil, String> mapTypeEvenementCivil;
	private Map<EtatEvenementCivil, String> mapStatusEvenementCivil;
	private Map<TypeRapportEntreTiersWeb, String> mapTypeRapportEntreTiers;
	private Map<EtatEvenementCivil, String> mapEtatsEvenementCivil;
	private Map<TypeEtatDeclaration, String> mapTypeEtatDeclaration;
	private Map<EtatCivil, String> mapEtatsCivil;
	private Map<TypeAdresseTiers, String> mapTypeAdresse;
	private Map<TypeAdresseTiers, String> mapTypeAdresseFiscale;
	private Map<TypeActivite, String> mapTypeActivite;
	private Map<TypeDocument, String> mapTypesDeclarationImpot;
	private Map<TypeDocument, String> mapTypesDeclarationImpotOrdinaire;
	private Map<TypeDocument, String> mapTypesDeclarationImpotPourParam;
	private Map<TypeAdresseRetour, String> mapTypesAdresseRetour;
	private Map<TarifImpotSource, String> mapTarifsImpotSource;
	private Map<PeriodeDecompte, String> mapPeriodeDecompte;
	private Map<TypeDroitAcces, String> mapDroitAcces;
	private Map<TypeOperation, String> mapTypeOperation;

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
	public Map<TiersCriteriaView.TypeRecherche, String> getMapTypeRechercheNom() {
		if (mapTypeRechercheNom == null) {
			mapTypeRechercheNom = initMapEnum(ApplicationConfig.masterKeyTypeRechercheNom, TiersCriteriaView.TypeRecherche.class);
		}
		return mapTypeRechercheNom;
	}

	/**
	 * Initialise la map des types de recherche par localite / pays
	 *
	 * @return une map
	 */
	public Map<TiersCriteriaView.TypeRechercheLocalitePays, String> getMapTypeRechercheLocalitePays() {
		if (mapTypeRechercheLocalitePays == null) {
			mapTypeRechercheLocalitePays = initMapEnum(ApplicationConfig.masterKeyTypeRechercheLocalitePays,
					TiersCriteriaView.TypeRechercheLocalitePays.class);
		}
		return mapTypeRechercheLocalitePays;
	}

	/**
	 * Initialise la map des types de recherche par for
	 *
	 * @return une map
	 */
	public Map<TiersCriteriaView.TypeRechercheForFiscal, String> getMapTypeRechercheForFiscal() {
		if (mapTypeRechercheForFiscal == null) {
			mapTypeRechercheForFiscal = initMapEnum(ApplicationConfig.masterKeyTypeRechercheForFiscal,
					TiersCriteriaView.TypeRechercheForFiscal.class);
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
	 *
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
	 * Initialise la map des periodicites decompte
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
	public Map<TypeRapportEntreTiersWeb, String> getMapTypeRapportEntreTiers() {
		if (mapTypeRapportEntreTiers == null) {
			mapTypeRapportEntreTiers = initMapEnum(ApplicationConfig.masterKeyTypeRapportEntreTiers, TypeRapportEntreTiersWeb.class);
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
	 * Initialise la map de types d'etat de document
	 *
	 * @return une map
	 */
	public Map<TypeEtatDeclaration, String> getMapTypeEtatDeclaration() {
		if (mapTypeEtatDeclaration == null) {
			mapTypeEtatDeclaration = initMapEnum(ApplicationConfig.masterKeyTypeEtatDocument, TypeEtatDeclaration.class);
		}
		return mapTypeEtatDeclaration;
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
	 * Initialise la map des types d'activite
	 *
	 * @return une map
	 */
	public Map<TypeActivite, String> getMapTypeActivite() {
		if (mapTypeActivite == null) {
			mapTypeActivite = initMapEnum(ApplicationConfig.masterKeyTypeActivite, TypeActivite.class);
		}
		return mapTypeActivite;
	}

	/**
	 * Initialise la map des types de declaration d'impot pour l'écran de paramétrage
	 *
	 * @return une map
	 */
	public Map<TypeDocument, String> getTypesDeclarationImpotPourParam() {

		if (mapTypesDeclarationImpotPourParam == null) {
			mapTypesDeclarationImpotPourParam = initMapEnum(ApplicationConfig.masterKeyTypeDeclarationImpot, TypeDocument.class);
		}
		return mapTypesDeclarationImpotPourParam;
	}

	/**
	 * Initialise la map des types de declaration d'impot pour l'écran d'édition de la DI
	 *
	 * @return une map
	 */
	public Map<TypeDocument, String> getTypesDeclarationImpot() {


		if (mapTypesDeclarationImpot == null) {
			mapTypesDeclarationImpot =
					initMapEnum(ApplicationConfig.masterKeyTypeDeclarationImpot, TypeDocument.class, TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		}
		return mapTypesDeclarationImpot;
	}

	/**
	 * Initialise la map des types de declarations d'impôt ordinaires pour l'écran d'édition de la DI (quittancement)
	 *
	 * @return une map
	 */
	public Map<TypeDocument, String> getTypesDeclarationsImpotOrdinaires() {

		if (mapTypesDeclarationImpotOrdinaire == null) {
			final List<TypeDocument> typesIgnores = new ArrayList<TypeDocument>();
			for (TypeDocument type : TypeDocument.values()) {
				// doivent être ignorées la version batch de la déclaration complète et toutes les déclarations non-ordinaires
				if (type == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH || !type.isOrdinaire()) {
					typesIgnores.add(type);
				}
			}
			mapTypesDeclarationImpotOrdinaire = initMapEnum(ApplicationConfig.masterKeyTypeDeclarationImpot, TypeDocument.class, typesIgnores.toArray(new TypeDocument[typesIgnores.size()]));
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
			mapPeriodeDecompte = new TreeMap<PeriodeDecompte, String>();
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
			final List<Logiciel> listeLogiciels = infraService.getLogicielsPour(LogicielMetier.EMPACI);
			final HashMap<Long, String> map = new HashMap<Long, String>();
			if (listeLogiciels != null && !listeLogiciels.isEmpty()) {
				for (Logiciel logiciel : listeLogiciels) {
					map.put(logiciel.getId(), logiciel.getFournisseur());
				}
			}
			return map;
		}
		catch (Exception e) {
			LOGGER.error("Impossible de récupérer la liste des logiciels", e);
			return Collections.emptyMap();
		}
	}
}
