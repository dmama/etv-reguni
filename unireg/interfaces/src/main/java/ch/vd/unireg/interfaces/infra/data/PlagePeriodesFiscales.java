package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

/**
 * Plage de périodes fiscales (la borne inférieure est obligatoire, la borne supérieure ne l'est pas,
 * mais les bornes sont inclues dans la plage, si elles sont présentes)
 */
public class PlagePeriodesFiscales implements Serializable {

	private static final long serialVersionUID = -2669016909224538797L;

	private final int periodeDebut;
	private final Integer periodeFin;

	public PlagePeriodesFiscales(int periodeDebut, @Nullable Integer periodeFin) {
		this.periodeDebut = periodeDebut;
		this.periodeFin = periodeFin;
	}

	/**
	 * @param periode une période fiscale
	 * @return <code>true</code> si la période fait partie de la plage, <code>false</code> sinon
	 */
	public boolean isDansPlage(int periode) {
		return periode >= periodeDebut && (periodeFin == null || periode <= periodeFin);
	}

	public int getPeriodeDebut() {
		return periodeDebut;
	}

	@Nullable
	public Integer getPeriodeFin() {
		return periodeFin;
	}
}
