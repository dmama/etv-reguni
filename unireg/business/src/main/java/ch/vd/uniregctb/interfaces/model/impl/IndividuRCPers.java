package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.ech.ech0011.v5.PlaceOfOrigin;
import ch.ech.ech0044.v2.NamedPersonId;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v3.HistoryContact;
import ch.vd.evd0001.v3.Identity;
import ch.vd.evd0001.v3.MaritalData;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.PersonIdentification;
import ch.vd.evd0001.v3.Relations;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0001.v3.Residence;
import ch.vd.evd0001.v3.ResidencePermit;
import ch.vd.evd0001.v3.UpiPerson;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.ech.EchHelper;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.EtatCivilListRCPers;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.PermisList;
import ch.vd.uniregctb.interfaces.model.RelationVersIndividu;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.Sexe;

public class IndividuRCPers implements Individu, Serializable {

	private static final long serialVersionUID = -2444268873580091585L;

	private static final String EST_CONJOINT = "1";
	private static final String EST_PARTENAIRE_ENREGISTRE = "2";
	private static final String EST_MERE = "3";
	private static final String EST_PERE = "4";
	private static final String EST_FILLE = "101";
	private static final String EST_FILS = "102";

	private long noTechnique;
	private String prenom;
	private String autresPrenoms;
	private String nom;
	private String nomNaissance;
	private String noAVS11;
	private String nouveauNoAVS;
	private String numeroRCE;
	private boolean isMasculin;
	private RegDate deces;
	private RegDate naissance;
	private RegDate dateArriveeVD;
	private Collection<Origine> origines;
	private Tutelle tutelle;
	private Collection<AdoptionReconnaissance> adoptions;
	private Collection<Adresse> adresses;
	private Collection<RelationVersIndividu> enfants;
	private EtatCivilList etatsCivils;
	private List<RelationVersIndividu> parents;
	private List<RelationVersIndividu> conjoints;
	private PermisList permis;
	private List<Nationalite> nationalites;
	private final Set<AttributeIndividu> availableParts = new HashSet<AttributeIndividu>();

	public static Individu get(Person target, @Nullable Relations relations, ServiceInfrastructureService infraService) {
		if (target == null) {
			return null;
		}

		return new IndividuRCPers(target, relations != null ? relations.getRelationshipHistory() : null, infraService);
	}
	
	public static Individu get(Person target, @Nullable List<Relationship> relationships, ServiceInfrastructureService infraService) {
		if (target == null) {
			return null;
		}

		return new IndividuRCPers(target, relationships, infraService);
	}
	
	public IndividuRCPers(Person person, @Nullable List<Relationship> relationships, ServiceInfrastructureService infraService) {
		this.noTechnique = getNoIndividu(person);

		final Identity identity = person.getIdentity();
		final PersonIdentification identification = identity.getPersonIdentification();
		final UpiPerson upiPerson = person.getUpiPerson();

		this.prenom = identity.getCallName();
		this.autresPrenoms = identification.getFirstNames();
		this.nom = identification.getOfficialName();
		this.nomNaissance = identity.getOriginalName();
		this.noAVS11 = initNumeroAVS11(identification.getOtherPersonId());
		if (upiPerson != null) {
			this.nouveauNoAVS = EchHelper.avs13FromEch(upiPerson.getVn());
		}
		this.numeroRCE = initNumeroRCE(identification.getOtherPersonId());
		this.isMasculin = initIsMasculin(identification);
		this.deces = XmlUtils.xmlcal2regdate(person.getDateOfDeath());
		this.naissance = EchHelper.partialDateFromEch44(identification.getDateOfBirth());
		this.dateArriveeVD = initDateArriveeVD(person.getResidenceHistory());
		this.origines = initOrigins(person);
		this.tutelle = null;
		this.adoptions = null; // RCPers ne distingue pas les adoptions des filiations
		this.adresses = initAdresses(person.getContactHistory(), person.getResidenceHistory(), infraService);
		try {
			this.etatsCivils = initEtatsCivils(person.getMaritalStatusHistory());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Individu n°" + this.noTechnique + ": " + e.getMessage(), e);
		}
		if (relationships != null) {
			this.enfants = initEnfants(relationships);
			this.parents = initParents(this.naissance, relationships);
			this.conjoints = initConjoints(relationships);
		}
		this.permis = initPermis(this.noTechnique, person.getResidencePermitHistory());
		this.nationalites = initNationalites(person, infraService);

		// avec RcPers, toutes les parts sont systématiquement retournées, à l'exception des relations qui doivent être demandées explicitement
		Collections.addAll(this.availableParts, AttributeIndividu.values());
		if (relationships == null) {
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
		this.isMasculin = right.isMasculin;
		this.deces = right.deces;
		this.naissance = right.naissance;
		this.dateArriveeVD = right.dateArriveeVD;
		this.etatsCivils = right.etatsCivils;

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
		if (parts != null && parts.contains(AttributeIndividu.NATIONALITE)) {
			nationalites = right.nationalites;
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
		if (parts != null && parts.contains(AttributeIndividu.TUTELLE)) {
			tutelle = right.tutelle;
		}

		if (parts != null) {
			this.availableParts.addAll(parts);
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
		return getNoIndividu(person.getIdentity().getPersonIdentification().getLocalPersonId());
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

	private static boolean initIsMasculin(PersonIdentification person) {
		final Sexe sexe = EchHelper.sexeFromEch44(person.getSex());
		return (sexe != Sexe.FEMININ);
	}

	private static List<Nationalite> initNationalites(Person person, ServiceInfrastructureService infraService) {
		if (person == null) { // TODO (rcpers) demander que RCPers expose l'historique des nationalités
			return null;
		}
		final List<Nationalite> list = new ArrayList<Nationalite>();
		list.add(NationaliteRCPers.get(person, infraService));
		return list;
	}

	private static PermisListRcPers initPermis(long numeroIndividu, List<ResidencePermit> permis) {
		if (permis == null) {
			return null;
		}
		final List<Permis> list = new ArrayList<Permis>(permis.size());
		for (ResidencePermit p : permis) {
			list.add(PermisRCPers.get(p));
		}
		return new PermisListRcPers(numeroIndividu, list);
	}

	protected static EtatCivilList initEtatsCivils(List<MaritalData> maritalStatus) {
		if (maritalStatus == null) {
			return null;
		}
		final List<EtatCivil> list = new ArrayList<EtatCivil>();
		for (MaritalData data : maritalStatus) {
			list.addAll(EtatCivilRCPers.get(data));
		}
		return new EtatCivilListRCPers(list);
	}

	private static Collection<Adresse> initAdresses(List<HistoryContact> contact, List<Residence> residence, ServiceInfrastructureService infraService) {
		final List<Adresse> adresses = new ArrayList<Adresse>();
		if (contact != null) {
			for (HistoryContact hc : contact) {
				adresses.add(AdresseRCPers.get(hc, infraService));
			}
		}
		if (residence != null) {

			// [SIREF-1794] les résidences doivent être triées par date d'arrivée croissant, puis par date de déménagement croissant
			Collections.sort(residence, new Comparator<Residence>() {
				@Override
				public int compare(Residence o1, Residence o2) {
					// FIXME (rcpers) il faut distinguer les résidences principales des résidences secondaires ET les groupes (par commune) de résidences secondaires entre-elles
					final RegDate arrivalDate1 = XmlUtils.xmlcal2regdate(o1.getArrivalDate());
					final RegDate arrivalDate2 = XmlUtils.xmlcal2regdate(o2.getArrivalDate());
					int c = NullDateBehavior.EARLIEST.compare(arrivalDate1, arrivalDate2);
					if (c == 0) {
						final RegDate movingDate1 = XmlUtils.xmlcal2regdate(o1.getDwellingAddress().getMovingDate());
						final RegDate movingDate2 = XmlUtils.xmlcal2regdate(o2.getDwellingAddress().getMovingDate());
						c = NullDateBehavior.EARLIEST.compare(movingDate1, movingDate2);
					}
					return c;
				}
			});

			final List<AdresseRCPers> residences = new ArrayList<AdresseRCPers>();
			for (int i = 0, residenceSize = residence.size(); i < residenceSize; i++) {
				final Residence r = residence.get(i);
				final Residence next = i + 1 < residenceSize ? residence.get(i + 1) : null;
				residences.add(AdresseRCPers.get(r, next, infraService));
			}

			adresses.addAll(residences);
		}
		return adresses;
	}

	private static Collection<RelationVersIndividu> initEnfants(List<Relationship> relationship) {
		if (relationship == null || relationship.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<RelationVersIndividu>();
		for (Relationship r : relationship) {
			if (EST_FILLE.equals(r.getTypeOfRelationship()) || EST_FILS.equals(r.getTypeOfRelationship())) {
				final Long numeroInd = getNoIndividu(r.getLocalPersonId());
				final RegDate validFrom = XmlUtils.xmlcal2regdate(r.getRelationValidFrom());
				final RegDate validTill = fixRelationEndDate(validFrom, XmlUtils.xmlcal2regdate(r.getRelationValidTill()));
				list.add(new RelationVersIndividuImpl(numeroInd, validFrom, validTill));
			}
		}
		return list.isEmpty() ? null : list;
	}

	private static List<RelationVersIndividu> initParents(RegDate dateNaissance, List<Relationship> relationship) {
		if (relationship == null || relationship.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<RelationVersIndividu>();
		for (Relationship r : relationship) {
			if (EST_MERE.equals(r.getTypeOfRelationship()) || EST_PERE.equals(r.getTypeOfRelationship())) {
				final Long numeroInd = getNoIndividu(r.getLocalPersonId());
				RegDate validFrom = XmlUtils.xmlcal2regdate(r.getRelationValidFrom());
				if (validFrom == null) {
					// à défaut de mieux, on considère que la relation s'établit à la naissance de l'enfant
					validFrom = dateNaissance;
				}
				final RegDate validTill = fixRelationEndDate(validFrom, XmlUtils.xmlcal2regdate(r.getRelationValidTill()));
				list.add(new RelationVersIndividuImpl(numeroInd, validFrom, validTill));
			}
		}
		return list.isEmpty() ? null : list;
	}

	private static List<RelationVersIndividu> initConjoints(List<Relationship> relationship) {
		if (relationship == null || relationship.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<RelationVersIndividu>();
		for (Relationship r : relationship) {
			if (EST_CONJOINT.equals(r.getTypeOfRelationship()) || EST_PARTENAIRE_ENREGISTRE.equals(r.getTypeOfRelationship())) {
				final Long numeroInd = getNoIndividu(r.getLocalPersonId());
				final RegDate validFrom = XmlUtils.xmlcal2regdate(r.getRelationValidFrom());
				final RegDate validTill = fixRelationEndDate(validFrom, XmlUtils.xmlcal2regdate(r.getRelationValidTill()));
				list.add(new RelationVersIndividuImpl(numeroInd, validFrom, validTill));
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
			final List<Origine> liste = new ArrayList<Origine>(origins.size());
			for (PlaceOfOrigin origin : origins) {
				liste.add(OrigineRCPers.get(origin));
			}
			return liste;
		}
	}

	private static RegDate initDateArriveeVD(List<Residence> residenceHistory) {
		if (residenceHistory == null || residenceHistory.isEmpty()) {
			return null;
		}
		RegDate dateArrivee = RegDate.getLateDate();
		for (Residence residence : residenceHistory) {
			dateArrivee = RegDateHelper.minimum(dateArrivee, XmlUtils.xmlcal2regdate(residence.getArrivalDate()), NullDateBehavior.EARLIEST);
		}
		return dateArrivee == RegDate.getLateDate() ? null : dateArrivee;
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
	public List<Nationalite> getNationalites() {
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
	public Tutelle getTutelle() {
		return tutelle;
	}

	@Override
	public boolean isSexeMasculin() {
		return isMasculin;
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
		if (parts != null && parts.contains(AttributeIndividu.NATIONALITE)) {
			nationalites = individu.getNationalites();
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
		if (parts != null && parts.contains(AttributeIndividu.TUTELLE)) {
			tutelle = individu.getTutelle();
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
	public Set<AttributeIndividu> getAvailableParts() {
		return availableParts;
	}

	@Override
	public Collection<Adresse> getAdresses() {
		return adresses;
	}

	private static class AdresseRCPersComparator implements Comparator<AdresseRCPers> {
		@Override
		public int compare(AdresseRCPers o1, AdresseRCPers o2) {
			return NullDateBehavior.EARLIEST.compare(o1.getDateDebut(), o2.getDateDebut());
		}
	}
}
