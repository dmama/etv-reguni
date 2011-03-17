package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc -->
 * <pre>
 * +------------------+                       +------------------+
 * | PersonnePhysique | (pupille)    (tuteur) | PersonnePhysique |
 * +------------------+                       +------------------+
 *         ^                                           ^
 *         ¦  sujet          +---------+        objet  ¦
 *         +-----------------| Tutelle |---------------+
 *                           +---------+
 * </pre>
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 */
@Entity
@DiscriminatorValue("Tutelle")
public class Tutelle extends RepresentationLegale {

	private static final long serialVersionUID = 8255992899554760735L;

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

	public RapportEntreTiers duplicate() {
		return new Tutelle(this);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.TUTELLE;
	}
}