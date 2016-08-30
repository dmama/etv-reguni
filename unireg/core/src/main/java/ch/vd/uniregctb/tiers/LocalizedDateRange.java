package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.DateRange;

/**
 * Interface qui définit un accès à une localisation (= type autorité fiscale + numéro OFS) sur une plage de validité ({@link DateRange})
 */
public interface LocalizedDateRange extends DateRange, LocalisationFiscale {
}
