package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneAvecAdresses;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Modélise un événement de déménagement Secondaire.
 *
 * @author B. NGOM </a>
 */
public class DemenagementSecondaire extends EvenementCivilInterneAvecAdresses {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(DemenagementSecondaire.class);





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
