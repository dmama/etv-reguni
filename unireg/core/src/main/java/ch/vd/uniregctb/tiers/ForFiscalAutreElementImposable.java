package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Tg9noBxAEd2SDKWRJy7Z3g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Tg9noBxAEd2SDKWRJy7Z3g"
 */
@Entity
@DiscriminatorValue("ForFiscalAutreElementImposable")
public class ForFiscalAutreElementImposable extends ForFiscalRevenuFortune {

	private static final long serialVersionUID = -2847235949313474922L;

	public ForFiscalAutreElementImposable() {
	}

	public ForFiscalAutreElementImposable(ForFiscalAutreElementImposable ffaei) {
		super(ffaei);
	}

	public ForFiscal duplicate() {
		return new ForFiscalAutreElementImposable(this);
	}
}
