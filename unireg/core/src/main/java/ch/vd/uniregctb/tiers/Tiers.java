package ch.vd.uniregctb.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Embedded;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.BusinessComparable;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(Tiers.class);

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

	/**
	 * Numero de compte bancaire ou du compte postal au format international IBAN (longueur maximum 21 pour les comptes suisses)
	 */
	private CoordonneesFinancieres coordonneesFinancieres;

	/**
	 * Titulaire du compte bancaire ou du compte postal
	 */
	private String titulaireCompteBancaire;

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
	private Set<Declaration> declarations;
	private Set<ForFiscal> forsFiscaux;
	private Set<Remarque> remarques;
	private Set<EtiquetteTiers> etiquettes;

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

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "iban", column = @Column(name = "NUMERO_COMPTE_BANCAIRE", length = LengthConstants.TIERS_NUMCOMPTE)),
			@AttributeOverride(name = "bicSwift", column = @Column(name = "ADRESSE_BIC_SWIFT", length = LengthConstants.TIERS_ADRESSEBICSWIFT))
	})
	public CoordonneesFinancieres getCoordonneesFinancieres() {
		return coordonneesFinancieres;
	}

	public void setCoordonneesFinancieres(CoordonneesFinancieres coordonneesFinancieres) {
		this.coordonneesFinancieres = coordonneesFinancieres;
	}

	@Transient
	public String getNumeroCompteBancaire() {
		return coordonneesFinancieres != null ? coordonneesFinancieres.getIban() : null;
	}

	@Transient
	public String getAdresseBicSwift() {
		return coordonneesFinancieres != null ? coordonneesFinancieres.getBicSwift() : null;
	}

	@Column(name = "TITULAIRE_COMPTE_BANCAIRE", length = LengthConstants.TIERS_PERSONNE)
	public String getTitulaireCompteBancaire() {
		return titulaireCompteBancaire;
	}

	public void setTitulaireCompteBancaire(String theTitulaireCompteBancaire) {
		titulaireCompteBancaire = theTitulaireCompteBancaire;
	}

	@Transient
	public List<AdresseTiers> getAdressesTiersSorted() {
		if (adressesTiers == null) {
			return null;
		}
		List<AdresseTiers> list = new ArrayList<>();
		list.addAll(adressesTiers);
		Collections.sort(list, new DateRangeComparator<AdresseTiers>());
		return list;
	}

	@Transient
	public List<AdresseTiers> getAdressesTiersSorted(TypeAdresseTiers type) {
		if (adressesTiers == null) {
			return null;
		}
		List<AdresseTiers> list = new ArrayList<>();
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
		List<AdresseTiers> list = new ArrayList<>();
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
	public AdresseTiers getAdresseActive(TypeAdresseTiers type, @Nullable RegDate date) {

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
	public RapportEntreTiers getRapportSujetValidAt(@Nullable RegDate date, TypeRapportEntreTiers type) {
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
						|| RegDateHelper.isAfterOrEqual(rapportSujet.getDateDebut(), dernierRapport.getDateDebut(), NullDateBehavior.EARLIEST)) {
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
	@ForeignKey(name = "FK_DECL_TRS_ID")
	public Set<Declaration> getDeclarations() {
		return declarations;
	}

	public void setDeclarations(Set<Declaration> theDeclarations) {
		declarations = theDeclarations;
	}

	/**
	 * @return la liste de toutes les déclarations du contribuable (y compris les déclarations annulées), triées
	 */
	@Transient
	public List<Declaration> getDeclarationsTriees() {
		return getDeclarationsTriees(Declaration.class, true);
	}

	@Transient
	public <T extends Declaration> List<T> getDeclarationsTriees(Class<T> clazz, boolean avecAnnulees) {
		if (declarations == null || declarations.isEmpty()) {
			return Collections.emptyList();
		}
		final List<T> triees = new ArrayList<>(declarations.size());
		for (Declaration declaration : declarations) {
			if (clazz.isAssignableFrom(declaration.getClass()) && (avecAnnulees || !declaration.isAnnule())) {
				//noinspection unchecked
				triees.add((T) declaration);
			}
		}
		Collections.sort(triees, new DateRangeComparator<>());
		return triees;
	}

	@Transient
	public <T extends Declaration> List<T> getDeclarationsDansPeriode(Class<T> clazz, int pf, boolean avecAnnulees) {
		if (declarations == null || declarations.isEmpty()) {
			return Collections.emptyList();
		}
		final List<T> filtrees = new ArrayList<>(declarations.size());
		for (Declaration declaration : declarations) {
			if ((avecAnnulees || !declaration.isAnnule()) && pf == declaration.getPeriode().getAnnee()) {
				if (clazz.isAssignableFrom(declaration.getClass())) {
					//noinspection unchecked
					filtrees.add((T) declaration);
				}
			}
		}
		if (filtrees.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			Collections.sort(filtrees, new DateRangeComparator<>());
			return filtrees;
		}
	}

	@Transient
	public <T extends Declaration> T getDerniereDeclaration(Class<T> clazz) {
		final List<T> toutes = getDeclarationsTriees(clazz, false);
		return toutes == null || toutes.isEmpty() ? null : CollectionsUtils.getLastElement(toutes);
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
	 * Trie les fors principaux par date, sans les annulés
	 *
	 * @return Renvoie les fors principaux
	 */
	@Transient
	public List<? extends ForFiscalPrincipal> getForsFiscauxPrincipauxActifsSorted() {
		List<ForFiscalPrincipal> ffps = null;
		if (forsFiscaux != null) {
			ffps = new ArrayList<>();
			for (ForFiscal ff : forsFiscaux) {
				if (ff instanceof ForFiscalPrincipal && !ff.isAnnule()) {
					ffps.add((ForFiscalPrincipal) ff);
				}
			}
			Collections.sort(ffps, new DateRangeComparator<ForFiscal>());
		}
		return ffps;
	}

	/**
	 * Trie les fors secondaires par date, sans les annulés
	 *
	 * @return Renvoie les fors secondaires dans une map indexée par no ofs de la commune
	 */
	@Transient
	public Map<Integer, List<ForFiscalSecondaire>> getForsFiscauxSecondairesActifsSortedMapped(MotifRattachement filtreMotifRattachement) {
		Map<Integer, List<ForFiscalSecondaire>> map = new HashMap<>();
		if (forsFiscaux != null) {
			for (ForFiscal ff : forsFiscaux) {
				if (ff instanceof ForFiscalSecondaire && !ff.isAnnule()) {
					ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
					if (filtreMotifRattachement != null && ffsec.getMotifRattachement() != filtreMotifRattachement) {
						continue;
					}
					List<ForFiscalSecondaire> ffps = map.get(ffsec.getNumeroOfsAutoriteFiscale());
					if (ffps == null) {
						ffps = new ArrayList<>();
						map.put(ffsec.getNumeroOfsAutoriteFiscale(), ffps);
					}
					ffps.add(ffsec);
				}
			}
			for (Map.Entry<Integer, List<ForFiscalSecondaire>> e : map.entrySet()) {
				Collections.sort(e.getValue(), new DateRangeComparator<ForFiscal>());
			}
		}
		return map;
	}

	/**
	 * Trie les fors secondaires par date, sans les annulés
	 *
	 * @return Renvoie les fors secondaires dans une map indexée par no ofs de la commune
	 */
	@Transient
	public Map<Integer, List<ForFiscalSecondaire>> getForsFiscauxSecondairesActifsSortedMapped() {
		return getForsFiscauxSecondairesActifsSortedMapped(null);
	}

	/**
	 *@return les fors triés par - La date d'ouverture - Leur type, selon l'ordinal de l'enum TypeAutoriteFiscale
	 *
	 */
	@Transient
	public List<ForFiscal> getForsFiscauxSorted() {
		List<ForFiscal> fors = null;
		if (forsFiscaux != null) {
			fors = new ArrayList<>(forsFiscaux);
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
		final List<ForFiscal> fors = AnnulableHelper.sansElementsAnnules(forsFiscaux);
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
			this.forsFiscaux = new HashSet<>();
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
	public ForFiscalPrincipal getForFiscalPrincipalAt(@Nullable RegDate date) {

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
	public List<ForFiscal> getForsFiscauxValidAt(@Nullable RegDate date) {

		List<ForFiscal> fors = new ArrayList<>();
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

	@Transient
	public ForFiscal getPremierForFiscalVd() {
		Set<ForFiscal> fors = getForsFiscaux();
		if (fors != null) {
			List<ForFiscal> list = new ArrayList<>(fors);
			Collections.sort(list, new ForFiscalFirstOpenedFirstComparator());
			return getFirstVdNonAnnule(list);
		}
		return null;
	}

	@Nullable
	private ForFiscal getFirstVdNonAnnule(List<ForFiscal> list) {
		for (ForFiscal forFiscal : list) {
			if (!forFiscal.isAnnule() && forFiscal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return forFiscal;
			}
		}
		return null;
	}

	@Transient
	public ForFiscal getDernierForFiscalVd() {
		Set<ForFiscal> fors = getForsFiscaux();
		if (fors != null) {
			List<ForFiscal> list = new ArrayList<>(fors);
			Collections.sort(list, new ForFiscalLastOpenFirstComparator());
			return getFirstVdNonAnnule(list);
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
	public ForFiscalPrincipal getDernierForFiscalPrincipalAvant(@Nullable RegDate date) {

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

		final List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				final ForFiscal forFiscal = list.get(i);
				if (!forFiscal.isAnnule() && forFiscal.isPrincipal() && TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forFiscal.getTypeAutoriteFiscale()) {
					return (ForFiscalPrincipal) forFiscal;
				}
			}
		}
		return null;
	}

	// ***********************************************
	@Transient
	public ForFiscalPrincipal getDernierForFiscalPrincipalVaudoisAvant(RegDate date) {

		final List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				final ForFiscal forFiscal = list.get(i);
				if (!forFiscal.isAnnule() && forFiscal.isPrincipal() && TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forFiscal.getTypeAutoriteFiscale() && RegDateHelper.isBeforeOrEqual(forFiscal.getDateDebut(), date, NullDateBehavior.LATEST)) {
					return (ForFiscalPrincipal) forFiscal;
				}
			}
		}
		return null;
	}

	// ***********************************************
	@Transient
	public ForDebiteurPrestationImposable getDernierForDebiteur() {

		final List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				final ForFiscal forFiscal = list.get(i);
				if (!forFiscal.isAnnule() && forFiscal.isDebiteur()) {
					return (ForDebiteurPrestationImposable) forFiscal;
				}
			}
		}
		return null;
	}

	// ***********************************************
	@Transient
	public ForDebiteurPrestationImposable getDernierForDebiteurAvant(RegDate date) {

		final List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				final ForFiscal forFiscal = list.get(i);
				if (!forFiscal.isAnnule() && forFiscal.isDebiteur() && RegDateHelper.isBeforeOrEqual(forFiscal.getDateDebut(), date, NullDateBehavior.LATEST)) {
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

	@Transient
	@NotNull
	protected synchronized Set<Declaration> getOrCreateDeclarationSet() {
		if (declarations == null) {
			declarations = new HashSet<>();
		}
		return declarations;
	}

	public synchronized void addDeclaration(Declaration declaration) {
		Assert.notNull(declaration);
		getOrCreateDeclarationSet().add(declaration);
		declaration.setTiers(this);
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
		// begin-user-code
		this.officeImpotId = officeImpotID;
		// end-user-code
	}

	/**
	 * Propriété présente pour afficher les données dans SuperGRA et la validation... Pour le véritable affichage, lui préférer l'accès par le DAO.
	 * @see ch.vd.uniregctb.tiers.dao.RemarqueDAO#getRemarques(Long)
	 */
	@OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, mappedBy = "tiers")
	public Set<Remarque> getRemarques() {
		return this.remarques;
	}

	/**
	 * Propriété présente pour afficher les données dans SuperGRA, ne doit pas être utilisée autrement
	 * @see ch.vd.uniregctb.tiers.dao.RemarqueDAO#save(Object)
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
			LOGGER.debug("Clone not supported", cnse);
		}
		// tiersClone.setAdressesTiers(null);

		// on renvoie le clone
		return tiersClone;
	}

	/**
	 * @return vrai s'il existe un for principal (ou une succession ininterrompue de fors principaux) durant la période spécifiée.
	 */
	public static boolean existForPrincipal(List<? extends ForFiscalPrincipal> principaux, @Nullable RegDate dateDebut, @Nullable RegDate dateFin) {

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
		RegDate date = RegDateHelper.getLateDate();
		if (forsFiscaux != null) {
			for (ForFiscal forFiscal : forsFiscaux) {
				if (forFiscal.isAnnule()) {
					continue;
				}
				date = RegDateHelper.minimum(date, forFiscal.getDateDebut(), NullDateBehavior.EARLIEST);
			}
		}
		if (date == RegDateHelper.getLateDate()) {
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
		RegDate date = RegDateHelper.getEarlyDate();
		if (forsFiscaux != null) {
			for (ForFiscal forFiscal : forsFiscaux) {
				if (forFiscal.isAnnule()) {
					continue;
				}
				date = RegDateHelper.maximum(date, forFiscal.getDateFin(), NullDateBehavior.LATEST);
			}
		}
		if (date == RegDateHelper.getEarlyDate()) {
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
	public ForDebiteurPrestationImposable getForDebiteurPrestationImposableAt(@Nullable RegDate date) {

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

		return getForsFiscauxPrincipauxOuvertsApres(date,true);
	}

	/**
	 * Renvoie la liste de fors fiscaux principaux débutant à ou après la date demandée (y compris les fors annulés).
	 * @param date date de référence
	 * @param withAnnule indique si on veut les fors annulées
	 * @return liste des fors principaux demandés
	 */
	@Transient
	public List<ForFiscalPrincipal> getForsFiscauxPrincipauxOuvertsApres(RegDate date, boolean withAnnule) {

		Assert.notNull(date);
		List<ForFiscalPrincipal> fors = new ArrayList<>();

		final List<ForFiscal> forsFiscauxSorted= withAnnule ? getForsFiscauxSorted(): getForsFiscauxNonAnnules(true);
		for (ForFiscal ff : forsFiscauxSorted) {
			if (ff.isPrincipal() && date.isBeforeOrEqual(ff.getDateDebut())) {
				fors.add((ForFiscalPrincipal) ff);
			}
		}
		return fors;
	}

    /**
     * @param date date a laquelle on doit verifié que le tiers possède un for annulé.
     * @param motif motif du for
     *
     * @return true si le tiers a un for fiscal principale annulé à la date précisée pour le motif précisé
     */
    @Transient
    public boolean hasForFiscalPrincipalAnnule(RegDate date, @Nullable MotifFor motif) {

        Assert.notNull(date);

        if (forsFiscaux == null) {
            return false;
        }

        for (ForFiscal f : forsFiscaux) {
            if (!f.isPrincipal() || !f.isAnnule() ) {
                 continue;
            }
            ForFiscalPrincipal ffp = (ForFiscalPrincipal) f;
            if (RegDateHelper.isBetween(date, ffp.getDateDebut(), ffp.getDateFin(), NullDateBehavior.EARLIEST)) {
                if (motif == null || ffp.getMotifOuverture() == motif) {
                    return true;
                }
            }
        }
        return false;
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
				&& ComparisonHelper.areEqual(declarations, obj.declarations)
				&& ComparisonHelper.areEqual(forsFiscaux, obj.forsFiscaux)
				&& ComparisonHelper.areEqual(numero, obj.numero)
				&& ComparisonHelper.areEqual(numeroTelecopie, obj.numeroTelecopie)
				&& ComparisonHelper.areEqual(numeroTelephonePortable, obj.numeroTelephonePortable)
				&& ComparisonHelper.areEqual(numeroTelephonePrive, obj.numeroTelephonePrive)
				&& ComparisonHelper.areEqual(numeroTelephoneProfessionnel, obj.numeroTelephoneProfessionnel)
				&& ComparisonHelper.areEqual(personneContact, obj.personneContact)
				&& ComparisonHelper.areEqual(rapportsObjet, obj.rapportsObjet)
				&& ComparisonHelper.areEqual(rapportsSujet, obj.rapportsSujet)
				&& ComparisonHelper.areEqual(titulaireCompteBancaire, obj.titulaireCompteBancaire)
				&& ComparisonHelper.areEqual(etiquettes, obj.etiquettes);
	}
}
