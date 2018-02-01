package ch.vd.unireg.fors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.validator.MotifsForHelper;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.fors.ForDebiteurPrestationImposableValidator;

public class AddForDebiteurValidator extends AddForAvecMotifsValidator {

	private final HibernateTemplate hibernateTemplate;

	public AddForDebiteurValidator(ServiceInfrastructureService infraService, HibernateTemplate hibernateTemplate) {
		super(infraService);
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddForDebiteurView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForDebiteurView view = (AddForDebiteurView) target;

		if (view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.incorrect");
		}

		final DebiteurPrestationImposable dpi = hibernateTemplate.get(DebiteurPrestationImposable.class, view.getTiersId());
		if (dpi == null) {
			throw new TiersNotFoundException(view.getTiersId());
		}

		if (view.getDateDebut() != null) {
			// on établi la liste des périodes des fors fiscaux existants
			final List<DateRange> fors = new ArrayList<>();
			for (ForFiscal f : dpi.getForsFiscauxNonAnnules(true)) {
				if (f.getDateFin() == null && (f.getDateDebut() == null || f.getDateDebut().isBefore(view.getDateDebut()))) {
					// simule la fermeture du for courant à la veille du nouveau for
					fors.add(new DateRangeHelper.Range(f.getDateDebut(), view.getDateDebut().getOneDayBefore()));
				}
				else {
					fors.add(new DateRangeHelper.Range(f));
				}
			}
			if (DateRangeHelper.intersect(new DateRangeHelper.Range(view.getDateDebut(), view.getDateFin()), fors)) {
				// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
				if (!errors.hasFieldErrors("dateDebut")) {
					errors.rejectValue("dateDebut", "error.date.chevauchement");
				}
				if (!errors.hasFieldErrors("dateFin")) {
					errors.rejectValue("dateFin", "error.date.chevauchement");
				}
			}
		}

		// validation du motif de début
		//La nullité est déja detectée en Amont SIFISC-25746
		if (view.getMotifDebut() != null) {
			final NatureTiers natureTiers = dpi.getNatureTiers();
			final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, null);
			ForValidatorHelper.validateMotifDebut(typeFor, view.getMotifDebut(), errors);
		}


		// validation du motif de fin
		if (view.getMotifFin() != null) {
			final NatureTiers natureTiers = dpi.getNatureTiers();
			final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, null);
			ForValidatorHelper.validateMotifFin(typeFor, view.getMotifFin(), errors);
		}

		// validation de la date de fin
		if (view.getDateFin() != null) {

			// [SIFISC-12888] la date ne doit pas dépasser le 31.12 de la PF en cours
			if (view.getDateFin().isAfter(RegDate.get(RegDate.get().year(), 12, 31))) {
				errors.rejectValue("dateFin", "error.date.fermeture.posterieure.pf.courante");
			}

			// [SIFISC-12888] ... et doit correspondre à une date qui va bien
			if (view.getDateDebut() != null) {

				// [SIFISC-16852] dans le cas où nous sommes en train de créer le premier for, il peut être nécessaire
				// de déplacer la date de début de la première périodicité (ce sera fait de toute façon au moment de la sauvegarde)
				// avant de valider la pertinence de la date de fin demandée
				// (le résultat de DPI.getPeriodicitesNonAnnules() est une nouvelle collection non-adossée à la persistence, on peut donc jouer avec comme ça)
				final List<Periodicite> periodicites = dpi.getPeriodicitesNonAnnulees(true);
				if (!periodicites.isEmpty() && dpi.getDernierForDebiteur() == null) {
					final Periodicite first = periodicites.get(0);
					if (first.getDateDebut().isAfter(view.getDateDebut())) {
						final Periodicite displacedFirst = first.duplicate();
						displacedFirst.setDateDebut(view.getDateDebut());
						periodicites.set(0, displacedFirst);
					}
				}

				final Set<RegDate> autorisees = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, view.getDateDebut(), view.getDateFin(), periodicites);
				if (!autorisees.contains(view.getDateFin())) {
					errors.rejectValue("dateFin", "error.date.fermeture.invalide");
				}
			}
		}
	}

	@Override
	protected final boolean isEmptyMotifDebutAllowed(AddForAvecMotifsView view) {
		return false;
	}
}
