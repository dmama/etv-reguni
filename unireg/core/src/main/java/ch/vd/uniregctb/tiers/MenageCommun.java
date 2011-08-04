package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 *
 * @author jec
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nsp7UJN8Edy7DqR-SPIh9g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nsp7UJN8Edy7DqR-SPIh9g"
 */
@Entity
@DiscriminatorValue("MenageCommun")
public class MenageCommun extends Contribuable {

    private static final long serialVersionUID = -2860998550744237583L;

    @Transient
    @Override
    public NatureTiers getNatureTiers() {
        return NatureTiers.MenageCommun;
    }

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.MENAGE_COMMUN;
	}

    @Transient
    @Override
    public String getRoleLigne1() {
        return "Contribuable PP";
	}

}
