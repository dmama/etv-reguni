package ch.vd.uniregctb.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class EditForSecondaireView implements EditForRevenuFortuneView {

	private long id;
	private long tiersId;

	private RegDate dateDebut;
	private RegDate dateFin;

	private MotifFor motifDebut;
	private MotifFor motifFin;

	private MotifRattachement motifRattachement;
	private GenreImpot genreImpot;
	private Integer noAutoriteFiscale;

	public EditForSecondaireView() {
	}

	public EditForSecondaireView(ForFiscalSecondaire ffs) {
		this.id = ffs.getId();
		this.tiersId = ffs.getTiers().getNumero();
		this.dateDebut = ffs.getDateDebut();
		this.dateFin = ffs.getDateFin();
		this.motifDebut = ffs.getMotifOuverture();
		this.motifFin = ffs.getMotifFermeture();
		this.motifRattachement = ffs.getMotifRattachement();
		this.noAutoriteFiscale = ffs.getNumeroOfsAutoriteFiscale();
		this.genreImpot = ffs.getGenreImpot();
	}

	public void initReadOnlyData(ForFiscalSecondaire ffs) {
		this.id = ffs.getId();
		this.tiersId = ffs.getTiers().getNumero();
		this.motifRattachement = ffs.getMotifRattachement();
		this.genreImpot = ffs.getGenreImpot();
	}

	@Override
	public boolean isDateFinFutureAutorisee() {
		return false;
	}

	@Override
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public long getTiersId() {
		return tiersId;
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

	public void setMotifFin(MotifFor motifFin) {
		this.motifFin = motifFin;
	}

	@Override
	public MotifRattachement getMotifRattachement() {
		return motifRattachement;
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
	public GenreImpot getGenreImpot() {
		return genreImpot;
	}

	@Override
	public boolean isMotifDebutNullAutorise() {
		return false;
	}

	@Override
	public boolean isDateDebutNulleAutorisee() {
		return false;
	}
}
