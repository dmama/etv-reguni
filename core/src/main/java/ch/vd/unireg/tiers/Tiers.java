package ch.vd.unireg.tiers;

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
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.BusinessComparable;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.documentfiscal.DocumentFiscal;
import ch.vd.unireg.etiquette.EtiquetteTiers;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Personne avec laquelle l'ACI entretien une relation, de nature fiscale ou autre. Cette
 * personne peut être:
 * <ul>
 *     <li>Une personne physique, connue ou non du contrôle des habitants</li>
 *     <li>Une organisation (personne morale ou entité sans personnalité juridique, connue ou non du registre des personnes morales)</li>
 *     <li>Une autre communauté de personnes sans personnalité juridique complète (Pour le moment, limité au couple de personnes mariées ou liées par un partenariat enregistré, vivant en ménage commun
 *     (c'est-à-dire non séparées ou dont le partenariat n'est pas pas dissous)).</li>
 * </ul>
 */
@Entity
@Table(name = "TIERS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TIERS_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class Tiers extends HibernateEntity implements BusinessComparable<Tiers> {

	/**
	 * Numero unique attribue au tiers, qui correspond pour les contribuables PP au numero de contribuable.
	 */
	private Long numero;

	private String complementNom;
	private String personneContact;
	private String numeroTelephonePrive;
	private String numeroTelephoneProfessionnel;
	private String numeroTelephonePortable;
	private String numeroTelecopie;
	private String adresseCourrierElectronique;
	private Boolean blocageRemboursementAutomatique = Boolean.TRUE;

	private boolean debiteurInactif = false;
	private Boolean indexDirty;

	/**
	 * [UNIREG-1979] Date à partir de laquelle le tiers devra être réindexé, si elle est renseignée.
	 */
	private RegDate reindexOn;

	/**
	 * L'office d'impôt qui gère le tiers. Cette valeur est automatiquement renseignée par un intercepteur
	 * hibernate.
	 * <p>
	 * On peut normalement déduire cette valeur à partir du for de gestion courant, mais pour des raisons de performances elle est cachée
	 * ici.
	 *
	 */
	private Integer officeImpotId;

	private Set<RapportEntreTiers> rapportsObjet;
	private Set<RapportEntreTiers> rapportsSujet;
	private Set<AdresseTiers> adressesTiers;
	private Set<DocumentFiscal> documentsFiscaux;
	private Set<ForFiscal> forsFiscaux;
	private Set<Remarque> remarques;
	private Set<EtiquetteTiers> etiquettes;
	private Set<CoordonneesFinancieres> coordonneesFinancieres;

	public Tiers() {
	}

	public Tiers(long numero) {
		this.numero = numero;
	}

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
	@GenericGenerator(name = "tiersSequence", strategy = "ch.vd.unireg.tiers.TiersMultiSequenceGenerator")
	/*
	 * @GenericGenerator(name = "tiersSequence", strategy = "ch.vd.unireg.tiers.MultiSequenceGenerator", parameters = { @Parameter(name =
	 * "max_lo", value = "50"), @Parameter(name = "sequence", value = "S_TIERS"), @Parameter(name = "entitiesSequencesMap", value =
	 * "ch.vd.unireg.tiers.Habitant=S_HABITANT, ch.vd.unireg.tiers.NonHabitant=S_NON_HABITANT"), @Parameter(name = "sequenceOffset",
	 * value = "1000000") } )
	 */
	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long theNumero) {
		numero = theNumero;
	}

	@Column(name = "COMPLEMENT_NOM", length = LengthConstants.TIERS_NOM)
	public String getComplementNom() {
		return complementNom;
	}

	public void setComplementNom(String theComplementNom) {
		complementNom = theComplementNom;
	}

	@Column(name = "PERSONNE_CONTACT", length = LengthConstants.TIERS_PERSONNE)
	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String thePersonneContact) {
		personneContact = thePersonneContact;
	}

	@Column(name = "NUMERO_TEL_PRIVE", length = LengthConstants.TIERS_NUMTEL)
	public String getNumeroTelephonePrive() {
		return numeroTelephonePrive;
	}

	public void setNumeroTelephonePrive(String theNumeroTelephonePrive) {
		numeroTelephonePrive = theNumeroTelephonePrive;
	}

	@Column(name = "NUMERO_TEL_PROF", length = LengthConstants.TIERS_NUMTEL)
	public String getNumeroTelephoneProfessionnel() {
		return numeroTelephoneProfessionnel;
	}

	public void setNumeroTelephoneProfessionnel(String theNumeroTelephoneProfessionnel) {
		numeroTelephoneProfessionnel = theNumeroTelephoneProfessionnel;
	}

	@Column(name = "NUMERO_TEL_PORTABLE", length = LengthConstants.TIERS_NUMTEL)
	public String getNumeroTelephonePortable() {
		return numeroTelephonePortable;
	}

	public void setNumeroTelephonePortable(String theNumeroTelephonePortable) {
		numeroTelephonePortable = theNumeroTelephonePortable;
	}

	@Column(name = "NUMERO_TELECOPIE", length = LengthConstants.TIERS_NUMTEL)
	public String getNumeroTelecopie() {
		return numeroTelecopie;
	}

	public void setNumeroTelecopie(String theNumeroTelecopie) {
		numeroTelecopie = theNumeroTelecopie;
	}

	@Column(name = "ADRESSE_EMAIL", length = LengthConstants.TIERS_EMAIL)
	public String getAdresseCourrierElectronique() {
		return adresseCourrierElectronique;
	}

	public void setAdresseCourrierElectronique(String theAdresseCourrierElectronique) {
		adresseCourrierElectronique = theAdresseCourrierElectronique;
	}

	/**
	 * @return le compte bancaire actuellement valable; ou <i>null</i> si aucun compte bancaire n'est valable actuellement.
	 */
	@Transient
	@Nullable
	public CompteBancaire getCompteBancaireCourant() {
		return coordonneesFinancieres == null ? null : coordonneesFinancieres.stream()
				.filter(c -> c.isValidAt(null))
				.findAny()
				.map(CoordonneesFinancieres::getCompteBancaire)
				.orElse(null);
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	public Set<CoordonneesFinancieres> getCoordonneesFinancieres() {
		return coordonneesFinancieres;
	}

	public void setCoordonneesFinancieres(Set<CoordonneesFinancieres> coordonneesFinancieres) {
		this.coordonneesFinancieres = coordonneesFinancieres;
	}

	public void addCoordonneesFinancieres(@NotNull CoordonneesFinancieres coordonnees) {
		if (this.coordonneesFinancieres == null) {
			this.coordonneesFinancieres = new HashSet<>();
		}
		this.coordonneesFinancieres.add(coordonnees);
		coordonnees.setTiers(this);
	}

	/**
	 * @return les coordonnées financières actuellement valable; ou <i>null</i> si aucune coordonnées financières n'est valable actuellement.
	 */
	@Transient
	@Nullable
	public CoordonneesFinancieres getCoordonneesFinancieresCourantes() {
		return coordonneesFinancieres == null ? null : coordonneesFinancieres.stream()
				.filter(c -> c.isValidAt(null))
				.findAny()
				.orElse(null);
	}

	@Transient
	public List<AdresseTiers> getAdressesTiersSorted() {
		if (adressesTiers == null) {
			return null;
		}
		final List<AdresseTiers> list = new ArrayList<>(adressesTiers);
		list.sort(DateRangeComparator::compareRanges);
		return list;
	}

	@Transient
	public List<AdresseTiers> getAdressesTiersSorted(TypeAdresseTiers type) {
		if (adressesTiers == null) {
			return null;
		}
		return adressesTiers.stream()
				.filter(a -> type == a.getUsage())
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());
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
		final List<AdresseTiers> list = adressesTiers.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(a -> type == a.getUsage())
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());

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

	@Nullable
	private static <T extends DateRange> T getAt(Collection<T> collection, @Nullable RegDate date, Predicate<? super T> subSetFilter) {
		if (collection == null || collection.isEmpty()) {
			return null;
		}
		return collection.stream()
				.filter(subSetFilter)
				.filter(elt -> elt.isValidAt(date))
				.findFirst()
				.orElse(null);
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
	public AdresseTiers getAdresseActive(TypeAdresseTiers type, @Nullable RegDate date) {
		Assert.notNull(type);
		return getAt(adressesTiers, date, adr -> adr.getUsage() == type);
	}

	@Column(name = "BLOC_REMB_AUTO")
	public Boolean getBlocageRemboursementAutomatique() {
		return blocageRemboursementAutomatique;
	}

	public void setBlocageRemboursementAutomatique(Boolean theBlocageRemboursementAutomatique) {
		blocageRemboursementAutomatique = theBlocageRemboursementAutomatique;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_RET_TRS_OBJ_ID")
	@JoinColumn(name = "TIERS_OBJET_ID")
	public Set<RapportEntreTiers> getRapportsObjet() {
		return rapportsObjet;
	}

	public void setRapportsObjet(Set<RapportEntreTiers> theRapportsObjet) {
		rapportsObjet = theRapportsObjet;
	}

	public void addRapportObjet(RapportEntreTiers rapport) {
		if (rapportsObjet == null) {
			rapportsObjet = new HashSet<>();
		}
		rapportsObjet.add(rapport);
		Assert.isTrue(rapport.getObjetId() == null || rapport.getObjetId().equals(numero));
		rapport.setObjet(this);
	}

	@OneToMany(fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_RET_TRS_SUJ_ID")
	@JoinColumn(name = "TIERS_SUJET_ID")
	public Set<RapportEntreTiers> getRapportsSujet() {
		return rapportsSujet;
	}

	public void setRapportsSujet(Set<RapportEntreTiers> theRapportsSujet) {
		rapportsSujet = theRapportsSujet;
	}

	public void addRapportSujet(RapportEntreTiers rapport) {
		if (rapportsSujet == null) {
			rapportsSujet = new HashSet<>();
		}
		rapportsSujet.add(rapport);
		Assert.isTrue(rapport.getSujetId() == null || rapport.getSujetId().equals(numero));
		rapport.setSujet(this);
	}

	/**
	 * @return le rapport sujet du type demandé valide à cette date.
	 */
	@Transient
	public RapportEntreTiers getRapportSujetValidAt(@Nullable RegDate date, TypeRapportEntreTiers type) {
		return getAt(rapportsSujet, date, ret -> ret.getType() == type);
	}

	private static RapportEntreTiers getDernierRapport(Collection<RapportEntreTiers> rapports, TypeRapportEntreTiers type) {
		if (rapports == null || rapports.isEmpty()) {
			return null;
		}
		return rapports.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(ret -> ret.getType() == type)
				.max(Comparator.comparing(RapportEntreTiers::getDateDebut, NullDateBehavior.EARLIEST::compare))
				.orElse(null);
	}

	/**
	 * @return le dernier rapport sujet du type demandé.
	 */
	@Transient
	public RapportEntreTiers getDernierRapportSujet(TypeRapportEntreTiers type) {
		return getDernierRapport(rapportsSujet, type);
	}

	private static RapportEntreTiers getPremierRapport(Collection<RapportEntreTiers> rapports, TypeRapportEntreTiers type, Predicate<? super RapportEntreTiers> subsetFilter) {
		if (rapports == null || rapports.isEmpty()) {
			return null;
		}
		return rapports.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(ret -> ret.getType() == type)
				.filter(subsetFilter)
				.min(Comparator.comparing(RapportEntreTiers::getDateDebut, NullDateBehavior.EARLIEST::compare))
				.orElse(null);
	}

	/**
	 * @return le premier rapport sujet dont le tiers est objet.
	 */
	@Transient
	public RapportEntreTiers getPremierRapportSujet(TypeRapportEntreTiers type, Tiers tiers) {
		return getPremierRapport(rapportsSujet, type, ret -> ret.getObjetId().equals(tiers.getNumero()));
	}

	/**
	 * @return le rapport objet du type demandé valide à cette date.
	 */
	@Transient
	public RapportEntreTiers getRapportObjetValidAt(RegDate date, TypeRapportEntreTiers type) {
		return getAt(rapportsObjet, date, ret -> ret.getType() == type);
	}

	/**
	 * @return le dernier rapport objet du type demandé.
	 */
	@Transient
	public RapportEntreTiers getDernierRapportObjet(TypeRapportEntreTiers type) {
		return getDernierRapport(rapportsObjet, type);
	}

	/**
	 * @return le premier rapport objet dont le tiers est sujet.
	 */
	@Transient
	public RapportEntreTiers getPremierRapportObjet(TypeRapportEntreTiers type, Tiers tiers) {
		return getPremierRapport(rapportsObjet, type, ret -> ret.getSujetId().equals(tiers.getNumero()));
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_ADR_TRS_ID")
	public Set<AdresseTiers> getAdressesTiers() {
		return adressesTiers;
	}

	public void setAdressesTiers(Set<AdresseTiers> theAdressesTiers) {
		adressesTiers = theAdressesTiers;
	}

	@OneToMany(mappedBy = "tiers", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_DOCFISC_TRS_ID")
	public Set<DocumentFiscal> getDocumentsFiscaux() {
		return documentsFiscaux;
	}

	/**
	 * @return les déclarations fiscales dans un {@link Set<Declaration>} en lecture seule.
	 */
	@Transient
	public Set<Declaration> getDeclarations() {
		if (documentsFiscaux == null) {
			return null;
		}
		final Set<Declaration> declarations = documentsFiscaux.stream()
				.filter(d -> Declaration.class.isAssignableFrom(d.getClass()))
				.map(d -> (Declaration) d)
				.collect(Collectors.toSet());
		return Collections.unmodifiableSet(declarations);
	}

	public void setDocumentsFiscaux(Set<DocumentFiscal> theDocumentsFiscaux) {
		documentsFiscaux = theDocumentsFiscaux;
	}

	/**
	 * @return la liste de toutes les déclarations du contribuable (y compris les déclarations annulées), triées
	 */
	@NotNull
	@Transient
	public List<Declaration> getDeclarationsTriees() {
		return getDeclarationsTriees(Declaration.class, true);
	}

	@NotNull
	@Transient
	public <T extends Declaration> List<T> getDeclarationsTriees(Class<T> clazz, boolean avecAnnulees) {
		final Set<Declaration> declarations = getDeclarations();
		if (declarations == null || declarations.isEmpty()) {
			return Collections.emptyList();
		}
		return declarations.stream()
				.filter(decl -> avecAnnulees || !decl.isAnnule())
				.filter(clazz::isInstance)
				.map(clazz::cast)
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());
	}

	@NotNull
	@Transient
	public <T extends Declaration> List<T> getDeclarationsDansPeriode(Class<T> clazz, int pf, boolean avecAnnulees) {
		final Set<Declaration> declarations = getDeclarations();
		if (declarations == null || declarations.isEmpty()) {
			return Collections.emptyList();
		}
		return declarations.stream()
				.filter(decl -> avecAnnulees || !decl.isAnnule())
				.filter(clazz::isInstance)
				.filter(decl -> decl.getPeriode().getAnnee() == pf)
				.map(clazz::cast)
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());
	}

	@Transient
	public <T extends Declaration> T getDerniereDeclaration(Class<T> clazz) {
		final List<T> toutes = getDeclarationsTriees(clazz, false);
		return toutes.isEmpty() ? null : CollectionsUtils.getLastElement(toutes);
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_FF_TIERS_ID")
	public Set<ForFiscal> getForsFiscaux() {
		return forsFiscaux;
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
	 * @return les fors triés par - La date d'ouverture - Leur type, selon l'ordinal de l'enum TypeAutoriteFiscale
	 */
	@NotNull
	@Transient
	public List<ForFiscal> getForsFiscauxSorted() {
		return getSortedStreamForsFiscaux().collect(Collectors.toList());
	}

	protected static final Comparator<ForFiscal> FOR_FISCAL_COMPARATOR = new DateRangeComparator<ForFiscal>().thenComparing(ForFiscal::getTypeAutoriteFiscale);

	@NotNull
	@Transient
	protected Stream<ForFiscal> getStreamForsFiscaux() {
		if (forsFiscaux == null || forsFiscaux.isEmpty()) {
			return Stream.empty();
		}
		return forsFiscaux.stream();
	}

	@NotNull
	@Transient
	protected Stream<ForFiscal> getSortedStreamForsFiscaux() {
		return getStreamForsFiscaux().sorted(FOR_FISCAL_COMPARATOR);
	}

	@NotNull
	@Transient
	protected <T extends ForFiscal> Stream<T> getStreamForsFiscaux(Class<T> clazz, boolean withAnnules) {
		return getStreamForsFiscaux()
				.filter(ff -> withAnnules || !ff.isAnnule())
				.filter(clazz::isInstance)
				.map(clazz::cast);
	}

	/**
	 * @param sort
	 *            <code>true</code> si les fors doivent être triés; <code>false</code> autrement.
	 * @return les fors fiscaux non annulés
	 */
	@Transient
	public List<ForFiscal> getForsFiscauxNonAnnules(boolean sort) {
		final List<ForFiscal> fors = AnnulableHelper.sansElementsAnnules(forsFiscaux);
		if (sort) {
			fors.sort(FOR_FISCAL_COMPARATOR);
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
			this.forsFiscaux = new HashSet<>();
		}
		this.forsFiscaux.add(nouveauForFiscal);
		Assert.isTrue(nouveauForFiscal.getTiers() == null || nouveauForFiscal.getTiers() == this);
		nouveauForFiscal.setTiers(this);
	}

	/**
	 * Retourne la liste de tous les fors actifs à une date donnée.
	 *
	 * @param date
	 *            la date à laquelle les fors sont actifs, ou <b>null</b> pour obtenir tous les fors courants.
	 *
	 * @return la liste des fors correspondant, qui peut être vide si aucun for ne correspond aux critères.
	 */
	@NotNull
	@Transient
	public List<ForFiscal> getForsFiscauxValidAt(@Nullable RegDate date) {
		return getStreamForsFiscaux()
				.filter(ff -> ff.isValidAt(date))
				.collect(Collectors.toList());
	}

	/**
	 * <b>Attention !</b> cette méthode ne doit pas être appelée directement ! Il faut utiliser {@link #addForFiscal(ForFiscal)} pour
	 * ajouter un for !
	 *
	 * @param theForsFiscaux
	 *            the forsFiscaux to set
	 */
	public void setForsFiscaux(Set<ForFiscal> theForsFiscaux) {
		forsFiscaux = theForsFiscaux;
	}

	@Column(name = "DEBITEUR_INACTIF", nullable = false)
	public boolean isDebiteurInactif() {
		return debiteurInactif;
	}

	public void setDebiteurInactif(boolean theDebiteurInactif) {
		debiteurInactif = theDebiteurInactif;
	}

	public void addAdresseTiers(AdresseTiers adresse) {
		Assert.notNull(adresse);
		if (adressesTiers == null) {
			adressesTiers = new HashSet<>();
		}
		adresse.setTiers(this);
		adressesTiers.add(adresse);
	}

	public synchronized void addDocumentFiscal(DocumentFiscal documentFiscal) {
		Assert.notNull(documentFiscal);
		if (documentsFiscaux == null) {
			documentsFiscaux = new HashSet<>();
		}
		documentsFiscaux.add(documentFiscal);
		documentFiscal.setTiers(this);
	}

	public void addDeclaration(Declaration declaration) {
		addDocumentFiscal(declaration);
	}


	void addAllDeclarations(Set<Declaration> declarations) {
		if (documentsFiscaux == null) {
			documentsFiscaux = new HashSet<>();
		}
		documentsFiscaux.addAll(declarations);
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
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getReindexOn() {
		return reindexOn;
	}

	public void setReindexOn(RegDate reindexOn) {
		this.reindexOn = reindexOn;
	}

	/**
	 * Agende une réindexation du tiers dans le futur.
	 * <p>
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
	 *
	 * @return L'office d'impôt qui gère le tiers. Retourne <b>null</b> si le tiers n'a pas encore été persisté, ou s'il ne possède pas de
	 *         for de gestion.
	 */
	@Column(name = "OID")
	public Integer getOfficeImpotId() {
		return officeImpotId;
	}

	/**
	 * <b>Utilisation uniquement autorisée pour l'intercepteur chargée de tenir à jour cette valeur !</b>
	 *
	 * @param officeImpotID
	 *            the officeImpotId to set
	 */
	public void setOfficeImpotId(Integer officeImpotID) {
		this.officeImpotId = officeImpotID;
	}

	/**
	 * Propriété présente pour afficher les données dans SuperGRA et la validation... Pour le véritable affichage, lui préférer l'accès par le DAO.
	 * @see ch.vd.unireg.tiers.dao.RemarqueDAO#getRemarques(Long)
	 */
	@OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, mappedBy = "tiers")
	public Set<Remarque> getRemarques() {
		return this.remarques;
	}

	/**
	 * Propriété présente pour afficher les données dans SuperGRA, ne doit pas être utilisée autrement
	 * @see ch.vd.unireg.tiers.dao.RemarqueDAO#save(Object)
	 */
	@Deprecated
	public void setRemarques(Set<Remarque> remarques) {
		this.remarques = remarques;
	}

	/**
	 * Les liens datés vers les étiquettes associées au tiers
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	public Set<EtiquetteTiers> getEtiquettes() {
		return etiquettes;
	}

	public void setEtiquettes(Set<EtiquetteTiers> etiquettes) {
		this.etiquettes = etiquettes;
	}

	@Transient
	public void addEtiquette(EtiquetteTiers etiquette) {
		if (etiquette.getTiers() != null && etiquette.getTiers() != this) {
			throw new IllegalArgumentException("Etiquette déjà associée à un autre tiers.");
		}
		etiquette.setTiers(this);
		getOrCreateEtiquetteTiersSet().add(etiquette);
	}

	@NotNull
	@Transient
	protected synchronized Set<EtiquetteTiers> getOrCreateEtiquetteTiersSet() {
		if (etiquettes == null) {
			etiquettes = new HashSet<>();
		}
		return etiquettes;
	}

	@Transient
	public List<EtiquetteTiers> getEtiquettesNonAnnuleesTriees() {
		final List<EtiquetteTiers> nonAnnulees = AnnulableHelper.sansElementsAnnules(etiquettes);
		nonAnnulees.sort(DateRangeComparator::compareRanges);
		return nonAnnulees;
	}

	/**
	 * Un tiers "annulé" (au sens technique de la date d'annulation) est dit "désactivé" pour toute date ;
	 * sinon, cela peut dépendre de ses fors
	 * @param date date de référencce
	 * @return si le tiers est désactivé ou non
	 */
	@Transient
	public final boolean isDesactive(@Nullable RegDate date) {
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
				&& RegDateHelper.isBetween(RegDateHelper.get(date), getDateDebutActivite(), getDateFinActivite(), NullDateBehavior.LATEST);
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
		return getStreamForsFiscaux()
				.filter(AnnulableHelper::nonAnnule)
				.min(Comparator.comparing(ForFiscal::getDateDebut, NullDateBehavior.EARLIEST::compare))
				.map(ForFiscal::getDateDebut)
				.orElse(null);
	}

	/**
	 * Calcul la date de fin d'activité du tiers.
	 *
	 * @return la date maximum de validité des fors fiscaux. Ou <b>null</b> s'il y a un for actif ouvert ou si le tiers ne possède pas du
	 *         tout de for.
	 */
	@Transient
	public RegDate getDateFinActivite() {
		return getStreamForsFiscaux()
				.filter(AnnulableHelper::nonAnnule)
				.max(Comparator.comparing(ForFiscal::getDateFin, NullDateBehavior.LATEST::compare))
				.map(ForFiscal::getDateFin)
				.orElse(null);
	}

	/**
	 * @return la nature du tiers courant
	 */
	@Transient
	public abstract NatureTiers getNatureTiers();

	/**
	 * @return le type concret du tiers courant.
	 */
	@Transient
	public abstract TypeTiers getType();

	@Override
	public String toString() {
		return getClass().getSimpleName() + " n°" + numero;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		return ComparisonHelper.areEqual(coordonneesFinancieres, obj.coordonneesFinancieres)
				&& ComparisonHelper.areEqual(adresseCourrierElectronique, obj.adresseCourrierElectronique)
				&& ComparisonHelper.areEqual(adressesTiers, obj.adressesTiers)
				&& ComparisonHelper.areEqual(blocageRemboursementAutomatique, obj.blocageRemboursementAutomatique)
				&& ComparisonHelper.areEqual(complementNom, obj.complementNom)
				&& ComparisonHelper.areEqual(debiteurInactif, obj.debiteurInactif)
				&& ComparisonHelper.areEqual(documentsFiscaux, obj.documentsFiscaux)
				&& ComparisonHelper.areEqual(forsFiscaux, obj.forsFiscaux)
				&& ComparisonHelper.areEqual(numero, obj.numero)
				&& ComparisonHelper.areEqual(numeroTelecopie, obj.numeroTelecopie)
				&& ComparisonHelper.areEqual(numeroTelephonePortable, obj.numeroTelephonePortable)
				&& ComparisonHelper.areEqual(numeroTelephonePrive, obj.numeroTelephonePrive)
				&& ComparisonHelper.areEqual(numeroTelephoneProfessionnel, obj.numeroTelephoneProfessionnel)
				&& ComparisonHelper.areEqual(personneContact, obj.personneContact)
				&& ComparisonHelper.areEqual(rapportsObjet, obj.rapportsObjet)
				&& ComparisonHelper.areEqual(rapportsSujet, obj.rapportsSujet)
				&& ComparisonHelper.areEqual(etiquettes, obj.etiquettes);
	}
}
