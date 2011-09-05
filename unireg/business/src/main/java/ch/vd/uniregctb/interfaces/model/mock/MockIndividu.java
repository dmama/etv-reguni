package ch.vd.uniregctb.interfaces.model.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
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

public class MockIndividu extends MockEntiteCivile implements Individu {

	private Collection<AdoptionReconnaissance> adoptionsReconnaissances;
	private MockIndividu conjoint;
	private RegDate dateDeces;
	private RegDate dateNaissance;
	private HistoriqueIndividu dernierHistoriqueIndividu;
	private List<Individu> parents;
	private Collection<Individu> enfants;
	private EtatCivilList etatsCivils;
	private Collection<HistoriqueIndividu> historiqueIndividu;
	private List<Nationalite> nationalites;
	private long noTechnique;
	private String nouveauNoAVS;
	private String numeroRCE;
	private Origine origine;
	private List<Permis> permis;
	private Tutelle tutelle;
	private boolean sexeMasculin;

	public MockIndividu() {
	}

	public MockIndividu(MockIndividu right, Set<AttributeIndividu> parts, int annee) {
		super(right, parts);
		this.dateDeces = right.dateDeces;
		this.dateNaissance = right.dateNaissance;
		this.dernierHistoriqueIndividu = right.dernierHistoriqueIndividu;
		this.etatsCivils = right.etatsCivils;
		this.historiqueIndividu = right.historiqueIndividu;
		this.noTechnique = right.noTechnique;
		this.nouveauNoAVS = right.nouveauNoAVS;
		this.numeroRCE = right.numeroRCE;
		this.sexeMasculin = right.sexeMasculin;

		copyPartsFrom(right, parts);
		limitPartsToBeforeYear(annee, parts);
	}

	@Override
	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		return adoptionsReconnaissances;
	}

	public void setAdoptionsReconnaissances(Collection<AdoptionReconnaissance> adoptionsReconnaissances) {
		this.adoptionsReconnaissances = adoptionsReconnaissances;
	}

	public MockIndividu getConjoint() {
		return conjoint;
	}

	public void setConjoint(MockIndividu conjoint) {
		this.conjoint = conjoint;
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
	public boolean isMineur(RegDate date) {
		return dateNaissance != null && dateNaissance.addYears(18).compareTo(date) > 0;
	}

	@Override
	public HistoriqueIndividu getDernierHistoriqueIndividu() {
		return dernierHistoriqueIndividu;
	}

	public void setDernierHistoriqueIndividu(HistoriqueIndividu dernierHistoriqueIndividu) {
		this.dernierHistoriqueIndividu = dernierHistoriqueIndividu;
	}

	@Override
	public List<Individu> getParents() {
		return parents;
	}

	public void setParents(List<Individu> parents) {
		this.parents = parents;
	}

	@Override
	public Collection<Individu> getEnfants() {
		return enfants;
	}

	public void setEnfants(Collection<Individu> enfants) {
		this.enfants = enfants;
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

	@Override
	public Collection<HistoriqueIndividu> getHistoriqueIndividu() {
		return historiqueIndividu;
	}

	@Override
	public HistoriqueIndividu getHistoriqueIndividuAt(RegDate date) {
		if (historiqueIndividu == null || historiqueIndividu.isEmpty()) {
			return null;
		}
		HistoriqueIndividu candidat = null;
		for (HistoriqueIndividu histo : historiqueIndividu) {
			final RegDate dateDebut = histo.getDateDebutValidite();
			if (dateDebut == null || date == null || dateDebut.isBeforeOrEqual(date)) {
				candidat = histo;
			}
		}
		return candidat;
	}

	public void setHistoriqueIndividu(Collection<HistoriqueIndividu> historiqueIndividu) {
		this.historiqueIndividu = historiqueIndividu;
	}

	/**
	 * Ajoute un historique à la liste, et défini cet historique comme le dernier.
	 */
	public void addHistoriqueIndividu(HistoriqueIndividu h) {
		if (historiqueIndividu == null) {
			historiqueIndividu = new ArrayList<HistoriqueIndividu>();
		}
		historiqueIndividu.add(h);
		dernierHistoriqueIndividu = h;
	}

	@Override
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
	public Origine getOrigine() {
		return origine;
	}

	public void setOrigine(Origine origine) {
		this.origine = origine;
	}

	@Override
	public List<Permis> getPermis() {
		return permis;
	}

	public void setPermis(List<Permis> permis) {
		this.permis = permis;
		if (this.permis != null) {
			Collections.sort(this.permis, new Comparator<Permis>() {
				@Override
				public int compare(Permis o1, Permis o2) {
					return o1.getDateDebutValidite().compareTo(o2.getDateDebutValidite());
				}
			});
		}
	}

	@Override
	public Tutelle getTutelle() {
		return tutelle;
	}

	public void setTutelle(Tutelle tutelle) {
		this.tutelle = tutelle;
	}

	@Override
	public boolean isSexeMasculin() {
		return sexeMasculin;
	}

	public void setSexeMasculin(boolean sexeMasculin) {
		this.sexeMasculin = sexeMasculin;
	}

	@Override
	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		super.copyPartsFrom(individu, parts);

		if (parts != null && parts.contains(AttributeIndividu.ADOPTIONS)) {
			adoptionsReconnaissances = individu.getAdoptionsReconnaissances();
		}
		if (parts != null && parts.contains(AttributeIndividu.CONJOINT)) {
			conjoint = ((MockIndividu) individu).getConjoint();
		}
		if (parts != null && parts.contains(AttributeIndividu.ENFANTS)) {
			enfants = individu.getEnfants();
		}
		if (parts != null && parts.contains(AttributeIndividu.NATIONALITE)) {
			nationalites = individu.getNationalites();
		}
		if (parts != null && parts.contains(AttributeIndividu.ORIGINE)) {
			origine = individu.getOrigine();
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
	}

	@Override
	public MockIndividu clone(Set<AttributeIndividu> parts) {
		return cloneUntil(parts, RegDate.getLateDate().year());
	}

	public MockIndividu cloneUntil(Set<AttributeIndividu> parts, int annee) {
		return new MockIndividu(this, parts, annee);
	}

	@Override
	public Permis getPermisActif(RegDate date) {
		return IndividuHelper.getPermisActif(this, date);
	}

	private static interface Limitator<T> {
		boolean keep(T element, int annee);
	}

	private static final Limitator<AdoptionReconnaissance> ADOPTION_LIMITATOR = new Limitator<AdoptionReconnaissance>() {
		@Override
		public boolean keep(AdoptionReconnaissance element, int annee) {
			return (element.getDateAdoption() != null && element.getDateAdoption().year() <= annee)
					|| (element.getDateReconnaissance() != null && element.getDateReconnaissance().year() <= annee);
		}
	};
	private static final Limitator<Individu> ENFANT_LIMITATOR = new Limitator<Individu>() {
		@Override
		public boolean keep(Individu element, int annee) {
			return element.getDateNaissance() == null || element.getDateNaissance().year() <= annee;
		}
	};
	private static final Limitator<HistoriqueIndividu> HISTORIQUE_LIMITATOR = new Limitator<HistoriqueIndividu>() {
		@Override
		public boolean keep(HistoriqueIndividu element, int annee) {
			return element.getDateDebutValidite() == null || element.getDateDebutValidite().year() <= annee;
		}
	};
	private static final Limitator<Nationalite> NATIONALITE_LIMITATOR = new Limitator<Nationalite>() {
		@Override
		public boolean keep(Nationalite element, int annee) {
			return element.getDateDebutValidite() == null || element.getDateDebutValidite().year() <= annee;
		}
	};
	private static final Limitator<Permis> PERMIS_LIMITATOR = new Limitator<Permis>() {
		@Override
		public boolean keep(Permis element, int annee) {
			return element.getDateDebutValidite() == null || element.getDateDebutValidite().year() <= annee;
		}
	};
	private static final Limitator<Adresse> ADRESSE_LIMITATOR = new Limitator<Adresse>() {
		@Override
		public boolean keep(Adresse element, int annee) {
			return element.getDateDebut() == null || element.getDateDebut().year() <= annee;
		}
	};

	private static <T> List<T> buildLimitedCollectionBeforeYear(Collection<T> original, int annee, Limitator<T> limitator) {
		if (original == null) {
			return null;
		}
		else if (original.size() == 0) {
			return Collections.emptyList();
		}
		else {
			final List<T> limited = new ArrayList<T>(original);
			final Iterator<T> iter = limited.iterator();
			while (iter.hasNext()) {
				final T element = iter.next();
				if (!limitator.keep(element, annee)) {
					iter.remove();
				}
			}
			return Collections.unmodifiableList(limited.size() == original.size() ? new ArrayList<T>(original) : limited);
		}
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
			return new FreezableIterator<Iterator<EtatCivil>>(super.iterator());
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

	private void limitPartsToBeforeYear(int annee, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADOPTIONS)) {
			adoptionsReconnaissances = buildLimitedCollectionBeforeYear(adoptionsReconnaissances, annee, ADOPTION_LIMITATOR);
		}
		if (parts != null && parts.contains(AttributeIndividu.ENFANTS)) {
			enfants = buildLimitedCollectionBeforeYear(enfants, annee, ENFANT_LIMITATOR);
		}
		if (etatsCivils != null && etatsCivils.size() > 0) {
			final FreezableEtatCivilList etatsCivilsTemp = new FreezableEtatCivilList(etatsCivils, false);
			final Iterator<EtatCivil> iterator = etatsCivilsTemp.iterator();
			while (iterator.hasNext()) {
				final EtatCivil etatCivil = iterator.next();
				if (etatCivil.getDateDebutValidite() != null && etatCivil.getDateDebutValidite().year() > annee) {
					iterator.remove();
				}
			}
			etatsCivilsTemp.freeze();
			etatsCivils = etatsCivilsTemp;
		}
		historiqueIndividu = buildLimitedCollectionBeforeYear(historiqueIndividu, annee, HISTORIQUE_LIMITATOR);
		dernierHistoriqueIndividu = historiqueIndividu == null || historiqueIndividu.size() == 0 ? null : (HistoriqueIndividu) historiqueIndividu.toArray()[historiqueIndividu.size() - 1];

		if (parts != null && parts.contains(AttributeIndividu.NATIONALITE)) {
			nationalites = buildLimitedCollectionBeforeYear(nationalites, annee, NATIONALITE_LIMITATOR);
		}
		if (parts != null && parts.contains(AttributeIndividu.PERMIS)) {
			permis = buildLimitedCollectionBeforeYear(permis, annee, PERMIS_LIMITATOR);
		}
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			setAdresses(buildLimitedCollectionBeforeYear(getAdresses(), annee, ADRESSE_LIMITATOR));
		}
	}
}
