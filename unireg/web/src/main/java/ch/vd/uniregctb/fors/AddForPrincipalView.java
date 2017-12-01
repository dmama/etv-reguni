package ch.vd.uniregctb.fors;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class AddForPrincipalView implements AddForRevenuFortuneView {

	private Long id;
	private long tiersId;

	private RegDate dateDebut;
	private RegDate dateFin;

	private MotifFor motifDebut;
	private MotifFor motifFin;

	private MotifRattachement motifRattachement;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;
	private String autoriteFiscaleNom;
	private ModeImposition modeImposition;
	private GenreImpot genreImpot;

	public AddForPrincipalView() {
	}

	public AddForPrincipalView(long tiersId, GenreImpot genreImpot) {
		this.tiersId = tiersId;
		this.genreImpot = genreImpot;
		this.motifRattachement = MotifRattachement.DOMICILE;
		this.typeAutoriteFiscale = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		this.modeImposition = ModeImposition.ORDINAIRE;
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
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public MotifFor getMotifDebut() {
		return motifDebut;
	}

	public void setMotifDebut(MotifFor motifDebut) {
		this.motifDebut = motifDebut;
	}

	@Override
	public MotifFor getMotifFin() {
		return motifFin;
	}

	public void setMotifFin(@Nullable MotifFor motifFin) {
		this.motifFin = motifFin;
	}

	@Override
	public MotifRattachement getMotifRattachement() {
		return motifRattachement;
	}

	public void setMotifRattachement(MotifRattachement motifRattachement) {
		this.motifRattachement = motifRattachement;
	}

	@Override
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	@Override
	public Integer getNoAutoriteFiscale() {
		return noAutoriteFiscale;
	}

	public String getAutoriteFiscaleNom() {
		return autoriteFiscaleNom;
	}

	public void setAutoriteFiscaleNom(String autoriteFiscaleNom) {
		this.autoriteFiscaleNom = autoriteFiscaleNom;
	}

	public void setNoAutoriteFiscale(Integer noAutoriteFiscale) {
		this.noAutoriteFiscale = noAutoriteFiscale;
	}

	public ModeImposition getModeImposition() {
		return modeImposition;
	}

	public void setModeImposition(ModeImposition modeImposition) {
		this.modeImposition = modeImposition;
	}

	public GenreImpot getGenreImpot() {
		return genreImpot;
	}

	public void setGenreImpot(GenreImpot genreImpot) {
		this.genreImpot = genreImpot;
	}
}
