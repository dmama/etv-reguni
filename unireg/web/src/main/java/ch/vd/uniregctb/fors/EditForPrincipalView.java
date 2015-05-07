package ch.vd.uniregctb.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class EditForPrincipalView implements EditForRevenuFortuneView {

	private long id;
	private long tiersId;

	private RegDate dateDebut;
	private RegDate dateFin;
	private boolean dateFinEditable;

	private MotifFor motifDebut;
	private MotifFor motifFin;

	private MotifRattachement motifRattachement;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;
	private ModeImposition modeImposition;

	public EditForPrincipalView() {
	}

	public EditForPrincipalView(ForFiscalPrincipal ffp) {
		this.id = ffp.getId();
		this.tiersId = ffp.getTiers().getNumero();
		this.dateDebut = ffp.getDateDebut();
		this.dateFin = ffp.getDateFin();
		this.dateFinEditable = ffp.getDateFin() == null || ffp.getDateFin().isAfter(RegDate.get());
		this.motifDebut = ffp.getMotifOuverture();
		this.motifFin = ffp.getMotifFermeture();
		this.motifRattachement = ffp.getMotifRattachement();
		this.typeAutoriteFiscale = ffp.getTypeAutoriteFiscale();
		this.noAutoriteFiscale = ffp.getNumeroOfsAutoriteFiscale();

		if (ffp instanceof ForFiscalPrincipalPP) {
			this.modeImposition = ((ForFiscalPrincipalPP) ffp).getModeImposition();
		}
	}

	public void initReadOnlyData(ForFiscalPrincipal ffp) {
		this.id = ffp.getId();
		this.tiersId = ffp.getTiers().getNumero();
		this.dateDebut = ffp.getDateDebut();
		this.dateFinEditable = ffp.getDateFin() == null || ffp.getDateFin().isAfter(RegDate.get());
		if (!this.dateFinEditable) {
			this.dateFin = ffp.getDateFin();
		}
		this.motifDebut = ffp.getMotifOuverture();
		this.motifRattachement = ffp.getMotifRattachement();
		this.typeAutoriteFiscale = ffp.getTypeAutoriteFiscale();

		if (ffp instanceof ForFiscalPrincipalPP) {
			this.modeImposition = ((ForFiscalPrincipalPP) ffp).getModeImposition();
		}
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

	public boolean isDateFinEditable() {
		return dateFinEditable;
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
		return typeAutoriteFiscale;
	}

	@Override
	public Integer getNoAutoriteFiscale() {
		return noAutoriteFiscale;
	}

	public void setNoAutoriteFiscale(Integer noAutoriteFiscale) {
		this.noAutoriteFiscale = noAutoriteFiscale;
	}

	public ModeImposition getModeImposition() {
		return modeImposition;
	}
}
