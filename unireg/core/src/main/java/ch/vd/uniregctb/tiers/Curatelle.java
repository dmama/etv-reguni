package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc -->
 * <pre>
 * +------------------+                           +------------------+
 * | PersonnePhysique | (pupille)      (curateur) | PersonnePhysique |
 * +------------------+                           +------------------+
 *         ^                                               ^
 *         ¦  sujet          +-----------+          objet  ¦
 *         +-----------------| Curatelle |-----------------+
 *                           +-----------+
 * </pre>
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 */
@Entity
@DiscriminatorValue("Curatelle")
public class Curatelle extends RepresentationLegale {

	private static final long serialVersionUID = -1357290304664777417L;

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

	public RapportEntreTiers duplicate() {
		return new Curatelle(this);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.CURATELLE;
	}
}