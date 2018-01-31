package ch.vd.uniregctb.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class AddForAutreImpotView implements AddForView {

	private Long id;
	private long tiersId;
	private RegDate dateEvenement;
	private GenreImpot genreImpot;
	private Integer noAutoriteFiscale;
	private String nomAutoriteFiscale;

	public AddForAutreImpotView() {
	}

	public AddForAutreImpotView(long tiersId) {
		this.tiersId = tiersId;
	}

	@Override
	public boolean isDateFinFutureAutorisee() {
		return false;
	}

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public long getTiersId() {
		return tiersId;
	}

	public void setTiersId(long tiersId) {
		this.tiersId = tiersId;
	}

	@Override
	public RegDate getDateDebut() {
		return dateEvenement;
	}

	@Override
	public RegDate getDateFin() {
		return dateEvenement;
	}

	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	public GenreImpot getGenreImpot() {
		return genreImpot;
	}

	public void setGenreImpot(GenreImpot genreImpot) {
		this.genreImpot = genreImpot;
	}

	@Override
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Override
	public Integer getNoAutoriteFiscale() {
		return noAutoriteFiscale;
	}

	public void setNoAutoriteFiscale(Integer noAutoriteFiscale) {
		this.noAutoriteFiscale = noAutoriteFiscale;
	}

	@Override
	public String getNomAutoriteFiscale() {
		return nomAutoriteFiscale;
	}

	public void setNomAutoriteFiscale(String nomAutoriteFiscale) {
		this.nomAutoriteFiscale = nomAutoriteFiscale;
	}

}
