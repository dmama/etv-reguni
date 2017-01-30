package ch.vd.uniregctb.foncier.migration.ifonc;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.foncier.migration.BaseMigrationData;

public class MigrationExoIFONC extends BaseMigrationData {

	private int pourdixmilleExoneration;
	private int anneeDebutExoneration;
	@Nullable
	private Integer anneeFinExoneration;

	public int getPourdixmilleExoneration() {
		return pourdixmilleExoneration;
	}

	public void setPourdixmilleExoneration(int pourdixmilleExoneration) {
		this.pourdixmilleExoneration = pourdixmilleExoneration;
	}

	public int getAnneeDebutExoneration() {
		return anneeDebutExoneration;
	}

	public void setAnneeDebutExoneration(int anneeDebutExoneration) {
		this.anneeDebutExoneration = anneeDebutExoneration;
	}

	@Nullable
	public Integer getAnneeFinExoneration() {
		return anneeFinExoneration;
	}

	public void setAnneeFinExoneration(@Nullable Integer anneeFinExoneration) {
		this.anneeFinExoneration = anneeFinExoneration;
	}

	@Override
	protected String getAttributesToString() {
		return super.getAttributesToString() +
				", pourdixmilleExoneration=" + pourdixmilleExoneration +
				", anneeDebutExoneration=" + anneeDebutExoneration +
				", anneeFinExoneration=" + anneeFinExoneration;
	}
}
