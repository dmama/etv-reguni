package ch.vd.uniregctb.interfaces.service.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilListImpl;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.RelationVersIndividu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.impl.RelationVersIndividuImpl;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockBatiment;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockNationalite;
import ch.vd.uniregctb.interfaces.model.mock.MockOrigine;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockPermis;
import ch.vd.uniregctb.interfaces.model.mock.MockPermisList;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.model.mock.MockTutelle;
import ch.vd.uniregctb.interfaces.model.mock.MockTuteurGeneral;
import ch.vd.uniregctb.interfaces.service.ServiceCivilServiceBase;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeTutelle;

/**
 * Mock du Service Civil.
 * <p>
 * Pour utiliser cette classe, le plus simple est d'instancier une classe anonyme et d'implémenter la méthode init() de manière à charger
 * les données de test voulues.
 * <p>
 * Par exemple:
 *
 * <pre>
 *  ServiceCivil serviceCivil = new MockServiceCivil() {
 *  protected void init() {
 *  MockIndividu pierre = addIndividu(...);
 *  addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, ...);
 *  addAdresse(pierre, TypeAdresseCivil.COURRIER, ...);
 *  ...
 *  }
 *  };
 * </pre>
 */
@SuppressWarnings({"JavaDoc"})
public abstract class MockServiceCivil extends ServiceCivilServiceBase {


	/**
	 * Map des individus par numéro.
	 */
	private final Map<Long, MockIndividu> individusMap = new HashMap<Long, MockIndividu>();

	/**
	 * Constructeur qui permet d'injecter le service infrastructure ; appelle init()
	 * pour l'initialisation des données
	 * @param infraService
	 */
	public MockServiceCivil(ServiceInfrastructureService infraService) {
		setInfraService(infraService);
		init();
	}

	/**
	 * Constructeur par défaut qui appelle init pour initialiser le mock
	 * (si le service infrastructure est nécessaire aux méthodes testées,
	 * il faut utiliser {@link #MockServiceCivil(ServiceInfrastructureService)})
	 */
	public MockServiceCivil() {
		this(null);
	}

	/**
	 * Cette méthode permet d'initialise le mock en fonction des données voulues.
	 */
	protected abstract void init();

	protected void setNationalite(MockIndividu ind, RegDate debut, @Nullable RegDate fin, MockPays pays) {
		final ArrayList<Nationalite> nationalites = new ArrayList<Nationalite>();
		final MockNationalite nati = new MockNationalite();
		nati.setDateDebutValidite(debut);
		nati.setDateFinValidite(fin);
		nati.setPays(pays);
		nationalites.add(nati);
		ind.setNationalites(nationalites);
	}

	/**
	 * Ajoute un invidu à la map des individus.
	 * @param numero numéro d'individu à utiliser
	 * @param dateNaissance date de naissance du nouvel individu
	 * @param nom nom (de famille) du nouvel individu
	 * @param prenom prénom du nouvel individu
	 * @param isMasculin <code>true</code> pour un homme, <code>false</code> pour une femme
	 * @return un {@link MockIndividu}
	 */
	protected MockIndividu addIndividu(long numero, @Nullable RegDate dateNaissance, String nom, String prenom, boolean isMasculin) {
		final MockIndividu individu = new MockIndividu();
		individu.setNoTechnique(numero);
		individu.setDateNaissance(dateNaissance);
		individu.setSexeMasculin(isMasculin);
		individu.setPrenom(prenom);
		individu.setNom(nom);

		// Etats civils
		final EtatCivilListImpl etatsCivils = new EtatCivilListImpl();
		etatsCivils.add(creeEtatCivil(dateNaissance, TypeEtatCivil.CELIBATAIRE));
		individu.setEtatsCivils(etatsCivils);

		// Adresses
		final List<Adresse> sdresses = new ArrayList<Adresse>();
		individu.setAdresses(sdresses);

		// Enfants
		final List<RelationVersIndividu> enfants = new ArrayList<RelationVersIndividu>();
		individu.setEnfants(enfants);

		// Adoptions et reconnaissances
		final List<AdoptionReconnaissance> adoptions = new ArrayList<AdoptionReconnaissance>();
		individu.setAdoptionsReconnaissances(adoptions);

		individu.setConjoints(new ArrayList<RelationVersIndividu>());

		// Permis
		individu.setPermis(new MockPermisList(numero));

		add(individu);
		return individu;
	}

	protected EtatCivil addEtatCivil(MockIndividu individu, RegDate dateDebut, TypeEtatCivil type) {
		final Collection<EtatCivil> etats = individu.getEtatsCivils();
		final EtatCivil etat = creeEtatCivil(dateDebut, type);
		etats.add(etat);
		return etat;
	}

	protected Individu addFieldsIndividu(MockIndividu individu, String nouveauNoAVS, String ancienNoAVS, String nomNaissance) {
		individu.setNouveauNoAVS(nouveauNoAVS);
		individu.setNoAVS11(ancienNoAVS);
		individu.setNomNaissance(nomNaissance);
		return individu;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié.
	 */
	protected MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, String rue, String numeroMaison, Integer numeroPostal, Localite localite, CasePostale casePostale,
									RegDate debutValidite, RegDate finValidite) {

		final MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(type);
		adresse.setLocalite(localite.getNomAbregeMinuscule());
		adresse.setRue(rue);
		adresse.setNumero(numeroMaison);
		adresse.setCasePostale(casePostale);
		adresse.setNumeroPostal(numeroPostal == null ? null : String.valueOf(numeroPostal));
		adresse.setCommuneAdresse(localite.getCommuneLocalite());

		adresse.setPays(MockPays.Suisse);
		final Integer complementNPA = localite.getComplementNPA();
		adresse.setNumeroPostalComplementaire(complementNPA == null ? null : complementNPA.toString());
		adresse.setNumeroOrdrePostal(localite.getNoOrdre());

		adresse.setDateDebutValidite(debutValidite);
		adresse.setDateFinValidite(finValidite);
		add(individu, adresse);

		return adresse;
	}

	/**
	 * Ajoute une adresse étrangère pour l'individu spécifié
	 */
	protected MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, @Nullable String rue, @Nullable String numeroMaison, @Nullable Integer numeroPostal, @Nullable CasePostale casePostale,
	                             @Nullable String localite, @NotNull Pays pays, RegDate debutValidite, @Nullable RegDate finValidite) {

		Assert.isFalse(pays.isSuisse());

		final MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(type);
		adresse.setLocalite(localite);
		adresse.setRue(rue);
		adresse.setNumero(numeroMaison);
		adresse.setCasePostale(casePostale);
		adresse.setNumeroPostal(numeroPostal == null ? null : String.valueOf(numeroPostal));
		adresse.setCommuneAdresse(null);
		adresse.setPays(pays);

		adresse.setDateDebutValidite(debutValidite);
		adresse.setDateFinValidite(finValidite);
		add(individu, adresse);

		return adresse;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié (à partir d'une rue).
	 */
	protected MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, MockRue rue, @Nullable CasePostale casePostale, @Nullable RegDate debutValidite, @Nullable RegDate finValidite) {
		final MockAdresse adresse = newAdresse(type, rue, casePostale, debutValidite, finValidite);
		add(individu, adresse);
		return adresse;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié (à partir d'un bâtiment).
	 */
	protected MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, MockBatiment batiment, @Nullable CasePostale casePostale, RegDate debutValidite, @Nullable RegDate finValidite) {
		final MockAdresse adresse = newAdresse(type, batiment, casePostale, debutValidite, finValidite);
		add(individu, adresse);
		return adresse;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié (à partir d'une localité).
	 */
	protected MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, @Nullable CasePostale casePostale, MockLocalite localite, RegDate debutValidite, @Nullable RegDate finValidite) {
		final MockAdresse adresse = newAdresse(type, casePostale, localite, debutValidite, finValidite);
		add(individu, adresse);
		return adresse;
	}

	/**
	 * Crée une nouvelle adresse à partie d'une rue.
	 */
	public static MockAdresse newAdresse(TypeAdresseCivil type, MockRue rue, CasePostale casePostale, RegDate debutValidite, RegDate finValidite) {
		Assert.notNull(rue);

		final MockLocalite localite = rue.getLocalite();
		Assert.notNull(localite);

		final MockAdresse adresse = (MockAdresse) newAdresse(type, casePostale, localite, debutValidite, finValidite);
		adresse.setRue(rue.getDesignationCourrier());
		adresse.setNumeroRue(rue.getNoRue());
		return adresse;
	}

	/**
	 * Crée une nouvelle adresse à partie d'un bâtiment.
	 */
	public static MockAdresse newAdresse(TypeAdresseCivil type, MockBatiment batiment, CasePostale casePostale, RegDate debutValidite, RegDate finValidite) {
		Assert.notNull(batiment);

		MockRue rue = batiment.getRue();

		final MockAdresse adresse = (MockAdresse) newAdresse(type, rue, casePostale, debutValidite, finValidite);
		adresse.setEgid(batiment.getEgid());
		return adresse;
	}

	/**
	 * Crée une nouvelle adresse à partie d'une localité.
	 */
	public static MockAdresse newAdresse(TypeAdresseCivil type, CasePostale casePostale, MockLocalite localite, RegDate debutValidite, RegDate finValidite) {
		Assert.notNull(localite);

		final MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(type);

		// localité
		adresse.setCasePostale(casePostale);
		adresse.setLocalite(localite.getNomAbregeMinuscule());
		adresse.setNumeroPostal(localite.getNPA().toString());
		adresse.setCommuneAdresse(localite.getCommuneLocalite());
		adresse.setPays(MockPays.Suisse);
		final Integer complementNPA = localite.getComplementNPA();
		adresse.setNumeroPostalComplementaire(complementNPA == null ? null : complementNPA.toString());
		adresse.setNumeroOrdrePostal(localite.getNoOrdre());

		// validité
		adresse.setDateDebutValidite(debutValidite);
		adresse.setDateFinValidite(finValidite);
		return adresse;
	}

	/**
	 * Crée une nouvelle adresse à l'étranger
	 */
	public static MockAdresse newAdresse(TypeAdresseCivil type, String rue, @Nullable CasePostale casePostale, String npaLocalite, MockPays pays, RegDate debutValidite, @Nullable RegDate finValidite) {
		Assert.notNull(pays);
		Assert.isFalse(pays.getNoOFS() == ServiceInfrastructureService.noOfsSuisse, "Pour la Suisse, il faut utiliser une autre méthode newAdresse");

		final MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(type);

		adresse.setCasePostale(casePostale);
		adresse.setCommuneAdresse(null);
		adresse.setPays(pays);
		adresse.setRue(rue);
		adresse.setLieu(npaLocalite);
		adresse.setDateDebutValidite(debutValidite);
		adresse.setDateFinValidite(finValidite);
		return adresse;
	}

	/**
	 * Unit les deux individus par le mariage. Si les individus sont du même sexe, l'état-civil est PACS; et dans le cas normal,
	 * l'état-civil est MARIE.
	 */
	public static void marieIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateMariage) {

		final TypeEtatCivil etatCivil = (individu.isSexeMasculin() == conjoint.isSexeMasculin() ? TypeEtatCivil.PACS : TypeEtatCivil.MARIE);

		final List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(dateMariage, etatCivil);
		etatsCivilIndividu.add(etatCivilIndividu);
		addRelationConjoint(individu, conjoint, dateMariage);

		final List<EtatCivil> etatsCivilConjoint = conjoint.getEtatsCivils();
		final EtatCivil etatCivilConjoint = creeEtatCivil(dateMariage, etatCivil);
		etatsCivilConjoint.add(etatCivilConjoint);
		addRelationConjoint(conjoint, individu, dateMariage);

		/* les maries peuvent s'embrasser */
	}
	
	public static void addRelationConjoint(MockIndividu individu, MockIndividu nouveauConjoint, RegDate dateMariage) {
		individu.getConjoints().add(new RelationVersIndividuImpl(nouveauConjoint.getNoTechnique(), dateMariage, null));
	}
	
	public static void reconcilieIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateReconciliation) {
		// pour ce que l'on fait ici, c'est équivalent à un re-mariage, non ?
		marieIndividus(individu, conjoint, dateReconciliation);
	}

	/**
	 * Marie un individu, mais seul.
	 */
	public static void marieIndividu(MockIndividu individu, RegDate dateMariage) {
		final List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(dateMariage, TypeEtatCivil.MARIE);
		etatsCivilIndividu.add(etatCivilIndividu);
	}

	public static void separeIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateSeparation) {
		separeIndividu(individu, dateSeparation);
		separeIndividu(conjoint, dateSeparation);
	}

	public static void separeIndividu(MockIndividu individu, RegDate dateSeparation) {
		final List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();

		final EtatCivil etatCivilIndividu = creeEtatCivil(dateSeparation, TypeEtatCivil.SEPARE);
		etatsCivilIndividu.add(etatCivilIndividu);

		// JDE, 20.02.2012 : le pseudo état-civil "séparé" ne met pas fin aux relations civiles, puisque le couple est toujours marié civilement
//		final List<RelationVersIndividu> conjoints = individu.getConjoints();
//		final RelationVersIndividu relation = DateRangeHelper.rangeAt(conjoints, dateSeparation);
//		if (relation != null) {
//			((RelationVersIndividuImpl)relation).setDateFin(dateSeparation);
//		}
	}

	public static void divorceIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateDivorce) {
		divorceIndividu(individu, dateDivorce);
		divorceIndividu(conjoint, dateDivorce);
	}

	public static void divorceIndividu(MockIndividu individu, RegDate dateDivorce) {
		final List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(dateDivorce, TypeEtatCivil.DIVORCE);
		etatsCivilIndividu.add(etatCivilIndividu);

		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		final RelationVersIndividu relation = DateRangeHelper.rangeAt(conjoints, dateDivorce);
		if (relation != null) {
			((RelationVersIndividuImpl)relation).setDateFin(dateDivorce);
		}
	}

	/**
	 * @param individu individu auquel on doit rajouté l'état civil correspondant à son veuvage
	 * @param dateVeuvage date de l'obtention du nouvel état civil
	 * @param partenariat <code>true</code> s'il s'agit d'un partenariat enregistré, <code>false</code> s'il s'agit d'un mariage
	 */
	public static void veuvifieIndividu(MockIndividu individu, RegDate dateVeuvage, boolean partenariat) {
		final List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(dateVeuvage, partenariat ? TypeEtatCivil.PACS_VEUF : TypeEtatCivil.VEUF);
		etatsCivilIndividu.add(etatCivilIndividu);

		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		final RelationVersIndividu relation = DateRangeHelper.rangeAt(conjoints, dateVeuvage);
		if (relation != null) {
			((RelationVersIndividuImpl)relation).setDateFin(dateVeuvage);
		}
	}

	public static void annuleMariage(MockIndividu individu) {

		final ch.vd.uniregctb.interfaces.model.EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
		individu.getEtatsCivils().remove(etatCivilIndividu);

		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		if (conjoints != null && !conjoints.isEmpty()) {
			final RelationVersIndividu last = conjoints.get(conjoints.size() - 1);
			throw new ProgrammingException(
					"L'individu n°" + individu.getNoTechnique() + " est en ménage avec le conjoint n°" + last.getNumeroAutreIndividu() + ", veuillez utiliser la méthode avec deux arguments !");
		}
	}

	public static void annuleMariage(MockIndividu individu, MockIndividu conjoint) {

		final ch.vd.uniregctb.interfaces.model.EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
		individu.getEtatsCivils().remove(etatCivilIndividu);
		annuleDernierConjoint(individu, conjoint.getNoTechnique());


		final ch.vd.uniregctb.interfaces.model.EtatCivil etatCivilConjoint = conjoint.getEtatCivilCourant();
		conjoint.getEtatsCivils().remove(etatCivilConjoint);
		annuleDernierConjoint(conjoint, individu.getNoTechnique());
	}

	private static void annuleDernierConjoint(MockIndividu individu, long noIndConjoint) {

		final List<RelationVersIndividu> relations = individu.getConjoints();
		if (relations == null || relations.isEmpty()) {
			return;
		}

		final RelationVersIndividu dernier = relations.get(relations.size() - 1);
		if (noIndConjoint != dernier.getNumeroAutreIndividu()) {
			throw new ProgrammingException(
					"L'individu n°" + individu.getNoTechnique() + " est en ménage avec une autre conjoint (n°" + dernier.getNumeroAutreIndividu() + ") que le conjoint spécifié (n°" +
							noIndConjoint + " !");
		}

		relations.remove(relations.size() - 1);

		if (!relations.isEmpty()) {
			final RelationVersIndividuImpl nouveauDernier = (RelationVersIndividuImpl) relations.get(relations.size()-1);
			nouveauDernier.setDateFin(null);
		}
	}

	protected Permis addPermis(MockIndividu individu, TypePermis type, RegDate debut, @Nullable RegDate fin, boolean permisAnnule) {
		final MockPermis permis = new MockPermis();
		permis.setTypePermis(type);
		permis.setDateDebutValidite(debut);
		permis.setDateFinValidite(fin);
		if (permisAnnule) {
			permis.setDateAnnulation(RegDate.get());
		}
		if (individu.getPermis() == null) {
			individu.setPermis(new MockPermisList(individu.getNoTechnique()));
		}
		individu.getPermis().add(permis);
		return permis;
	}

	protected void addOrigine(MockIndividu individu, Commune commune) {
		addOrigine(individu, commune.getNomMinuscule());
	}

	protected void addOrigine(MockIndividu individu, String nomLieu) {
		final MockOrigine origine = new MockOrigine();
		origine.setNomLieu(nomLieu);
		Collection<Origine> origines = individu.getOrigines();
		if (origines == null) {
			origines = new ArrayList<Origine>();
			individu.setOrigines(origines);
		}
		origines.add(origine);
	}

	protected void addNationalite(MockIndividu individu, Pays pays, RegDate debut, @Nullable RegDate fin) {
		final MockNationalite nationalite = new MockNationalite();
		nationalite.setDateDebutValidite(debut);
		nationalite.setDateFinValidite(fin);
		nationalite.setPays(pays);
		List<Nationalite> nationalites = individu.getNationalites();
		if (nationalites == null) {
			nationalites = new ArrayList<Nationalite>();
			individu.setNationalites(nationalites);
		}
		nationalites.add(nationalite);
	}

	private static String merge(String... bits) {
		final StringBuilder b = new StringBuilder();
		for (String bit : bits) {
			if (StringUtils.isNotBlank(bit)) {
				b.append(StringUtils.trimToEmpty(bit));
			}
		}
		return b.toString();
	}

	protected void setTutelle(MockIndividu pupille, MockIndividu tuteur, MockCollectiviteAdministrative autoriteTutelaire, TypeTutelle type) {
		final MockTutelle tutelle = new MockTutelle();
		tutelle.setLibelleMotif("BlaBla");
		tutelle.setTuteur(tuteur);
		if (autoriteTutelaire != null) {
			tutelle.setNomAutoriteTutelaire(merge(autoriteTutelaire.getNomComplet1(), autoriteTutelaire.getNomComplet2(), autoriteTutelaire.getNomComplet3()));
			tutelle.setNumeroCollectiviteAutoriteTutelaire((long) autoriteTutelaire.getNoColAdm());
		}
		else {
			tutelle.setNomAutoriteTutelaire(null);
			tutelle.setNumeroCollectiviteAutoriteTutelaire(null);
		}
		tutelle.setTuteurGeneral(null);
		tutelle.setTypeTutelle(type);

		pupille.setTutelle(tutelle);
	}

	protected void setTutelle(MockIndividu pupille, MockCollectiviteAdministrative autoriteTutelaire, TypeTutelle type) {
		final MockTuteurGeneral tuteurGeneral = new MockTuteurGeneral();
		tuteurGeneral.setNomContact("Ouilles");
		tuteurGeneral.setNomOffice("Office du Tuteur General de Vaud");
		tuteurGeneral.setNoTelephoneContact("+41123224568");
		tuteurGeneral.setPrenomContact("Jacques");

		final MockTutelle tutelle = new MockTutelle();
		tutelle.setLibelleMotif("BlaBla");
		tutelle.setTuteur(null);
		if (autoriteTutelaire != null) {
			tutelle.setNomAutoriteTutelaire(merge(autoriteTutelaire.getNomComplet1(), autoriteTutelaire.getNomComplet2(), autoriteTutelaire.getNomComplet3()));
			tutelle.setNumeroCollectiviteAutoriteTutelaire((long) autoriteTutelaire.getNoColAdm());
		}
		else {
			tutelle.setNomAutoriteTutelaire(null);
			tutelle.setNumeroCollectiviteAutoriteTutelaire(null);
		}
		tutelle.setTuteurGeneral(tuteurGeneral);
		tutelle.setTypeTutelle(type);

		pupille.setTutelle(tutelle);
	}

	/**
	 * Crée un état civil.
	 *
	 * @param date
	 *            la date de debut de validité de l'état civil
	 * @param typeEtatCivil
	 *            le type
	 *
	 */
	private static EtatCivil creeEtatCivil(RegDate date, TypeEtatCivil typeEtatCivil) {
		final MockEtatCivil etatCivil = new MockEtatCivil();
		etatCivil.setDateDebut(date);
		etatCivil.setTypeEtatCivil(typeEtatCivil);
		return etatCivil;
	}

	/**
	 * Ajoute un individu dans le mock
	 */
	private void add(MockIndividu individu) {
		Assert.notNull(individu);
		final Long numero = individu.getNoTechnique();
		Assert.notNull(numero);
		Assert.isNull(individusMap.get(numero));
		individusMap.put(numero, individu);
	}

	/**
	 * Ajoute une adresse pour individu dans le mock
	 */
	protected void add(MockIndividu individu, Adresse adresse) {
		Assert.notNull(individu);

		final Long numero = individu.getNoTechnique();
		Assert.notNull(numero);

		Collection<Adresse> list = individu.getAdresses();
		if (list == null) {
			list = new ArrayList<Adresse>();
			individu.setAdresses(list);
		}
		list.add(adresse);
	}

	public MockIndividu getIndividu(Long numeroIndividu) {
		return individusMap.get(numeroIndividu);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.HostCivilService#getAdressesActives(java.lang.Long, java.util.Date)
	 */
	public Collection<Adresse> getAdressesActives(Long numeroIndividu, RegDate date) {

		final Individu individu = individusMap.get(numeroIndividu);
		if (individu == null) {
			return null;
		}

		final Collection<Adresse> adressesActives = new ArrayList<Adresse>();
		final Collection<Adresse> adresses = individu.getAdresses();
		if (adresses != null) {
			if (date == null) {
				adressesActives.addAll(adresses);
			}
			else {
				for (Adresse adresse : adresses) {
					if (adresse.isValidAt(date)) {
						adressesActives.add(adresse);
					}
				}
			}
		}

		return adressesActives;
	}

	@Override
	public Individu getIndividu(long noIndividu, RegDate date, AttributeIndividu... parties) {
		final MockIndividu individu = getIndividu(noIndividu);
		if (individu != null && !individu.isNonHabitantNonRenvoye()) {
			// on fait la copie avec les parts demandées seulements
			final Set<AttributeIndividu> parts;
			if (parties == null) {
				parts = Collections.emptySet();
			}
			else {
				parts = new HashSet<AttributeIndividu>(Arrays.asList(parties));
			}
			return new MockIndividu(individu, parts, date);
		}
		else {
			return null;
		}
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {
		final List<Individu> individus = new ArrayList<Individu>(nosIndividus.size());
		for (Long noIndividu : nosIndividus) {
			final Individu individu = getIndividu(noIndividu, date, parties);
			if (individu != null) {
				individus.add(individu);
			}
		}
		return individus;
	}

	@Override
	public boolean isWarmable() {
		return false;
	}

	@Override
	public void setIndividuLogger(boolean value) {
		// rien à faire
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void onIndividuChange(long numero) {
		// rien à faire
	}
}
