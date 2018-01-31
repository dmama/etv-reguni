package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.common.Adresse;

/**
 * Détermine un lieu civil avec un certain niveau de précision (au niveau de la commune ou du pays) et - éventuellement - une adresse courrier.
 */
public class Localisation implements Serializable {

	private static final long serialVersionUID = 3291002122245844939L;

	private final LocalisationType type;
	private final Integer noOfs;
	private final Adresse adresseCourrier;

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

	/**
	 * @return le numéro Ofs du lieu civil concerné (numéro Ofs de commune ou de pays en fonction du type).
	 */
	@Nullable
	public Integer getNoOfs() {
		return noOfs;
	}

	/**
	 * @return l'adresse courrier si elle est connue.
	 */
	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}
}
