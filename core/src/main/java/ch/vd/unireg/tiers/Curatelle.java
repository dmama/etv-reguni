package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 * +------------------+                           +------------------+
 * | PersonnePhysique | (pupille)      (curateur) | PersonnePhysique |
 * +------------------+                           +------------------+
 *         ^                                               ^
 *         ¦  sujet          +-----------+          objet  ¦
 *         +-----------------| Curatelle |-----------------+
 *                           +-----------+
 * </pre>
 */
@Entity
@DiscriminatorValue("Curatelle")
public class Curatelle extends RepresentationLegale {

	private static final String CURATEUR = "curateur";

	public Curatelle() {
		// vide, nécessaire pour la persistence
	}

	protected Curatelle(Curatelle representation) {
		super(representation);
	}

	/**
	 * Curatelle entre une pupille et un curateur, potentiellement ordonnée par une autorité tutélaire (justice de paix)
	 */
	public Curatelle(RegDate dateDebut, RegDate dateFin, PersonnePhysique pupille, Tiers curateur, CollectiviteAdministrative autoriteTutelaire) {
		super(dateDebut, dateFin, pupille, curateur, autoriteTutelaire);
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new Curatelle(this);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.CURATELLE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return CURATEUR;
	}
}