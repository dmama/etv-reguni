package ch.vd.uniregctb.declaration;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.Tiers;
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
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = false))
})
public abstract class Declaration extends HibernateDateRangeEntity implements LinkedEntity {

	/**
	 * The ID
	 */
	private Long id;

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
	 * Clef du document necessaire pour l'editique
	 */
	private String nomDocument;

	@Column(name = "NOM_DOCUMENT")
	public String getNomDocument() {
		return nomDocument;
	}

	public void setNomDocument(String nomDocument) {
		this.nomDocument = nomDocument;
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
		Collections.sort(list, new DelaiDeclaration.Comparator());

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

	/**
	 * @return le dernier délai (= le plus permissif)
	 */
	@Transient
	public DelaiDeclaration getDernierDelais() {
		if (delais == null || delais.isEmpty()) {
			return null;
		}

		final List<DelaiDeclaration> list = new ArrayList<>(delais.size());
		for (DelaiDeclaration delai : delais) {
			if (!delai.isAnnule()) {
				list.add(delai);
			}
		}
		Collections.sort(list, new DelaiDeclaration.Comparator());

		final DelaiDeclaration d = list.get(list.size() - 1);
		return d.isAnnule() ? null : d;
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

		// récupère le dernier état non-annulé (qui n'est pas un retour, donc)
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
		Collections.sort(list, new EtatDeclaration.Comparator());

		return list;
	}

	@Transient
	public RegDate getDelaiAccordeAu() {
		RegDate dateMax = null;
		Set<DelaiDeclaration> echeances = delais;
		if (echeances != null) {
			for (DelaiDeclaration echeance : echeances) {
				if (!echeance.isAnnule()) {
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
		RegDate premierDelai = null;
		Set<DelaiDeclaration> delais = this.delais;
		if (delais != null) {
			for (DelaiDeclaration delai : delais) {
				if (!delai.isAnnule()) {
					if (premierDelai == null) {
						premierDelai = delai.getDelaiAccordeAu();
					}
					if (delai.getDelaiAccordeAu().isBefore(premierDelai)) {
						premierDelai = delai.getDelaiAccordeAu();
					}
				}
			}
		}
		return premierDelai;
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

	@Override
	@Transient
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
	}
}
