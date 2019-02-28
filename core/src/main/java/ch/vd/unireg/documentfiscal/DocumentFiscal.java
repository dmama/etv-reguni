package ch.vd.unireg.documentfiscal;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

@Entity
@Table(name = "DOCUMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DOCUMENT_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class DocumentFiscal extends HibernateEntity implements LinkedEntity {

	private Long id;
	private Tiers tiers;
	private Set<DelaiDocumentFiscal> delais; // Ex: Echeances
	private Set<EtatDocumentFiscal> etats; // Ex: Envois
	private Set<LiberationDocumentFiscal> liberations;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long theId) {
		this.id = theId;
	}


	/**
	 * @return the tiers
	 */
	@ManyToOne
	// msi: pas de cascade, parce qu'on veut pouvoir ajouter une déclaration à un tiers sans automatiquement modifier celui-ci (perfs)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@Index(name = "IDX_DOCFISC_TRS_ID", columnNames = "TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	/**
	 * @param theTiers the tiers to set
	 */
	public void setTiers(Tiers theTiers) {
		tiers = theTiers;
	}

	/**
	 * @return the delais
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_DEL_DOCFISC_DOCFISC_ID")
	public Set<DelaiDocumentFiscal> getDelais() {
		return delais;
	}

	/**
	 * @param theDelais the delais to set
	 */
	public void setDelais(Set<DelaiDocumentFiscal> theDelais) {
		delais = theDelais;
	}

	/**
	 * @return la liste des délais triés par ordre croissant (du plus ancien ou plus récent).
	 * @see EtatDocumentFiscal.Comparator
	 */
	@Transient
	public List<DelaiDocumentFiscal> getDelaisSorted() {

		if (getDelais() == null) {
			return null;
		}

		// tri par ordre croissant
		final List<DelaiDocumentFiscal> list = new ArrayList<>(getDelais());
		list.sort(new DelaiDocumentFiscal.Comparator());

		return list;
	}

	/**
	 * @return les liberations
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_LIB_DOCFISC_DOCFISC_ID")
	public Set<LiberationDocumentFiscal> getLiberations() {
		return liberations;
	}

	public void setLiberations(Set<LiberationDocumentFiscal> liberations) {
		this.liberations = liberations;
	}

	/**
	 * @return la liste des états triés par ordre croissant (du plus ancien ou plus récent).
	 * @see EtatDocumentFiscal.Comparator
	 */
	@Transient
	public List<LiberationDocumentFiscal> getLiberationsSorted() {

		if (getLiberations() == null) {
			return null;
		}

		// tri par ordre croissant
		final List<LiberationDocumentFiscal> list = new ArrayList<>(getLiberations());
		list.sort(new LiberationDocumentFiscal.Comparator());
		return list;
	}

	/**
	 * @return the etats
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", nullable = false)
	@ForeignKey(name = "FK_ET_DOCFISC_DOCFISC_ID")
	public Set<EtatDocumentFiscal> getEtats() {
		return etats;
	}

	/**
	 * @param theEtats the etats to set
	 */
	public void setEtats(Set<EtatDocumentFiscal> theEtats) {
		etats = theEtats;
	}

	/**
	 * @return la date de retour de la déclaration; c'est-à-dire la date d'obtention de l'état 'retourné' le plus récent. Ou <b>null</b> si la déclaration n'est pas retournée.
	 */
	@Transient
	public RegDate getDateRetour() {
		EtatDocumentFiscal etatDeclaration = getDernierEtatOfType(TypeEtatDocumentFiscal.RETOURNE);
		if (etatDeclaration != null) {
			return etatDeclaration.getDateObtention();
		}
		return null;
	}

	/**
	 * @return la date d'expédition de la déclaration; c'est-à-dire la date d'obtention de l'état 'émis'.
	 */
	@Transient
	public RegDate getDateExpedition() {
		EtatDocumentFiscal etatDeclaration = getDernierEtatOfType(TypeEtatDocumentFiscal.EMIS);
		if (etatDeclaration != null) {
			return etatDeclaration.getDateObtention();
		}
		return null;
	}

	/**
	 * @param type le type d'état demandé
	 * @return le dernier état (= le plus récent) non-annulé du type demandé
	 */
	@Transient
	public EtatDocumentFiscal getDernierEtatOfType(TypeEtatDocumentFiscal type) {
		// tri par ordre croissant
		final List<EtatDocumentFiscal> etatsSorted = getEtatsSorted();
		if (etatsSorted == null || etatsSorted.isEmpty()) {
			return null;
		}
		return getDernierEtatOfType(type, etatsSorted);
	}

	private EtatDocumentFiscal getDernierEtatOfType(TypeEtatDocumentFiscal etatRecherche, List<EtatDocumentFiscal> etatsSorted) {
		if (etatRecherche == null) {
			throw new IllegalArgumentException("etatDeclaration required.");
		}

		// récupère le dernier état non-annulé du type spécifié
		for (int i = etatsSorted.size() - 1; i >= 0; --i) {
			final EtatDocumentFiscal e = etatsSorted.get(i);
			if (!e.isAnnule() && e.getEtat() == etatRecherche) {
				return e;
			}
		}
		return null;
	}

	@NotNull
	@Transient
	public List<EtatDocumentFiscal> getEtatsOfType(TypeEtatDocumentFiscal type, boolean withCanceled) {
		final List<EtatDocumentFiscal> etatsSorted = getEtatsSorted();
		if (etatsSorted == null || etatsSorted.isEmpty()) {
			return Collections.emptyList();
		}
		final List<EtatDocumentFiscal> etatsOfType = new ArrayList<>(etatsSorted.size());
		for (EtatDocumentFiscal etat : etatsSorted) {
			if ((withCanceled || !etat.isAnnule()) && etat.getEtat() == type) {
				etatsOfType.add(etat);
			}
		}
		return etatsOfType.isEmpty() ? Collections.emptyList() : etatsOfType;
	}

	/**
	 * @return le dernier délai accordé (= le plus permissif)
	 */
	@Transient
	public DelaiDocumentFiscal getDernierDelaiAccorde() {
		final Set<DelaiDocumentFiscal> delais = getDelais();
		if (delais == null || delais.isEmpty()) {
			return null;
		}

		final List<DelaiDocumentFiscal> list = new ArrayList<>(delais.size());
		for (DelaiDocumentFiscal delai : delais) {
			if (!delai.isAnnule() && delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE) {
				list.add(delai);
			}
		}
		list.sort(new DelaiDocumentFiscal.Comparator());
		return list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * @return l'état courant de la déclaration
	 */
	@Transient
	public EtatDocumentFiscal getDernierEtat() {

		// tri par ordre croissant
		final List<EtatDocumentFiscal> etatsSorted = getEtatsSorted();
		if (etatsSorted == null || etatsSorted.isEmpty()) {
			return null;
		}

		// [UNIREG-2489] : si la déclaration a été retournée, alors son état est retourné, même si les dates ne jouent pas
		final EtatDocumentFiscal retour = getDernierEtatOfType(TypeEtatDocumentFiscal.RETOURNE, etatsSorted);
		if (retour != null) {
			return retour;
		}

		// l'état "suspendu" est directement derrière l'état "retourné", en termes de priorité
		final EtatDocumentFiscal suspension = getDernierEtatOfType(TypeEtatDocumentFiscal.SUSPENDU, etatsSorted);
		if (suspension != null) {
			return suspension;
		}

		// récupère le dernier état non-annulé (qui n'est pas un retour ni une suspension, donc)
		for (int i = etatsSorted.size() - 1; i >= 0; --i) {
			final EtatDocumentFiscal e = etatsSorted.get(i);
			if (!e.isAnnule()) {
				return e;
			}
		}

		return null;
	}

	/**
	 * @return la liste des états triés par ordre croissant (du plus ancien ou plus récent).
	 * @see EtatDocumentFiscal.Comparator
	 */
	@Transient
	public List<EtatDocumentFiscal> getEtatsSorted() {

		if (getEtats() == null) {
			return null;
		}

		// tri par ordre croissant
		final List<EtatDocumentFiscal> list = new ArrayList<>(getEtats());

		if (!list.isEmpty()) {
			final Comparator<EtatDocumentFiscal> comparator = list.get(0).getComparator();
			list.sort(comparator);
		}

		return list;
	}

	@Transient
	public RegDate getDelaiAccordeAu() {
		RegDate dateMax = null;
		Set<DelaiDocumentFiscal> echeances = getDelais();
		if (echeances != null) {
			for (DelaiDocumentFiscal echeance : echeances) {
				if (!echeance.isAnnule() && echeance.getEtat() == EtatDelaiDocumentFiscal.ACCORDE) {
					if (dateMax == null) {
						dateMax = echeance.getDelaiAccordeAu();
					}
					if (echeance.getDelaiAccordeAu().isAfter(dateMax)) {
						dateMax = echeance.getDelaiAccordeAu();
					}
				}
			}
		}
		return dateMax;
	}

	@Transient
	public RegDate getPremierDelai() {
		final Set<DelaiDocumentFiscal> delais = getDelais();
		if (delais != null) {
			return delais.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(delai -> delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE)
					.map(DelaiDocumentFiscal::getDelaiAccordeAu)
					.min(Comparator.naturalOrder())
					.orElse(null);
		}
		return null;
	}

	public void addEtat(EtatDocumentFiscal etat) {

		if (etats == null) {
			etats = new HashSet<>();
		}

		etat.setDocumentFiscal(this);
		etats.add(etat);
	}

	public void addDelai(DelaiDocumentFiscal delai) {

		if (delais == null) {
			delais = new HashSet<>();
		}

		delai.setDocumentFiscal(this);
		delais.add(delai);
	}

	public void addLiberation(LiberationDocumentFiscal liberation) {

		if (liberations == null) {
			liberations = new HashSet<>();
		}

		liberation.setDocumentFiscal(this);
		liberations.add(liberation);
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
	}

	/**
	 * @return <code>true</code> si le document supporte le suivi dans son état; <code>false</code> autrement.
	 */
	// TODO (msi) : gérer proprement la notion de document fiscal avec suivi  : actuellement, il y a des méthodes de suivi un peu partout dans la hiérarchie des documents (ajout d'une interface ou retravailler la hiérarchie, à voir)
	@Transient
	public boolean isAvecSuivi() {
		// par défaut, pas de suivi
		return false;
	}

	/**
	 * @return <code>true</code> si la déclaration est d'un type qui supporte les sommations
	 * @see #isRappelable()
	 */
	@Transient
	public abstract boolean isSommable();

	/**
	 * @return <code>true</code> si la déclaration est d'un type qui supporte les rappels
	 * @see #isSommable()
	 */
	@Transient
	public abstract boolean isRappelable();

	@Override
	public String toString() {
		return getBusinessName();
	}

	@Transient
	protected String getBusinessName() {
		return getClass().getSimpleName();
	}

	@Transient
	public abstract Integer getAnneePeriodeFiscale();
}
