package ch.vd.unireg.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.MandatOuAssimile;
import ch.vd.unireg.type.TypeMandat;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +----------------+                   +------------------+
 *   |    Mandant     |                   |    Mandataire    |
 *   +----------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------|      Mandat        |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("Mandat")
public class Mandat extends RapportEntreTiers implements MandatOuAssimile {

	private static final String MANDANT = "mandant";
	private static final String MANDATAIRE = "mandataire";

	private TypeMandat typeMandat;
	private CompteBancaire compteBancaire;
	private String personneContact;
	private String noTelephoneContact;
	private Boolean withCopy;
	private String codeGenreImpot;

	public Mandat() {
		// empty
	}

	@NotNull
	public static Mandat tiers(RegDate dateDebut, RegDate dateFin, Contribuable mandant, Contribuable mandataire, CompteBancaire compteBancaire) {
		if (compteBancaire == null) {
			throw new IllegalArgumentException("Un mandat 'tiers' doit avoir une donnée de coordonnées financières");
		}
		final Mandat mandat = new Mandat(dateDebut, dateFin, mandant, mandataire, TypeMandat.TIERS, null, null);
		mandat.setCompteBancaire(new CompteBancaire(compteBancaire));
		return mandat;
	}

	@NotNull
	public static Mandat general(RegDate dateDebut, RegDate dateFin, Contribuable mandant, Contribuable mandataire, boolean withCopy) {
		return new Mandat(dateDebut, dateFin, mandant, mandataire, TypeMandat.GENERAL, withCopy, null);
	}

	@NotNull
	public static Mandat special(RegDate dateDebut, RegDate dateFin, Contribuable mandant, Contribuable mandataire, boolean withCopy, String codeGenreImpot) {
		if (StringUtils.isBlank(codeGenreImpot)) {
			throw new IllegalArgumentException("Un mandat 'special' doit avoir un code de genre d'impôt");
		}
		return new Mandat(dateDebut, dateFin, mandant, mandataire, TypeMandat.SPECIAL, withCopy, codeGenreImpot);
	}

	private Mandat(RegDate dateDebut, RegDate dateFin, Contribuable mandant, Contribuable mandataire, TypeMandat typeMandat, Boolean withCopy, String codeGenreImpot) {
		super(dateDebut, dateFin, mandant, mandataire);
		this.typeMandat = typeMandat;
		this.withCopy = withCopy;
		this.codeGenreImpot = codeGenreImpot;
	}

	private Mandat(Mandat src) {
		super(src);
		this.typeMandat = src.typeMandat;
		this.compteBancaire = src.compteBancaire != null ? new CompteBancaire(src.getCompteBancaire()) : null;
		this.personneContact = src.personneContact;
		this.noTelephoneContact = src.noTelephoneContact;
		this.withCopy = src.withCopy;
		this.codeGenreImpot = src.codeGenreImpot;
	}

	@Override
	@Column(name = "TYPE_MANDAT", length = LengthConstants.MANDAT_TYPE)
	@Enumerated(EnumType.STRING)
	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public void setTypeMandat(TypeMandat typeMandat) {
		this.typeMandat = typeMandat;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "iban", column = @Column(name = "IBAN_MANDAT", length = LengthConstants.TIERS_NUMCOMPTE)),
			@AttributeOverride(name = "bicSwift", column = @Column(name = "BIC_SWIFT_MANDAT", length = LengthConstants.TIERS_ADRESSEBICSWIFT))
	})
	public CompteBancaire getCompteBancaire() {
		return compteBancaire;
	}

	public void setCompteBancaire(CompteBancaire compteBancaire) {
		this.compteBancaire = compteBancaire;
	}

	@Column(name = "PERSONNE_CONTACT_MANDAT", length = LengthConstants.MANDAT_PERSONNE_CONTACT)
	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	@Column(name = "TEL_CONTACT_MANDAT", length = LengthConstants.TIERS_NUMTEL)
	public String getNoTelephoneContact() {
		return noTelephoneContact;
	}

	public void setNoTelephoneContact(String noTelephoneContact) {
		this.noTelephoneContact = noTelephoneContact;
	}

	@Column(name = "WITH_COPY_MANDAT")
	public Boolean getWithCopy() {
		return withCopy;
	}

	public void setWithCopy(Boolean withCopy) {
		this.withCopy = withCopy;
	}

	@Override
	@Column(name = "GENRE_IMPOT_MANDAT", length = LengthConstants.MANDAT_GENRE_IMPOT)
	public String getCodeGenreImpot() {
		return codeGenreImpot;
	}

	public void setCodeGenreImpot(String codeGenreImpot) {
		this.codeGenreImpot = codeGenreImpot;
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.MANDAT;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return MANDATAIRE;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return MANDANT;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new Mandat(this);
	}

	/**
	 * @param autre au rapport entre tiers
	 * @return si le rapport est un Mandat de dates, parties prenantes, type et genre d'impôt équivalents (= doublon...)
	 */
	@Override
	public boolean equalsTo(RapportEntreTiers autre) {
		final boolean baseEqualsTo = super.equalsTo(autre);
		if (baseEqualsTo && autre instanceof Mandat) {
			final Mandat autreMandat = (Mandat) autre;
			return typeMandat == autreMandat.typeMandat && Objects.equals(codeGenreImpot, autreMandat.codeGenreImpot);
		}
		return baseEqualsTo;
	}
}
