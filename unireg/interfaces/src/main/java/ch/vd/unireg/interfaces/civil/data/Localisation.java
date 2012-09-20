package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

/**
 * Détermine un lieu civil avec un certain niveau de précision (au niveau de la commune ou du pays) et - éventuellement - une adresse courrier.
 */
public class Localisation implements Serializable {

	private static final long serialVersionUID = 2685740788096283313L;

	private LocalisationType type;
	private Integer noOfs;
	private Adresse adresseCourrier;

	public Localisation() {
	}

	public Localisation(LocalisationType type, Integer noOfs, @Nullable Adresse adresseCourrier) {
		this.type = type;
		this.noOfs = noOfs;
		this.adresseCourrier = adresseCourrier;
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

	/**
	 * @return l'adresse courrier si elle est connue.
	 */
	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}

	public void setAdresseCourrier(Adresse adresseCourrier) {
		this.adresseCourrier = adresseCourrier;
	}
}
