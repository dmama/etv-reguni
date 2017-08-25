package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
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

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uCd8AOqeEdySTq6PFlf9jQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uCd8AOqeEdySTq6PFlf9jQ"
 */
@Entity
@Table(name = "DECLARATION")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DOCUMENT_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class Declaration extends HibernateEntity implements DateRange, LinkedEntity {

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <p>
	 * Date de début d'imposition pour la déclaration.
	 * <p>
	 * Dans la majeure partie des cas, cette date est égale au 1er janvier de la période fiscale considérée. Elle peut être différente dans
	 * le cas d'une arrivée en cours d'année (et à ce moment-là elle est égale à la date d'arrivée).
	 * <p>
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XJ1FcOqgEdySTq6PFlf9jQ"
	 */
	private RegDate dateDebut;

	/**
	 * <!-- begin-user-doc -->
	 * <p>
	 * Date de fin d'imposition pour la déclaration.
	 * <p>
	 * Dans la majeure partie des cas, cette date est égale au 31 décembre de la période fiscale considérée. elle peut être différente dans
	 * le cas d'un départ en cours d'année (et à ce moment-là elle est égale à la date de départ).
	 * <p>
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ajGHUOqgEdySTq6PFlf9jQ"
	 */
	private RegDate dateFin;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkeqfEdySTq6PFlf9jQ"
	 */
	private Set<EtatDeclaration> etats; // Ex: Envois

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
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R75A8uqgEdySTq6PFlf9jQ"
	 */
	private PeriodeFiscale periode;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_e6QxUeqfEdySTq6PFlf9jQ"
	 */
	private Set<DelaiDeclaration> delais; // Ex: Echeances

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pEXLgS4DEd2H4bonmeBdag"
	 */
	private ModeleDocument modeleDocument;

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
	@Index(name = "IDX_DECL_TRS_ID", columnNames = "TIERS_ID")
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
	@JoinColumn(name = "DECLARATION_ID", nullable= false)
	@ForeignKey(name = "FK_DECL_DEL_DI_ID")
	public Set<DelaiDeclaration> getDelais() {
		// begin-user-code
		return delais;
		// end-user-code
	}

	/**
	 * @return la liste des délaos triés par ordre croissant (du plus ancien ou plus récent).
	 * @see EtatDeclaration.Comparator
	 */
	@Transient
	public List<DelaiDeclaration> getDelaisSorted() {

		if (delais == null) {
			return null;
		}

		// tri par ordre croissant
		final List<DelaiDeclaration> list = new ArrayList<>(delais);
		list.sort(new DelaiDeclaration.Comparator());

		return list;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDelais the delais to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_e6QxUeqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDelais(Set<DelaiDeclaration> theDelais) {
		// begin-user-code
		delais = theDelais;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the etats
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkeqfEdySTq6PFlf9jQ?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DECLARATION_ID", nullable = false)
	@ForeignKey(name = "FK_ET_DI_DI_ID")
	public Set<EtatDeclaration> getEtats() {
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
	public void setEtats(Set<EtatDeclaration> theEtats) {
		// begin-user-code
		etats = theEtats;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the periode
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R75A8uqgEdySTq6PFlf9jQ?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, fetch=FetchType.LAZY)
	@JoinColumn(name = "PERIODE_ID", nullable = false)
	@ForeignKey(name = "FK_DECL_PF_ID")
	public PeriodeFiscale getPeriode() {
		// begin-user-code
		return periode;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateDebut
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XJ1FcOqgEdySTq6PFlf9jQ?GETTER"
	 */
	@Override
	@Column(name = "DATE_DEBUT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		// begin-user-code
		return dateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateDebut the dateDebut to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XJ1FcOqgEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDateDebut(RegDate theDateDebut) {
		// begin-user-code
		dateDebut = theDateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateFin
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ajGHUOqgEdySTq6PFlf9jQ?GETTER"
	 */
	@Override
	@Column(name = "DATE_FIN", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		// begin-user-code
		return dateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateFin the dateFin to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ajGHUOqgEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDateFin(RegDate theDateFin) {
		// begin-user-code
		dateFin = theDateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param thePeriode the periode to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R75A8uqgEdySTq6PFlf9jQ?SETTER"
	 */
	public void setPeriode(PeriodeFiscale thePeriode) {
		// begin-user-code
		periode = thePeriode;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the modeleDocument
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pEXLgS4DEd2H4bonmeBdag?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, fetch=FetchType.LAZY)
	@JoinColumn(name = "MODELE_DOC_ID")
	@ForeignKey(name = "FK_DECL_DOC_ID")
	public ModeleDocument getModeleDocument() {
		// begin-user-code
		return modeleDocument;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theModeleDocument the modeleDocument to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pEXLgS4DEd2H4bonmeBdag?SETTER"
	 */
	public void setModeleDocument(ModeleDocument theModeleDocument) {
		// begin-user-code
		modeleDocument = theModeleDocument;
		// end-user-code
	}

	/**
	 * @return la date de retour de la déclaration; c'est-à-dire la date d'obtention de l'état 'retourné' le plus récent. Ou <b>null</b> si la déclaration n'est pas retournée.
	 */
	@Transient
	public RegDate getDateRetour() {
		EtatDeclaration etatDeclaration = getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE);
		if (etatDeclaration != null) {
			return etatDeclaration.getDateObtention();
		}
		return null;
	}

	@Transient
	public RegDate getDateExpedition() {
		EtatDeclaration etatDeclaration = getDernierEtatOfType(TypeEtatDeclaration.EMISE);
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
	public EtatDeclaration getDernierEtatOfType(TypeEtatDeclaration type) {
		// tri par ordre croissant
		final List<EtatDeclaration> etatsSorted = getEtatsSorted();
		if (etatsSorted == null || etatsSorted.isEmpty()) {
			return null;
		}
		return getDernierEtatOfType(type, etatsSorted);
	}

	private static EtatDeclaration getDernierEtatOfType(TypeEtatDeclaration etatRecherche, List<EtatDeclaration> etatsSorted) {
		Assert.notNull(etatRecherche, "etatDeclaration required.");

		// récupère le dernier état non-annulé du type spécifié
		for (int i = etatsSorted.size() - 1; i >= 0; --i) {
			final EtatDeclaration e = etatsSorted.get(i);
			if (!e.isAnnule() && e.getEtat() == etatRecherche) {
				return e;
			}
		}
		return null;
	}

	@NotNull
	@Transient
	public List<EtatDeclaration> getEtatsOfType(TypeEtatDeclaration type, boolean withCanceled) {
		final List<EtatDeclaration> etatsSorted = getEtatsSorted();
		if (etatsSorted == null || etatsSorted.isEmpty()) {
			return Collections.emptyList();
		}
		final List<EtatDeclaration> etatsOfType = new ArrayList<>(etatsSorted.size());
		for (EtatDeclaration etat : etatsSorted) {
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
	public DelaiDeclaration getDernierDelaiAccorde() {
		if (delais == null || delais.isEmpty()) {
			return null;
		}

		final List<DelaiDeclaration> list = new ArrayList<>(delais.size());
		for (DelaiDeclaration delai : delais) {
			if (!delai.isAnnule() && delai.getEtat() == EtatDelaiDeclaration.ACCORDE) {
				list.add(delai);
			}
		}
		list.sort(new DelaiDeclaration.Comparator());
		return list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * @return l'état courant de la déclaration
	 */
	@Transient
	public EtatDeclaration getDernierEtat() {

		// tri par ordre croissant
		final List<EtatDeclaration> etatsSorted = getEtatsSorted();
		if (etatsSorted == null || etatsSorted.isEmpty()) {
			return null;
		}

		// [UNIREG-2489] : si la déclaration a été retournée, alors son état est retourné, même si les dates ne jouent pas
		final EtatDeclaration retour = getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE, etatsSorted);
		if (retour != null) {
			return retour;
		}

		// l'état "suspendu" est directement derrière l'état "retourné", en termes de priorité
		final EtatDeclaration suspension = getDernierEtatOfType(TypeEtatDeclaration.SUSPENDUE, etatsSorted);
		if (suspension != null) {
			return suspension;
		}

		// récupère le dernier état non-annulé (qui n'est pas un retour ni une suspension, donc)
		for (int i = etatsSorted.size() - 1; i >= 0; --i) {
			final EtatDeclaration e = etatsSorted.get(i);
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
	public List<EtatDeclaration> getEtatsSorted() {

		if (etats == null) {
			return null;
		}

		// tri par ordre croissant
		final List<EtatDeclaration> list = new ArrayList<>(etats);
		list.sort(new EtatDeclaration.Comparator());

		return list;
	}

	@Transient
	public RegDate getDelaiAccordeAu() {
		RegDate dateMax = null;
		Set<DelaiDeclaration> echeances = delais;
		if (echeances != null) {
			for (DelaiDeclaration echeance : echeances) {
				if (!echeance.isAnnule() && echeance.getEtat() == EtatDelaiDeclaration.ACCORDE) {
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
		final Set<DelaiDeclaration> delais = this.delais;
		if (delais != null) {
			return delais.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(delai -> delai.getEtat() == EtatDelaiDeclaration.ACCORDE)
					.map(DelaiDeclaration::getDelaiAccordeAu)
					.min(Comparator.naturalOrder())
					.orElse(null);
		}
		return null;
	}

	public void addEtat(EtatDeclaration etat) {

		if (etats == null) {
			etats = new HashSet<>();
		}

		etat.setDeclaration(this);
		etats.add(etat);
	}

	public void addDelai(DelaiDeclaration delai) {

		if (delais == null) {
			delais = new HashSet<>();
		}

		delai.setDeclaration(this);
		delais.add(delai);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
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

	/**
	 * Repris de {@link ch.vd.uniregctb.common.HibernateDateRangeEntity}, description avec début et fin.
	 */
	@Override
	public String toString() {
		final String dateDebutStr = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateDebut), "?");
		final String dateFinStr = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?");
		return String.format("%s (%s - %s)", getBusinessName(), dateDebutStr, dateFinStr);
	}

	@Transient
	protected String getBusinessName() {
		return getClass().getSimpleName();
	}

}
