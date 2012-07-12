package ch.vd.uniregctb.metier.assujettissement;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de base abstraite représentant une période d'assujettissement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class Assujettissement implements CollatableDateRange {

	private final Contribuable contribuable;
	private RegDate dateDebut;
	private RegDate dateFin;
	private MotifFor motifDebut;
	private MotifFor motifFin;
	private DecompositionFors fors;

	protected Assujettissement(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.contribuable = contribuable;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.motifDebut = motifDebut;
		this.motifFin = motifFin;
		this.fors = null;
	}

	/**
	 * Permet de construire un assujettissement unique composé de deux assujettissement de même types qui se touchent
	 *
	 * @param courant l'assujettissement courant
	 * @param suivant l'assujettissement suivant
	 */
	protected Assujettissement(Assujettissement courant, Assujettissement suivant) {
		if (!courant.isCollatable(suivant)) {
			throw new IllegalArgumentException();
		}
		this.contribuable = courant.contribuable;
		this.dateDebut = courant.dateDebut;
		this.dateFin = suivant.dateFin;
		this.motifDebut = courant.motifDebut;
		this.motifFin = suivant.motifFin;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.fors = null;
	}

	public Contribuable getContribuable() {
		return contribuable;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	protected void setDateDebut(RegDate date) {
		this.dateDebut = date;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	protected void setDateFin(RegDate date) {
		this.dateFin = date;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
	}

	@Override
	public boolean isCollatable(DateRange next) {
		// dans le cas d'un départ HS et d'une arrivée HC, on ne veut pas collater les deux assujettissements
		final boolean departHSEtArriveeHC = (this.motifFin == MotifFor.DEPART_HS && ((Assujettissement) next).motifDebut == MotifFor.ARRIVEE_HC);

		// vente du dernier immeuble une année et rachat d'un autre l'année suivante
		final boolean venteDernierImmeubleEtRachatAnneeSuivante = this.motifFin == MotifFor.VENTE_IMMOBILIER && ((Assujettissement) next).motifDebut == MotifFor.ACHAT_IMMOBILIER && isYearSwitch(this.dateFin, next.getDateDebut());

		return !departHSEtArriveeHC && !venteDernierImmeubleEtRachatAnneeSuivante && getClass() == next.getClass() && DateRangeHelper.isCollatable(this, next);
	}

	private static boolean isYearSwitch(RegDate one, RegDate two) {
		return one != null && two != null && two.year() == one.year() + 1;
	}

	protected void setMotifDebut(@Nullable MotifFor motifDebut) {
		this.motifDebut = motifDebut;
	}

	/**
	 * @return le motif de début de l'assujettissement
	 */
	public MotifFor getMotifFractDebut() {
		return motifDebut;
	}

	protected void setMotifFin(@Nullable MotifFor motifFin) {
		this.motifFin = motifFin;
	}

	/**
	 * @return le motif de fin de l'assujettissement
	 */
	public MotifFor getMotifFractFin() {
		return motifFin;
	}

	public DecompositionFors getFors() {
		if (fors == null) { // lazy init
			fors = new DecompositionForsPeriode(contribuable, dateDebut, dateFin);
		}
		return fors;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * @return une description orienté utilisateur du type d'assujettissement
	 */
	public abstract String getDescription();

	/**
	 * Un assujettissement est dit "actif" sur une commune donnée si cette commune doit recevoir des sous par la répartition inter-communale concernant ce contribuable, donc dans le cas d'un contribuable
	 * résident vaudois qui possède un for secondaire sur une autre commune vaudoise, les deux communes concernées seront considéreées comme "actives".
	 * <p/>
	 * Cette méthode <b>n'est pas appelable</b> sur un assujettissement résultat d'une "collation" car alors les fors sous-jacents ne sont pas conservés.
	 *
	 * @param noOfsCommune numéro OFS de la commune vaudoise pour laquelle la question est posée
	 * @return vrai si l'assujettissement est actif sur la commune considérée, faux sinon
	 */
	public boolean isActifSurCommune(int noOfsCommune) {

		final DecompositionFors fors = getFors();

		boolean actif = false;
		final ForFiscalPrincipal ffp = (fors.principal != null ? fors.principal : fors.principauxDansLaPeriode.last());
		if (ffp != null && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffp.getNumeroOfsAutoriteFiscale() == noOfsCommune) {
			actif = true;
		}
		else if (!fors.secondairesDansLaPeriode.isEmpty()) {
			for (ForFiscalSecondaire ffs : fors.secondairesDansLaPeriode) {
				if (ffs.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffs.getNumeroOfsAutoriteFiscale() == noOfsCommune) {
					actif = true;
					break;
				}
			}
		}
		return actif;
	}

	@Override
	public abstract String toString();
}
