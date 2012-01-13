package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.ech.ech0011.v5.MaritalData;
import ch.ech.ech0011.v5.PlaceOfOrigin;
import ch.ech.ech0044.v2.NamedPersonId;

import ch.vd.evd0001.v3.HistoryContact;
import ch.vd.evd0001.v3.Identity;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.PersonIdentification;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0001.v3.Residence;
import ch.vd.evd0001.v3.UpiPerson;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.ech.EchHelper;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.EtatCivilListImpl;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.RelationVersIndividu;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.Sexe;

public class IndividuRCPers implements Individu, Serializable {

	private static final long serialVersionUID = -2609344381248103504L;

	private static final String EST_FILLE = "101";
	private static final String EST_FILS = "102";
	private static final String EST_MERE = "3";
	private static final String EST_PERE = "4";

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
	private Collection<Origine> origines;
	private Tutelle tutelle;
	private Collection<AdoptionReconnaissance> adoptions;
	private Collection<Adresse> adresses;
	private Collection<RelationVersIndividu> enfants;
	private EtatCivilListImpl etatsCivils;
	private List<RelationVersIndividu> parents;
	private Permis permis;
	private List<Nationalite> nationalites;

	public static Individu get(Person target, ServiceInfrastructureService infraService) {
		if (target == null) {
			return null;
		}

		return new IndividuRCPers(target, infraService);
	}

	public IndividuRCPers(Person person, ServiceInfrastructureService infraService) {
		final Identity identity = person.getIdentity();
		final PersonIdentification identification = identity.getPersonIdentification();
		final UpiPerson upiPerson = person.getUpiPerson();

		this.noTechnique = getNoIndividu(identification.getLocalPersonId());
		this.prenom = identity.getCallName();
		this.autresPrenoms = identification.getFirstNames();
		this.nom = identification.getOfficialName();
		this.nomNaissance = identity.getOriginalName();
		if (upiPerson != null) {
			this.noAVS11 = EchHelper.avs13FromEch(upiPerson.getVn());
			this.nouveauNoAVS = String.valueOf(upiPerson.getVn());
		}
		this.numeroRCE = initNumeroRCE(identification.getOtherPersonId());
		this.isMasculin = initIsMasculin(identification);
		this.deces = XmlUtils.xmlcal2regdate(person.getDateOfDeath());
		this.naissance = EchHelper.partialDateFromEch44(identification.getDateOfBirth());
		this.origines = initOrigins(person);
		this.tutelle = null;
		this.adoptions = null; // RCPers ne distingue pas les adoptions des filiations
		this.adresses = initAdresses(person.getContactHistory(), person.getResidenceHistory(), infraService);
		this.enfants = initEnfants(person.getRelationshipHistory());
		this.etatsCivils = initEtatsCivils(person.getMaritalStatusHistory());
		this.parents = initParents(person.getRelationshipHistory());
		this.permis = initPermis(person);
		this.nationalites = initNationalites(person, infraService);
	}

	private static long getNoIndividu(NamedPersonId personId) {
		return Long.parseLong(personId.getPersonId());
	}

	private static String initNumeroRCE(List<NamedPersonId> otherPersonIds) {
		if (otherPersonIds == null) {
			return null;
		}
		String numeroRCE = null;
		for (NamedPersonId id : otherPersonIds) {
			if ("CH.ZAR".equals(id.getPersonIdCategory())) {
				numeroRCE = id.getPersonId();
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

	private static Permis initPermis(Person person) {
		return PermisRCPers.get(person.getResidencePermit());
	}

	private static EtatCivilListImpl initEtatsCivils(List<MaritalData> maritalStatus) {
		if (maritalStatus == null) {
			return null;
		}
		final List<EtatCivil> list = new ArrayList<EtatCivil>();
		for (MaritalData data : maritalStatus) {
			list.add(EtatCivilRCPers.get(data));
		}
		return new EtatCivilListImpl(list);
	}

	private static Collection<Adresse> initAdresses(List<HistoryContact> contact, List<Residence> residence, ServiceInfrastructureService infraService) {
		final List<Adresse> adresses = new ArrayList<Adresse>();
		if (contact != null) {
			for (HistoryContact hc : contact) {
				adresses.add(AdresseRCPers.get(hc, infraService));
			}
		}
		if (residence != null) {
			for (Residence r : residence) {
				adresses.add(AdresseRCPers.get(r, infraService));
			}
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
				final RegDate validTill = XmlUtils.xmlcal2regdate(r.getRelationValidTill());
				list.add(new RelationVersIndividuImpl(numeroInd, validFrom, validTill));
			}
		}
		return list;
	}

	private static List<RelationVersIndividu> initParents(List<Relationship> relationship) {
		if (relationship == null || relationship.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<RelationVersIndividu>();
		for (Relationship r : relationship) {
			if (EST_MERE.equals(r.getTypeOfRelationship()) || EST_PERE.equals(r.getTypeOfRelationship())) {
				final Long numeroInd = getNoIndividu(r.getLocalPersonId());
				final RegDate validFrom = XmlUtils.xmlcal2regdate(r.getRelationValidFrom());
				final RegDate validTill = XmlUtils.xmlcal2regdate(r.getRelationValidTill());
				list.add(new RelationVersIndividuImpl(numeroInd, validFrom, validTill));
			}
		}
		return list;
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
	public boolean isMineur(RegDate date) {
		return naissance != null && naissance.addYears(18).compareTo(date) > 0;
	}

	@Override
	public List<RelationVersIndividu> getParents() {
		return parents;
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
	public Permis getPermis() {
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
		throw new NotImplementedException();
	}

	@Override
	public Individu clone(Set<AttributeIndividu> parts) {
		throw new NotImplementedException();
	}

	@Override
	public Collection<Adresse> getAdresses() {
		return adresses;
	}
}
