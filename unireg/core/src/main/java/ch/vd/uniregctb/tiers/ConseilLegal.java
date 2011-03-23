package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc -->
 * <pre>
 * +------------------+                              +------------------+
 * | PersonnePhysique | (pupille)       (conseiller) | PersonnePhysique |
 * +------------------+                              +------------------+
 *         ^                                                  ^
 *         ¦  sujet          +--------------+          objet  ¦
 *         +-----------------| ConseilLegal |-----------------+
 *                           +--------------+
 * </pre>
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 */
@Entity
@DiscriminatorValue("ConseilLegal")
public class ConseilLegal extends RepresentationLegale {

	private static final long serialVersionUID = 4695352371610248334L;

	public ConseilLegal() {
		// vide, nécessaire pour la persistence
	}

	protected ConseilLegal(ConseilLegal conseilLegal) {
		super(conseilLegal);
	}

	/**
	 * Conseil légal entre une pupille et un conseiller, potentiellement ordonné par une autorité tutélaire (justice de paix)
	 */
	public ConseilLegal(RegDate dateDebut, RegDate dateFin, PersonnePhysique pupille, Tiers conseiller, CollectiviteAdministrative autoriteTutelaire) {
		super(dateDebut, dateFin, pupille, conseiller, autoriteTutelaire);
	}

	public RapportEntreTiers duplicate() {
		return new ConseilLegal(this);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.CONSEIL_LEGAL;
	}
}