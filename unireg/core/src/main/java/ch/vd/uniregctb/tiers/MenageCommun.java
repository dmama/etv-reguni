package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 *
 * @author jec
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nsp7UJN8Edy7DqR-SPIh9g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nsp7UJN8Edy7DqR-SPIh9g"
 */
@Entity
@DiscriminatorValue("MenageCommun")
public class MenageCommun extends Contribuable {

    private static final long serialVersionUID = -2860998550744237583L;

    @Transient
    @Override
    public String getNatureTiers() {
        return MenageCommun.class.getSimpleName();
    }

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.MENAGE_COMMUN;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValidationResults validateRapports() {
        final ValidationResults results = super.validateRapports();

        // vérifie que le ménage commun ne comporte au plus que 2 personnes physiques distinctes
        final Set<RapportEntreTiers> rapports = getRapportsObjet();
        if (rapports != null) {
            final Set<Long> idComposants = new HashSet<Long>(4);
            for (RapportEntreTiers r : rapports) {
                if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType()) {
                    final Long id = r.getSujetId();
                    if (id != null) {
                        idComposants.add(id);
                    }
                }
            }
            if (idComposants.size() > 2) {
                results.addError("Le ménage commun est lié avec plus de 2 personnes physiques distinctes [n°="
                        + ArrayUtils.toString(idComposants.toArray()) + "]");
            }
        }

        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationResults validateFors() {

        ValidationResults results = super.validateFors();

        /*
           * On n'autorise la présence de fors que durant la ou les périodes de validité du couple.
           */
        // Détermine les périodes de validités ininterrompues du ménage commun
        List<RapportEntreTiers> rapportsMenages = new ArrayList<RapportEntreTiers>();
        Set<RapportEntreTiers> rapports = getRapportsObjet();
        if (rapports != null) {
            for (RapportEntreTiers r : rapports) {
                if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType()) {
                    rapportsMenages.add(r);
                }
            }
        }
        Collections.sort(rapportsMenages, new DateRangeComparator<RapportEntreTiers>());
        final List<DateRange> periodes = DateRangeHelper.collateRange(rapportsMenages);

        // Vérifie que chaque for est entièrement défini à l'intérieur d'une période de validité
        final Set<ForFiscal> fors = getForsFiscaux();
        if (fors != null) {
            for (ForFiscal f : fors) {
                if (f.isAnnule()) {
                    continue;
                }
                DateRange intersection = DateRangeHelper.intersection(f, periodes);
                if (intersection == null || !DateRangeHelper.equals(f, intersection)) {
                    results.addError("Le for fiscal [" + f
                            + "] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [" + getNumero() + "]");
                }
            }
        }

        return results;
    }

    @Override
    protected ValidationResults validateTypeAdresses() {

        ValidationResults results = new ValidationResults();

        final Set<AdresseTiers> adresses = getAdressesTiers();
        if (adresses != null) {
            for (AdresseTiers a : adresses) {
                if (a.isAnnule()) {
                    continue;
                }
                if (a instanceof AdressePM) {
                    results.addError("L'adresse de type 'personne morale' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
                            + a.getDateFin() + ") n'est pas autorisée sur un ménage commun.");
                } else if (a instanceof AdresseCivile) {
                    results.addError("L'adresse de type 'personne civile' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
                            + a.getDateFin() + ") n'est pas autorisée sur un ménage commun.");
                }
            }
        }

        return results;
    }

    @Transient
    @Override
    public String getRoleLigne1() {
        return "Contribuable PP";
	}

}
