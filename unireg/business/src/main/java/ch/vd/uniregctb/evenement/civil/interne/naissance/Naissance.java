package ch.vd.uniregctb.evenement.civil.interne.naissance;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class Naissance extends EvenementCivilInterne {

	private static final Logger LOGGER = Logger.getLogger(Naissance.class);

	private final boolean okSiContribuableExiste;

	protected Naissance(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		this.okSiContribuableExiste = false;
	}

	protected Naissance(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		if (getPrincipalPP() != null) {
			evenement.setCommentaireTraitement("Le contribuable correspondant au nouveau-né existe déjà, seuls les événements à destination des applications fiscales ont été envoyés.");
		}
		this.okSiContribuableExiste = true;
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Naissance(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, List<Individu> parents, EvenementCivilContext context, boolean regpp) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
		this.okSiContribuableExiste = !regpp;
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.EvenementCivilInterne#isContribuablePresentBefore()
	 */
	@Override
	public boolean isContribuablePresentBefore() {
		/* Le contribuable n'existe pas à l'arrivée d'un événement naissance */
		return false;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PARENTS);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if ( FiscalDateHelper.isMajeurAt(getIndividu(), RegDate.get()) ) {
			erreurs.addErreur("L'individu ne devrait pas être majeur à la naissance");
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		LOGGER.debug("Traitement de la naissance de l'individu : " + getNoIndividu() );


		/*
		 * Transtypage de l'événement en naissance
		 */
		final Individu individu = getIndividu();
		final RegDate dateEvenement = getDate();

		/*
		 * Vérifie qu'aucun tiers n'existe encore rattaché à cet individu
		 */
		final PersonnePhysique bebe;
		if (getPrincipalPP() != null) {
			if (!okSiContribuableExiste) {
				throw new EvenementCivilException("Le tiers existe déjà avec cet individu " + individu.getNoTechnique() + " alors que c'est une naissance");
			}
			bebe = getPrincipalPP();
		}
		else {

			/*
			 *  Création d'un nouveau Tiers et sauvegarde de celui-ci
			 */
			final PersonnePhysique nouveauNe = new PersonnePhysique(true);
			nouveauNe.setNumeroIndividu(individu.getNoTechnique());
			bebe = (PersonnePhysique) context.getTiersDAO().save(nouveauNe);
			Audit.info(getNumeroEvenement(), "Création d'un nouveau tiers habitant (numéro: " + bebe.getNumero() + ')');
		}

		context.getEvenementFiscalService().publierEvenementFiscalChangementSituation(bebe, dateEvenement, bebe.getId());

		// [UNIREG-3244] on envoie les faire-parts de naissance
		final Contribuable parent = context.getTiersService().getAutoriteParentaleDe(bebe, dateEvenement);
		if (parent != null) {
			context.getEvenementFiscalService().publierEvenementFiscalNaissance(bebe, parent, dateEvenement);
		}

		return HandleStatus.TRAITE;
	}
}
