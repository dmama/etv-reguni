package ch.vd.unireg.interfaces.civil.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AdoptionReconnaissance;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.EtatCivilList;
import ch.vd.unireg.interfaces.civil.data.EtatCivilListImpl;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividuImpl;
import ch.vd.unireg.interfaces.civil.data.TypeRelationVersIndividu;
import ch.vd.uniregctb.type.Sexe;

public class MockIndividu extends MockEntiteCivile implements Individu {

	private String prenom;
	private String autresPrenoms;
	private String nom;
	private String nomNaissance;
	private Collection<AdoptionReconnaissance> adoptionsReconnaissances;
	private RegDate dateDeces;
	private RegDate dateNaissance;
	private List<RelationVersIndividu> parents;
	private List<RelationVersIndividu> conjoints;
	private Collection<RelationVersIndividu> enfants;
	private EtatCivilList etatsCivils;
	private List<Nationalite> nationalites;
	private long noTechnique;
	private String noAVS11;
	private String nouveauNoAVS;
	private String numeroRCE;
	private Collection<Origine> origines;
	private PermisList permis;
	private Sexe sexe;
	private RegDate dateArriveeVD;
	private final Set<AttributeIndividu> availableParts = new HashSet<>();
	private boolean nonHabitantNonRenvoye = false;

	public MockIndividu() {
		// à priori, toutes les parts *peuvent* être renseignées
		Collections.addAll(this.availableParts, AttributeIndividu.values());
	}

	public MockIndividu(MockIndividu right, @NotNull RegDate upTo) {
		super(right, right.availableParts);
		this.prenom = right.prenom;
		this.autresPrenoms = right.autresPrenoms;
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

		this.adoptionsReconnaissances = right.adoptionsReconnaissances;
		this.parents = right.parents;
		this.enfants = right.enfants;
		this.conjoints = right.conjoints;
		this.etatsCivils = right.etatsCivils;
		this.permis = right.permis;
		limitHistoTo(upTo);

		this.availableParts.addAll(right.availableParts);
	}

	private void limitHistoTo(RegDate date) {

		if (getAdresses() != null) {
			setAdresses(CollectionLimitator.limit(getAdresses(), date, CollectionLimitator.ADRESSE_LIMITATOR));
		}
		if (adoptionsReconnaissances != null) {
			adoptionsReconnaissances = CollectionLimitator.limit(adoptionsReconnaissances, date, CollectionLimitator.ADOPTION_LIMITATOR);
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
			etatsCivils = new FreezableEtatCivilList(CollectionLimitator.limit(etatsCivils, date, CollectionLimitator.ETAT_CIVIL_LIMITATOR), true);
		}
		if (permis != null) {
			final List<Permis> limited = CollectionLimitator.limit(permis, date, CollectionLimitator.PERMIS_LIMITATOR);
			permis = (limited == null ? null : new MockPermisList(limited));
		}
	}

	public MockIndividu(MockIndividu right, Set<AttributeIndividu> parts) {
		super(right, parts);
		this.prenom = right.prenom;
		this.autresPrenoms = right.autresPrenoms;
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

		copyPartsFrom(right, parts);

		if (parts != null) {
			this.availableParts.addAll(parts);
		}
	}

	@Override
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Override
	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	public void setAutresPrenoms(String autresPrenoms) {
		this.autresPrenoms = autresPrenoms;
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
	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		return adoptionsReconnaissances;
	}

	public void setAdoptionsReconnaissances(Collection<AdoptionReconnaissance> adoptionsReconnaissances) {
		this.adoptionsReconnaissances = adoptionsReconnaissances;
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

	public void setParentsFromIndividus(List<Individu> individus) {
		if (individus == null) {
			this.parents = null;
		}
		else {
			final List<RelationVersIndividu> list = new ArrayList<>();
			for (Individu parent : individus) {
				final TypeRelationVersIndividu type = parent.getSexe() == Sexe.MASCULIN ? TypeRelationVersIndividu.PERE : TypeRelationVersIndividu.MERE;
				list.add(new RelationVersIndividuImpl(parent.getNoTechnique(), type, parent.getDateNaissance(), parent.getDateDeces()));
			}
			this.parents = list;
		}
	}

	@Override
	public Collection<RelationVersIndividu> getEnfants() {
		return enfants;
	}

	public void setEnfants(Collection<RelationVersIndividu> enfants) {
		this.enfants = enfants;
	}

	public void setEnfantsFromIndividus(List<Individu> individus) {
		if (individus == null) {
			this.enfants = null;
		}
		else {
			final Collection<RelationVersIndividu> list = new ArrayList<>();
			for (Individu enfant : individus) {
				final TypeRelationVersIndividu type = enfant.getSexe() == Sexe.MASCULIN ? TypeRelationVersIndividu.FILS : TypeRelationVersIndividu.FILLE;
				list.add(new RelationVersIndividuImpl(enfant.getNoTechnique(), type, enfant.getDateNaissance(), enfant.getDateDeces()));
			}
			this.enfants = list;
		}
	}

	@Override
	public List<RelationVersIndividu> getConjoints() {
		return conjoints;
	}

	public void setConjoints(List<RelationVersIndividu> conjoints) {
		this.conjoints = conjoints;
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
		if (etatsCivils == null) {
			return null;
		}
		return etatsCivils.getEtatCivilAt(date);
	}

	public void setEtatsCivils(EtatCivilList etatsCivils) {
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
		this.permis = new MockPermisList(Arrays.asList(permis));
	}

	@Override
	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	public boolean isNonHabitantNonRenvoye() {
		return nonHabitantNonRenvoye;
	}

	public void setNonHabitantNonRenvoye(boolean nonHabitantNonRenvoye) {
		this.nonHabitantNonRenvoye = nonHabitantNonRenvoye;
	}

	@Override
	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		super.copyPartsFrom(individu, parts);

		if (parts != null && parts.contains(AttributeIndividu.ADOPTIONS)) {
			adoptionsReconnaissances = individu.getAdoptionsReconnaissances();
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

	private static final class FreezableEtatCivilList extends EtatCivilListImpl {

		private boolean frozen;

		private class FreezableIterator<T extends Iterator<EtatCivil>> implements Iterator<EtatCivil> {

			protected final T iterator;

			protected FreezableIterator(T iterator) {
				this.iterator = iterator;
			}

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public EtatCivil next() {
				return iterator.next();
			}

			@Override
			public void remove() {
				checkNotFrozen();
				iterator.remove();
			}
		}

		private class FreezableListIterator extends FreezableIterator<ListIterator<EtatCivil>> implements ListIterator<EtatCivil> {

			protected FreezableListIterator(ListIterator<EtatCivil> iterator) {
				super(iterator);
			}

			@Override
			public boolean hasPrevious() {
				return iterator.hasPrevious();
			}

			@Override
			public EtatCivil previous() {
				return iterator.previous();
			}

			@Override
			public int nextIndex() {
				return iterator.nextIndex();
			}

			@Override
			public int previousIndex() {
				return iterator.previousIndex();
			}

			@Override
			public void set(EtatCivil o) {
				checkNotFrozen();
				iterator.set(o);
			}

			@Override
			public void add(EtatCivil o) {
				checkNotFrozen();
				iterator.add(o);
			}
		}

		public FreezableEtatCivilList(Collection<EtatCivil> listHost, boolean frozen) {
			super(listHost);
			this.frozen = frozen;
		}

		public void freeze() {
			frozen = true;
		}

		public boolean isFrozen() {
			return frozen;
		}

		private void checkNotFrozen() {
			if (frozen) {
				throw new UnsupportedOperationException("List is frozen!");
			}
		}

		@Override
		public boolean add(EtatCivil o) {
			checkNotFrozen();
			return super.add(o);
		}

		@Override
		public void add(int index, EtatCivil element) {
			checkNotFrozen();
			super.add(index, element);
		}

		@Override
		public boolean addAll(Collection<? extends EtatCivil> c) {
			checkNotFrozen();
			return super.addAll(c);
		}

		@Override
		public boolean addAll(int index, Collection<? extends EtatCivil> c) {
			checkNotFrozen();
			return super.addAll(index, c);
		}

		@Override
		public void clear() {
			checkNotFrozen();
			super.clear();
		}

		@Override
		public boolean remove(Object o) {
			checkNotFrozen();
			return super.remove(o);
		}

		@Override
		public EtatCivil remove(int index) {
			checkNotFrozen();
			return super.remove(index);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			checkNotFrozen();
			return super.removeAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			checkNotFrozen();
			return super.retainAll(c);
		}

		@Override
		public EtatCivil set(int index, EtatCivil element) {
			checkNotFrozen();
			return super.set(index, element);
		}

		@Override
		public Iterator<EtatCivil> iterator() {
			return new FreezableIterator<>(super.iterator());
		}

		@Override
		public ListIterator<EtatCivil> listIterator() {
			return new FreezableListIterator(super.listIterator());
		}

		@Override
		public ListIterator<EtatCivil> listIterator(int index) {
			return new FreezableListIterator(super.listIterator(index));
		}
	}
}
