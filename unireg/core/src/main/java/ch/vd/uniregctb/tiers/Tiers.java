package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.BusinessComparable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc --> Personne avec laquelle l'ACI entretien une relation, de nature fiscale ou autre. Cette
 * personne peut être: - Une personne physique, connu ou non du contrôle des habitants - Une organisation (personne morale ou entité sans
 * personnalité juridique, connue ou non du registre des personnes morales). - Une autre communauté de personnes sans personnalité juridique
 * complète (Pour le moment, limité au couple de personnes mariées ou liées par un partenariat enregistré, vivant en ménage commun
 * (c'est-à-dire non séparées ou dont le partenariat n'est pas pas dissous)).
 *
 * @generated "UML to Java V5.0 (com.ibm.xtools.transform.uml2.java5.internal.UML2JavaTransform)"
 */
@Entity
@Table(name = "TIERS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TIERS_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class Tiers extends HibernateEntity implements Validateable, BusinessComparable<Tiers> {

	public static final String NATURE_HABITANT = "Habitant";
	public static final String NATURE_NONHABITANT = "NonHabitant";
	public static final String NATURE_ENTREPRISE = "Entreprise";
	public static final String NATURE_ETABLISSEMENT = "Etablissement";
	public static final String NATURE_MENAGECOMMUN = "MenageCommun";
	public static final String NATURE_AUTRECOMMUNAUTE = "AutreCommunaute";
	public static final String NATURE_DPI = "DebiteurPrestationImposable";
	public static final String NATURE_COLLECTIVITEADMINISTRATIVE = "CollectiviteAdministrative";

	private static final Logger LOGGER = Logger.getLogger(Tiers.class);

	/**
	 *
	 */
	private static final long serialVersionUID = -265874466414875812L;

	/**
	 * Numero unique attribue au tiers, qui correspond pour les contribuables PP au numero de contribuable.
	 */
	private Long numero;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_X56CINjGEdyNDriNIUNZFw"
	 */
	private String complementNom;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_SE9LMJNdEdygKK6Oe0tVlw"
	 */
	private String personneContact;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_W-_08NMnEdy4-c1RAQqlyw"
	 */
	private String numeroTelephonePrive;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_jaly8NMnEdy4-c1RAQqlyw"
	 */
	private String numeroTelephoneProfessionnel;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_gNFnwO9YEdyEV8rfFv3rEg"
	 */
	private String numeroTelephonePortable;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_cn6lQNMnEdy4-c1RAQqlyw"
	 */
	private String numeroTelecopie;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tGDLoNMnEdy4-c1RAQqlyw"
	 */
	private String adresseCourrierElectronique;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_gzNEIJNkEdygKK6Oe0tVlw"
	 */
	private Boolean blocageRemboursementAutomatique = Boolean.TRUE;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * Numero de compte bancaire ou du compte postal au format international IBAN (longueur maximum 21 pour les comptes suisses)
	 */
	private String numeroCompteBancaire;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * Titulaire du compte bancaire ou du compte postal
	 */
	private String titulaireCompteBancaire;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mMBqUJNhEdygKK6Oe0tVlw"
	 */
	private String adresseBicSwift;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_stv7YEE7Ed2XrapGHNAWZw"
	 */
	private boolean debiteurInactif = false;

	/**
	 * Détermine si l'entity à correctement été indexée (faux => entité correctement indexée) Par défaut, l'index est OK ce qui est le cas
	 * le plus courant
	 */
	private Boolean indexDirty;

	/**
	 * [UNIREG-1979] Date à partir de laquelle le tiers devra être réindexé, si elle est renseignée.
	 */
	private RegDate reindexOn;

	/**
	 * <!-- begin-user-doc --> L'office d'impôt qui gère le tiers. Cette valeur est automatiquement renseignée par un intercepteur
	 * hibernate.
	 * <p>
	 * On peut normalement déduire cette valeur à partir du for de gestion courant, mais pour des raisons de performances elle est cachée
	 * ici. <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R3578KdlEd2Ebedj8uu8CQ"
	 */
	private Integer officeImpotId;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcZNZEdygKK6Oe0tVlw"
	 */
	private Set<RapportEntreTiers> rapportsObjet;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BeZlcpNZEdygKK6Oe0tVlw"
	 */
	private Set<RapportEntreTiers> rapportsSujet;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_2w8Y0afTEdy6qP7Nc3dO8g"
	 */
	private Set<AdresseTiers> adressesTiers;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZtOEseqfEdySTq6PFlf9jQ"
	 */
	private Set<Declaration> declarations;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7ofwMV-eEdyCxumqfWBxMQ"
	 */
	private Set<ForFiscal> forsFiscaux;

	@Transient
	@Override
	public Object getKey() {
		return numero;
	}

	/**
	 * Pour que la sous-classe soit contente
	 */
	@Transient
	public Long getId() {
		return numero;
	}

	@Id
	@Column(name = "NUMERO")
	@GeneratedValue(generator = "tiersSequence")
	@GenericGenerator(name = "tiersSequence", strategy = "ch.vd.uniregctb.tiers.TiersMultiSequenceGenerator")
	/*
	 * @GenericGenerator(name = "tiersSequence", strategy = "ch.vd.uniregctb.tiers.MultiSequenceGenerator", parameters = { @Parameter(name =
	 * "max_lo", value = "50"), @Parameter(name = "sequence", value = "S_TIERS"), @Parameter(name = "entitiesSequencesMap", value =
	 * "ch.vd.uniregctb.tiers.Habitant=S_HABITANT, ch.vd.uniregctb.tiers.NonHabitant=S_NON_HABITANT"), @Parameter(name = "sequenceOffset",
	 * value = "1000000") } )
	 */
	public Long getNumero() {
		return numero;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theNumero
	 *            the numero to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8EVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumero(Long theNumero) {
		// begin-user-code
		numero = theNumero;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the complementNom
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_X56CINjGEdyNDriNIUNZFw?GETTER"
	 */
	@Column(name = "COMPLEMENT_NOM", length = LengthConstants.TIERS_NOM)
	public String getComplementNom() {
		// begin-user-code
		return complementNom;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theComplementNom
	 *            the complementNom to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_X56CINjGEdyNDriNIUNZFw?SETTER"
	 */
	public void setComplementNom(String theComplementNom) {
		// begin-user-code
		complementNom = theComplementNom;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the personneContact
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_SE9LMJNdEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "PERSONNE_CONTACT", length = LengthConstants.TIERS_PERSONNE)
	public String getPersonneContact() {
		// begin-user-code
		return personneContact;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param thePersonneContact
	 *            the personneContact to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_SE9LMJNdEdygKK6Oe0tVlw?SETTER"
	 */
	public void setPersonneContact(String thePersonneContact) {
		// begin-user-code
		personneContact = thePersonneContact;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the numeroTelephonePrive
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_W-_08NMnEdy4-c1RAQqlyw?GETTER"
	 */
	@Column(name = "NUMERO_TEL_PRIVE", length = LengthConstants.TIERS_NUMTEL)
	public String getNumeroTelephonePrive() {
		// begin-user-code
		return numeroTelephonePrive;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theNumeroTelephonePrive
	 *            the numeroTelephonePrive to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_W-_08NMnEdy4-c1RAQqlyw?SETTER"
	 */
	public void setNumeroTelephonePrive(String theNumeroTelephonePrive) {
		// begin-user-code
		numeroTelephonePrive = theNumeroTelephonePrive;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the numeroTelephoneProfessionnel
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_jaly8NMnEdy4-c1RAQqlyw?GETTER"
	 */
	@Column(name = "NUMERO_TEL_PROF", length = LengthConstants.TIERS_NUMTEL)
	public String getNumeroTelephoneProfessionnel() {
		// begin-user-code
		return numeroTelephoneProfessionnel;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theNumeroTelephoneProfessionnel
	 *         the numeroTelephoneProfessionnel to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_jaly8NMnEdy4-c1RAQqlyw?SETTER"
	 */
	public void setNumeroTelephoneProfessionnel(String theNumeroTelephoneProfessionnel) {
		// begin-user-code
		numeroTelephoneProfessionnel = theNumeroTelephoneProfessionnel;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the numeroTelephonePortable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_gNFnwO9YEdyEV8rfFv3rEg?GETTER"
	 */
	@Column(name = "NUMERO_TEL_PORTABLE", length = LengthConstants.TIERS_NUMTEL)
	public String getNumeroTelephonePortable() {
		// begin-user-code
		return numeroTelephonePortable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theNumeroTelephonePortable
	 *            the numeroTelephonePortable to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_gNFnwO9YEdyEV8rfFv3rEg?SETTER"
	 */
	public void setNumeroTelephonePortable(String theNumeroTelephonePortable) {
		// begin-user-code
		numeroTelephonePortable = theNumeroTelephonePortable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the numeroTelecopie
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_cn6lQNMnEdy4-c1RAQqlyw?GETTER"
	 */
	@Column(name = "NUMERO_TELECOPIE", length = LengthConstants.TIERS_NUMTEL)
	public String getNumeroTelecopie() {
		// begin-user-code
		return numeroTelecopie;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theNumeroTelecopie
	 *            the numeroTelecopie to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_cn6lQNMnEdy4-c1RAQqlyw?SETTER"
	 */
	public void setNumeroTelecopie(String theNumeroTelecopie) {
		// begin-user-code
		numeroTelecopie = theNumeroTelecopie;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the adresseCourrierElectronique
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tGDLoNMnEdy4-c1RAQqlyw?GETTER"
	 */
	@Column(name = "ADRESSE_EMAIL", length = LengthConstants.TIERS_EMAIL)
	public String getAdresseCourrierElectronique() {
		// begin-user-code
		return adresseCourrierElectronique;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theAdresseCourrierElectronique
	 *            the adresseCourrierElectronique to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tGDLoNMnEdy4-c1RAQqlyw?SETTER"
	 */
	public void setAdresseCourrierElectronique(String theAdresseCourrierElectronique) {
		// begin-user-code
		adresseCourrierElectronique = theAdresseCourrierElectronique;
		// end-user-code
	}

	/**
	 * @return Returns the numeroCompteBancaire.
	 */
	@Column(name = "NUMERO_COMPTE_BANCAIRE", length = LengthConstants.TIERS_NUMCOMPTE)
	public String getNumeroCompteBancaire() {
		return numeroCompteBancaire;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theNumeroCompteBancaire
	 *            the numeroCompteBancaire to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8JFx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroCompteBancaire(String theNumeroCompteBancaire) {
		// begin-user-code
		numeroCompteBancaire = theNumeroCompteBancaire;
		// end-user-code
	}

	/**
	 * @return Returns the titulaireCompteBancaire.
	 */
	@Column(name = "TITULAIRE_COMPTE_BANCAIRE", length = LengthConstants.TIERS_PERSONNE)
	public String getTitulaireCompteBancaire() {
		return titulaireCompteBancaire;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theTitulaireCompteBancaire
	 *            the titulaireCompteBancaire to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Jlx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setTitulaireCompteBancaire(String theTitulaireCompteBancaire) {
		// begin-user-code
		titulaireCompteBancaire = theTitulaireCompteBancaire;
		// end-user-code
	}

	@Transient
	public List<AdresseTiers> getAdressesTiersSorted() {
		if (adressesTiers == null) {
			return null;
		}
		List<AdresseTiers> list = new ArrayList<AdresseTiers>();
		list.addAll(adressesTiers);
		Collections.sort(list, new DateRangeComparator<AdresseTiers>());
		return list;
	}

	@Transient
	public List<AdresseTiers> getAdressesTiersSorted(TypeAdresseTiers type) {
		if (adressesTiers == null) {
			return null;
		}
		List<AdresseTiers> list = new ArrayList<AdresseTiers>();
		for (AdresseTiers adr : adressesTiers) {
			if (adr.getUsage() == type) {
				list.add(adr);
			}
		}
		Collections.sort(list, new DateRangeComparator<AdresseTiers>());
		return list;
	}

	/**
	 * Retourne l'adresse tiers du type et à l'index spécifié. Note: les adresses annulées sont ignorées.
	 *
	 * @param index
	 *            l'index de l'adresse dans la sous-collection des adresses du type spécifié. L'index 0 retourne la première adresse.
	 *            L'index -1 retourne la dernière adresse (-2 l'avant dernière, etc... et -size retourne la première)
	 * @param type
	 *            le type de l'adresse spécifié.
	 * @return une adresse tiers, ou <b>null</b> si aucune adresse n'existe
	 */
	@Transient
	public AdresseTiers getAdresseTiersAt(int index, TypeAdresseTiers type) {
		if (adressesTiers == null) {
			return null;
		}

		// Construit une collection triée des adresses du type spécifié
		List<AdresseTiers> list = new ArrayList<AdresseTiers>();
		for (AdresseTiers a : adressesTiers) {
			if (!a.isAnnule() && type == a.getUsage()) {
				list.add(a);
			}
		}
		Collections.sort(list, new DateRangeComparator<AdresseTiers>());

		// Extrait et retourne l'adresse demandée
		final int size = list.size();
		if (0 <= index && index < size) {
			return list.get(index);
		}
		else if (-size <= index && index <= -1) {
			return list.get(size + index);
		}
		else {
			return null;
		}
	}

	/**
	 * Retourne l'adresse d'un type donné active à une date donnée.
	 *
	 * @param type
	 *            le type d'adresse à retourner.
	 * @param date
	 *            la date à laquelle l'adresse est active, ou <b>null</b> pour obtenir l'adresse courante.
	 *
	 * @return l'adresse correspondante, ou nulle si aucune adresse ne correspond aux critères.
	 */
	@Transient
	public AdresseTiers getAdresseActive(TypeAdresseTiers type, RegDate date) {

		Assert.notNull(type);

		if (adressesTiers == null) {
			return null;
		}

		AdresseTiers result = null;
		for (AdresseTiers adresse : adressesTiers) {
			if (adresse.isValidAt(date) && type == adresse.getUsage()) {
				result = adresse;
				break;
			}
		}

		return result;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the blocageRemboursementAutomatique
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_gzNEIJNkEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "BLOC_REMB_AUTO")
	public Boolean getBlocageRemboursementAutomatique() {
		// begin-user-code
		return blocageRemboursementAutomatique;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theBlocageRemboursementAutomatique
	 *         the blocageRemboursementAutomatique to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_gzNEIJNkEdygKK6Oe0tVlw?SETTER"
	 */
	public void setBlocageRemboursementAutomatique(Boolean theBlocageRemboursementAutomatique) {
		// begin-user-code
		blocageRemboursementAutomatique = theBlocageRemboursementAutomatique;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the adresseBicSwift
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mMBqUJNhEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "ADRESSE_BIC_SWIFT", length = LengthConstants.TIERS_ADRESSEBICSWIFT)
	public String getAdresseBicSwift() {
		// begin-user-code
		return adresseBicSwift;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theAdresseBicSwift
	 *            the adresseBicSwift to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mMBqUJNhEdygKK6Oe0tVlw?SETTER"
	 */
	public void setAdresseBicSwift(String theAdresseBicSwift) {
		// begin-user-code
		adresseBicSwift = theAdresseBicSwift;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the rapportsObjet
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcZNZEdygKK6Oe0tVlw?GETTER"
	 */
	@OneToMany(fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_RET_TRS_OBJ_ID")
	@JoinColumn(name = "TIERS_OBJET_ID")
	public Set<RapportEntreTiers> getRapportsObjet() {
		// begin-user-code
		return rapportsObjet;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theRapportsObjet
	 *            the rapportsObjet to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcZNZEdygKK6Oe0tVlw?SETTER"
	 */
	public void setRapportsObjet(Set<RapportEntreTiers> theRapportsObjet) {
		// begin-user-code
		rapportsObjet = theRapportsObjet;
		// end-user-code
	}

	/**
	 *
	 * @param rapport
	 *            the rapport to add
	 */
	public void addRapportObjet(RapportEntreTiers rapport) {
		if (rapportsObjet == null) {
			rapportsObjet = new HashSet<RapportEntreTiers>();
		}
		for (RapportEntreTiers r : rapportsObjet) {
			if (r instanceof RapportPrestationImposable) {
				continue; // [UNIREG-859] d'un point-de-vue métier, on peut ajouter deux fois le même rapport de travail
			}
			if (r != rapport && r.equalsTo(rapport)) {
				final String message = String.format(
						"Impossible d'ajouter le rapport-objet de type %s pour la période %s sur le tiers n°%d car il existe déjà.",
						rapport.getType(), DateRangeHelper.toString(rapport), numero);
				Assert.fail(message);
			}
		}
		rapportsObjet.add(rapport);
		Assert.isTrue(rapport.getObjetId() == null || rapport.getObjetId().equals(numero));
		rapport.setObjet(this);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the rapportsSujet
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BeZlcpNZEdygKK6Oe0tVlw?GETTER"
	 */
	@OneToMany(fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_RET_TRS_SUJ_ID")
	@JoinColumn(name = "TIERS_SUJET_ID")
	public Set<RapportEntreTiers> getRapportsSujet() {
		// begin-user-code
		return rapportsSujet;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theRapportsSujet
	 *            the rapportsSujet to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BeZlcpNZEdygKK6Oe0tVlw?SETTER"
	 */
	public void setRapportsSujet(Set<RapportEntreTiers> theRapportsSujet) {
		// begin-user-code
		rapportsSujet = theRapportsSujet;
		// end-user-code
	}

	/**
	 *
	 * @param rapport
	 *            the rapport to add
	 */
	public void addRapportSujet(RapportEntreTiers rapport) {
		if (rapportsSujet == null) {
			rapportsSujet = new HashSet<RapportEntreTiers>();
		}
		for (RapportEntreTiers r : rapportsSujet) {
			if (r instanceof RapportPrestationImposable) {
				continue; // [UNIREG-859] d'un point-de-vue métier, on peut ajouter deux fois le même rapport de travail
			}
			if (r != rapport && r.equalsTo(rapport)) {
				final String message = String.format(
						"Impossible d'ajouter le rapport-sujet de type %s pour la période %s sur le tiers n°%d car il existe déjà.",
						rapport.getType(), DateRangeHelper.toString(rapport), numero);
				Assert.fail(message);
			}
		}
		rapportsSujet.add(rapport);
		Assert.isTrue(rapport.getSujetId() == null || rapport.getSujetId().equals(numero));
		rapport.setSujet(this);
	}

	/**
	 * @return le rapport sujet du type demandé valide à cette date.
	 */
	@Transient
	public RapportEntreTiers getRapportSujetValidAt(RegDate date, TypeRapportEntreTiers type) {
		for (RapportEntreTiers rapportSujet : rapportsSujet) {
			if (rapportSujet.isValidAt(date) && rapportSujet.getType() == type) {
				return rapportSujet;
			}
		}
		return null;
	}

	/**
	 * @return le dernier rapport sujet du type demandé.
	 */
	@Transient
	public RapportEntreTiers getDernierRapportSujet(TypeRapportEntreTiers type) {
		RapportEntreTiers dernierRapport = null;
		for (RapportEntreTiers rapportSujet : rapportsSujet) {
			if (!rapportSujet.isAnnule() && type == rapportSujet.getType()) {
				if (dernierRapport == null
						|| RegDateHelper.isAfterOrEqual(rapportSujet.getDateDebut(), dernierRapport.getDateDebut(),
						NullDateBehavior.EARLIEST)) {
					dernierRapport = rapportSujet;
				}
			}
		}
		return dernierRapport;
	}

	/**
	 * @return le premier rapport sujet dont le tiers est objet.
	 */
	@Transient
	public RapportEntreTiers getPremierRapportSujet(TypeRapportEntreTiers type, Tiers tiers) {
		RapportEntreTiers premierRapport = null;
		for (RapportEntreTiers rapportSujet : rapportsSujet) {
			if (!rapportSujet.isAnnule() && type == rapportSujet.getType() && rapportSujet.getObjetId().equals(tiers.getId())) {
				if (premierRapport == null || RegDateHelper.isBeforeOrEqual(rapportSujet.getDateDebut(), premierRapport.getDateDebut(), NullDateBehavior.EARLIEST)) {
					premierRapport = rapportSujet;
				}
			}
		}
		return premierRapport;
	}

	/**
	 * @return le rapport objet du type demandé valide à cette date.
	 */
	@Transient
	public RapportEntreTiers getRapportObjetValidAt(RegDate date, TypeRapportEntreTiers type) {
		for (RapportEntreTiers rapportObjet : rapportsObjet) {
			if (rapportObjet.isValidAt(date) && rapportObjet.getType() == type) {
				return rapportObjet;
			}
		}
		return null;
	}

	/**
	 * @return le dernier rapport objet du type demandé.
	 */
	@Transient
	public RapportEntreTiers getDernierRapportObjet(TypeRapportEntreTiers type) {
		RapportEntreTiers dernierRapport = null;
		for (RapportEntreTiers rapportObjet : rapportsObjet) {
			if (!rapportObjet.isAnnule() && type == rapportObjet.getType()) {
				if (dernierRapport == null
						|| RegDateHelper.isAfterOrEqual(rapportObjet.getDateDebut(), dernierRapport.getDateDebut(),
						NullDateBehavior.EARLIEST)) {
					dernierRapport = rapportObjet;
				}
			}
		}
		return dernierRapport;
	}

	/**
	 * @return le premier rapport objet dont le tiers est sujet.
	 */
	@Transient
	public RapportEntreTiers getPremierRapportObjet(TypeRapportEntreTiers type, Tiers tiers) {
		RapportEntreTiers premierRapport = null;
		for (RapportEntreTiers rapportObjet : rapportsObjet) {
			if (!rapportObjet.isAnnule() && type == rapportObjet.getType() && rapportObjet.getSujetId().equals(tiers.getId())) {
				if (premierRapport == null || RegDateHelper.isBeforeOrEqual(rapportObjet.getDateDebut(), premierRapport.getDateDebut(), NullDateBehavior.EARLIEST)) {
					premierRapport = rapportObjet;
				}
			}
		}
		return premierRapport;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the adressesTiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_2w8Y0afTEdy6qP7Nc3dO8g?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_ADR_TRS_ID")
	public Set<AdresseTiers> getAdressesTiers() {
		// begin-user-code
		return adressesTiers;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theAdressesTiers
	 *            the adressesTiers to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_2w8Y0afTEdy6qP7Nc3dO8g?SETTER"
	 */
	public void setAdressesTiers(Set<AdresseTiers> theAdressesTiers) {
		// begin-user-code
		adressesTiers = theAdressesTiers;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the declarations
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZtOEseqfEdySTq6PFlf9jQ?GETTER"
	 */
	@OneToMany(mappedBy = "tiers", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_DECL_TRS_ID")
	public Set<Declaration> getDeclarations() {
		// begin-user-code
		return declarations;
		// end-user-code
	}

	/**
	 * @return les déclarations triées par ordre croissant.
	 */
	@Transient
	public List<Declaration> getDeclarationsSorted() {
		if (declarations == null) {
			return null;
		}
		List<Declaration> list = new ArrayList<Declaration>();
		list.addAll(declarations);
		Collections.sort(list, new DateRangeComparator<Declaration>());
		return list;
	}

	/**
	 * Retourne la déclaration active à une date donnée.
	 *
	 * @param date
	 *            la date à laquelle la déclaration est active, ou <b>null</b> pour obtenir la déclaration courante.
	 * @return la déclaration trouvée, ou nulle si aucune déclaration ne correspond aux critères.
	 */
	@Transient
	public Declaration getDeclarationActive(RegDate date) {

		if (declarations == null) {
			return null;
		}

		Declaration result = null;
		for (Declaration declaration : declarations) {
			if (declaration.isValidAt(date)) {
				result = declaration;
				break;
			}
		}

		return result;
	}

	/**Retourne la dernière declaration non annulée du tiers
	 *
	 * @return la derniere declaration ou null si le tiers n'a aucune déclaration
	 */
	@Transient
	public Declaration getDerniereDeclaration() {
		List<Declaration> listeTriees = getDeclarationsSorted();
		if (listeTriees != null) {
			final ListIterator<Declaration> iterator = listeTriees.listIterator(listeTriees.size());
			while (iterator.hasPrevious()) {
				final Declaration candidate = iterator.previous();
				if (!candidate.isAnnule()) {
					return candidate;

				}
			}
		}

		return null;

	}

	/**
	 * Retourne la liste des déclarations correspondantes à la période fiscale (= année) spécifiée.
	 *
	 * @param annee
	 *            l'année correspondant à la période fiscale
	 * @return une liste de déclaration, ou <b>null</b> si le tiers ne possède pas de déclaration pour la période spécifiée.
	 */
	public List<Declaration> getDeclarationForPeriode(int annee) {
		if (declarations == null) {
			return null;
		}

		List<Declaration> result = new ArrayList<Declaration>();
		for (Declaration declaration : getDeclarationsSorted()) {
			if (!declaration.isAnnule() && declaration.getPeriode().getAnnee() == annee) {
				result.add(declaration);
			}
		}

		return result;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theDeclarations
	 *            the declarations to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZtOEseqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDeclarations(Set<Declaration> theDeclarations) {
		// begin-user-code
		declarations = theDeclarations;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the forsFiscaux
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7ofwMV-eEdyCxumqfWBxMQ?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_FF_TIERS_ID")
	public Set<ForFiscal> getForsFiscaux() {
		// begin-user-code
		return forsFiscaux;
		// end-user-code
	}

	/**
	 * Retourne les fors fiscaux valides à une date donnée et séparés par type (sauf les fors autres impots). Les fors annulés sont ignorés.
	 *
	 * @param date la date de validité des fors retournés
	 * @param sort vrai si les fors doivent être triés par ordre chronologique
	 * @return les fors fiscaux triés par types
	 */
	@Transient
	public ForsParTypeAt getForsParTypeAt(RegDate date, boolean sort) {
		return new ForsParTypeAt(forsFiscaux, date, sort);
	}

	/**
	 * Retourne les fors fiscaux séparés par type (sauf les fors autres impots). Les fors annulés sont ignorés.
	 *
	 * @param sort vrai si les fors doivent être triés par ordre chronologique
	 * @return les fors fiscaux triés par types
	 */
	@Transient
	public ForsParType getForsParType(boolean sort) {
		return new ForsParType(forsFiscaux, sort);
	}

	/**
	 * Trie les fors principaux par date, sans les annulés
	 *
	 * @return Renvoie les fors principaux
	 */
	@Transient
	public List<ForFiscalPrincipal> getForsFiscauxPrincipauxActifsSorted() {
		List<ForFiscalPrincipal> ffps = null;
		if (forsFiscaux != null) {
			ffps = new ArrayList<ForFiscalPrincipal>();
			for (ForFiscal ff : forsFiscaux) {
				if (ff instanceof ForFiscalPrincipal && !ff.isAnnule()) {
					ffps.add((ForFiscalPrincipal) ff);
				}
			}
			Collections.sort(ffps, new DateRangeComparator<ForFiscalPrincipal>());
		}
		return ffps;
	}

	/**
	 * Retourne les fors triés par - La date d'ouverture - Leur type, selon l'ordinal de l'enum TypeAutoriteFiscale
	 *
	 * @return
	 */
	@Transient
	public List<ForFiscal> getForsFiscauxSorted() {
		List<ForFiscal> fors = null;
		if (forsFiscaux != null) {
			fors = new ArrayList<ForFiscal>();
			fors.addAll(forsFiscaux);
			Collections.sort(fors, new DateRangeComparator<ForFiscal>() {
				@Override
				public int compare(ForFiscal o1, ForFiscal o2) {
					int comparisonDates = super.compare(o1, o2);
					if (comparisonDates == 0) {
						// à dates égales, il faut comparer selon le type d'autorité fiscale
						return o1.getTypeAutoriteFiscale().ordinal() - o2.getTypeAutoriteFiscale().ordinal();
					}
					else {
						return comparisonDates;
					}
				}
			});
		}
		return fors;
	}

	/**
	 * @param sort
	 *            <code>true</code> si les fors doivent être triés; <code>false</code> autrement.
	 * @return les fors fiscaux non annulés
	 */
	@Transient
	public List<ForFiscal> getForsFiscauxNonAnnules(boolean sort) {
		List<ForFiscal> fors = new ArrayList<ForFiscal>();
		if (forsFiscaux != null) {
			for (ForFiscal f : forsFiscaux) {
				if (!f.isAnnule()) {
					fors.add(f);
				}
			}
		}
		if (sort) {
			Collections.sort(fors, new DateRangeComparator<ForFiscal>());
		}
		return fors;
	}

	/**
	 * Ajoute un for fiscal.
	 *
	 * @param nouveauForFiscal
	 *            le for fiscal à ajouter
	 */
	public void addForFiscal(ForFiscal nouveauForFiscal) {
		if (this.forsFiscaux == null) {
			this.forsFiscaux = new HashSet<ForFiscal>();
		}
		this.forsFiscaux.add(nouveauForFiscal);
		Assert.isTrue(nouveauForFiscal.getTiers() == null || nouveauForFiscal.getTiers() == this);
		nouveauForFiscal.setTiers(this);
	}

	/**
	 * Retourne le for principal actif à une date donnée.
	 *
	 * @param date
	 *            la date à laquelle le for principal est actif, ou <b>null</b> pour obtenir le for courant.
	 *
	 * @return le for principal correspondant, ou nulle si aucun for ne correspond aux critères.
	 */
	@Transient
	public ForFiscalPrincipal getForFiscalPrincipalAt(RegDate date) {

		if (forsFiscaux == null) {
			return null;
		}

		for (ForFiscal f : forsFiscaux) {
			if (f.isPrincipal() && f.isValidAt(date)) {
				return (ForFiscalPrincipal) f;
			}
		}

		return null;
	}

	/**
	 * Retourne la liste de tous les fors actifs à une date donnée.
	 *
	 * @param date
	 *            la date à laquelle les fors sont actifs, ou <b>null</b> pour obtenir tous les fors courants.
	 *
	 * @return la liste des fors correspondant, qui peut être vide si aucun for ne correspond aux critères.
	 */
	@Transient
	public List<ForFiscal> getForsFiscauxValidAt(RegDate date) {

		List<ForFiscal> fors = new ArrayList<ForFiscal>();
		if (forsFiscaux != null) {
			for (ForFiscal f : forsFiscaux) {
				if (f.isValidAt(date)) {
					fors.add(f);
				}
			}
		}
		return fors;
	}

	@Transient
	public ForFiscalPrincipal getPremierForFiscalPrincipal() {
		List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (ForFiscal forFiscal : list) {
				if (!forFiscal.isAnnule() && forFiscal.isPrincipal()) {
					return (ForFiscalPrincipal) forFiscal;
				}
			}
		}
		return null;
	}

	// ***********************************************
	@Transient
	public ForFiscalPrincipal getDernierForFiscalPrincipal() {

		List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				ForFiscal forFiscal = list.get(i);
				if (!forFiscal.isAnnule() && forFiscal.isPrincipal()) {
					return (ForFiscalPrincipal) forFiscal;
				}
			}
		}
		return null;
	}

	// ***********************************************
	@Transient
	public ForFiscalPrincipal getDernierForFiscalPrincipalAvant(RegDate date) {

		final List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				final ForFiscal forFiscal = list.get(i);
				if (!forFiscal.isAnnule() && forFiscal.isPrincipal() && RegDateHelper.isBeforeOrEqual(forFiscal.getDateDebut(), date, NullDateBehavior.LATEST)) {
					return (ForFiscalPrincipal) forFiscal;
				}
			}
		}
		return null;
	}

	// ***********************************************
	@Transient
	public ForFiscalPrincipal getDernierForFiscalPrincipalVaudois() {

		List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				ForFiscal forFiscal = list.get(i);
				if (!forFiscal.isAnnule() && forFiscal.isPrincipal()
						&& TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forFiscal.getTypeAutoriteFiscale()) {
					return (ForFiscalPrincipal) forFiscal;
				}
			}
		}
		return null;
	}

	// ***********************************************
	@Transient
	public ForDebiteurPrestationImposable getDernierForDebiteur() {

		List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				ForFiscal forFiscal = list.get(i);
				if (!forFiscal.isAnnule() && forFiscal.isDebiteur()) {
					return (ForDebiteurPrestationImposable) forFiscal;
				}
			}
		}
		return null;
	}

	/**
	 * <b>Attention !</b> cette méthode ne doit pas être appelée directement ! Il faut utiliser {@link #addForFiscal(ForFiscal)} pour
	 * ajouter un for !
	 *
	 * @param theForsFiscaux
	 *            the forsFiscaux to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7ofwMV-eEdyCxumqfWBxMQ?SETTER"
	 */
	public void setForsFiscaux(Set<ForFiscal> theForsFiscaux) {
		// begin-user-code
		forsFiscaux = theForsFiscaux;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the debiteurInactif
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_stv7YEE7Ed2XrapGHNAWZw?GETTER"
	 */
	@Column(name = "DEBITEUR_INACTIF", nullable = false)
	public boolean isDebiteurInactif() {
		// begin-user-code
		return debiteurInactif;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theDebiteurInactif
	 *            the debiteurInactif to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_stv7YEE7Ed2XrapGHNAWZw?SETTER"
	 */
	public void setDebiteurInactif(boolean theDebiteurInactif) {
		// begin-user-code
		debiteurInactif = theDebiteurInactif;
		// end-user-code
	}

	public void addAdresseTiers(AdresseTiers adresse) {
		Assert.notNull(adresse);
		if (adressesTiers == null) {
			adressesTiers = new HashSet<AdresseTiers>();
		}
		adresse.setTiers(this);
		adressesTiers.add(adresse);
	}

	public void addDeclaration(Declaration declaration) {
		Assert.notNull(declaration);

		if (declarations == null) {
			declarations = new HashSet<Declaration>();
		}

		if (declaration instanceof DeclarationImpotOrdinaire) {
			/*
			 * Les déclarations d'impôt ordinaires possèdent un numéro de séquence (unique par année) qui doit être calculé au moment de
			 * l'insertion.
			 */
			DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) declaration;
			if (di.getNumero() == null) {
				int numero = 0;
				final int annee = declaration.getPeriode().getAnnee().intValue();
				for (Declaration d : declarations) {
					if (d.getPeriode().getAnnee() == annee) {
						++numero;
					}
				}
				di.setNumero(numero + 1);
			}
		}

		declarations.add(declaration);
		declaration.setTiers(this);
	}

	/**
	 * Méthode réservée à la migration qui ajoute toutes les déclarations d'un coup. Les déclarations doivent posséder leur numéro de
	 * séquence.
	 */
	public void addAllDeclarations(Collection<Declaration> coll) {
		Assert.notNull(coll);
		if (declarations == null) {
			declarations = new HashSet<Declaration>();
		}
		for (Declaration declaration : coll) {
			if (declaration instanceof DeclarationImpotOrdinaire) {
				Assert.notNull(((DeclarationImpotOrdinaire) declaration).getNumero());
			}
			declarations.add(declaration);
			declaration.setTiers(this);
		}
	}

	@Transient
	public boolean isDirty() {
		return indexDirty == null ? false : indexDirty;
	}

	@Column(name = "INDEX_DIRTY", updatable = false)
	// updatable false => la mise-à-jour de cette valeur doit être faite uniquement par des update SQL pour éviter des problèmes d'optimistic locking
	public Boolean getIndexDirty() {
		return indexDirty;
	}

	// protected -> mise-à-jour réservée à Hibernate
	public void setIndexDirty(Boolean dirty) {
		this.indexDirty = dirty;
	}

	/**
	 * @return la date à partir de laquelle le tiers devra être réindexé; ou <b>null</b> si le tiers n'a pas besoin d'être réindexé dans le futur.
	 */
	@Column(name = "REINDEX_ON")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getReindexOn() {
		return reindexOn;
	}

	public void setReindexOn(RegDate reindexOn) {
		this.reindexOn = reindexOn;
	}

	/**
	 * Agende une réindexation du tiers dans le futur.
	 * <p/>
	 * En cas de collision entre deux dates futures de réindexation, la date la plus éloignée dans le futur sera choisie. L'idée est de s'assurer que
	 * l'indexation du tiers soit cohérente à partir d'une certaine date dans le futur, même s'il pour cela elle risque d'être temporairement incohérente
	 * entre aujourd'hui et cette date.
	 *
	 * @param reindexOn la date à partir de laquelle le tiers devra être réindexé.
	 */
	public void scheduleReindexationOn(RegDate reindexOn) {
		Assert.notNull(reindexOn);
		if (this.reindexOn == null) {
			this.reindexOn = reindexOn;
		}
		else {
			this.reindexOn = RegDateHelper.maximum(this.reindexOn, reindexOn, NullDateBehavior.EARLIEST);
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return L'office d'impôt qui gère le tiers. Retourne <b>null</b> si le tiers n'a pas encore été persisté, ou s'il ne possède pas de
	 *         for de gestion.
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R3578KdlEd2Ebedj8uu8CQ?GETTER"
	 */
	@Column(name = "OID")
	public Integer getOfficeImpotId() {
		// begin-user-code
		return officeImpotId;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <b>Utilisation uniquement autorisée pour l'intercepteur chargée de tenir à jour cette valeur !</b> <!--
	 * end-user-doc -->
	 *
	 * @param theOfficeImpotId
	 *            the officeImpotId to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R3578KdlEd2Ebedj8uu8CQ?SETTER"
	 */
	public void setOfficeImpotId(Integer officeImpotID) {
		// begin-user-code
		this.officeImpotId = officeImpotID;
		// end-user-code
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		Tiers tiersClone = null;
		try {
			// On récupère l'instance à renvoyer par l'appel de la
			// méthode super.clone()
			tiersClone = (Tiers) super.clone();
			tiersClone.setForsFiscaux(null);
		}
		catch (CloneNotSupportedException cnse) {
			LOGGER.debug(cnse);
		}
		// tiersClone.setAdressesTiers(null);

		// on renvoie le clone
		return tiersClone;
	}

	/**
	 * {@inheritDoc}
	 */
	public ValidationResults validate() {

		ValidationResults results = new ValidationResults();

		// UNIREG-601 on ignore toutes les erreurs pour un tiers annulé
		if (!isAnnule()) {
			results.merge(validateAdresses());
			results.merge(validateFors());
			results.merge(validateDeclarations());
			results.merge(validateRapports());
		}

		return results;
	}

	protected ValidationResults validateRapports() {
		// rien de spécial ici
		return new ValidationResults();
	}

	private ValidationResults validateDeclarations() {

		ValidationResults results = new ValidationResults();

		final List<Declaration> decls = getDeclarationsSorted();
		if (decls != null) {
			Declaration last = null;
			for (Declaration d : decls) {
				if (d.isAnnule()) {
					continue;
				}
				// On valide la déclaration pour elle-même
				results.merge(d.validate());

				// Les plages de validité des déclarations ne doivent pas se chevaucher
				if (last != null && DateRangeHelper.intersect(last, d)) {
					final String message = String.format("La déclaration n°%d %s chevauche la déclaration précédente n°%d %s", d.getId(),
							DateRangeHelper.toString(d), last.getId(), DateRangeHelper.toString(last));
					results.addError(message);
				}
				last = d;
			}
		}

		return results;
	}

	private ValidationResults validateAdresses() {

		ValidationResults results = new ValidationResults();

		results.merge(validateTypeAdresses());

		for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
			results.merge(validateAdresses(getAdressesTiersSorted(type)));
		}

		return results;
	}

	/**
	 * Valide les adresses tiers en fonction de leurs type
	 */
	protected abstract ValidationResults validateTypeAdresses();

	public ValidationResults validateFors() {

		// dumpForDebug();

		ValidationResults results = new ValidationResults();

		// On valide tous les fors pour eux-mêmes
		if (forsFiscaux != null) {
			for (ForFiscal f : forsFiscaux) {
				results.merge(f.validate());
			}
		}

		return results;
	}

	/**
	 * @return vrai s'il existe un for principal (ou une succession ininterrompue de fors principaux) durant la période spécifiée.
	 */
	public static boolean existForPrincipal(List<ForFiscalPrincipal> principaux, RegDate dateDebut, RegDate dateFin) {

		int indexCandidat = -1;

		// Vérification de la date de début
		for (int i = 0; i < principaux.size(); ++i) {
			final ForFiscalPrincipal f = principaux.get(i);

			if (dateDebut != null && f.getDateFin() != null && f.getDateFin().isBefore(dateDebut)) {
				// on est pas encore arrivé à la date de début => on continue
				continue;
			}
			else if (f.getDateDebut() == null || (dateDebut != null && f.getDateDebut().isBeforeOrEqual(dateDebut))) {
				// on a trouvé un for qui contient la date de début => on saute à la vérification de la date de fin
				indexCandidat = i;
				break;
			}
			else if (dateDebut == null || (dateFin != null && f.getDateDebut().isAfter(dateFin))) {
				// on a dépassé la date de fin => rien trouvé
				return false;
			}
		}
		if (indexCandidat < 0) {
			// on a rien trouvé.
			return false;
		}

		// Vérification de la date de fin
		RegDate dateRaccord = null;
		for (int i = indexCandidat; i < principaux.size(); ++i) {
			final ForFiscalPrincipal f = principaux.get(i);

			if (dateRaccord != null && !dateRaccord.equals(f.getDateDebut())) {
				// il y a bien deux fors dans la plage spécifiée, mais ils ne se touchent pas => pas trouvé
				return false;
			}
			else if (f.getDateFin() == null || (dateFin != null && f.getDateFin().isAfterOrEqual(dateFin))) {
				// le for courant contient la date de fin => on a trouvé
				return true;
			}
			else {
				// le for ne s'étend pas sur toute la plage spécifiée => on continue avec le for suivant en spécifiant une date de raccord
				dateRaccord = f.getDateFin().getOneDayAfter();
			}
		}

		// on a pas trouvé de for s'étendant sur toute la plage demandée
		return false;
	}

	private ValidationResults validateAdresses(List<AdresseTiers> sorted) {

		if (sorted == null || sorted.size() == 0) {
			return null;
		}

		// on ignore les adresses annulées
		// [UNIREG-467] on crée une nouvelle liste pour avoir les indexes corrects
		List<AdresseTiers> list = new ArrayList<AdresseTiers>(sorted.size());
		for (AdresseTiers a : sorted) {
			if (!a.isAnnule()) {
				list.add(a);
			}
		}

		ValidationResults results = new ValidationResults();

		RegDate lastDateFin = null;
		RegDate lastDateDebut = null;
		for (int i = 0; i < list.size(); i++) {
			AdresseTiers adr = list.get(i);
			if (i > 0) {
				if (lastDateFin == null || adr.getDateDebut().isBeforeOrEqual(lastDateFin)) {
					// Overlap
					final String message =
							String.format("L'adresse fiscale numéro %d (type=%s début=%s fin=%s) chevauche l'adresse fiscale numéro %d (type=%s début=%s fin=%s)", i,
									adr.getUsage().name().toLowerCase(), RegDateHelper.dateToDisplayString(lastDateDebut), RegDateHelper.dateToDisplayString(lastDateFin), (i + 1),
									adr.getUsage().name().toLowerCase(), RegDateHelper.dateToDisplayString(adr.getDateDebut()), RegDateHelper.dateToDisplayString(adr.getDateFin()));
					results.addError(message);
				}
			}

			// Date de début doit être avant la date de fin
			if (adr.getDateDebut() != null && adr.getDateFin() != null && !adr.getDateDebut().isBefore(adr.getDateFin())) {
				results.addError("La date de début de l'adresse " + i + " est après la date de fin");
			}

			// Date debut peut pas etre nulle
			if (adr.getDateDebut() == null) {
				results.addError("L'adresse " + i + " n'a pas de date de début");
			}

			lastDateDebut = adr.getDateDebut();
			lastDateFin = adr.getDateFin();
		}

		return results;
	}

	/**
	 * Un tiers "annulé" (au sens technique de la date d'annulation) est dit "désactivé" pour toute date ;
	 * sinon, cela dépend de ses fors
	 * @param date date de référencce
	 */
	@Transient
	public final boolean isDesactive(RegDate date) {
		return isAnnule() || isDesactiveSelonFors(date);
	}

	@Transient
	protected boolean isDesactiveSelonFors(RegDate date) {
		return false;
	}

	@Transient
	public RegDate getDateDesactivation() {
		return null;
	}

	/**
	 * Vérifie l'activité du tiers pour une date donnée.
	 * <p>
	 * Un tiers est dit <i>actif</i> à une date donnée si:
	 *
	 * <pre>
	 * date = [dateDebutActivite, dateFinActivite]
	 * </pre>
	 *
	 * @param date
	 *            la date donnée, ou null pour vérifier si le tiers est actif.
	 * @return vrai si le tiers est actif, faux autrement
	 */
	@Deprecated
	public boolean isActif(Date date) {
		return !isAnnule()
				&& RegDateHelper.isBetween(RegDate.get(date), getDateDebutActivite(), getDateFinActivite(), NullDateBehavior.LATEST);
	}

	/**
	 * @return La ligne 1 du role de ce tiers (la ligne2 est obtenue au travers du service tiers)
	 */
	@Transient
	public abstract String getRoleLigne1();

	/**
	 * Calcul la date de début d'activité du tiers.
	 *
	 * @return la date minimum de validité des fors fiscaux. Ou <b>null</b> si la date d'ouverture d'un des fors est nulle ou si le tiers ne
	 *         possède pas du tout de for.
	 */
	@Transient
	public RegDate getDateDebutActivite() {
		RegDate date = RegDate.getLateDate();
		if (forsFiscaux != null) {
			for (ForFiscal forFiscal : forsFiscaux) {
				if (forFiscal.isAnnule()) {
					continue;
				}
				date = RegDateHelper.minimum(date, forFiscal.getDateDebut(), NullDateBehavior.EARLIEST);
			}
		}
		if (date == RegDate.getLateDate()) {
			date = null;
		}
		return date;
	}

	/**
	 * Calcul la date de fin d'activité du tiers.
	 *
	 * @return la date maximum de validité des fors fiscaux. Ou <b>null</b> s'il y a un for actif ouvert ou si le tiers ne possède pas du
	 *         tout de for.
	 */
	@Transient
	public RegDate getDateFinActivite() {
		RegDate date = RegDate.getEarlyDate();
		if (forsFiscaux != null) {
			for (ForFiscal forFiscal : forsFiscaux) {
				if (forFiscal.isAnnule()) {
					continue;
				}
				date = RegDateHelper.maximum(date, forFiscal.getDateFin(), NullDateBehavior.LATEST);
			}
		}
		if (date == RegDate.getEarlyDate()) {
			date = null;
		}
		return date;
	}

	/**
	 * Renvoi le ForDebiteurPrestationImposable actif à la date donnée en entrée
	 *
	 * @param date
	 * @return
	 */
	@Transient
	public ForDebiteurPrestationImposable getForDebiteurPrestationImposableAt(RegDate date) {

		if (forsFiscaux == null) {
			return null;
		}

		for (ForFiscal f : forsFiscaux) {
			if (f instanceof ForDebiteurPrestationImposable) {
				ForDebiteurPrestationImposable forDpi = (ForDebiteurPrestationImposable) f;
				if (forDpi.isValidAt(date)) {
					return forDpi;
				}
			}
		}

		return null;
	}

	/**
	 * Renvoi le premier ForDebiteurPrestationImposable actif après la date donnée en entrée
	 *
	 * @param date
	 * @return
	 */
	@Transient
	public ForDebiteurPrestationImposable getForDebiteurPrestationImposableAfter(RegDate date) {

		if (forsFiscaux == null) {
			return null;
		}

		for (ForFiscal f : getForsFiscauxSorted()) {
			if (f instanceof ForDebiteurPrestationImposable) {
				ForDebiteurPrestationImposable forDpi = (ForDebiteurPrestationImposable) f;
				if (forDpi.getDateDebut().isAfter(date) && !forDpi.isAnnule()) {
					return forDpi;
				}
			}
		}

		return null;
	}

	/**
	 * Renvoie la liste de fors fiscaux principaux débutant à ou après la date demandée (y compris les fors annulés).
	 * @param date date de référence
	 * @return liste des fors principaux demandés
	 */
	@Transient
	public List<ForFiscalPrincipal> getForsFiscauxPrincipauxOuvertsApres(RegDate date) {

		Assert.notNull(date);
		List<ForFiscalPrincipal> fors = new ArrayList<ForFiscalPrincipal>();
		for (ForFiscal ff : getForsFiscauxSorted()) {
			if (ff.isPrincipal() && date.isBeforeOrEqual(ff.getDateDebut())) {
				fors.add((ForFiscalPrincipal) ff);
			}
		}
		return fors;
	}

	public void dumpForDebug() {
		dumpForDebug(0);
	}

	protected void dumpForDebug(int nbTabs) {
		ddump(nbTabs, "Tiers: " + numero);
		if (getForsFiscauxSorted() != null) {
			for (ForFiscal ff : getForsFiscauxSorted()) {
				ddump(nbTabs, ff.getClass().getSimpleName());
				ff.dumpForDebug(nbTabs + 1);
			}
		}
	}

	/**
	 * return le nom de la classe du tiers
	 */
	@Transient
	public abstract String getNatureTiers();

	@Override
	public String toString() {
		return getClass().getSimpleName() + " n°" + numero;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		Tiers other = obj;
		if (adresseBicSwift == null) {
			if (other.adresseBicSwift != null)
				return false;
		}
		else if (!adresseBicSwift.equals(other.adresseBicSwift))
			return false;
		if (adresseCourrierElectronique == null) {
			if (other.adresseCourrierElectronique != null)
				return false;
		}
		else if (!adresseCourrierElectronique.equals(other.adresseCourrierElectronique))
			return false;
		if (adressesTiers == null) {
			if (other.adressesTiers != null)
				return false;
		}
		else if (!adressesTiers.equals(other.adressesTiers))
			return false;
		if (blocageRemboursementAutomatique == null) {
			if (other.blocageRemboursementAutomatique != null)
				return false;
		}
		else if (!blocageRemboursementAutomatique.equals(other.blocageRemboursementAutomatique))
			return false;
		if (complementNom == null) {
			if (other.complementNom != null)
				return false;
		}
		else if (!complementNom.equals(other.complementNom))
			return false;
		if (debiteurInactif != other.debiteurInactif)
			return false;
		if (declarations == null) {
			if (other.declarations != null)
				return false;
		}
		else if (!declarations.equals(other.declarations))
			return false;
		if (forsFiscaux == null) {
			if (other.forsFiscaux != null)
				return false;
		}
		else if (!forsFiscaux.equals(other.forsFiscaux))
			return false;
		if (numero == null) {
			if (other.numero != null)
				return false;
		}
		else if (!numero.equals(other.numero))
			return false;
		if (numeroCompteBancaire == null) {
			if (other.numeroCompteBancaire != null)
				return false;
		}
		else if (!numeroCompteBancaire.equals(other.numeroCompteBancaire))
			return false;
		if (numeroTelecopie == null) {
			if (other.numeroTelecopie != null)
				return false;
		}
		else if (!numeroTelecopie.equals(other.numeroTelecopie))
			return false;
		if (numeroTelephonePortable == null) {
			if (other.numeroTelephonePortable != null)
				return false;
		}
		else if (!numeroTelephonePortable.equals(other.numeroTelephonePortable))
			return false;
		if (numeroTelephonePrive == null) {
			if (other.numeroTelephonePrive != null)
				return false;
		}
		else if (!numeroTelephonePrive.equals(other.numeroTelephonePrive))
			return false;
		if (numeroTelephoneProfessionnel == null) {
			if (other.numeroTelephoneProfessionnel != null)
				return false;
		}
		else if (!numeroTelephoneProfessionnel.equals(other.numeroTelephoneProfessionnel))
			return false;
		if (personneContact == null) {
			if (other.personneContact != null)
				return false;
		}
		else if (!personneContact.equals(other.personneContact))
			return false;
		if (rapportsObjet == null) {
			if (other.rapportsObjet != null)
				return false;
		}
		else if (!rapportsObjet.equals(other.rapportsObjet))
			return false;
		if (rapportsSujet == null) {
			if (other.rapportsSujet != null)
				return false;
		}
		else if (!rapportsSujet.equals(other.rapportsSujet))
			return false;
		if (titulaireCompteBancaire == null) {
			if (other.titulaireCompteBancaire != null)
				return false;
		}
		else if (!titulaireCompteBancaire.equals(other.titulaireCompteBancaire))
			return false;
		return true;
	}
}
