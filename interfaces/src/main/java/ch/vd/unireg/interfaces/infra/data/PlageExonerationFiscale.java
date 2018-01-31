package ch.vd.unireg.interfaces.infra.data;

import org.jetbrains.annotations.Nullable;

/**
 * @author Raphaël Marmier, 2017-03-10, <raphael.marmier@vd.ch>
 */
public class PlageExonerationFiscale extends PlagePeriodesFiscales {

	private final GenreImpotExoneration genreImpot;
	private final ModeExoneration mode;

	public PlageExonerationFiscale(int periodeDebut, @Nullable Integer periodeFin, GenreImpotExoneration genreImpot, ModeExoneration mode) {
		super(periodeDebut, periodeFin);
		this.genreImpot = genreImpot;
		this.mode = mode;
	}

	public GenreImpotExoneration getGenreImpot() {
		return genreImpot;
	}

	public ModeExoneration getMode() {
		return mode;
	}
}
