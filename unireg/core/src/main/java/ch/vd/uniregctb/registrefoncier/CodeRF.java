package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Embeddable;

/**
 * Code et description générique utilisé dans le registre foncier pour toute sortes de types.
 */
@Embeddable
public class CodeRF {
	private String code;
	private String description;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
