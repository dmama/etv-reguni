package ch.vd.unireg.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.ForFiscalAutreElementImposable;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class EditForAutreElementImposableView implements EditForRevenuFortuneView {

	private long id;
	private long tiersId;

	private RegDate dateDebut;
	private RegDate dateFin;

	private MotifFor motifDebut;
	private MotifFor motifFin;

	private MotifRattachement motifRattachement;
	private Integer noAutoriteFiscale;

	public EditForAutreElementImposableView() {
	}

	public EditForAutreElementImposableView(ForFiscalAutreElementImposable ffaei) {
		this.id = ffaei.getId();
		this.tiersId = ffaei.getTiers().getNumero();
		this.dateDebut = ffaei.getDateDebut();
		this.dateFin = ffaei.getDateFin();
		this.motifDebut = ffaei.getMotifOuverture();
		this.motifFin = ffaei.getMotifFermeture();
		this.motifRattachement = ffaei.getMotifRattachement();
		this.noAutoriteFiscale = ffaei.getNumeroOfsAutoriteFiscale();
	}

	public void initReadOnlyData(ForFiscalAutreElementImposable ffaei) {
		this.id = ffaei.getId();
		this.tiersId = ffaei.getTiers().getNumero();
		this.dateDebut = ffaei.getDateDebut();
		this.motifDebut = ffaei.getMotifOuverture();
		this.motifRattachement = ffaei.getMotifRattachement();
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
		return GenreImpot.REVENU_FORTUNE;
	}

	@Override
	public boolean isMotifDebutNullAutorise() {
		// donnée non-éditable, donc non présente dans le bean en retour du POST
		return true;
	}

	@Override
	public boolean isDateDebutNulleAutorisee() {
		// donnée non-éditable, donc non présente dans le bean en retour du POST
		return true;
	}
}
