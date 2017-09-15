package ch.vd.uniregctb.declaration;

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
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

@Entity
@Table(name = "DOCUMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DOCUMENT_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class DocumentFiscal extends HibernateEntity implements LinkedEntity {

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZtXOoOqfEdySTq6PFlf9jQ"
	 */
	private Tiers tiers;



	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_e6QxUeqfEdySTq6PFlf9jQ"
	 */
	private Set<DelaiDocumentFiscal> delais; // Ex: Echeances

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkeqfEdySTq6PFlf9jQ"
	 */
	private Set<EtatDocumentFiscal> etats; // Ex: Envois

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
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the tiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZtXOoOqfEdySTq6PFlf9jQ?GETTER"
	 */
	@ManyToOne
	// msi: pas de cascade, parce qu'on veut pouvoir ajouter une déclaration à un tiers sans automatiquement modifier celui-ci (perfs)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@Index(name = "IDX_DOCFISC_TRS_ID", columnNames = "TIERS_ID")
	public Tiers getTiers() {
		// begin-user-code
		return tiers;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTiers the tiers to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZtXOoOqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setTiers(Tiers theTiers) {
		// begin-user-code
		tiers = theTiers;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the delais
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_e6QxUeqfEdySTq6PFlf9jQ?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable=false, updatable=false, nullable= false)
	@ForeignKey(name = "FK_DEL_DOCFISC_DOCFISC_ID")
	public Set<DelaiDocumentFiscal> getDelais() {
		// begin-user-code
		return delais;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDelais the delais to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_e6QxUeqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDelais(Set<DelaiDocumentFiscal> theDelais) {
		// begin-user-code
		delais = theDelais;
		// end-user-code
	}

	/**
	 * @return la liste des délais triés par ordre croissant (du plus ancien ou plus récent).
	 * @see EtatDeclaration.Comparator
	 */
	@Transient
	public List<DelaiDocumentFiscal> getDelaisSorted() {

		if (getDelais() == null) {
			return null;
		}

		// tri par ordre croissant
		final List<DelaiDocumentFiscal> list = new ArrayList<>(getDelais());
		list.sort(new DelaiDeclaration.Comparator());

		return list;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the etats
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkeqfEdySTq6PFlf9jQ?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", nullable = false)
	@ForeignKey(name = "FK_ET_DOCFISC_DOCFISC_ID")
	public Set<EtatDocumentFiscal> getEtats() {
		// begin-user-code
		return etats;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theEtats the etats to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkeqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setEtats(Set<EtatDocumentFiscal> theEtats) {
		// begin-user-code
		etats = theEtats;
		// end-user-code
	}

	/**
	 * @return la date de retour de la déclaration; c'est-à-dire la date d'obtention de l'état 'retourné' le plus récent. Ou <b>null</b> si la déclaration n'est pas retournée.
	 */
	@Transient
	public RegDate getDateRetour() {
		EtatDocumentFiscal etatDeclaration = getDernierEtatOfType(TypeEtatDocumentFiscal.RETOURNEE);
		if (etatDeclaration != null) {
			return etatDeclaration.getDateObtention();
		}
		return null;
	}

	@Transient
	public RegDate getDateExpedition() {
		EtatDocumentFiscal etatDeclaration = getDernierEtatOfType(TypeEtatDocumentFiscal.EMISE);
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
		Assert.notNull(etatRecherche, "etatDeclaration required.");

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
		final EtatDocumentFiscal retour = getDernierEtatOfType(TypeEtatDocumentFiscal.RETOURNEE, etatsSorted);
		if (retour != null) {
			return retour;
		}

		// l'état "suspendu" est directement derrière l'état "retourné", en termes de priorité
		final EtatDocumentFiscal suspension = getDernierEtatOfType(TypeEtatDocumentFiscal.SUSPENDUE, etatsSorted);
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
	 * @see EtatDeclaration.Comparator
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

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
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

}
