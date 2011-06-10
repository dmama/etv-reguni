package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Représente un for de gestion, c'est-à-dire la commune vaudoise désignée responsable de la gestion d'un contribuable. De fait, l'entité
 * véritablement responsable du contribuable est l'office d'impôt du district qui contient la commune désignée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ForGestion implements DateRange {

	private final RegDate dateDebut;
	private RegDate dateFin;
	private final int noOfsCommune;
	private final ForFiscalRevenuFortune sousjacent;

	/**
	 * Crée un for de gestion basé sur un for fiscal particulier, en reprenant les dates de début et de fin.
	 *
	 * @param sousjacent
	 *            le for fiscal sur lequel sera basé le for de gestion
	 */
	public ForGestion(ForFiscalRevenuFortune sousjacent) {
		Assert.isEqual(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, sousjacent.getTypeAutoriteFiscale());
		this.dateDebut = sousjacent.getDateDebut();
		this.dateFin = sousjacent.getDateFin();
		this.noOfsCommune = sousjacent.getNumeroOfsAutoriteFiscale();
		this.sousjacent = sousjacent;
	}

	/**
	 * Crée un for de gestion basé sur un for fiscal particulier, en adaptant les dates de début et de fin.
	 *
	 * @param dateDebut
	 *            la date de début effective du for de gestion
	 * @param dateFin
	 *            la date de fin effective du for de gestion
	 * @param sousjacent
	 *            le for fiscal sur lequel sera basé le for de gestion
	 */
	public ForGestion(RegDate dateDebut, RegDate dateFin, ForFiscalRevenuFortune sousjacent) {
		Assert.isEqual(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, sousjacent.getTypeAutoriteFiscale());
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.noOfsCommune = sousjacent.getNumeroOfsAutoriteFiscale();
		this.sousjacent = sousjacent;
	}

	/**
	 * Crée un for de gestion basé sur un autre for de gestion, mais en adaptant les dates de début et de fin.
	 *
	 * @param dateDebut
	 *            la date de début effective du for de gestion
	 * @param dateFin
	 *            la date de fin effective du for de gestion
	 * @param autre
	 *            l'autre for de gestion
	 */
	public ForGestion(RegDate dateDebut, RegDate dateFin, ForGestion autre) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.noOfsCommune = autre.noOfsCommune;
		this.sousjacent = autre.sousjacent;
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

	public int getNoOfsCommune() {
		return noOfsCommune;
	}

	public boolean isColletable(ForGestion right) {
		return (dateFin != null && dateFin.getOneDayAfter() == right.dateDebut && noOfsCommune == right.noOfsCommune);
	}

	public void collate(ForGestion right) {
		dateFin = right.dateFin;
	}

	/**
	 * @return le for fiscal sur lequel est basé le for de gestion. Ce for fiscal possède le même numéro Ofs de commune, mais peut posséder
	 *         des dates de début et de fin différentes.
	 */
	public ForFiscalRevenuFortune getSousjacent() {
		return sousjacent;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
