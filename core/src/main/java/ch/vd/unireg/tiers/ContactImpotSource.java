package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +--------------+                 +-----------------------------+
 *   | Contribuable |                 | DebiteurPrestationImposable |
 *   +--------------+                 +-----------------------------+
 *          ^                                        ^
 *          ¦  sujet  +--------------------+  objet  ¦
 *          +---------| ContactImpotSource |---------+
 *                    +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("ContactImpotSource")
public class ContactImpotSource extends RapportEntreTiers {

	private static final String DPI = "débiteur de prestations imposables";
	private static final String CTB = "référent";

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