package ch.vd.unireg.interfaces.civil.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.data.PermisListImpl;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.StatutIndividu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.type.Sexe;

public class MockIndividu implements Individu {

	private StatutIndividu statut;
	private String prenomUsuel;
	private String tousPrenoms;
	private String nom;
	private String nomNaissance;
	private RegDate dateDeces;
	private RegDate dateNaissance;
	private List<RelationVersIndividu> parents;
	private List<RelationVersIndividu> conjoints;
	private MockEtatCivilList etatsCivils;
	private List<Nationalite> nationalites;
	private List<MockAdresse> adresses;
	private long noTechnique;
	private String noAVS11;
	private String nouveauNoAVS;
	private String numeroRCE;
	private Collection<Origine> origines;
	private PermisList permis;
	private Sexe sexe;
	private RegDate dateArriveeVD;
	private final Set<AttributeIndividu> availableParts = EnumSet.noneOf(AttributeIndividu.class);
	private boolean nonHabitantNonRenvoye = false;
	private NomPrenom nomOfficielMere;
	private NomPrenom nomOfficielPere;

	public MockIndividu() {
		// à priori, toutes les parts *peuvent* être renseignées
		Collections.addAll(this.availableParts, AttributeIndividu.values());
		statut = StatutIndividu.active();
	}

	public MockIndividu(MockIndividu right, @NotNull RegDate upTo) {
		this.statut = right.statut;
		this.prenomUsuel = right.prenomUsuel;
		this.tousPrenoms = right.tousPrenoms;
		this.nom = right.nom;
		this.nomNaissance = right.nomNaissance;
		this.dateDeces = right.dateDeces;
		this.dateNaissance = right.dateNaissance;
		this.dateArriveeVD = right.dateArriveeVD;
		this.etatsCivils = right.etatsCivils;
		this.conjoints = right.conjoints;
		this.noTechnique = right.noTechnique;
		this.noAVS11 = right.noAVS11;
		this.nouveauNoAVS = right.nouveauNoAVS;
		this.numeroRCE = right.numeroRCE;
		this.origines = right.origines;
		this.sexe = right.sexe;
		this.nationalites = right.nationalites;
		this.nomOfficielMere = right.nomOfficielMere;
		this.nomOfficielPere = right.nomOfficielPere;

		this.parents = right.parents;
		this.conjoints = right.conjoints;
		this.etatsCivils = right.etatsCivils;
		this.permis = right.permis;
		this.adresses = deepCopy(right.adresses);

		limitHistoTo(upTo);

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
			etatsCivils = new MockEtatCivilList(CollectionLimitator.limit(etatsCivils.asList(), date, CollectionLimitator.ETAT_CIVIL_LIMITATOR), true);
		}
		if (permis != null) {
			final List<Permis> limited = CollectionLimitator.limit(permis, date, CollectionLimitator.PERMIS_LIMITATOR);
			permis = (limited == null ? null : new PermisListImpl(limited));
		}
	}

	public MockIndividu(MockIndividu right, Set<AttributeIndividu> parts) {
		this.statut = right.statut;
		this.prenomUsuel = right.prenomUsuel;
		this.tousPrenoms = right.tousPrenoms;
		this.nom = right.nom;
		this.nomNaissance = right.nomNaissance;
		this.dateDeces = right.dateDeces;
		this.dateNaissance = right.dateNaissance;
		this.dateArriveeVD = right.dateArriveeVD;
		this.etatsCivils = right.etatsCivils;
		this.conjoints = right.conjoints;
		this.noTechnique = right.noTechnique;
		this.noAVS11 = right.noAVS11;
		this.nouveauNoAVS = right.nouveauNoAVS;
		this.numeroRCE = right.numeroRCE;
		this.sexe = right.sexe;
		this.nationalites = right.nationalites;
		this.nomOfficielMere = right.nomOfficielMere;
		this.nomOfficielPere = right.nomOfficielPere;

		copyPartsFrom(right, parts);

		if (parts != null) {
			this.availableParts.addAll(parts);
		}
	}

	@Override
	public StatutIndividu getStatut() {
		return statut;
	}

	public void setStatut(StatutIndividu statut) {
		Assert.notNull(statut);
		this.statut = statut;
	}

	@Override
	public String getPrenomUsuel() {
		return prenomUsuel;
	}

	public void setPrenomUsuel(String prenomUsuel) {
		this.prenomUsuel = prenomUsuel;
	}

	@Override
	public String getTousPrenoms() {
		return tousPrenoms;
	}

	public void setTousPrenoms(String tousPrenoms) {
		this.tousPrenoms = tousPrenoms;
	}

	@Override
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Override
	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	@Override
	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

	@Override
	public RegDate getDateNaissance() {
		return dateNaissance;
	}

    public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	@Override
	public RegDate getDateArriveeVD() {
		return dateArriveeVD;
	}

	public void setDateArriveeVD(RegDate dateArriveeVD) {
		this.dateArriveeVD = dateArriveeVD;
	}

	@Override
	public boolean isMineur(RegDate date) {
		return dateNaissance != null && dateNaissance.addYears(18).compareTo(date) > 0;
	}

	@Override
	public List<RelationVersIndividu> getParents() {
		return parents;
	}

	public void setParents(List<RelationVersIndividu> parents) {
		this.parents = parents;
	}

	@Override
	public List<RelationVersIndividu> getConjoints() {
		return conjoints;
	}

	public void setConjoints(List<RelationVersIndividu> conjoints) {
		this.conjoints = conjoints;
	}

	@Override
	public MockEtatCivilList getEtatsCivils() {
		return etatsCivils;
	}

	@Override
	public EtatCivil getEtatCivilCourant() {
		return getEtatCivil(null);
	}

	@Override
	public EtatCivil getEtatCivil(RegDate date) {
		if (etatsCivils == null) {
			return null;
		}
		return etatsCivils.getEtatCivilAt(date);
	}

	public void setEtatsCivils(MockEtatCivilList etatsCivils) {
		this.etatsCivils = etatsCivils;
	}

	public void addNationalite(Nationalite nationalite) {
		this.nationalites.add(nationalite);
	}

	public List<Nationalite> getNationalites() {
		return nationalites;
	}

	public void setNationalites(List<Nationalite> nationalites) {
		this.nationalites = nationalites;
	}

	@Override
	public long getNoTechnique() {
		return noTechnique;
	}

	public void setNoTechnique(long noTechnique) {
		this.noTechnique = noTechnique;
	}

	@Override
	public String getNoAVS11() {
		return noAVS11;
	}

	public void setNoAVS11(String noAVS11) {
		this.noAVS11 = noAVS11;
	}

	@Override
	public String getNouveauNoAVS() {
		return nouveauNoAVS;
	}

	public void setNouveauNoAVS(String nouveauNoAVS) {
		this.nouveauNoAVS = nouveauNoAVS;
	}

	@Override
	public String getNumeroRCE() {
		return numeroRCE;
	}

	public void setNumeroRCE(String numeroRCE) {
		this.numeroRCE = numeroRCE;
	}

	@Override
	public NomPrenom getNomOfficielMere() {
		return nomOfficielMere;
	}

	public void setNomOfficielMere(NomPrenom nomOfficielMere) {
		this.nomOfficielMere = nomOfficielMere;
	}

	@Override
	public NomPrenom getNomOfficielPere() {
		return nomOfficielPere;
	}

	public void setNomOfficielPere(NomPrenom nomOfficielPere) {
		this.nomOfficielPere = nomOfficielPere;
	}

	@Override
	public Collection<Origine> getOrigines() {
		return origines;
	}

	public void setOrigines(Collection<Origine> origines) {
		this.origines = origines;
	}

	@Override
	public PermisList getPermis() {
		return permis;
	}

	public void setPermis(PermisList permis) {
		this.permis = permis;
	}

	public void setPermis(Permis... permis) {
		this.permis = new PermisListImpl(Arrays.asList(permis));
	}

	@Override
	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	/**
	 * Collection en lecture seule !!!
	 * -> utiliser {@link #addAdresse} si on veut en rajouter une
	 */
	@Override
	public Collection<Adresse> getAdresses() {
		if (adresses == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(adresses);
	}

	public void addAdresse(MockAdresse adresse) {
		if (adresses == null ) {
			adresses = new ArrayList<>();
		}
		adresses.add(adresse);
	}

	public boolean isNonHabitantNonRenvoye() {
		return nonHabitantNonRenvoye;
	}

	public void setNonHabitantNonRenvoye(boolean nonHabitantNonRenvoye) {
		this.nonHabitantNonRenvoye = nonHabitantNonRenvoye;
	}

	@Override
	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		final MockIndividu source = (MockIndividu) individu;
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			adresses = deepCopy(source.adresses);
		}
		if (parts != null && parts.contains(AttributeIndividu.CONJOINTS)) {
			conjoints = source.getConjoints();
		}
		if (parts != null && parts.contains(AttributeIndividu.ORIGINE)) {
			origines = source.getOrigines();
		}
		if (parts != null && parts.contains(AttributeIndividu.PARENTS)) {
			parents = source.getParents();
		}
		if (parts != null && parts.contains(AttributeIndividu.PERMIS)) {
			permis = source.getPermis();
		}
	}

	private static <T extends Duplicable<? extends T>> List<T> deepCopy(List<T> original) {
		if (original == null) {
			return null;
		}

		final List<T> newCollection = new ArrayList<>(original.size());
		for (T elt : original) {
			newCollection.add(elt.duplicate());
		}
		return newCollection;
	}

	@Override
	public MockIndividu clone(Set<AttributeIndividu> parts) {
		return new MockIndividu(this, parts);
	}

	@Override
	public Set<AttributeIndividu> getAvailableParts() {
		return availableParts;
	}

	@Override
	public Individu cloneUpTo(@NotNull RegDate date) {
		return new MockIndividu(this, date);
	}
}
