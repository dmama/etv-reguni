/**
 *
 */
package ch.vd.unireg.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TypeAdresseTiers;

@Entity
@DiscriminatorValue("AdresseAutreTiers")
public class AdresseAutreTiers extends AdresseTiers {

	private TypeAdresseTiers type;


	private Long autreTiersId;

	public AdresseAutreTiers() {
	}

	protected AdresseAutreTiers(AdresseAutreTiers src) {
		super(src);
		this.type = src.type;
		this.autreTiersId = src.autreTiersId;
	}

	@Column(name = "AUTRE_TYPE", length = LengthConstants.ADRESSE_TYPETIERS)
	@Type(type = "ch.vd.unireg.hibernate.TypeAdresseTiersUserType")
	public TypeAdresseTiers getType() {
		return type;
	}

	public void setType(TypeAdresseTiers theType) {
		type = theType;
	}

	@Column(name = "AUTRE_TIERS_ID")
	@Index(name = "IDX_ADR_AT_TRS_ID", columnNames = "AUTRE_TIERS_ID")
	public Long getAutreTiersId() {
		return autreTiersId;
	}

	public void setAutreTiersId(Long id) {
		autreTiersId = id;
	}

	@Override
	public AdresseAutreTiers duplicate() {
		return new AdresseAutreTiers(this);
	}
}
