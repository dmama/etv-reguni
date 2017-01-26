package ch.vd.unireg.interfaces.infra.data;

import org.jetbrains.annotations.Nullable;

/**
 * @author Raphaël Marmier, 2017-03-10, <raphael.marmier@vd.ch>
 */
public class PlageExonerationFiscales extends PlagePeriodesFiscales {

	private GenreImpot genreImpot;
	private ModeExoneration mode;

	public PlageExonerationFiscales(int periodeDebut, @Nullable Integer periodeFin, GenreImpot genreImpot, ModeExoneration mode) {
		super(periodeDebut, periodeFin);
		this.genreImpot = genreImpot;
		this.mode = mode;
	}

	public GenreImpot getGenreImpot() {
		return genreImpot;
	}

	public ModeExoneration getMode() {
		return mode;
	}
}
