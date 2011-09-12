package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

@Entity
@DiscriminatorValue("RETOURNEE")
public class EtatDeclarationRetournee extends EtatDeclaration {

	/**
	 * Nom de la source en cas de quittance à travers l'interface web d'Unireg.
	 */
	public static final String SOURCE_WEB = "WEB";

	/**
	 * Nom de la source en cas de quittance automatique des DIs des contribuables indigents.
	 */
	public static final String SOURCE_INDIGENT = "INDIGENT";

	/**
	 * Nom de la source en cas de quittance par le web-service lorsque la source n'est pas explicitement spécifiée (= anciens clients).
	 */
	public static final String SOURCE_CEDI = "CEDI";

	/**
	 * [SIFISC-1782] La source du quittancement de la déclaration (CEDI, ADDI ou manuel).
	 */
	private String source;

	public EtatDeclarationRetournee() {
		super();
	}

	public EtatDeclarationRetournee(RegDate dateObtention, String source) {
		super(dateObtention);
		this.source = source;
	}

	@Override
	@Transient
	public TypeEtatDeclaration getEtat() {
		return TypeEtatDeclaration.RETOURNEE;
	}

	@Column(name = "SOURCE")
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
