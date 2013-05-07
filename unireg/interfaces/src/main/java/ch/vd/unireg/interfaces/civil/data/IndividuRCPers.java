package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ech.ech0010.v4.MailAddress;
import ch.ech.ech0011.v5.PlaceOfOrigin;
import ch.ech.ech0044.v2.NamedPersonId;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v3.HistoryContact;
import ch.vd.evd0001.v3.Identity;
import ch.vd.evd0001.v3.MaritalData;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.PersonIdentification;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0001.v3.Residence;
import ch.vd.evd0001.v3.ResidencePermit;
import ch.vd.evd0001.v3.UpiPerson;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.EvdHelper;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.mock.CollectionLimitator;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.AdresseCourrierMinimale;
import ch.vd.unireg.interfaces.infra.data.AdresseRCPers;
import ch.vd.unireg.interfaces.infra.data.RangeChangingAdresseWrapper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class IndividuRCPers implements Individu, Serializable {

	private static final long serialVersionUID = -5634728139412585597L;

	private long noTechnique;
	private String prenom;
	private String autresPrenoms;
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
	private Collection<AdoptionReconnaissance> adoptions;
	private Collection<Adresse> adresses;
	private Collection<RelationVersIndividu> enfants;
	private EtatCivilList etatsCivils;
	private List<RelationVersIndividu> parents;
	private List<RelationVersIndividu> conjoints;
	private PermisList permis;
	private Nationalite derniereNationalite;
	private final Set<AttributeIndividu> availableParts = new HashSet<>();

	public static Individu get(Person target, @Nullable List<Relationship> relations, boolean history, boolean withRelations, ServiceInfrastructureRaw infraService) {
		if (target == null) {
			return null;
		}

		return new IndividuRCPers(target, relations, history, withRelations, infraService);
	}

	public IndividuRCPers(Person person, @Nullable List<Relationship> relations, boolean history, boolean withRelations, ServiceInfrastructureRaw infraService) {
		this.noTechnique = getNoIndividu(person);

		final Identity identity = person.getIdentity();
		final PersonIdentification identification = identity.getPersonIdentification();
		final UpiPerson upiPerson = person.getUpiPerson();

		this.prenom = initPrenom(identity.getCallName(), identification.getFirstNames());
		this.autresPrenoms = identification.getFirstNames();
		this.nom = identification.getOfficialName();
		this.nomNaissance = identity.getOriginalName();
		this.noAVS11 = initNumeroAVS11(identification.getOtherPersonId());
		if (upiPerson != null) {
			this.nouveauNoAVS = EchHelper.avs13FromEch(upiPerson.getVn());
		}
		this.numeroRCE = initNumeroRCE(identification.getOtherPersonId());
		this.sexe = EchHelper.sexeFromEch44(identification.getSex());
		this.deces = XmlUtils.xmlcal2regdate(person.getDateOfDeath());
		this.naissance = EchHelper.partialDateFromEch44(identification.getDateOfBirth());
		this.dateArriveeVD = initDateArriveeVD(person.getResidenceHistory());
		this.origines = initOrigins(person);
		this.adoptions = null; // RCPers ne distingue pas les adoptions des filiations

		if (history) {
			this.adresses = initAdresses(null, person.getContactHistory(), person.getResidenceHistory(), infraService);
		}
		else {
			this.adresses = initAdresses(person.getCurrentContact(), null, person.getCurrentResidence(), infraService);
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

		if (relations != null) {
			this.enfants = initEnfants(relations);
			this.parents = initParents(this.naissance, relations);
			this.conjoints = initConjoints(relations);
		}

		if (history) {
			this.permis = initPermis(this.noTechnique, person.getResidencePermitHistory());
		}
		else { // [SIFISC-5181] prise en compte des valeurs courantes
			this.permis = initPermis(this.noTechnique, person.getCurrentResidencePermit());
		}

		this.derniereNationalite = initNationalite(person, infraService);

		// avec RcPers, toutes les parts sont systématiquement retournées, à l'exception des relations qui doivent être demandées explicitement
		Collections.addAll(this.availableParts, AttributeIndividu.values());
		if (!withRelations) {
			this.availableParts.remove(AttributeIndividu.CONJOINTS);
			this.availableParts.remove(AttributeIndividu.ENFANTS);
			this.availableParts.remove(AttributeIndividu.PARENTS);
		}
	}

	public IndividuRCPers(IndividuRCPers right, Set<AttributeIndividu> parts) {
		this.noTechnique = right.noTechnique;
		this.prenom = right.getPrenom();
		this.autresPrenoms = right.getAutresPrenoms();
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
		this.derniereNationalite = right.derniereNationalite;

		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			this.adresses = right.adresses;
		}
		if (parts != null && parts.contains(AttributeIndividu.ADOPTIONS)) {
			adoptions = right.adoptions;
		}
		if (parts != null && parts.contains(AttributeIndividu.CONJOINTS)) {
			conjoints = right.conjoints;
		}
		if (parts != null && parts.contains(AttributeIndividu.ENFANTS)) {
			enfants = right.enfants;
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

		if (parts != null) {
			this.availableParts.addAll(parts);
		}
	}

	public IndividuRCPers(IndividuRCPers right, @NotNull RegDate date) {
		this.noTechnique = right.noTechnique;
		this.prenom = right.getPrenom();
		this.autresPrenoms = right.getAutresPrenoms();
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
		this.derniereNationalite = right.derniereNationalite;

		this.adresses = right.adresses;
		this.adoptions = right.adoptions;
		this.parents = right.parents;
		this.enfants = right.enfants;
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
		if (adoptions != null) {
			adoptions = CollectionLimitator.limit(adoptions, date, CollectionLimitator.ADOPTION_LIMITATOR);
		}
		if (parents != null) {
			parents = CollectionLimitator.limit(parents, date, CollectionLimitator.RELATION_LIMITATOR);
		}
		if (enfants != null) {
			enfants = CollectionLimitator.limit(enfants, date, CollectionLimitator.RELATION_LIMITATOR);
		}
		if (conjoints != null) {
			conjoints = CollectionLimitator.limit(conjoints, date, CollectionLimitator.RELATION_LIMITATOR);
		}
		if (etatsCivils != null) {
			etatsCivils = new EtatCivilListRCPers(CollectionLimitator.limit(etatsCivils, date, CollectionLimitator.ETAT_CIVIL_LIMITATOR));
		}
		if (permis != null) {
			final List<Permis> limited = CollectionLimitator.limit(permis, date, CollectionLimitator.PERMIS_LIMITATOR);
			permis = (limited == null ? null : new PermisListRcPers(permis.getNumeroIndividu(), limited));
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
		return getNoIndividu(person.getIdentity().getPersonIdentification());
	}

	public static long getNoIndividu(PersonIdentification identification) {
		return getNoIndividu(identification.getLocalPersonId());
	}

	private static String initNumeroAVS11(List<NamedPersonId> otherPersonIds) {
		if (otherPersonIds == null) {
			return null;
		}
		String numeroAVS11 = null;
		for (NamedPersonId id : otherPersonIds) {
			if ("CH.AHV".equals(id.getPersonIdCategory())) {
				numeroAVS11 = id.getPersonId();
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

	private static Nationalite initNationalite(Person person, ServiceInfrastructureRaw infraService) {
		if (person == null) {
			return null;
		}
		return NationaliteRCPers.get(person, infraService);
	}

	private static PermisListRcPers initPermis(long numeroIndividu, ResidencePermit permis) {
		if (permis == null) {
			return null;
		}
		return new PermisListRcPers(numeroIndividu, Arrays.asList(PermisRCPers.get(permis)));
	}

	private static PermisListRcPers initPermis(long numeroIndividu, List<ResidencePermit> permis) {
		if (permis == null) {
			return null;
		}
		final List<Permis> list = new ArrayList<>(permis.size());
		for (ResidencePermit p : permis) {
			list.add(PermisRCPers.get(p));
		}
		return new PermisListRcPers(numeroIndividu, list);
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

	protected static List<Adresse> initAdresses(@Nullable MailAddress currentContact, @Nullable List<HistoryContact> contact, List<Residence> residence, ServiceInfrastructureRaw infraService) {

		final List<Adresse> adresses = new ArrayList<>();

		if (contact != null) {
			// l'historique est renseigné
			for (HistoryContact hc : contact) {
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
						res.add(AdresseRCPers.get(r, next, infraService)); // va calculer une date de fin si nécessaire
					}

					adresses.addAll(res);
				}
			}
		}

		// [SIFISC-6604] si l'historique est renseigné, on essaie de remplir les trous d'adresse avec le goesTo de l'adresse de résidence précédente...
		if (contact != null) {
			fillHoles(adresses);
		}

		Collections.sort(adresses, new DateRangeComparator<Adresse>());
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
		List<DateRange> holes = Arrays.asList(life);
		for (Adresse adr : adresses) {
			if (typesPrisEnCompte.contains(adr.getTypeAdresse())) {
				holes = DateRangeHelper.subtract(holes, Arrays.asList(adr), callback);
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

	private static Collection<RelationVersIndividu> initEnfants(List<Relationship> relationship) {
		if (relationship == null || relationship.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<>();
		for (Relationship r : relationship) {
			final TypeRelationVersIndividu type = EvdHelper.typeRelationFromEvd1(r.getTypeOfRelationship());
			if (type != null && type.isEnfant()) {
				final Long numeroInd = getNoIndividu(r.getLocalPersonId());
				final RegDate validFrom = XmlUtils.xmlcal2regdate(r.getRelationValidFrom());
				final RegDate validTill = XmlUtils.xmlcal2regdate(r.getRelationValidTill());
				list.add(new RelationVersIndividuImpl(numeroInd, type, validFrom, validTill));
			}
		}
		return list.isEmpty() ? null : list;
	}

	private static List<RelationVersIndividu> initParents(RegDate dateNaissance, List<Relationship> relationship) {
		if (relationship == null || relationship.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<>();
		for (Relationship r : relationship) {
			final TypeRelationVersIndividu type = EvdHelper.typeRelationFromEvd1(r.getTypeOfRelationship());
			if (type != null && type.isParent()) {
				final Long numeroInd = getNoIndividu(r.getLocalPersonId());
				RegDate validFrom = XmlUtils.xmlcal2regdate(r.getRelationValidFrom());
				if (validFrom == null) {
					// à défaut de mieux, on considère que la relation s'établit à la naissance de l'enfant
					validFrom = dateNaissance;
				}
				final RegDate validTill = XmlUtils.xmlcal2regdate(r.getRelationValidTill());
				list.add(new RelationVersIndividuImpl(numeroInd, type, validFrom, validTill));
			}
		}
		return list.isEmpty() ? null : list;
	}

	private static List<RelationVersIndividu> initConjoints(List<Relationship> relationship) {
		if (relationship == null || relationship.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<>();
		for (Relationship r : relationship) {
			final TypeRelationVersIndividu type = EvdHelper.typeRelationFromEvd1(r.getTypeOfRelationship());
			if (type != null && type.isConjointOuPartenaire()) {
				final Long numeroInd = getNoIndividu(r.getLocalPersonId());
				final RegDate validFrom = XmlUtils.xmlcal2regdate(r.getRelationValidFrom());
				final RegDate validTill = fixRelationEndDate(validFrom, XmlUtils.xmlcal2regdate(r.getRelationValidTill()));
				list.add(new RelationVersIndividuImpl(numeroInd, type, validFrom, validTill));
			}
		}
		return list.isEmpty() ? null : list;
	}

	private static RegDate fixRelationEndDate(RegDate validFrom, RegDate validTill) {
		if (validTill != null) {
			// RcPers étend les relations un jour de plus que ce que faisait Host-Interfaces, on corrige le tir ici (voir SIREF-1588).
			//
			// Note: l'idée derrière cette extension est que, dans les faits, une fin de relation survient en cours de journée. Cela veut
			// dire que la relation est factuellement encore valide le matin, mais ne l'est plus le soir. Mais évidemment, l'heure exacte
			// de cette fin de relation n'est pas connue de RcPers, qui retourne donc simplement le jour où la relation s'est arrêtée.
			// Ce qui peut poser des problèmes d'interprétation dans certains cas : notamment le cas du divorce. Prenons l'exemple
			// d'une personne divorce le 13 juin 2010, dans ce cas-là RcPers exposera :
			//  - un état civil 'divorcé' avec date valeur au 13 juin 2010, ainsi que
			//  - une relation vers son ex-femme avec date de fin le même jour (le 13 juin, donc)
			// Bizarre, mais c'est comme ça que RcPers fonctionne...

			if (validFrom == validTill) {
				// cas ultra-rare de la personne qui se marie et dont le conjoint décède le même jour. D'un point-de-vue civil,
				// cette personne a bien été mariée un jour et acquiert le même jour les états-civils 'marié' et 'veuf'.
				// Dans ces cas-là, on ne décale pas la date de fin de la relation, parce que cela ferait une durée négative.
			}
			else {
				validTill = validTill.getOneDayBefore();
			}
		}
		return validTill;
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

	@Override
	public String getPrenom() {
		return prenom;
	}

	@Override
	public String getAutresPrenoms() {
		return autresPrenoms;
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
	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		return adoptions;
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
	public Collection<RelationVersIndividu> getEnfants() {
		return enfants;
	}

	@Override
	public EtatCivilList getEtatsCivils() {
		return etatsCivils;
	}

	@Override
	public EtatCivil getEtatCivilCourant() {
		if (etatsCivils == null || etatsCivils.isEmpty()) {
			return null;
		}
		return etatsCivils.get(etatsCivils.size() - 1);
	}

	@Override
	public EtatCivil getEtatCivil(RegDate date) {
		return etatsCivils.getEtatCivilAt(date);
	}

	@Override
	public Nationalite getDerniereNationalite() {
		return derniereNationalite;
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
	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			adresses = individu.getAdresses();
		}
		if (parts != null && parts.contains(AttributeIndividu.ADOPTIONS)) {
			adoptions = individu.getAdoptionsReconnaissances();
		}
		if (parts != null && parts.contains(AttributeIndividu.CONJOINTS)) {
			conjoints = individu.getConjoints();
		}
		if (parts != null && parts.contains(AttributeIndividu.ENFANTS)) {
			enfants = individu.getEnfants();
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
