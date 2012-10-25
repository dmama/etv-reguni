package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc -->
 * <pre>
 *   +--------------+                 +-----------------------------+
 *   | Contribuable |                 | DebiteurPrestationImposable |
 *   +--------------+                 +-----------------------------+
 *          ^                                        ^
 *          ¦  sujet  +--------------------+  objet  ¦
 *          +---------| ContactImpotSource |---------+
 *                    +--------------------+
 * </pre>
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 */
@Entity
@DiscriminatorValue("ContactImpotSource")
public class ContactImpotSource extends RapportEntreTiers {

	private static final String DPI = "débiteur de prestations imposables";
	private static final String CTB = "référent";

	private static final long serialVersionUID = -5845209265638768969L;

	public ContactImpotSource() {
		// empty
	}

	public ContactImpotSource(ContactImpotSource contact) {
		super(contact);
	}

	public ContactImpotSource(RegDate dateDebut, RegDate dateFin, Contribuable sujet, DebiteurPrestationImposable objet) {
		super(dateDebut, dateFin, sujet, objet);
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new ContactImpotSource(this);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return DPI;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return CTB;
	}
}