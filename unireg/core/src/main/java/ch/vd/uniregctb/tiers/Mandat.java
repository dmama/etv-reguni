package ch.vd.uniregctb.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

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
public class Mandat extends RapportEntreTiers {

	private static final String MANDANT = "mandant";
	private static final String MANDATAIRE = "mandataire";

	private TypeMandat typeMandat;
	private CoordonneesFinancieres coordonneesFinancieres;

	public Mandat() {
		// empty
	}

	public Mandat(RegDate dateDebut, RegDate dateFin, Contribuable mandant, Contribuable mandataire, TypeMandat typeMandat) {
		super(dateDebut, dateFin, mandant, mandataire);
		this.typeMandat = typeMandat;
	}

	protected Mandat(Mandat src) {
		super(src);
		this.typeMandat = src.typeMandat;
		this.coordonneesFinancieres = src.coordonneesFinancieres != null ? new CoordonneesFinancieres(src.getCoordonneesFinancieres()) : null;
	}

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
	public CoordonneesFinancieres getCoordonneesFinancieres() {
		return coordonneesFinancieres;
	}

	public void setCoordonneesFinancieres(CoordonneesFinancieres coordonneesFinancieres) {
		this.coordonneesFinancieres = coordonneesFinancieres;
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

}
