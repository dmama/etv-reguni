package ch.vd.unireg.evenement.civil.interne.demenagement;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterneAvecAdresses;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Modélise un événement de déménagement Secondaire.
 *
 * @author B. NGOM </a>
 */
public class DemenagementSecondaire extends EvenementCivilInterneAvecAdresses {

	/** LOGGER log4J */
	protected static Logger LOGGER = LoggerFactory.getLogger(DemenagementSecondaire.class);





	protected DemenagementSecondaire(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected DemenagementSecondaire(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce,
	                                 Adresse nouvelleAdressePrincipale, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, nouvelleAdressePrincipale, null, null, context);
	}

	public DemenagementSecondaire(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);

	}



	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		return HandleStatus.TRAITE;
	}

	@Override
	protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

	}
}
