package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.type.GenreImpot;

public abstract class EditForRevenuFortuneValidator extends EditForAvecMotifsValidator {

	private HibernateTemplate hibernateTemplate;

	protected EditForRevenuFortuneValidator(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final EditForRevenuFortuneView view = (EditForRevenuFortuneView) target;

		final ForFiscalRevenuFortune ffrf = hibernateTemplate.get(ForFiscalRevenuFortune.class, view.getId());
		if (ffrf == null) {
			throw new ObjectNotFoundException("Impossible de trouver le for fiscal avec l'id=" + view.getId());
		}

		// validation du motif de debut
		if (view.getDateDebut() != null) {
			if (view.getMotifDebut() == null) {
				// [SIFISC-8563] On ne valide la non-nullité du motif d'ouverture que s'il n'était pas null avant
				if (ffrf.getMotifOuverture() != null) {
					errors.rejectValue("motifDebut", "error.motif.ouverture.vide");
				}
			}
			else if (ffrf.getMotifOuverture() != view.getMotifDebut()) { // [SIFISC-7909] on ne valide le motif d'ouverture que s'il a changé
				final NatureTiers natureTiers = ffrf.getTiers().getNatureTiers();
				final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, ffrf.getMotifRattachement());
				ForValidatorHelper.validateMotifDebut(typeFor, view.getMotifDebut(), errors);
			}
		}

		// validation du motif de fin
		if (view.getMotifFin() != null) {
			final NatureTiers natureTiers = ffrf.getTiers().getNatureTiers();
			final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, ffrf.getMotifRattachement());
			ForValidatorHelper.validateMotifFin(typeFor, view.getMotifFin(), errors);
		}
	}
}
