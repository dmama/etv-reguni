package ch.vd.uniregctb.interfaces.service.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.registre.civil.model.EnumTypeTutelle;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.mock.*;
import ch.vd.uniregctb.interfaces.service.CivilListener;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

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
 *  addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, ...);
 *  addAdresse(pierre, EnumTypeAdresse.COURRIER, ...);
 *  ...
 *  }
 *  };
 * </pre>
 */
public abstract class MockServiceCivil implements ServiceCivilService {

	/**
	 * Map des individus par numéro.
	 */
	private final Map<Long, Individu> individusMap = new HashMap<Long, Individu>();

	/**
	 * Constructeur par défaut qui appel init pour initialiser le mock.
	 */
	public MockServiceCivil() {
		init();
	}

	/**
	 * Cette méthode permet d'initialise le mock en fonction des données voulues.
	 */
	protected abstract void init();

	protected void setNationalite(MockIndividu ind, RegDate debut, RegDate fin, MockPays pays) {
		ArrayList<Nationalite> nationalites = new ArrayList<Nationalite>();
		MockNationalite nati = new MockNationalite();
		nati.setDateDebutValidite(debut);
		nati.setDateFinValidite(fin);
		nati.setPays(pays);
		nationalites.add(nati);
		ind.setNationalites(nationalites);
	}

	/**
	 * Ajoute un invidu à la map des individus.
	 */
	protected MockIndividu addIndividu(long numero, RegDate dateNaissance, String nom, String prenom, boolean isMasculin) {
		MockIndividu individu = new MockIndividu();
		individu.setNoTechnique(numero);
		individu.setDateNaissance(dateNaissance);
		individu.setSexeMasculin(isMasculin);

		// Histo
		MockHistoriqueIndividu histo = new MockHistoriqueIndividu(dateNaissance, nom, prenom);
		individu.addHistoriqueIndividu(histo);

		// Etats civils
		ArrayList<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();
		etatsCivils.add(creeEtatCivil(dateNaissance, EnumTypeEtatCivil.CELIBATAIRE, 0));
		individu.setEtatsCivils(etatsCivils);
		// Adresses
		ArrayList<Adresse> sdresses = new ArrayList<Adresse>();
		individu.setAdresses(sdresses);

		// Enfants
		ArrayList<Individu> enfants = new ArrayList<Individu>();
		individu.setEnfants(enfants);

		// Permis
		ArrayList<Permis> permis = new ArrayList<Permis>();
		individu.setPermis(permis);

		add(individu);
		return individu;
	}

	protected EtatCivil addEtatCivil(MockIndividu individu, RegDate dateDebut, EnumTypeEtatCivil type) {
		Collection<EtatCivil> etats = individu.getEtatsCivils();
		EtatCivil etat = creeEtatCivil(dateDebut, type, etats.size());
		etats.add(etat);
		return etat;
	}

	protected Individu addFieldsIndividu(MockIndividu individu, String nouveauNoAVS, String ancienNoAVS, String nomNaissance) {

		individu.setNouveauNoAVS(nouveauNoAVS);
		MockHistoriqueIndividu histo = (MockHistoriqueIndividu) individu.getDernierHistoriqueIndividu();
		histo.setNoAVS(ancienNoAVS);
		histo.setNomNaissance(nomNaissance);

		return individu;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié.
	 */
	protected Adresse addAdresse(Individu individu, EnumTypeAdresse type, String rue, String numeroMaison, Integer numeroPostal, Localite localite, String casePostale,
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
	protected Adresse addAdresse(Individu individu, EnumTypeAdresse type, String rue, String numeroMaison, Integer numeroPostal, String casePostale, String localite, Pays pays, RegDate debutValidite, RegDate finValidite) {

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
	 * Ajoute une adresse pour l'individu spécifié.
	 * @param rue
	 * @param casePostale
	 */
	protected Adresse addAdresse(Individu individu, EnumTypeAdresse type, Rue rue, String casePostale, Localite localite, RegDate debutValidite, RegDate finValidite) {

		Adresse adresse = newAdresse(type, rue, casePostale, localite, debutValidite, finValidite);
		add(individu, adresse);
		return adresse;
	}

	/**
	 * Crée une nouvelle adresse.
	 * @param rue
	 * @param casePostale
	 */
	public static Adresse newAdresse(EnumTypeAdresse type, Rue rue, String casePostale, Localite localite, RegDate debutValidite, RegDate finValidite) {

		Assert.notNull(localite);
		Assert.isTrue(rue == null || rue.getNoLocalite().equals(localite.getNoOrdre()), "La rue et la localité ne correspondent pas");

		MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(type);

		// localité
		if (rue != null) {
			adresse.setRue(rue.getDesignationCourrier());
			adresse.setNumeroRue(rue.getNoRue());
		}
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
	 * Unit les deux individus par le mariage. Si les individus sont du même sexe, l'état-civil est PACS; et dans le cas normal,
	 * l'état-civil est MARIE.
	 */
	protected void marieIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateMariage) {

		final EnumTypeEtatCivil etatCivil = (individu.isSexeMasculin() == conjoint.isSexeMasculin() ? EnumTypeEtatCivil.PACS
				: EnumTypeEtatCivil.MARIE);

		List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();
		MockEtatCivil dernierEtatCivilIndividu = (MockEtatCivil) etatsCivilIndividu.get(etatsCivilIndividu.size() - 1);
		EtatCivil etatCivilIndividu = creeEtatCivil(dateMariage, etatCivil, dernierEtatCivilIndividu.getNoSequence() + 1);
		etatsCivilIndividu.add(etatCivilIndividu);
		individu.setConjoint(conjoint);

		List<EtatCivil> etatsCivilConjoint = conjoint.getEtatsCivils();
		MockEtatCivil dernierEtatCivilConjoint = (MockEtatCivil) etatsCivilConjoint.get(etatsCivilConjoint.size() - 1);
		EtatCivil etatCivilConjoint = creeEtatCivil(dateMariage, etatCivil, dernierEtatCivilConjoint.getNoSequence() + 1);
		etatsCivilConjoint.add(etatCivilConjoint);
		conjoint.setConjoint(individu);
		/* les maries peuvent s'embrasser */
	}

	/**
	 * Marie un individu, mais seul.
	 */
	protected void marieIndividu(MockIndividu individu, RegDate dateMariage) {
		List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();
		MockEtatCivil dernierEtatCivilIndividu = (MockEtatCivil) etatsCivilIndividu.get(etatsCivilIndividu.size() - 1);
		EtatCivil etatCivilIndividu = creeEtatCivil(dateMariage, EnumTypeEtatCivil.MARIE, dernierEtatCivilIndividu.getNoSequence() + 1);
		etatsCivilIndividu.add(etatCivilIndividu);
	}

	protected void separeIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateSeparation) {
		separeIndividu(individu, dateSeparation);
		separeIndividu(conjoint, dateSeparation);
	}

	protected void separeIndividu(MockIndividu individu, RegDate dateSeparation) {
		List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();
		MockEtatCivil dernierEtatCivilIndividu = (MockEtatCivil) etatsCivilIndividu.get(etatsCivilIndividu.size() - 1);
		EtatCivil etatCivilIndividu = creeEtatCivil(dateSeparation, EnumTypeEtatCivil.SEPARE, dernierEtatCivilIndividu.getNoSequence() + 1);
		etatsCivilIndividu.add(etatCivilIndividu);
	}

	protected void divorceIndividus(MockIndividu individu, MockIndividu conjoint, RegDate dateDivorce) {
		divorceIndividu(individu, dateDivorce);
		divorceIndividu(conjoint, dateDivorce);
	}

	protected void divorceIndividu(MockIndividu individu, RegDate dateDivorce) {
		List<EtatCivil> etatsCivilIndividu = individu.getEtatsCivils();
		MockEtatCivil dernierEtatCivilIndividu = (MockEtatCivil) etatsCivilIndividu.get(etatsCivilIndividu.size() - 1);
		EtatCivil etatCivilIndividu = creeEtatCivil(dateDivorce, EnumTypeEtatCivil.DIVORCE, dernierEtatCivilIndividu.getNoSequence() + 1);
		etatsCivilIndividu.add(etatCivilIndividu);
	}

	protected Permis addPermis(MockIndividu individu, EnumTypePermis type, RegDate debut, RegDate fin, int noSequence, boolean permisAnnule) {
		MockPermis permis = new MockPermis();
		permis.setTypePermis(type);
		permis.setDateDebutValidite(debut);
		permis.setDateFinValidite(fin);
		permis.setNoSequence(noSequence);
		if (permisAnnule) {
			permis.setDateAnnulation(RegDate.get());
		}
		if (individu.getPermis() == null) {
			individu.setPermis(new ArrayList<Permis>());
		}
		individu.getPermis().add(permis);
		return permis;
	}

	protected void addOrigine(MockIndividu individu, Pays pays, Commune commune, RegDate debut) {
		MockOrigine origine = new MockOrigine();
		origine.setCommune(commune);
		origine.setPays(pays);
		origine.setDebutValidite(debut);
		individu.setOrigine(origine);
	}

	protected void addNationalite(MockIndividu individu, Pays pays, RegDate debut, RegDate fin, int noSequence) {
		MockNationalite nationalite = new MockNationalite();
		nationalite.setDateDebutValidite(debut);
		nationalite.setDateFinValidite(fin);
		nationalite.setPays(pays);
		nationalite.setNoSequence(noSequence);
		Collection<Nationalite> nationalites = individu.getNationalites();
		if (nationalites == null) {
			nationalites = new ArrayList<Nationalite>();
			individu.setNationalites(nationalites);
		}
		nationalites.add(nationalite);
	}

	protected void setTutelle(MockIndividu pupille, MockIndividu tuteur, EnumTypeTutelle type) {
		MockTutelle tutelle = new MockTutelle();
		tutelle.setLibelleMotif("BlaBla");
		tutelle.setTuteur(tuteur);
		tutelle.setNomAutoriteTutelaire(null);
		tutelle.setTuteurGeneral(null);
		tutelle.setTypeTutelle(type);

		pupille.setTutelle(tutelle);
	}

	protected void setTutelle(MockIndividu pupille, EnumTypeTutelle type) {
		MockTuteurGeneral tuteurGeneral = new MockTuteurGeneral();
		tuteurGeneral.setNomContact("Ouilles");
		tuteurGeneral.setNomOffice("Office du Tuteur General de Vaud");
		tuteurGeneral.setNoTelephoneContact("+41123224568");
		tuteurGeneral.setPrenomContact("Jacques");

		MockTutelle tutelle = new MockTutelle();
		tutelle.setLibelleMotif("BlaBla");
		tutelle.setTuteur(null);
		tutelle.setNomAutoriteTutelaire(null);
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
	 * @param noSequence
	 *            le numero de sequence
	 *
	 * @return l'état civil créé
	 */
	private EtatCivil creeEtatCivil(RegDate date, EnumTypeEtatCivil typeEtatCivil, int noSequence) {
		MockEtatCivil etatCivil = new MockEtatCivil();
		etatCivil.setDateDebutValidite(date);
		etatCivil.setTypeEtatCivil(typeEtatCivil);
		etatCivil.setNoSequence(noSequence);

		return etatCivil;
	}

	/**
	 * Ajoute un individu dans le mock
	 */
	private void add(Individu individu) {
		Assert.notNull(individu);
		Long numero = individu.getNoTechnique();
		Assert.notNull(numero);
		Assert.isNull(individusMap.get(numero));
		individusMap.put(numero, individu);
	}

	/**
	 * Ajoute une adresse pour individu dans le mock
	 */
	protected void add(Individu individu, Adresse adresse) {
		Assert.notNull(individu);
		Assert.isInstanceOf(MockIndividu.class, individu);
		
		final Long numero = individu.getNoTechnique();
		Assert.notNull(numero);

		final MockIndividu mi = (MockIndividu) individu;

		Collection<Adresse> list = mi.getAdresses();
		if (list == null) {
			list = new ArrayList<Adresse>();
			mi.setAdresses(list);
		}
		list.add(adresse);
	}

	/**
	 * @see ch.vd.uniregctb.individu.HostCivilService#getIndividu(java.lang.Long)
	 */
	public Individu getIndividu(Long numeroIndividu) {
		return individusMap.get(numeroIndividu);
	}

	public AdressesCiviles getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {
		final Collection<Adresse> adressesCiviles = getAdressesActives(noIndividu, date);

		AdressesCiviles resultat = new AdressesCiviles();
		if (adressesCiviles != null) {
			for (Object object : adressesCiviles) {
				Adresse adresse = (Adresse) object;
				if (adresse.isValidAt(date)) {
					resultat.set(adresse, strict);
				}
			}
		}
		return resultat;
	}

	public AdressesCivilesHisto getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {
		final Collection<Adresse> adressesCiviles = getAdressesActives(noIndividu, null);

		AdressesCivilesHisto resultat = new AdressesCivilesHisto();
		if (adressesCiviles != null) {
			for (Adresse adresse : adressesCiviles) {
				resultat.add(adresse);
			}
		}
		resultat.finish(strict);

		return resultat;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.HostCivilService#getAdressesActives(java.lang.Long, java.util.Date)
	 */
	public Collection<Adresse> getAdressesActives(Long numeroIndividu, RegDate date) {

		Collection<Adresse> adressesActives = new ArrayList<Adresse>();

		Individu individu = individusMap.get(numeroIndividu);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.HostCivilService#getAdressesActives(java.lang.Long)
	 */
	public Collection<Adresse> getAdressesActives(Long numeroIndividu) {
		return getAdressesActives(numeroIndividu, RegDate.get());
	}

	public Collection<Adresse> getAdresses(long noIndividu, int annee) {
		return getAdressesActives(noIndividu, null);
	}

	public Individu getIndividu(long noIndividu, int annee) {
		MockIndividu individu = (MockIndividu) getIndividu(noIndividu);
		if (individu != null) {
			individu.setAdresses(getAdressesActives(noIndividu, null));
		}
		return individu;
	}

	public Individu getIndividu(long noIndividu, int annee, EnumAttributeIndividu... parties) {
		MockIndividu individu = (MockIndividu) getIndividu(noIndividu);
		if (individu != null) {
			individu.setAdresses(getAdressesActives(noIndividu, null));
		}
		return individu;
	}

	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, EnumAttributeIndividu... parties) {
		throw new NotImplementedException();
	}

	public Origine getOrigine(long noIndividu, int annee) {
		MockIndividu individu = (MockIndividu) getIndividu(noIndividu);
		if (individu != null) {
			return individu.getOrigine();
		}
		return null;
	}

	public Collection<Permis> getPermis(long noIndividu, int annee) {
		MockIndividu individu = (MockIndividu) getIndividu(noIndividu);
		if (individu != null) {
			return individu.getPermis();
		}
		return null;
	}

	public Tutelle getTutelle(long noIndividu, int annee) {
		MockIndividu individu = (MockIndividu) getIndividu(noIndividu);
		if (individu != null) {
			return individu.getTutelle();
		}
		return null;
	}

	public Collection<Nationalite> getNationalites(long noIndividu, int annee) {
		Individu ind = getIndividu(noIndividu);
		return ind.getNationalites();
	}

	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date)  {

		EtatCivil etat = null;

		final int year = (date == null ? 2400 : date.year());
		final MockIndividu individu = (MockIndividu) getIndividu(noIndividu, year);

		final Collection<?> etats = individu.getEtatsCivils();
		if (etats != null) {
			for (Object o : etats) {

				final EtatCivil e = (EtatCivil) o;
				final RegDate debutValidite = e.getDateDebutValidite();

				/*
				 * Attention: les état-civils ne sont pas triés dans la collection, et certains n'ont pas de date de fin de validité (=
				 * implicite à la date d'ouverture du suivant)
				 */
				if (RegDateHelper.isBetween(date, debutValidite, null, NullDateBehavior.LATEST)) {
					if (etat == null) {
						/* premier état-civil trouvé */
						etat = e;
					}
					else if (debutValidite != null && etat.getDateDebutValidite() != null && debutValidite.isAfterOrEqual(etat.getDateDebutValidite())) {
						/* trouvé un état-civil valide et dont la date de validité est après celle de l'état-civil courant */
						etat = e;
					}
				}
			}
		}
		return etat;
	}

	public Permis getPermisActif(long noIndividu, RegDate date) {

		final int year = (date == null ? 2400 : date.year());
		final MockIndividu individu = (MockIndividu) getIndividu(noIndividu, year);

		Permis permis = null;

		final Collection<?> coll = individu.getPermis();
		if (coll != null) {
			for (Object o : coll) {

				final Permis e = (Permis) o;
				final RegDate debutValidite = e.getDateDebutValidite();
				final RegDate finValidite = e.getDateFinValidite();

				/*
				 * Attention: les permis ne sont pas triés dans la collection, et certains n'ont pas de date de fin de validité (= implicite
				 * à la date d'ouverture du suivant)
				 */
				if (RegDateHelper.isBetween(date, debutValidite, finValidite, NullDateBehavior.LATEST)) {
					if (permis == null) {
						/* premier permis trouvé */
						permis = e;
					}
					else if (debutValidite.isAfterOrEqual(permis.getDateDebutValidite())) {
						/* trouvé un permis valide et dont la date de validité est après celle de l'état-civil courant */
						permis = e;
					}
				}
			}
		}

		return permis;
	}

	public void setUp(ServiceCivilService target) {
		Assert.fail("Not implemented");
	}

	public void tearDown() {
		Assert.fail("Not implemented");
	}

	public boolean isWarmable() {
		return false;
	}

	public void warmCache(List<Individu> individus, RegDate date, EnumAttributeIndividu... parties) {
		throw new NotImplementedException();
	}

	public void register(CivilListener listener) {
		// rien à faire
	}

	public void onIndividuChange(long numero) {
		// rien à faire
	}
}
