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
		// empty
	}
	public ConseilLegal(ConseilLegal representation) {
		super(representation);
	}

	/**
	 * Conseil légal entre une pupille et un conseiller ordinaire.
	 */
	public ConseilLegal(RegDate dateDebut, RegDate dateFin, PersonnePhysique pupille, PersonnePhysique conseiller) {
		super(dateDebut, dateFin, pupille, conseiller);
	}

	/**
	 * Conseil légal entre une pupille et un conseiller professionel membre d'un office de tuteur général.
	 */
	public ConseilLegal(RegDate dateDebut, RegDate dateFin, PersonnePhysique pupille, PersonnePhysique conseiller,
			CollectiviteAdministrative autoriteTutelaire) {
		super(dateDebut, dateFin, pupille, conseiller, autoriteTutelaire);
	}

	/**
	 * Conseil légal entre une pupille et un office de tuteur général en l'absence d'un conseiller professionel connu.
	 */
	public ConseilLegal(RegDate dateDebut, RegDate dateFin, PersonnePhysique pupille, CollectiviteAdministrative conseiller) {
		super(dateDebut, dateFin, pupille, conseiller);
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