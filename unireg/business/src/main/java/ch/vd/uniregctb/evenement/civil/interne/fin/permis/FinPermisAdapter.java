package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Adapter pour la fin obtention d'un permis.
 *
 * @author Pavel BLANCO
 *
 */
public class FinPermisAdapter extends EvenementCivilInterneBase implements FinPermis {

	private final static Logger LOGGER = Logger.getLogger(FinPermisAdapter.class);

	/** Le permis arrivant échéance. */
	private Permis permis;

	private FinPermisHandler handler;

	protected FinPermisAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, FinPermisHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
		this.handler = handler;

		try {
			// on récupère le permis à partir de sa date de fin (= à la date d'événement)
			final int anneeCourante = evenement.getDateEvenement().year();
			final Collection<Permis> listePermis = context.getServiceCivil().getPermis(super.getNoIndividu(), anneeCourante);
			if (listePermis == null) {
				throw new EvenementCivilInterneException("Le permis n'a pas été trouvé dans le registre civil");
			}
			for (Permis permis : listePermis) {
				if (RegDateHelper.equals(permis.getDateFinValidite(), evenement.getDateEvenement())) {
					this.permis = permis;
					break;
				}
			}

			// si le permis n'a pas été trouvé, on lance une exception
			if ( this.permis == null ) {
				throw new EvenementCivilInterneException("Le permis n'a pas été trouvé dans le registre civil");
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new EvenementCivilInterneException(e.getMessage(), e);
		}
	}

	public TypePermis getTypePermis() {
		return permis.getTypePermis();
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
