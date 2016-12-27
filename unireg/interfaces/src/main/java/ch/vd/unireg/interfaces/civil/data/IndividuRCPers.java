package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ech.ech0011.v5.PlaceOfOrigin;
import ch.ech.ech0044.v2.NamedPersonId;
import ch.ech.ech0044.v2.PersonIdentificationPartner;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.evd0001.v5.Contact;
import ch.vd.evd0001.v5.FullName;
import ch.vd.evd0001.v5.Identity;
import ch.vd.evd0001.v5.MaritalData;
import ch.vd.evd0001.v5.Nationality;
import ch.vd.evd0001.v5.Parent;
import ch.vd.evd0001.v5.Person;
import ch.vd.evd0001.v5.PersonIdentification;
import ch.vd.evd0001.v5.Residence;
import ch.vd.evd0001.v5.ResidencePermit;
import ch.vd.evd0001.v5.UpiPerson;
import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.mock.CollectionLimitator;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.AdresseCourrierMinimale;
import ch.vd.unireg.interfaces.infra.data.RangeChangingAdresseWrapper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermisInvalideException;

public class IndividuRCPers implements Individu, Serializable {

	private static final long serialVersionUID = 9048462821163407226L;
	protected final Logger LOGGER = LoggerFactory.getLogger(IndividuRCPers.class);

	private long noTechnique;
	private StatutIndividu statut;
	private String prenomUsuel;
	private String tousPrenoms;
	private String nom;
	private String nomNaissance;
	private String noAVS11;
	private String nouveauNoAVS;
	private String numeroRCE;
	private Sexe sexe;
	private RegDate deces;
	private RegDate naissance;
	private RegDate dateArriveeVD;
	private Collection<Origine> origines;
	private Collection<Adresse> adresses;
	private EtatCivilList etatsCivils;
	private List<RelationVersIndividu> parents;
	private List<RelationVersIndividu> conjoints;
	private PermisList permis;
	private Collection<Nationalite> nationalites;
	private final Set<AttributeIndividu> availableParts = EnumSet.noneOf(AttributeIndividu.class);
	private NomPrenom nomOfficielMere;
	private NomPrenom nomOfficielPere;

	public static Individu get(Person target, boolean history, ServiceInfrastructureRaw infraService) {
		if (target == null) {
			return null;
		}

		return new IndividuRCPers(target, history, infraService);
	}

	public IndividuRCPers(Person person, boolean history, ServiceInfrastructureRaw infraService) {
		this.noTechnique = getNoIndividu(person);
		this.statut = initStatut(person.getStatus());

		final Identity identity = person.getIdentity();
		final UpiPerson upiPerson = person.getUpiPerson();

		this.prenomUsuel = initPrenom(identity.getCallName(), identity.getFirstNames());
		this.tousPrenoms = identity.getFirstNames();
		this.nom = identity.getOfficialName();
		this.nomNaissance = identity.getOriginalName();
		this.sexe = EchHelper.sexeFromEch44(identity.getSex());
		this.deces = XmlUtils.xmlcal2regdate(person.getDateOfDeath());
		this.naissance = EchHelper.partialDateFromEch44(identity.getDateOfBirth());
		this.noAVS11 = initNumeroAVS11(identity.getOtherPersonId(), this.naissance, this.sexe);
		if (upiPerson != null) {
			this.nouveauNoAVS = EchHelper.avs13FromEch(upiPerson.getVn());
			this.nomOfficielMere = buildNomPrenom(upiPerson.getMothersName());
			this.nomOfficielPere = buildNomPrenom(upiPerson.getFathersName());
		}
		this.numeroRCE = initNumeroRCE(identity.getOtherPersonId());
		this.dateArriveeVD = initDateArriveeVD(person.getResidenceHistory());
		this.origines = initOrigins(person);

		if (history) {
			this.adresses = initAdresses(null, person.getContactHistory(), person.getResidenceHistory(), deces, infraService);
		}
		else {
			this.adresses = initAdresses(person.getCurrentContact(), null, person.getCurrentResidence(), deces, infraService);
		}

		try {
			if (history) {
				this.etatsCivils = initEtatsCivils(person.getMaritalStatusHistory());
			}
			else {
				this.etatsCivils = initEtatsCivils(person.getCurrentMaritalStatus());
			}
		}
		catch (ServiceCivilException e) {
			throw new ServiceCivilException("Individu n°" + this.noTechnique + ": " + e.getMessage(), e);
		}

		if (history) {
			this.parents = initParents(this.naissance, person.getParentHistory());
			this.conjoints = initConjoints(this.deces, person.getMaritalStatusHistory());
		}
		else {
			this.parents = initParents(this.naissance, person.getCurrentParent());
			this.conjoints = initConjoints(this.deces, Collections.singletonList(person.getCurrentMaritalStatus()));
		}

		try {

			if (history) {
				this.permis = initPermis(person.getResidencePermitHistory());
				this.nationalites = initNationalites(person.getNationalityHistory(), infraService);
			}
			else { // [SIFISC-5181] prise en compte des valeurs courantes
				this.permis = initPermis(person.getCurrentResidencePermit());
				this.nationalites = initNationalites(person.getCurrentNationality(), infraService);
			}
		}
		catch (TypePermisInvalideException e) {
			LOGGER.error(String.format("Type de permis invalide détecté!! Detail de l'individu reçu : %s",person.toString()));
			throw new TypePermisInvalideException(this.noTechnique,e.getMessage());
		}

		// avec RcPers, toutes les parts sont systématiquement retournées
		this.availableParts.addAll(EnumSet.allOf(AttributeIndividu.class));
	}

	public IndividuRCPers(IndividuRCPers right, Set<AttributeIndividu> parts) {
		this.noTechnique = right.noTechnique;
		this.statut = right.statut;
		this.prenomUsuel = right.getPrenomUsuel();
		this.tousPrenoms = right.getTousPrenoms();
		this.nom = right.getNom();
		this.nomNaissance = right.getNomNaissance();
		this.noAVS11 = right.getNoAVS11();
		this.nouveauNoAVS = right.nouveauNoAVS;
		this.numeroRCE = right.numeroRCE;
		this.sexe = right.sexe;
		this.deces = right.deces;
		this.naissance = right.naissance;
		this.dateArriveeVD = right.dateArriveeVD;
		this.etatsCivils = right.etatsCivils;
		this.nomOfficielMere = right.nomOfficielMere;
		this.nomOfficielPere = right.nomOfficielPere;

		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			this.adresses = right.adresses;
		}
		if (parts != null && parts.contains(AttributeIndividu.CONJOINTS)) {
			conjoints = right.conjoints;
		}
		if (parts != null && parts.contains(AttributeIndividu.ORIGINE)) {
			origines = right.origines;
		}
		if (parts != null && parts.contains(AttributeIndividu.PARENTS)) {
			parents = right.parents;
		}
		if (parts != null && parts.contains(AttributeIndividu.PERMIS)) {
			permis = right.permis;
		}
		if (parts != null && parts.contains(AttributeIndividu.NATIONALITES)) {
			nationalites = right.nationalites;
		}

		if (parts != null) {
			this.availableParts.addAll(parts);
		}
	}

	public IndividuRCPers(IndividuRCPers right, @NotNull RegDate date) {
		this.noTechnique = right.noTechnique;
		this.statut = right.statut;
		this.prenomUsuel = right.getPrenomUsuel();
		this.tousPrenoms = right.getTousPrenoms();
		this.nom = right.getNom();
		this.nomNaissance = right.getNomNaissance();
		this.noAVS11 = right.getNoAVS11();
		this.nouveauNoAVS = right.nouveauNoAVS;
		this.numeroRCE = right.numeroRCE;
		this.sexe = right.sexe;
		this.deces = right.deces;
		this.naissance = right.naissance;
		this.dateArriveeVD = right.dateArriveeVD;
		this.origines = right.origines;
		this.nationalites = right.nationalites;
		this.nomOfficielMere = right.nomOfficielMere;
		this.nomOfficielPere = right.nomOfficielPere;

		this.adresses = right.adresses;
		this.parents = right.parents;
		this.conjoints = right.conjoints;
		this.etatsCivils = right.etatsCivils;
		this.permis = right.permis;
		limitHistoTo(date);

		this.availableParts.addAll(right.availableParts);
	}

	private void limitHistoTo(RegDate date) {

		if (adresses != null) {
			adresses = CollectionLimitator.limit(adresses, date, CollectionLimitator.ADRESSE_LIMITATOR);
		}
		if (parents != null) {
			parents = CollectionLimitator.limit(parents, date, CollectionLimitator.RELATION_LIMITATOR);
		}
		if (conjoints != null) {
			conjoints = CollectionLimitator.limit(conjoints, date, CollectionLimitator.RELATION_LIMITATOR);
		}
		if (etatsCivils != null) {
			etatsCivils = new EtatCivilListRCPers(CollectionLimitator.limit(etatsCivils.asList(), date, CollectionLimitator.ETAT_CIVIL_LIMITATOR));
		}
		if (permis != null) {
			final List<Permis> limited = CollectionLimitator.limit(permis, date, CollectionLimitator.PERMIS_LIMITATOR);
			permis = (limited == null ? null : new PermisListImpl(limited));
		}
		if (nationalites != null) {
			nationalites = CollectionLimitator.limit(nationalites, date, CollectionLimitator.NATIONALITE_LIMITATOR);
		}
	}

	public static long getNoIndividu(NamedPersonId personId) {
		return Long.parseLong(personId.getPersonId());
	}

	/**
	 * Méthode helper publique pour cacher au monde extérieur la complexité de la récupération du numéro d'individu
	 *
	 * @param person individu renvoyé par RCPers
	 * @return le numéro d'individu tel que connu par chez nous
	 */
	public static long getNoIndividu(Person person) {
		return getNoIndividu(person.getIdentity());
	}

	public static long getNoIndividu(PersonIdentification pi) {
		return getNoIndividu(pi.getIdentification());
	}

	public static long getNoIndividu(PersonIdentificationPartner pip) {
		return getNoIndividu(pip.getLocalPersonId());
	}

	public static long getNoIndividu(Identity identity) {
		return getNoIndividu(identity.getLocalPersonId());
	}

	private static String initNumeroAVS11(List<NamedPersonId> otherPersonIds, RegDate dateNaissance, Sexe sexe) {
		if (otherPersonIds == null) {
			return null;
		}
		String numeroAVS11 = null;
		for (NamedPersonId id : otherPersonIds) {
			if ("CH.AHV".equals(id.getPersonIdCategory())) {
				// [SIFISC-7610] on ne prend en compte l'identifiant ici que s'il a la bonne tête pour être un NAVS11 (la validation sur le sexe est bypassée si on ne le connait pas)
				final String avs11 = AvsHelper.removeSpaceAndDash(id.getPersonId());
				if (AvsHelper.isValidAncienNumAVS(avs11, dateNaissance, sexe == Sexe.MASCULIN) || (sexe == null && AvsHelper.isValidAncienNumAVS(avs11, dateNaissance, true))) {
					numeroAVS11 = id.getPersonId();
				}
				break;
			}
		}
		return numeroAVS11;
	}

	private static String initNumeroRCE(List<NamedPersonId> otherPersonIds) {
		if (otherPersonIds == null) {
			return null;
		}
		String numeroRCE = null;
		for (NamedPersonId id : otherPersonIds) {
			if ("CH.ZAR".equals(id.getPersonIdCategory())) {
				numeroRCE = id.getPersonId();
				break;
			}
		}
		return numeroRCE;
	}

	private static StatutIndividu initStatut(Person.Status status) {
		if (status == null) {
			return null;
		}

		if (status.getActive() != null) {
			return StatutIndividu.active();
		}
		else if (status.getCanceled() != null) {
			final PersonIdentificationPartner replacedBy = status.getCanceled().getReplacedBy();
			if (replacedBy == null) {
				return StatutIndividu.inactiveWithoutReplacement();
			}
			else {
				return StatutIndividu.replaced(getNoIndividu(replacedBy.getLocalPersonId()));
			}
		}
		else {
			throw new IllegalArgumentException("Valeur de Status non-supportée : " + status);
		}
	}

	/**
	 * [SIFISC-5365] Le prénom doit être issu du prénom usuel civil, sauf s'il n'y en a pas, auquel cas on doit prendre
	 * le premier prénom de la liste des prénoms fournis
	 * @param callName prénom usuel
	 * @param firstNames tous les prénoms
	 * @return le prénom à utiliser
	 */
	protected static String initPrenom(@Nullable String callName, String firstNames) {
		if (StringUtils.isNotBlank(callName)) {
			return callName;
		}
		else if (StringUtils.isNotBlank(firstNames)) {
			final String[] list = StringUtils.trimToEmpty(firstNames).split("\\s");
			if (list.length > 0) {
				return list[0];
			}
		}
		return null;
	}

	protected static List<Nationalite> initNationalites(Nationality nationalite, ServiceInfrastructureRaw infraService) {
		return initNationalites(Collections.singletonList(nationalite), infraService);
	}

	protected static List<Nationalite> initNationalites(List<Nationality> nationalities, ServiceInfrastructureRaw infraService) {
		if (nationalities == null || nationalities.size() == 0) {
			return Collections.emptyList();
		}

		final List<Nationalite> list = new ArrayList<>(nationalities.size());
		for (Nationality nat : nationalities) {
			final Nationalite nationalite = NationaliteRCPers.get(nat, infraService);
			if (nationalite != null) {
				list.add(nationalite);
			}
		}
		return list;
	}

	private static PermisList initPermis(ResidencePermit permis) {
		if (permis == null) {
			return null;
		}
		return new PermisListImpl(Collections.singletonList(PermisRCPers.get(permis)));
	}

	private static PermisList initPermis(List<ResidencePermit> permis) {
		if (permis == null) {
			return null;
		}
		final List<Permis> list = new ArrayList<>(permis.size());
		for (ResidencePermit p : permis) {
			list.add(PermisRCPers.get(p));
		}
		return new PermisListImpl(list);
	}

	private static EtatCivilList initEtatsCivils(MaritalData maritalStatus) {
		final List<EtatCivil> list = new ArrayList<>();
		if (maritalStatus != null) {
			list.addAll(EtatCivilRCPers.get(maritalStatus));
		}
		return new EtatCivilListRCPers(list);
	}

	protected static EtatCivilList initEtatsCivils(List<MaritalData> maritalStatus) {
		if (maritalStatus == null) {
			return null;
		}
		final List<EtatCivil> list = new ArrayList<>();
		for (MaritalData data : maritalStatus) {
			list.addAll(EtatCivilRCPers.get(data));
		}
		return new EtatCivilListRCPers(list);
	}

	protected static List<Adresse> initAdresses(@Nullable Contact currentContact, @Nullable List<Contact> contact, List<Residence> residence, @Nullable RegDate dateDeces, ServiceInfrastructureRaw infraService) {

		final List<Adresse> adresses = new ArrayList<>();

		if (contact != null) {
			// l'historique est renseigné
			for (Contact hc : contact) {
				adresses.add(AdresseRCPers.get(hc, infraService));
			}
		}
		else if (currentContact != null) {
			// l'état courant est renseigné (voir SIFISC-5181)
			adresses.add(AdresseRCPers.get(currentContact, infraService));
		}

		if (residence != null) {

			final Map<Integer, List<Residence>> adressesParCommune = splitParCommune(residence);

			for (List<Residence> list : adressesParCommune.values()) {

				// [SIREF-1794] les résidences doivent être triées par date d'arrivée croissant, puis par date de déménagement croissant
				Collections.sort(list, new Comparator<Residence>() {
					@Override
					public int compare(Residence o1, Residence o2) {
						// on trie par date d'arrivée dans la commune
						final RegDate arrivalDate1 = XmlUtils.xmlcal2regdate(o1.getArrivalDate());
						final RegDate arrivalDate2 = XmlUtils.xmlcal2regdate(o2.getArrivalDate());
						int c = NullDateBehavior.EARLIEST.compare(arrivalDate1, arrivalDate2);
						if (c == 0) {
							// puis par date de déménagement dans la commune
							final RegDate movingDate1 = XmlUtils.xmlcal2regdate(o1.getDwellingAddress().getMovingDate());
							final RegDate movingDate2 = XmlUtils.xmlcal2regdate(o2.getDwellingAddress().getMovingDate());
							c = NullDateBehavior.EARLIEST.compare(movingDate1, movingDate2);
						}
						return c;
					}
				});

				// Attention, il ne faut pas non plus, dans une même commune, mélanger les adresses de différents types !
				final Map<TypeAdresseCivil, List<Residence>> parType = splitParType(list);
				for (List<Residence> listeTypee : parType.values()) {

					// on crée les adresses au format Unireg
					final List<AdresseRCPers> res = new ArrayList<>();
					for (int i = 0, residenceSize = listeTypee.size(); i < residenceSize; i++) {
						final Residence r = listeTypee.get(i);
						final Residence next = i + 1 < residenceSize ? listeTypee.get(i + 1) : null;
						res.add(AdresseRCPers.get(r, next, dateDeces, infraService)); // va calculer une date de fin si nécessaire
					}

					adresses.addAll(res);
				}
			}
		}

		// [SIFISC-6604] si l'historique est renseigné, on essaie de remplir les trous d'adresse avec le goesTo de l'adresse de résidence précédente...
		if (contact != null) {
			fillHoles(adresses);
		}

		Collections.sort(adresses, new DateRangeComparator<>());
		return adresses;
	}

	private static void fillHoles(List<Adresse> adresses) {
		// y a-t-il seulement des trous ?
		if (adresses.size() == 0) {
			return;
		}

		// je garde les adresses de résidence sur le côté, j'en aurai besoin plus tard
		final List<Adresse> residences = new ArrayList<>(adresses.size());

		// d'abord, on cherche la date de début la plus ancienne
		RegDate bigBang = RegDateHelper.getLateDate();
		for (Adresse adr : adresses) {
			if (RegDateHelper.isBefore(adr.getDateDebut(), bigBang, NullDateBehavior.EARLIEST)) {
				bigBang = adr.getDateDebut();
			}

			// collecte des adresses de résidence (principales seulement -> l'AdresseService ne tient pas compte des adresses secondaires pour les défauts de courrier)
			if (adr.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				residences.add(adr);
			}
		}

		// voici la période pendant laquelle on doit connaître les adresses : depuis le big bang
		final DateRange life = new DateRangeHelper.Range(bigBang, null);
		final DateRangeAdapterCallback callback = new DateRangeAdapterCallback();
		final Set<TypeAdresseCivil> typesPrisEnCompte = EnumSet.of(TypeAdresseCivil.COURRIER, TypeAdresseCivil.PRINCIPALE);
		List<DateRange> holes = Collections.singletonList(life);
		for (Adresse adr : adresses) {
			if (typesPrisEnCompte.contains(adr.getTypeAdresse())) {
				holes = DateRangeHelper.subtract(holes, Collections.singletonList(adr), callback);
			}
		}

		// alors, reste-t-il des trous ?
		if (holes.size() > 0) {
			// essayons de les remplir...

			// procédure de remplissage... enclanchée
			for (final DateRange hole : holes) {
				Localisation goesTo = null;

				// si trou sans adresse précédente possible, tant pis, on n'a pas d'adresse connue
				if (hole.getDateDebut() != null) {

					// à la veille du début d'un trou, il doit y avoir une adresse qui se termine
					// cette adresse peut être une adresse de courrier, donc on va chercher la dernière adresse de résidence
					// qui s'est fermée avant cette date, et on utilise, s'il est présent, la champ "goesTo"
					RegDate dateFinTrouvee = null;
					for (Adresse adr : residences) {
						if (RegDateHelper.isBefore(adr.getDateFin(), hole.getDateDebut(), NullDateBehavior.LATEST)) {
							if (dateFinTrouvee == null || RegDateHelper.isAfter(adr.getDateFin(), dateFinTrouvee, NullDateBehavior.LATEST)) {
								dateFinTrouvee = adr.getDateFin();
								goesTo = adr.getLocalisationSuivante();
							}
						}
					}
				}

				// remplissage
				final Adresse fillingAddress;
				if (goesTo != null) {
					// on a trouvé quelque chose, au final... faisons-en une adresse courrier avec les bonnes dates
					final Adresse adresseCourrier = goesTo.getAdresseCourrier();
					if (adresseCourrier != null) {
						fillingAddress = new RangeChangingAdresseWrapper(adresseCourrier, hole.getDateDebut(), hole.getDateFin());
					}
					else if (goesTo.getType() == LocalisationType.CANTON_VD || goesTo.getType() == LocalisationType.HORS_CANTON) {
						// adresse en Suisse, dont on ne connait que la commune
						fillingAddress = new AdresseCourrierMinimale(hole.getDateDebut(), hole.getDateFin(), goesTo.getNoOfs(), ServiceInfrastructureRaw.noOfsSuisse);
					}
					else {
						// adresse étrangère, dont on ne connait finalement que le pays (si une ville est donnée, alors une adresse courrier
						// a déjà été créée dans le goesTo (voir AdresseRCPers))
						final Integer noOfs = goesTo.getNoOfs();
						fillingAddress = new AdresseCourrierMinimale(hole.getDateDebut(), hole.getDateFin(), null, noOfs != null ? noOfs : ServiceInfrastructureRaw.noPaysInconnu);
					}
				}
				else {
					fillingAddress = new AdresseCourrierMinimale(hole.getDateDebut(), hole.getDateFin(), null, ServiceInfrastructureRaw.noPaysInconnu);
				}
				adresses.add(fillingAddress);
			}
		}
	}

	private static Map<Integer, List<Residence>> splitParCommune(List<Residence> residence) {

		final Map<Integer, List<Residence>> map = new HashMap<>(residence.size());

		for (Residence r : residence) {
			final Integer key = r.getResidenceMunicipality().getMunicipalityId();
			List<Residence> list = map.get(key);
			if (list == null) {
				list = new ArrayList<>();
				map.put(key, list);
			}
			list.add(r);
		}

		return map;
	}

	private static Map<TypeAdresseCivil, List<Residence>> splitParType(List<Residence> residence) {
		final Map<TypeAdresseCivil, List<Residence>> map = new HashMap<>(TypeAdresseCivil.values().length);
		for (Residence r : residence) {
			final TypeAdresseCivil type = AdresseRCPers.getTypeAdresseResidence(r);
			List<Residence> list = map.get(type);
			if (list == null) {
				list = new ArrayList<>();
				map.put(type, list);
			}
			list.add(r);
		}
		return map;
	}

	protected static List<RelationVersIndividu> initParents(RegDate dateNaissance, List<Parent> parents) {
		if (parents == null || parents.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<>();
		for (Parent p : parents) {
			final PersonIdentificationPartner partnerIdentification = p.getIdentification().getIdentification();
			if (partnerIdentification != null) {
				final NamedPersonId partnerId = partnerIdentification.getLocalPersonId();
				if (partnerId != null) {
					final long numeroInd = getNoIndividu(partnerId);
					RegDate validFrom = XmlUtils.xmlcal2regdate(p.getParentFrom());
					if (validFrom == null) {
						// à défaut de mieux, on considère que la relation s'établit à la naissance de l'enfant
						validFrom = dateNaissance;
					}
					final RegDate validTill = XmlUtils.xmlcal2regdate(p.getParentTill());
					final Sexe sexeParent = EchHelper.sexeFromEch44(partnerIdentification.getSex());
					final TypeRelationVersIndividu type = (sexeParent == Sexe.MASCULIN ? TypeRelationVersIndividu.PERE : (sexeParent == Sexe.FEMININ ? TypeRelationVersIndividu.MERE : null));
					list.add(new RelationVersIndividuImpl(numeroInd, type, validFrom, validTill));
				}
			}
		}
		return list.isEmpty() ? null : list;
	}

	protected static List<RelationVersIndividu> initConjoints(RegDate dateDeces, List<MaritalData> etatsCivils) {
		if (etatsCivils == null || etatsCivils.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividuImpl> list = new ArrayList<>();
		for (MaritalData md : etatsCivils) {
			final RegDate validFrom = XmlUtils.xmlcal2regdate(md.getDateOfMaritalStatus());

			// récupération de la date de fin de l'état civil précédent
			if (!list.isEmpty() && validFrom != null) {
				final RegDate oldValidTill = validFrom.getOneDayBefore();
				for (int i = list.size() - 1 ; i >= 0 ; -- i) {
					final RelationVersIndividuImpl oldRelation = list.get(i);
					if (oldRelation.getDateFin() == null) {
						oldRelation.setDateFin(RegDateHelper.maximum(oldValidTill, oldRelation.getDateDebut(), NullDateBehavior.EARLIEST));
					}
					else {
						break;
					}
				}
			}

			final PersonIdentification partnerIdentification = md.getPartner();
			if (partnerIdentification != null) {
				final PersonIdentificationPartner partner = partnerIdentification.getIdentification();
				if (partner != null) {
					final NamedPersonId partnerId = partner.getLocalPersonId();
					if (partnerId != null) {
						final long numeroInd = getNoIndividu(partnerId);
						final TypeEtatCivil etatCivil = EchHelper.etatCivilFromEch11(md.getMaritalStatus(), md.getCancelationReason());
						final TypeRelationVersIndividu type = etatCivil == TypeEtatCivil.PACS ? TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE : TypeRelationVersIndividu.CONJOINT;
						list.add(new RelationVersIndividuImpl(numeroInd, type, validFrom, null));
					}
				}
			}
		}

		if (list.isEmpty()) {
			return null;
		}
		if (dateDeces != null) {
			final RelationVersIndividuImpl last = list.get(list.size() - 1);
			if (last.getDateFin() == null) {
				last.setDateFin(dateDeces.getOneDayBefore());
			}
		}
		return DateRangeHelper.collate(new ArrayList<RelationVersIndividu>(list));
	}

	private static Collection<Origine> initOrigins(Person person) {
		final List<PlaceOfOrigin> origins = person.getIdentity().getOrigin();
		if (origins == null || origins.isEmpty()) {
			return null;
		}
		else {
			// Contournement du SIREF-2786 qui affiche des doublons d'origines
			final Set<Origine> set = new LinkedHashSet<>(origins.size());
			for (PlaceOfOrigin origin : origins) {
				set.add(OrigineRCPers.get(origin));
			}
			return set;
		}
	}

	private static RegDate initDateArriveeVD(List<Residence> residenceHistory) {
		if (residenceHistory == null || residenceHistory.isEmpty()) {
			return null;
		}
		RegDate dateArrivee = RegDateHelper.getLateDate();
		for (Residence residence : residenceHistory) {
			dateArrivee = RegDateHelper.minimum(dateArrivee, XmlUtils.xmlcal2regdate(residence.getArrivalDate()), NullDateBehavior.EARLIEST);
		}
		return dateArrivee == RegDateHelper.getLateDate() ? null : dateArrivee;
	}

	private static NomPrenom buildNomPrenom(FullName fullName) {
		return fullName == null ? null : new NomPrenom(fullName.getLastName(), fullName.getFirstNames());
	}

	@Override
	public StatutIndividu getStatut() {
		return statut;
	}

	@Override
	public String getPrenomUsuel() {
		return prenomUsuel;
	}

	@Override
	public String getTousPrenoms() {
		return tousPrenoms;
	}

	@Override
	public String getNom() {
		return nom;
	}

	@Override
	public String getNomNaissance() {
		return nomNaissance;
	}

	@Override
	public String getNoAVS11() {
		return noAVS11;
	}

	@Override
	public RegDate getDateDeces() {
		return deces;
	}

	@Override
	public RegDate getDateNaissance() {
		return naissance;
	}

	@Override
	public RegDate getDateArriveeVD() {
		return dateArriveeVD;
	}

	@Override
	public boolean isMineur(RegDate date) {
		return naissance != null && naissance.addYears(18).compareTo(date) > 0;
	}

	@Override
	public List<RelationVersIndividu> getParents() {
		return parents;
	}

	@Override
	public List<RelationVersIndividu> getConjoints() {
		return conjoints;
	}

	@Override
	public EtatCivilList getEtatsCivils() {
		return etatsCivils;
	}

	@Override
	public EtatCivil getEtatCivilCourant() {
		return getEtatCivil(null);
	}

	@Override
	public EtatCivil getEtatCivil(RegDate date) {
		return etatsCivils.getEtatCivilAt(date);
	}

	@Override
	public Collection<Nationalite> getNationalites() {
		return nationalites;
	}

	@Override
	public long getNoTechnique() {
		return noTechnique;
	}

	@Override
	public String getNouveauNoAVS() {
		return nouveauNoAVS;
	}

	@Override
	public String getNumeroRCE() {
		return numeroRCE;
	}

	@Override
	public Collection<Origine> getOrigines() {
		return origines;
	}

	@Override
	public PermisList getPermis() {
		return permis;
	}

	@Override
	public Sexe getSexe() {
		return sexe;
	}

	@Override
	public NomPrenom getNomOfficielMere() {
		return nomOfficielMere;
	}

	@Override
	public NomPrenom getNomOfficielPere() {
		return nomOfficielPere;
	}

	@Override
	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			adresses = individu.getAdresses();
		}
		if (parts != null && parts.contains(AttributeIndividu.CONJOINTS)) {
			conjoints = individu.getConjoints();
		}
		if (parts != null && parts.contains(AttributeIndividu.ORIGINE)) {
			origines = individu.getOrigines();
		}
		if (parts != null && parts.contains(AttributeIndividu.PARENTS)) {
			parents = individu.getParents();
		}
		if (parts != null && parts.contains(AttributeIndividu.PERMIS)) {
			permis = individu.getPermis();
		}
		if (parts != null && parts.contains(AttributeIndividu.NATIONALITES)) {
			nationalites = individu.getNationalites();
		}

		if (parts != null) {
			this.availableParts.addAll(parts);
		}
	}

	@Override
	public Individu clone(Set<AttributeIndividu> parts) {
		return new IndividuRCPers(this, parts);
	}

	@Override
	public Individu cloneUpTo(@NotNull RegDate date) {
		return new IndividuRCPers(this, date);
	}

	@Override
	public Set<AttributeIndividu> getAvailableParts() {
		return availableParts;
	}

	@Override
	public Collection<Adresse> getAdresses() {
		return adresses;
	}
}
