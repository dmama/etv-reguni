package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XYLmQBw_Ed2SDKWRJy7Z3g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XYLmQBw_Ed2SDKWRJy7Z3g"
 */
@Entity
@DiscriminatorValue("ForFiscalAutreImpot")
public class ForFiscalAutreImpot extends ForFiscal {

	private static final long serialVersionUID = 2472800148068209440L;

	public ForFiscalAutreImpot() {
	}

	public ForFiscalAutreImpot(ForFiscalAutreImpot ffai) {
		super(ffai);
	}

	public ForFiscal duplicate() {
		return new ForFiscalAutreImpot(this);
	}

}
