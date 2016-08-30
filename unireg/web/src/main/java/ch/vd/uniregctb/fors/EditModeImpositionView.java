package ch.vd.uniregctb.fors;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class EditModeImpositionView {

	private long id;
	private long tiersId;

	private RegDate dateDebut;
	private MotifFor motifDebut;

	private MotifRattachement motifRattachement;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;

	private ModeImposition modeImposition;
	private RegDate dateChangement;
	private MotifFor motifChangement;

	public EditModeImpositionView() {
	}

	public EditModeImpositionView(ForFiscalPrincipalPP ffp) {
		this.id = ffp.getId();
		this.tiersId = ffp.getTiers().getNumero();
		this.dateDebut = ffp.getDateDebut();
		this.motifDebut = ffp.getMotifOuverture();
		this.motifRattachement = ffp.getMotifRattachement();
		this.typeAutoriteFiscale = ffp.getTypeAutoriteFiscale();
		this.noAutoriteFiscale = ffp.getNumeroOfsAutoriteFiscale();
		this.modeImposition = ffp.getModeImposition();
	}

	public void initReadOnlyData(ForFiscalPrincipal ffp) {
		this.id = ffp.getId();
		this.tiersId = ffp.getTiers().getNumero();
		this.dateDebut = ffp.getDateDebut();
		this.motifDebut = ffp.getMotifOuverture();
		this.motifRattachement = ffp.getMotifRattachement();
		this.typeAutoriteFiscale = ffp.getTypeAutoriteFiscale();
		this.noAutoriteFiscale = ffp.getNumeroOfsAutoriteFiscale();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getTiersId() {
		return tiersId;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public MotifFor getMotifDebut() {
		return motifDebut;
	}

	public MotifRattachement getMotifRattachement() {
		return motifRattachement;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public Integer getNoAutoriteFiscale() {
		return noAutoriteFiscale;
	}

	public ModeImposition getModeImposition() {
		return modeImposition;
	}

	public void setModeImposition(ModeImposition modeImposition) {
		this.modeImposition = modeImposition;
	}

	public RegDate getDateChangement() {
		return dateChangement;
	}

	public void setDateChangement(@Nullable RegDate dateChangement) {
		this.dateChangement = dateChangement;
	}

	public MotifFor getMotifChangement() {
		return motifChangement;
	}

	public void setMotifChangement(MotifFor motifChangement) {
		this.motifChangement = motifChangement;
	}
}
