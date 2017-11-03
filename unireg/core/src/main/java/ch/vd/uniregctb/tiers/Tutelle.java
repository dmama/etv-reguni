package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 * +------------------+                       +------------------+
 * | PersonnePhysique | (pupille)    (tuteur) | PersonnePhysique |
 * +------------------+                       +------------------+
 *         ^                                           ^
 *         ¦  sujet          +---------+        objet  ¦
 *         +-----------------| Tutelle |---------------+
 *                           +---------+
 * </pre>
 */
@Entity
@DiscriminatorValue("Tutelle")
public class Tutelle extends RepresentationLegale {

	private static final String TUTEUR = "tuteur";

	public Tutelle() {
		// vide, nécessaire pour la persistence
	}

	protected Tutelle(Tutelle representation) {
		super(representation);
	}

	/**
	 * Tutelle entre une pupille et son tuteur, potentiellement ordonné par une autorité tutélaire (justice de paix)
	 */
	public Tutelle(RegDate dateDebut, RegDate dateFin, PersonnePhysique pupille, Tiers tuteur, CollectiviteAdministrative autoriteTutelaire) {
		super(dateDebut, dateFin, pupille, tuteur, autoriteTutelaire);
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new Tutelle(this);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.TUTELLE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return TUTEUR;
	}
}