package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * Classe utilitaire pour associer une catégorie d'entreprise à une plage de dates
 */
public class CategorieEntrepriseHisto implements CollatableDateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final CategorieEntreprise categorie;

	public CategorieEntrepriseHisto(RegDate dateDebut, RegDate dateFin, CategorieEntreprise categorie) {
		this.categorie = categorie;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	@Override
	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next) && next instanceof CategorieEntrepriseHisto && ((CategorieEntrepriseHisto) next).categorie == categorie;
	}

	@Override
	public CategorieEntrepriseHisto collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ranges are not collatable!");
		}
		return new CategorieEntrepriseHisto(dateDebut, next.getDateFin(), categorie);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public CategorieEntreprise getCategorie() {
		return categorie;
	}
}
