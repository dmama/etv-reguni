package ch.vd.uniregctb.interfaces.model;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

/**
 * Détermine un lieu civil avec un certain niveau de précision (au niveau de la commune ou du pays).
 */
public class Localisation implements Serializable {

	private static final long serialVersionUID = 3692017738468930175L;

	private LocalisationType type;
	private Integer noOfs;

	public Localisation() {
	}

	public Localisation(LocalisationType type, Integer noOfs) {
		this.type = type;
		this.noOfs = noOfs;
	}

	/**
	 * @return le type de localisation, c'est-à-dire le lieu civil concerné
	 */
	public LocalisationType getType() {
		return type;
	}

	public void setType(LocalisationType type) {
		this.type = type;
	}

	/**
	 * @return le numéro Ofs du lieu civil concerné (numéro Ofs de commune ou de pays en fonction du type).
	 */
	@Nullable
	public Integer getNoOfs() {
		return noOfs;
	}

	public void setNoOfs(Integer noOfs) {
		this.noOfs = noOfs;
	}
}
