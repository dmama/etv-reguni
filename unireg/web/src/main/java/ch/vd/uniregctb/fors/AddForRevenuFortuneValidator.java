package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class AddForRevenuFortuneValidator extends AddForAvecMotifsValidator {

	private final HibernateTemplate hibernateTemplate;

	protected AddForRevenuFortuneValidator(ServiceInfrastructureService infraService, HibernateTemplate hibernateTemplate) {
		super(infraService);
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForRevenuFortuneView view = (AddForRevenuFortuneView) target;

		final Tiers tiers = hibernateTemplate.get(Tiers.class, view.getTiersId());
		if (tiers == null) {
			throw new TiersNotFoundException(view.getTiersId());
		}

		// validation du motif de début
		if (view.getMotifDebut() != null) {
			final NatureTiers natureTiers = tiers.getNatureTiers();
			final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, view.getMotifRattachement());
			ForValidatorHelper.validateMotifDebut(typeFor, view.getMotifDebut(), errors);
		}

		// validation du motif de fin
		if (view.getMotifFin() != null) {
			final NatureTiers natureTiers = tiers.getNatureTiers();
			final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, view.getMotifRattachement());
			ForValidatorHelper.validateMotifFin(typeFor, view.getMotifFin(), errors);
		}

		// le mode de rattachement
		if (view.getMotifRattachement() == null) {
			errors.rejectValue("motifRattachement", "error.motif.rattachement.vide");
		}
	}

	@Override
	protected final boolean isEmptyMotifDebutAllowed(AddForAvecMotifsView view) {
		final boolean nonVaudois = view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
		boolean premierFor = false;
		if (nonVaudois) {
			// [SIFISC-4065] On n'autorise les motifs d'ouverture vide que s'il n'y a pas de for principal non annulé existant avant le for que l'on veut créer maintenant
			final Contribuable ctb = hibernateTemplate.get(Contribuable.class, view.getTiersId());
			premierFor = ctb != null && ctb.getDernierForFiscalPrincipalAvant(view.getDateDebut()) == null;
		}
		return nonVaudois && premierFor;
	}
}
