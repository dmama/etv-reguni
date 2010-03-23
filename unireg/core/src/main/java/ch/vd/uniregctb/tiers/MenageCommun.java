package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import org.apache.commons.lang.ArrayUtils;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.*;

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

    /**
     * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun
     *         (0, 1 ou 2 personnes max, par définition) en ignorant les rapports annulés
     */
    @Transient
    public Set<PersonnePhysique> getPersonnesPhysiques() {
        return getPersonnesPhysiques(false).keySet();
    }

    /**
     * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun
     *         en prenant en compte les rapports éventuellement annulés (il peut donc y avoir plus de
     *         deux personnes physiques concernées en cas de correction de données) ; le dernier
     *         rapport entre tiers est également indiqué
     */
    @Transient
    public Map<PersonnePhysique, RapportEntreTiers> getToutesPersonnesPhysiquesImpliquees() {
        return getPersonnesPhysiques(true);
    }

    /**
     * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun
     *         en ignorant (ou pas) les rapports annulés ; le dernier rapport entre tiers est également indiqué
     */
    private Map<PersonnePhysique, RapportEntreTiers> getPersonnesPhysiques(boolean aussiRapportsAnnules) {
        final Map<PersonnePhysique, RapportEntreTiers> personnes = new HashMap<PersonnePhysique, RapportEntreTiers>(aussiRapportsAnnules ? 4 : 2);
        final Set<RapportEntreTiers> rapports = getRapportsObjet();
        if (rapports != null) {
            for (RapportEntreTiers r : rapports) {
                if ((aussiRapportsAnnules || !r.isAnnule()) && r.getType().equals(TypeRapportEntreTiers.APPARTENANCE_MENAGE)) {

	                // on ne considère que les rapport dont le ménage commun est l'objet
	                // (les autres correspondent à des rattrapages de données en prod...)
	                final Tiers objet = r.getObjet();
	                if (objet.getId().equals(getId())) {

						final PersonnePhysique sujet = (PersonnePhysique) r.getSujet();

						// si le rapport est annulé, on vérifie qu'il n'existe pas un
						// autre rapport avec la même personne physique (le non-annulé a la priorité !)
						boolean ignore = false;
						if (r.isAnnule()) {
							// s'il n'y est pas déjà, ou
							// s'il y est déjà, et que l'autre date d'annulation est antérieure,
							// alors cette nouvelle date remplace la valeur précédente
							final RapportEntreTiers rapportConnu = personnes.get(sujet);
							if (rapportConnu != null) {
								final Date annulationConnue = rapportConnu.getAnnulationDate();
								if (annulationConnue == null || annulationConnue.after(r.getAnnulationDate())) {
									ignore = true;
								}
							}
						}
						if (!ignore) {
							personnes.put(sujet, r);
						}
	                }
                }
            }
        }
        return personnes;
    }

    @Transient
    @Override
    public String getNatureTiers() {
        return MenageCommun.class.getSimpleName();
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
                if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(r.getType())) {
                    final Tiers composant = r.getSujet();
                    if (composant != null) {
                        idComposants.add(composant.getNumero());
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
                if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(r.getType())) {
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
