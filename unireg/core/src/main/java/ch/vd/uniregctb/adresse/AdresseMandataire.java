package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeMandat;

@Entity
@Table(name = "ADRESSE_MANDATAIRE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ADR_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class AdresseMandataire extends HibernateDateRangeEntity implements Comparable<AdresseMandataire>, LinkedEntity, Duplicable<AdresseMandataire>, AdresseFiscale {

	private Long id;
	private TypeMandat typeMandat;
	private boolean withCopy;
	private String codeGenreImpot;
	private Contribuable mandant;
	private String nomDestinataire;
	private String complement;
	private String rue;
	private String numeroMaison;
	private TexteCasePostale texteCasePostale;
	private Integer numeroCasePostale;

	private String personneContact;
	private String noTelephoneContact;

	public AdresseMandataire() {
	}

	public AdresseMandataire(AdresseMandataire src) {
		super(src);
		this.typeMandat = src.typeMandat;
		this.withCopy = src.withCopy;
		this.codeGenreImpot = src.codeGenreImpot;
		this.nomDestinataire = src.nomDestinataire;
		this.complement = src.complement;
		this.rue = src.rue;
		this.numeroMaison = src.numeroMaison;
		this.texteCasePostale = src.texteCasePostale;
		this.numeroCasePostale = src.numeroCasePostale;
		this.personneContact = src.personneContact;
		this.noTelephoneContact = src.noTelephoneContact;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "TYPE_MANDAT", length = LengthConstants.MANDAT_TYPE, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public void setTypeMandat(TypeMandat typeMandat) {
		this.typeMandat = typeMandat;
	}

	@Column(name = "WITH_COPY", nullable = false)
	public boolean isWithCopy() {
		return withCopy;
	}

	public void setWithCopy(boolean withCopy) {
		this.withCopy = withCopy;
	}

	@Column(name = "GENRE_IMPOT", length = LengthConstants.MANDAT_GENRE_IMPOT)
	public String getCodeGenreImpot() {
		return codeGenreImpot;
	}

	public void setCodeGenreImpot(String codeGenreImpot) {
		this.codeGenreImpot = codeGenreImpot;
	}

	@ManyToOne
	@JoinColumn(name = "CTB_ID", nullable = false, insertable = false, updatable = false)
	public Contribuable getMandant() {
		return mandant;
	}

	public void setMandant(Contribuable mandant) {
		this.mandant = mandant;
	}

	@Column(name = "NOM_DESTINATAIRE", length = LengthConstants.ADRESSE_NOM)
	public String getNomDestinataire() {
		return nomDestinataire;
	}

	public void setNomDestinataire(String nomDestinataire) {
		this.nomDestinataire = nomDestinataire;
	}

	@Column(name = "COMPLEMENT", length = LengthConstants.ADRESSE_NOM)
	public String getComplement() {
		return complement;
	}

	public void setComplement(String complement) {
		this.complement = complement;
	}

	@Column(name = "RUE", length = LengthConstants.ADRESSE_NOM)
	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	@Column(name = "NUMERO_MAISON", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroMaison() {
		return numeroMaison;
	}

	public void setNumeroMaison(String numeroMaison) {
		this.numeroMaison = numeroMaison;
	}

	@Column(name = "TEXTE_CASE_POSTALE", length = LengthConstants.ADRESSE_TYPESUPPLEM)
	@Enumerated(EnumType.STRING)
	public TexteCasePostale getTexteCasePostale() {
		return texteCasePostale;
	}

	public void setTexteCasePostale(TexteCasePostale texteCasePostale) {
		this.texteCasePostale = texteCasePostale;
	}

	@Column(name = "NUMERO_CASE_POSTALE")
	public Integer getNumeroCasePostale() {
		return numeroCasePostale;
	}

	public void setNumeroCasePostale(Integer numeroCasePostale) {
		this.numeroCasePostale = numeroCasePostale;
	}

	@Column(name = "PERSONNE_CONTACT", length = LengthConstants.MANDAT_PERSONNE_CONTACT)
	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	@Column(name = "TEL_CONTACT", length = LengthConstants.TIERS_NUMTEL)
	public String getNoTelephoneContact() {
		return noTelephoneContact;
	}

	public void setNoTelephoneContact(String noTelephoneContact) {
		this.noTelephoneContact = noTelephoneContact;
	}

	@Transient
	@Override
	public boolean isPermanente() {
		// convention : une adresse de mandataire est toujours permanente
		return true;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return mandant != null ? Collections.singletonList(mandant) : null;
	}

	@Override
	public int compareTo(@NotNull AdresseMandataire o) {
		return NullDateBehavior.EARLIEST.compare(getDateDebut(), o.getDateDebut());
	}
}
