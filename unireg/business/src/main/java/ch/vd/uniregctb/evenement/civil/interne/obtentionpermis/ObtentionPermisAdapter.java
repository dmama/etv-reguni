package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.Collection;
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
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Adapter pour l'obtention de permis.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionPermisAdapter extends EvenementCivilInterneAvecAdressesBase implements ObtentionPermis {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(ObtentionPermisAdapter.class);

	/**
	 * Le permis obtenu.
	 */
	private Permis permis;

	/**
	 * le numero OFS étendu de la commune vaudoise de l'adresse principale
	 */
	private Integer numeroOfsEtenduCommunePrincipale;

	private ObtentionPermisHandler handler;

	protected ObtentionPermisAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, ObtentionPermisHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
		this.handler = handler;

		try {
			// on récupère le permis (= à la date d'événement)
			final int anneeCourante = evenement.getDateEvenement().year();
			final Collection<Permis> listePermis = context.getServiceCivil().getPermis(super.getNoIndividu(), anneeCourante);
			if (listePermis == null) {
				throw new EvenementCivilInterneException("Aucun permis trouvé dans le registre civil");
			}
			for (Permis permis : listePermis) {
				if (evenement.getDateEvenement().equals(permis.getDateDebutValidite())) {
					this.permis = permis;
					break;
				}
			}

			// si le permis n'a pas été trouvé, on lance une exception
			if ( this.permis == null ) {
				throw new EvenementCivilInterneException("Aucun permis trouvé dans le registre civil");
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new EvenementCivilInterneException(e.getMessage(), e);
		}

		try {
			// on récupère la commune de l'adresse principale en gérant les fractions
			// à utiliser pour déterminer le numeroOFS si besoin d'ouvrir un nouveau for vaudois
			final Adresse adressePrincipale = getAdressePrincipale();
			if (context.getServiceInfra().estDansLeCanton(adressePrincipale)) {
				final CommuneSimple communePrincipale = context.getServiceInfra().getCommuneByAdresse(adressePrincipale);
				if (communePrincipale == null) {
					throw new EvenementCivilInterneException("Incohérence dans l'adresse principale");
				}
				this.numeroOfsEtenduCommunePrincipale = communePrincipale.getNoOFSEtendu();
			}
			else {
				this.numeroOfsEtenduCommunePrincipale = 0;
			}
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilInterneException("Echec de résolution de l'adresse principale.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.obtentionpermis.ObtentionPermis#getTypePermis()
	 */
	public TypePermis getTypePermis() {
		return permis.getTypePermis();
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
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.validateSpecific(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
