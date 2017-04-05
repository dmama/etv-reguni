package ch.vd.unireg.interfaces.civil.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.data.PermisListImpl;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividuImpl;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.data.TypeRelationVersIndividu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypePermis;

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
public abstract class MockServiceCivil implements ServiceCivilRaw {

	/**
	 * Map des individus par numéro
	 */
	private final Map<Long, MockIndividu> individusMap = new HashMap<>();

	/**
	 * Ensemble des numéros d'individu pour lesquels l'appel à {@link #getIndividu(Long)} ou {@link #getIndividu(long, ch.vd.unireg.interfaces.civil.data.AttributeIndividu...)} renvoie une exception
	 * (individus dits "minés")
	 */
	private final Set<Long> explodingIndividus = new HashSet<>();

	/**
	 * Map des données d'événements civils par identifiant d'événement
	 */
	private final Map<Long, IndividuApresEvenement> evenementsMap = new HashMap<>();

	/**
	 * Constructeur qui permet d'injecter le service infrastructure ; appelle init()
	 * pour l'initialisation des données
	 */
	public MockServiceCivil() {
		init();
	}

	/**
	 * Cette méthode permet d'initialise le mock en fonction des données voulues.
	 */
	protected abstract void init();

	/**
	 * Cette méthode permet de modifier les données du service civil après sa création.
	 */
	public void step1() {
	}

	/**
	 * Ajoute un invidu à la map des individus.
	 *
	 * @param numero        numéro d'individu à utiliser
	 * @param dateNaissance date de naissance du nouvel individu
	 * @param nom           nom (de famille) du nouvel individu
	 * @param prenom        prénom du nouvel individu
	 * @param sexe          sexe de l'individu
	 * @return un {@link MockIndividu}
	 */
	protected MockIndividu addIndividu(long numero, @Nullable RegDate dateNaissance, String nom, String prenom, @Nullable Sexe sexe) {
		final MockIndividu individu = createIndividu(numero, dateNaissance, nom, prenom, sexe);
		add(individu);
		return individu;
	}

	/**
	 * Ajoute un invidu à la map des individus.
	 * <p/><b>Préférer l'utilisation de la méthode homonyme qui n'assume pas que le sexe est connu</b> : {@link #addIndividu(long, ch.vd.registre.base.date.RegDate, String, String, ch.vd.uniregctb.type.Sexe)}
	 *
	 * @param numero        numéro d'individu à utiliser
	 * @param dateNaissance date de naissance du nouvel individu
	 * @param nom           nom (de famille) du nouvel individu
	 * @param prenom        prénom du nouvel individu
	 * @param isMasculin    <code>true</code> pour un homme, <code>false</code> pour une femme
	 * @return un {@link MockIndividu}
	 */
	protected MockIndividu addIndividu(long numero, @Nullable RegDate dateNaissance, String nom, String prenom, boolean isMasculin) {
		final MockIndividu individu = createIndividu(numero, dateNaissance, nom, prenom, isMasculin);
		add(individu);
		return individu;
	}

	/**
	 * Ajoute un numéro individu dans la liste des parias qui explosent dès qu'on essaie de les toucher...
	 * (il y en a dans le vrai service civil, autant les utiliser ici aussi...)
	 * @param numero le numéro d'individu miné
	 */
	protected void addIndividuMine(long numero) {
		explodingIndividus.add(numero);
	}

	/**
	 * Crée un invidu (sans l'ajouter à la map des individus).
	 *
	 * @param numero        numéro d'individu à utiliser
	 * @param dateNaissance date de naissance du nouvel individu
	 * @param nom           nom (de famille) du nouvel individu
	 * @param prenom        prénom du nouvel individu
	 * @param sexe          sexe de l'individu
	 * @return un {@link MockIndividu}
	 */
	protected MockIndividu createIndividu(long numero, @Nullable RegDate dateNaissance, String nom, String prenom, @Nullable Sexe sexe) {
		final MockIndividu individu = new MockIndividu();
		individu.setNoTechnique(numero);
		individu.setDateNaissance(dateNaissance);
		individu.setSexe(sexe);
		individu.setPrenomUsuel(prenom);
		individu.setNom(nom);

		// Etats civils
		final MockEtatCivilList etatsCivils = new MockEtatCivilList();
		etatsCivils.add(new MockEtatCivil(dateNaissance, TypeEtatCivil.CELIBATAIRE));
		individu.setEtatsCivils(etatsCivils);

		// Conjoints
		individu.setConjoints(new ArrayList<>());

		// Parents
		individu.setParents(new ArrayList<>());

		// Permis
		individu.setPermis(new PermisListImpl());

		return individu;
	}

	/**
	 * Crée un invidu (sans l'ajouter à la map des individus).
	 * <p/><b>Préférer l'utilisation de la méthode homonyme qui n'assume pas que le sexe est connu</b> : {@link #createIndividu(long, ch.vd.registre.base.date.RegDate, String, String, ch.vd.uniregctb.type.Sexe)}
	 *
	 * @param numero        numéro d'individu à utiliser
	 * @param dateNaissance date de naissance du nouvel individu
	 * @param nom           nom (de famille) du nouvel individu
	 * @param prenom        prénom du nouvel individu
	 * @param isMasculin    <code>true</code> pour un homme, <code>false</code> pour une femme
	 * @return un {@link MockIndividu}
	 */
	protected MockIndividu createIndividu(long numero, @Nullable RegDate dateNaissance, String nom, String prenom, boolean isMasculin) {
		return createIndividu(numero, dateNaissance, nom, prenom, isMasculin ? Sexe.MASCULIN : Sexe.FEMININ);
	}

	protected void addIndividuAfterEvent(long eventId, MockIndividu individu, RegDate dateEvenement, TypeEvenementCivilEch type) {
		addIndividuAfterEvent(eventId, individu, dateEvenement, type, ActionEvenementCivilEch.PREMIERE_LIVRAISON, null);
	}

	protected void addIndividuAfterEvent(long eventId, MockIndividu individu, RegDate dateEvenement, TypeEvenementCivilEch type, ActionEvenementCivilEch action, @Nullable Long idEvenementRef) {
		evenementsMap.put(eventId, new IndividuApresEvenement(individu, dateEvenement, type, action, idEvenementRef));
	}

	protected EtatCivil addEtatCivil(MockIndividu individu, @Nullable RegDate dateDebut, TypeEtatCivil type) {
		final MockEtatCivilList etats = individu.getEtatsCivils();
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
		adresse.setLocalite(localite.getNomAbrege());
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
		final MockAdresse adresse = new MockAdresse(type, rue, casePostale, debutValidite, finValidite);
		add(individu, adresse);
		return adresse;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié (à partir d'un pays).
	 */
	protected MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, @Nullable CasePostale casePostale, String rue, String npaLocalite, MockPays pays,
	                                 @Nullable RegDate debutValidite,
	                                 @Nullable RegDate finValidite) {
		final MockAdresse adresse = new MockAdresse(type, rue, casePostale, npaLocalite, pays, debutValidite, finValidite);
		add(individu, adresse);
		return adresse;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié (à partir d'un bâtiment).
	 */
	protected MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, MockBatiment batiment, @Nullable Integer ewid, @Nullable CasePostale casePostale, RegDate debutValidite,
	                                 @Nullable RegDate finValidite) {
		final MockAdresse adresse = new MockAdresse(type, batiment, ewid, casePostale, debutValidite, finValidite);
		add(individu, adresse);
		return adresse;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié (à partir d'une localité).
	 */
	protected MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, @Nullable CasePostale casePostale, MockLocalite localite, RegDate debutValidite, @Nullable RegDate finValidite) {
		final MockAdresse adresse = new MockAdresse(type, casePostale, null, localite, debutValidite, finValidite);
		add(individu, adresse);
		return adresse;
	}

	public static void addLienVersParent(MockIndividu enfant, Individu parent, RegDate dateDebut, @Nullable RegDate dateFin) {
		if (parent.getSexe() == null) {
			throw new IllegalArgumentException("Le sexe du parent doit être connu");
		}
		final TypeRelationVersIndividu typeRelation = parent.getSexe() == Sexe.FEMININ ? TypeRelationVersIndividu.MERE : TypeRelationVersIndividu.PERE;
		enfant.getParents().add(new RelationVersIndividuImpl(parent.getNoTechnique(), typeRelation, dateDebut, dateFin));
	}

	public static void addLiensFiliation(MockIndividu enfant, @Nullable Individu papa, @Nullable Individu maman, RegDate dateDebut, @Nullable RegDate dateFin) {
		if (papa != null) {
			addLienVersParent(enfant, papa, dateDebut, dateFin);
		}
		if (maman != null) {
			addLienVersParent(enfant, maman, dateDebut, dateFin);
		}
	}

	/**
	 * Unit les deux individus par le mariage. Si les individus sont du même sexe, l'état-civil est PACS; et dans le cas normal,
	 * l'état-civil est MARIE.
	 */
	public static void marieIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateMariage) {

		final TypeEtatCivil etatCivil = (individu.getSexe() == conjoint.getSexe() ? TypeEtatCivil.PACS : TypeEtatCivil.MARIE);

		final MockEtatCivilList etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(dateMariage, etatCivil);
		etatsCivilIndividu.add(etatCivilIndividu);
		addRelationConjoint(individu, conjoint, dateMariage);

		final MockEtatCivilList etatsCivilConjoint = conjoint.getEtatsCivils();
		final EtatCivil etatCivilConjoint = creeEtatCivil(dateMariage, etatCivil);
		etatsCivilConjoint.add(etatCivilConjoint);
		addRelationConjoint(conjoint, individu, dateMariage);

		/* les maries peuvent s'embrasser */
	}
	
	public static void addRelationConjoint(MockIndividu individu, MockIndividu nouveauConjoint, RegDate dateMariage) {
		final TypeRelationVersIndividu type = individu.getSexe() == nouveauConjoint.getSexe() ? TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE : TypeRelationVersIndividu.CONJOINT;
		individu.getConjoints().add(new RelationVersIndividuImpl(nouveauConjoint.getNoTechnique(), type, dateMariage, null));
	}
	
	public static void reconcilieIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateReconciliation) {
		// pour ce que l'on fait ici, c'est équivalent à un re-mariage, non ?
		marieIndividus(individu, conjoint, dateReconciliation);
	}

	/**
	 * lie un individu, mais seul.
	 */
	private static void lieIndividu(MockIndividu individu, RegDate date, TypeEtatCivil etatCivil) {

		final MockEtatCivilList etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(date, etatCivil);
		etatsCivilIndividu.add(etatCivilIndividu);
	}

	/**
	 * Marie un individu, mais seul.
	 */
	public static void marieIndividu(MockIndividu individu, RegDate dateMariage) {
		lieIndividu(individu, dateMariage, TypeEtatCivil.MARIE);
	}

	/**
	 * Pacse un individu, mais seul.
	 */
	public static void pacseIndividu(MockIndividu individu, RegDate datePacs) {
		lieIndividu(individu, datePacs, TypeEtatCivil.PACS);
	}


	public static void separeIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateSeparation) {
		separeIndividu(individu, dateSeparation);
		separeIndividu(conjoint, dateSeparation);
	}

	public static void separeIndividu(MockIndividu individu, RegDate dateSeparation) {
		final MockEtatCivilList etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(dateSeparation, TypeEtatCivil.SEPARE);
		etatsCivilIndividu.add(etatCivilIndividu);
	}

	public static void divorceIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateDivorce) {
		delieIndividu(individu, dateDivorce, individu.getSexe() == conjoint.getSexe() ? TypeEtatCivil.PACS_TERMINE : TypeEtatCivil.DIVORCE);
		delieIndividu(conjoint, dateDivorce, individu.getSexe() == conjoint.getSexe() ? TypeEtatCivil.PACS_TERMINE : TypeEtatCivil.DIVORCE);
	}

	public static void divorceIndividu(MockIndividu individu, RegDate dateDivorce) {
		TypeEtatCivil nouveau;
		if (individu.getEtatCivil(dateDivorce).getTypeEtatCivil() == TypeEtatCivil.PACS) {
			nouveau = TypeEtatCivil.PACS_TERMINE;
		} else {
			// Par défaut, s'il n'y a pas d'état civil courant ou tout autre état civil
			// l'individu sera divorcé
			nouveau = TypeEtatCivil.DIVORCE;
		}
		delieIndividu(individu, dateDivorce, nouveau);
	}

	public static void terminePacsIndividu(MockIndividu individu, RegDate dateDivorce) {
		delieIndividu(individu, dateDivorce, TypeEtatCivil.PACS_TERMINE);
	}

	private static void delieIndividu(MockIndividu individu, RegDate dateSeparation, TypeEtatCivil etatCivilResultant) {
		final MockEtatCivilList etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(dateSeparation, etatCivilResultant);
		etatsCivilIndividu.add(etatCivilIndividu);

		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		final RelationVersIndividu relation = DateRangeHelper.rangeAt(conjoints, dateSeparation);
		if (relation != null) {
			((RelationVersIndividuImpl)relation).setDateFin(dateSeparation);
		}
	}

	public static void  dissouePartenartiatParAnnulation (MockIndividu individu, MockIndividu conjoint, RegDate dateDissolution) {
		if (individu.getSexe() != conjoint.getSexe()) {
			throw new IllegalArgumentException("individu et conjoint doivent être du même sexe");
		}
		delieIndividu(individu, dateDissolution, TypeEtatCivil.NON_MARIE);
		delieIndividu(conjoint, dateDissolution, TypeEtatCivil.NON_MARIE);
	}

	/**
	 * @param individu individu auquel on doit rajouté l'état civil correspondant à son veuvage
	 * @param dateVeuvage date de l'obtention du nouvel état civil
	 * @param partenariat <code>true</code> s'il s'agit d'un partenariat enregistré, <code>false</code> s'il s'agit d'un mariage
	 */
	public static void veuvifieIndividu(MockIndividu individu, RegDate dateVeuvage, boolean partenariat) {
		final MockEtatCivilList etatsCivilIndividu = individu.getEtatsCivils();
		final EtatCivil etatCivilIndividu = creeEtatCivil(dateVeuvage, partenariat ? TypeEtatCivil.PACS_VEUF : TypeEtatCivil.VEUF);
		etatsCivilIndividu.add(etatCivilIndividu);

		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		final RelationVersIndividu relation = DateRangeHelper.rangeAt(conjoints, dateVeuvage);
		if (relation != null) {
			((RelationVersIndividuImpl)relation).setDateFin(dateVeuvage);
		}
	}

	public static void annuleMariage(MockIndividu individu) {

		final EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
		individu.getEtatsCivils().remove(etatCivilIndividu);

		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		if (conjoints != null && !conjoints.isEmpty()) {
			final RelationVersIndividu last = conjoints.get(conjoints.size() - 1);
			throw new ProgrammingException(
					"L'individu n°" + individu.getNoTechnique() + " est en ménage avec le conjoint n°" + last.getNumeroAutreIndividu() + ", veuillez utiliser la méthode avec deux arguments !");
		}
	}

	public static void annuleMariage(MockIndividu individu, MockIndividu conjoint) {

		final EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
		individu.getEtatsCivils().remove(etatCivilIndividu);
		annuleDernierConjoint(individu, conjoint.getNoTechnique());


		final EtatCivil etatCivilConjoint = conjoint.getEtatCivilCourant();
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
		final PermisList permisList = individu.getPermis();
		if (permisList == null) {
			individu.setPermis(permis);
		}
		else {
			permisList.add(permis);
		}
		return permis;
	}

	protected void addOrigine(MockIndividu individu, Commune commune) {
		addOrigine(individu, commune.getNomOfficiel(), commune.getSigleCanton());
	}

	protected void addOrigine(MockIndividu individu, String nomLieu, String sigleCanton) {
		Collection<Origine> origines = individu.getOrigines();
		if (origines == null) {
			origines = new ArrayList<>();
			individu.setOrigines(origines);
		}

		final MockOrigine origine = new MockOrigine(nomLieu, sigleCanton);
		origines.add(origine);
	}

	protected void addNationalite(MockIndividu individu, Pays pays, RegDate debut, @Nullable RegDate fin) {
		final MockNationalite nationalite = new MockNationalite();
		nationalite.setDateDebutValidite(debut);
		nationalite.setDateFinValidite(fin);
		nationalite.setPays(pays);
		List<Nationalite> nationalites = individu.getNationalites();
		if (nationalites == null) {
			nationalites = new ArrayList<>();
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
	protected void add(MockIndividu individu, MockAdresse adresse) {
		Assert.notNull(individu);

		final Long numero = individu.getNoTechnique();
		Assert.notNull(numero);

		individu.addAdresse(adresse);
	}

	public MockIndividu getIndividu(Long numeroIndividu) {
		if (explodingIndividus.contains(numeroIndividu)) {
			throw new ServiceCivilException("Individu miné !");
		}
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

		final Collection<Adresse> adressesActives = new ArrayList<>();
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
	public Individu getIndividu(long noIndividu, AttributeIndividu... parties) throws ServiceCivilException {
		final MockIndividu individu = getIndividu(noIndividu);
		if (individu != null && !individu.isNonHabitantNonRenvoye()) {
			// on fait la copie avec les parts demandées seulements
			final Set<AttributeIndividu> parts;
			if (parties == null) {
				parts = Collections.emptySet();
			}
			else {
				parts = new HashSet<>(Arrays.asList(parties));
			}
			return new MockIndividu(individu, parts);
		}
		else {
			return null;
		}
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, AttributeIndividu... parties) throws ServiceCivilException {
		final List<Individu> individus = new ArrayList<>(nosIndividus.size());
		for (Long noIndividu : nosIndividus) {
			final Individu individu = getIndividu(noIndividu, parties);
			if (individu != null) {
				individus.add(individu);
			}
		}
		return individus;
	}

	@Override
	public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
		return evenementsMap.get(eventId);
	}

	@Override
	public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws ServiceCivilException {
		final IndividuApresEvenement ind = evenementsMap.get(evtId);
		return ind != null ? getIndividu(ind.getIndividu().getNoTechnique(), parties) : null;
	}

	@Override
	public void ping() throws ServiceCivilException {
		// rien à faire
	}

	@Override
	public boolean isWarmable() {
		return false;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void onIndividuChange(long numero) {
		// rien à faire
	}
}
