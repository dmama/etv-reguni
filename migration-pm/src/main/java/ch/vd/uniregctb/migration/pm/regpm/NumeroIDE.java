package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.LongZeroIsNullUserType;

@Embeddable
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "LongZeroIsNull", typeClass = LongZeroIsNullUserType.class)
})
public class NumeroIDE implements Serializable {

	private String categorie;
	private Long numero;

	@Column(name = "CATEGORIE_IDE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "3"))
	public String getCategorie() {
		return categorie;
	}

	public void setCategorie(String categorie) {
		this.categorie = categorie;
	}

	@Column(name = "NO_IDE")
	@Type(type = "LongZeroIsNull")
	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}
}
