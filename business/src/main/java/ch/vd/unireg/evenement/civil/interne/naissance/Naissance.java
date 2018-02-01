package ch.vd.unireg.evenement.civil.interne.naissance;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FiscalDateHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.PersonnePhysique;

public class Naissance extends EvenementCivilInterne {

	private static final Logger LOGGER = LoggerFactory.getLogger(Naissance.class);

	private final boolean okSiContribuableExiste;

	protected Naissance(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		this.okSiContribuableExiste = false;
		refreshParentsCache(evenement.getNumeroIndividuPrincipal(), context); // [SIFISC-5521]
	}

	protected Naissance(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		if (getPrincipalPP() != null) {
			evenement.setCommentaireTraitement("Le contribuable correspondant au nouveau-né existe déjà, seuls les événements à destination des applications fiscales ont été envoyés.");
		}
		this.okSiContribuableExiste = true;
		refreshParentsCache(evenement.getNumeroIndividu(), context); // [SIFISC-5521]
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Naissance(Individu individu, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context, boolean regpp) {
		super(individu, null, date, numeroOfsCommuneAnnonce, context);
		this.okSiContribuableExiste = !regpp;
		refreshParentsCache(individu.getNoTechnique(), context); // [SIFISC-5521]
	}

	/**
	 * Force le refraîchissement du cache du service civil pour les parents connus de l'individu spécifié.
	 *
	 * @param noInd   le numéro d'un individu
	 * @param context le context d'exécution de l'événement civil.
	 */
	private void refreshParentsCache(long noInd, EvenementCivilContext context) {
		final Set<Long> parents = context.getServiceCivil().getNumerosIndividusParents(noInd);
		if (parents != null) {
			for (Long noParent : parents) {
				context.getDataEventService().onIndividuChange(noParent);
			}
		}
	}

	/* (non-Javadoc)
	 * @see ch.vd.unireg.evenement.EvenementCivilInterne#isContribuablePresentBefore()
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
		final RegDate dateEvenement = getDate();

		/*
		 * Vérifie qu'aucun tiers n'existe encore rattaché à cet individu
		 */
		final PersonnePhysique bebe;
		if (getPrincipalPP() != null) {
			if (!okSiContribuableExiste) {
				throw new EvenementCivilException(String.format("Le tiers existe déjà avec l'individu %d alors que c'est une naissance", getNoIndividu()));
			}
			bebe = getPrincipalPP();
			Audit.info(getNumeroEvenement(), String.format("Un tiers personne physique existe déjà avec cet individu: %d", bebe.getNumero()));
		}
		else {

			/*
			 *  Création d'un nouveau Tiers et sauvegarde de celui-ci
			 */
			final PersonnePhysique nouveauNe = new PersonnePhysique(true);
			nouveauNe.setNumeroIndividu(getNoIndividu());
			bebe = (PersonnePhysique) context.getTiersDAO().save(nouveauNe);
			Audit.info(getNumeroEvenement(), String.format("Création d'un nouveau tiers habitant (numéro: %d)", bebe.getNumero()));
		}

		context.getTiersService().refreshParentesSurPersonnePhysique(bebe, false);

		context.getEvenementFiscalService().publierEvenementFiscalChangementSituationFamille(dateEvenement, bebe);

		// [UNIREG-3244] on envoie les faire-parts de naissance
		final ContribuableImpositionPersonnesPhysiques parent = context.getTiersService().getAutoriteParentaleDe(bebe, dateEvenement);
		if (parent != null) {
			context.getEvenementFiscalService().publierEvenementFiscalNaissance(bebe, parent, dateEvenement);
		}
		else {
			Audit.warn(getNumeroEvenement(), "Contribuable de l'autorité parentale non trouvé, pas d'envoi de faire-part...");
		}

		return HandleStatus.TRAITE;
	}
}
