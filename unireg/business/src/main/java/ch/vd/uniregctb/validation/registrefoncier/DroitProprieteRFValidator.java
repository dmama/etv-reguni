package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;

@SuppressWarnings("Duplicates")
public class DroitProprieteRFValidator extends DroitRFValidator<DroitProprieteRF> {
	@Override
	protected Class<DroitProprieteRF> getValidatedClass() {
		return DroitProprieteRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le droit de propriété RF";
	}

	@Override
	public ValidationResults validate(DroitProprieteRF entity) {
		final ValidationResults results = super.validate(entity);

		// une entité annulée est toujours valide...
		if (entity.isAnnule()) {
			return results;
		}

		// [SIFISC-23894] la date de début métier et le motif de début doit correspondre à la première valeur dans l'historique des raisons d'acquisition
		final Set<RaisonAcquisitionRF> raisons = entity.getRaisonsAcquisition();
		if (raisons != null) {  // note: si la collection n'est pas renseignée, c'est qu'on est dans la première phase (save) : on attend la seconde phase (update) pour vérifier la cohérence.
			final RaisonAcquisitionRF premiereRaison = raisons.stream()
					.min(Comparator.naturalOrder())
					.orElse(null);

			if (premiereRaison == null) {
				if (entity.getDateDebutMetier() != null) {
					results.addError(String.format("%s %s possède une date de début métier renseignée (%s) alors qu'il n'y a pas de raison d'acquisition",
					                               getEntityCategoryName(),
					                               getEntityDisplayString(entity),
					                               RegDateHelper.dateToDisplayString(entity.getDateDebutMetier())));
				}
				if (StringUtils.isNotBlank(entity.getMotifDebut())) {
					results.addError(String.format("%s %s possède un motif de début métier renseigné (%s) alors qu'il n'y a pas de raison d'acquisition",
					                               getEntityCategoryName(),
					                               getEntityDisplayString(entity),
					                               entity.getMotifDebut()));
				}
			}
			else {
				if (entity.getDateDebutMetier() != premiereRaison.getDateAcquisition()) {
					results.addError(String.format("%s %s possède une date de début métier (%s) différente de la date de la première raison d'acquisition (%s)",
					                               getEntityCategoryName(),
					                               getEntityDisplayString(entity),
					                               RegDateHelper.dateToDisplayString(entity.getDateDebutMetier()),
					                               RegDateHelper.dateToDisplayString(premiereRaison.getDateAcquisition())));
				}
				if (!Objects.equals(entity.getMotifDebut(), premiereRaison.getMotifAcquisition())) {
					results.addError(String.format("%s %s possède un motif de début (%s) différent du motif de la première raison d'acquisition (%s)",
					                               getEntityCategoryName(),
					                               getEntityDisplayString(entity),
					                               entity.getMotifDebut(),
					                               premiereRaison.getMotifAcquisition()));
				}
			}
		}

		return results;
	}
}
