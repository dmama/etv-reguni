package ch.vd.unireg.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class EditForDebiteurView implements EditForAvecMotifsView {

	private long id;
	private long tiersId;

	private RegDate dateDebut;
	private MotifFor motifDebut;
	private RegDate dateFin;
	private MotifFor motifFin;
	private boolean ouvertureEditable;
	private boolean fermetureEditable;
	private boolean forFerme;

	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;

	public EditForDebiteurView() {
	}

	public EditForDebiteurView(ForDebiteurPrestationImposable fdpi) {
		this.id = fdpi.getId();
		this.tiersId = fdpi.getTiers().getId();
		this.dateDebut = fdpi.getDateDebut();
		this.motifDebut = fdpi.getMotifOuverture();
		this.dateFin = fdpi.getDateFin();
		this.motifFin = fdpi.getMotifFermeture();
		this.ouvertureEditable = fdpi.getDateDebut() == null;
		this.fermetureEditable = fdpi.getDateFin() == null || fdpi.getDateFin().isAfter(RegDate.get());
		this.forFerme = fdpi.getDateFin() != null;
		this.typeAutoriteFiscale = fdpi.getTypeAutoriteFiscale();
		this.noAutoriteFiscale = fdpi.getNumeroOfsAutoriteFiscale();
	}

	public void reinitReadOnlyData(ForDebiteurPrestationImposable fdpi) {
		this.id = fdpi.getId();
		this.tiersId = fdpi.getTiers().getId();
		this.ouvertureEditable = fdpi.getDateDebut() == null;
		if (!this.ouvertureEditable) {
			this.dateDebut = fdpi.getDateDebut();
			this.motifDebut = fdpi.getMotifOuverture();
		}
		this.fermetureEditable = fdpi.getDateFin() == null || fdpi.getDateFin().isAfter(RegDate.get());
		if (!this.fermetureEditable) {
			this.dateFin = fdpi.getDateFin();
			this.motifFin = fdpi.getMotifFermeture();
		}
		this.forFerme = fdpi.getDateFin() != null;
		this.typeAutoriteFiscale = fdpi.getTypeAutoriteFiscale();
		this.noAutoriteFiscale = fdpi.getNumeroOfsAutoriteFiscale();
	}

	@Override
	public boolean isDateFinFutureAutorisee() {
		return true;
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
	public boolean isMotifDebutNullAutorise() {
		return false;
	}

	@Override
	public boolean isDateDebutNulleAutorisee() {
		return false;
	}

	public boolean isOuvertureEditable() {
		return ouvertureEditable;
	}

	public boolean isFermetureEditable() {
		return fermetureEditable;
	}

	public boolean isForFerme() {
		return forFerme;
	}

	public void setForFerme(boolean forFerme) {
		this.forFerme = forFerme;
	}

	public MotifFor getMotifFin() {
		return motifFin;
	}

	public void setMotifFin(MotifFor motifFin) {
		this.motifFin = motifFin;
	}

	public MotifFor getMotifDebut() {
		return motifDebut;
	}

	public void setMotifDebut(MotifFor motifDebut) {
		this.motifDebut = motifDebut;
	}

	@Override
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	@Override
	public Integer getNoAutoriteFiscale() {
		return noAutoriteFiscale;
	}

	@Override
	public GenreImpot getGenreImpot() {
		return GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE;
	}
}
