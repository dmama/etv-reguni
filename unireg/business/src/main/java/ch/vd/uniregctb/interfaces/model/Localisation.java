package ch.vd.uniregctb.interfaces.model;

/**
 * Détermine un lieu civil avec un certain niveau de précision (au niveau de la commune ou du pays).
 */
public class Localisation {

	private LocalisationType type;
	private int noOfs;

	public Localisation() {
	}

	public Localisation(LocalisationType type, int noOfs) {
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
	public int getNoOfs() {
		return noOfs;
	}

	public void setNoOfs(int noOfs) {
		this.noOfs = noOfs;
	}
}
