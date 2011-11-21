package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.ech.ech0011.v5.MaritalData;
import ch.ech.ech0011.v5.PlaceOfOrigin;
import ch.ech.ech0044.v2.NamedPersonId;
import ch.ech.ech0084.v1.PersonInformation;

import ch.vd.evd0001.v3.HistoryContact;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.PersonIdentification;
import ch.vd.evd0001.v3.Residence;
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
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.helper.IndividuHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.Sexe;

public class IndividuRCPers implements Individu, Serializable {

	private static final long serialVersionUID = -2609344381248103504L;

	private long noTechnique;
	private String nouveauNoAVS;
	private String numeroRCE;
	private boolean isMasculin;
	private RegDate deces;
	private RegDate naissance;
	private Collection<Origine> origines;
	private Tutelle tutelle;
	private Collection<AdoptionReconnaissance> adoptions;
	private Collection<Adresse> adresses;
	private Collection<Individu> enfants;
	private EtatCivilListImpl etatsCivils;
	private List<HistoriqueIndividu> historique;
	private List<Individu> parents;
	private List<Permis> permis;
	private List<Nationalite> nationalites;

	public static Individu get(Person target, ServiceInfrastructureService infraService) {
		if (target == null) {
			return null;
		}

		return new IndividuRCPers(target, infraService);
	}

	public IndividuRCPers(Person person, ServiceInfrastructureService infraService) {
		final PersonInformation personInformation = person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson();
		this.noTechnique = Long.parseLong(person.getIdentity().getPersonIdentification().getLocalPersonId().getPersonId());
		this.nouveauNoAVS = String.valueOf(person.getUpiPerson().getVn());
		this.numeroRCE = initNumeroRCE(person.getIdentity().getPersonIdentification().getOtherPersonId());
		this.isMasculin = initIsMasculin(personInformation);
		this.deces = XmlUtils.xmlcal2regdate(person.getDateOfDeath());
		this.naissance = EchHelper.partialDateFromEch44(personInformation.getDateOfBirth());
		this.origines = initOrigins(person);
		this.tutelle = null;
		this.adoptions = null; // TODO (rcpers)
		this.adresses = initAdresses(person.getContactHistory(), person.getResidencesHistory(), infraService);
		this.enfants = null; // TODO (rcpers)
		this.etatsCivils = initEtatsCivils(person.getMaritalStatusHistory());
		this.historique = initHistorique(person);
		this.parents = initParents(person.getIdentity().getParent());
		this.permis = initPermis(person);
		this.nationalites = initNationalites(person, infraService);
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

	private static boolean initIsMasculin(PersonInformation person) {
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

	private static List<Permis> initPermis(Person person) {
		if (person == null) { // TODO (rcpers) demander que RCPers expose l'historique des permis
			return null;
		}
		final List<Permis> list = new ArrayList<Permis>();
		list.add(PermisRCPers.get(person.getResidencePermit()));
		return list;
	}

	private static List<HistoriqueIndividu> initHistorique(Person person) {
		if (person == null) { // TODO (msi) demander que RCPers expose l'historique des individus
			return null;
		}
		final List<HistoriqueIndividu> list = new ArrayList<HistoriqueIndividu>();
		list.add(HistoriqueIndividuRCPers.get(person));
		return list;
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

	private static List<Individu> initParents(List<PersonIdentification> parentIds) {
		if (parentIds == null || parentIds.isEmpty()) {
			return null;
		}
		return null;
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
	public HistoriqueIndividu getDernierHistoriqueIndividu() {
		return historique == null || historique.isEmpty() ? null : historique.get(historique.size() - 1);
	}

	@Override
	public List<Individu> getParents() {
		return parents;
	}

	@Override
	public Collection<Individu> getEnfants() {
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
	public Collection<HistoriqueIndividu> getHistoriqueIndividu() {
		return historique;
	}

	@Override
	public HistoriqueIndividu getHistoriqueIndividuAt(RegDate date) {
		HistoriqueIndividu candidat = null;
		for (HistoriqueIndividu histo : historique) {
			final RegDate dateDebut = histo.getDateDebutValidite();
			if (dateDebut == null || date == null || dateDebut.isBeforeOrEqual(date)) {
				candidat = histo;
			}
		}
		return candidat;
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
	public List<Permis> getPermis() {
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
	public Permis getPermisActif(RegDate date) {
		return IndividuHelper.getPermisActif(this, date);
	}

	@Override
	public Collection<Adresse> getAdresses() {
		return adresses;
	}
}
