package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour l'obtention de nationalité.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public abstract class ObtentionNationalite extends ObtentionPermisCOuNationaliteSuisse {

	/**
	 * LOGGER log4J
	 */
	protected static Logger LOGGER = Logger.getLogger(ObtentionNationalite.class);

	/**
	 * le numero OFS étendu de la commune de l'adresse principale
	 */
	private Integer numeroOfsEtenduCommunePrincipale;

	protected ObtentionNationalite(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		init(evenement.getDateEvenement(), context);
	}

	protected ObtentionNationalite(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		init(evenement.getDateEvenement(), context);
	}
	
	private void init(RegDate date, EvenementCivilContext context) throws EvenementCivilException {
		try {
			// on récupère la commune de l'adresse principale en gérant les fractions
			//à utiliser pour déterminer le numeroOFS si besoin d'ouvrir un nouveau for
			final Commune communePrincipale = context.getServiceInfra().getCommuneByAdresse(getAdressePrincipale(), date);
			this.numeroOfsEtenduCommunePrincipale = communePrincipale == null ? 0 : communePrincipale.getNoOFSEtendu();
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ObtentionNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Integer numeroOfsEtenduCommunePrincipale, boolean nationaliteSuisse,
	                               EvenementCivilContext context) {
		super(individu, conjoint, nationaliteSuisse ? TypeEvenementCivil.NATIONALITE_SUISSE : TypeEvenementCivil.NATIONALITE_NON_SUISSE, date, numeroOfsCommuneAnnonce, null, null, null, context);
		this.numeroOfsEtenduCommunePrincipale = numeroOfsEtenduCommunePrincipale;
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}

	/**
	 * Appelé par la méthode {@link #handle} une fois le tiers personne physique identifié
	 * @param pp le tiers personne physique impliqué dans l'obtention de nationalité
	 * @param warnings la liste des warnings à compléter au besoin
	 * @return <code>true</code> s'il faut continuer à appeler la méthode handle parente, <code>false</code> sinon
	 * @throws EvenementCivilException en cas de problème
	 */
	protected abstract boolean doHandle(PersonnePhysique pp, EvenementCivilWarningCollector warnings) throws EvenementCivilException;

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// quelle que soit la nationalité, si l'individu correspond à un non-habitant (= ancien habitant)
		// il faut mettre à jour la nationalité chez nous
		final PersonnePhysique pp = getPrincipalPP();
		if (doHandle(pp, warnings)) {
			return super.handle(warnings);
		}
		else {
			return HandleStatus.TRAITE;
		}
	}
}
