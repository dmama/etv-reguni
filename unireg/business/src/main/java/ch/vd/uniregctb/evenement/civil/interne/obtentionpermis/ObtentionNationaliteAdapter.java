package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneAvecAdressesBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Adapter pour l'obtention de nationalité.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionNationaliteAdapter extends EvenementCivilInterneAvecAdressesBase implements ObtentionNationalite {

	/**
	 * LOGGER log4J
	 */
	protected static Logger LOGGER = Logger.getLogger(ObtentionNationaliteAdapter.class);

	/**
	 * le numero OFS étendu de la commune de l'adresse principale
	 */
	private Integer numeroOfsEtenduCommunePrincipale;

	private ObtentionNationaliteHandler handler;

	protected ObtentionNationaliteAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, ObtentionNationaliteHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
		this.handler = handler;

		try {
			// on récupère la commune de l'adresse principale en gérant les fractions
			//à utiliser pour déterminer le numeroOFS si besoin d'ouvrir un nouveau for
			final CommuneSimple communePrincipale = context.getServiceInfra().getCommuneByAdresse(getAdressePrincipale());
			this.numeroOfsEtenduCommunePrincipale = communePrincipale == null ? 0 : communePrincipale.getNoOFSEtendu();
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilInterneException(e);
		}
	}
	
	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
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
