package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Adapter pour l'obtention de permis.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionPermis extends ObtentionPermisCOuNationaliteSuisse {

	/** LOGGER log4J */
	protected static final Logger LOGGER = Logger.getLogger(ObtentionPermis.class);

	/**
	 * le numero OFS de la commune vaudoise de l'adresse principale
	 */
	private Integer numeroOfsCommunePrincipale;

	private TypePermis typePermis;

	protected ObtentionPermis(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		init(evenement.getDateEvenement(), context);
	}

	protected ObtentionPermis(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		init(evenement.getDateEvenement(), context);
	}
	
	private void init(RegDate date, EvenementCivilContext context) throws EvenementCivilException {
		try {
			// on récupère le permis (= à la date d'événement)
			final Permis permis = getIndividuOrThrowException().getPermis().getPermisActif(date);
			if (permis == null) {
				throw new EvenementCivilException("Aucun permis trouvé dans le registre civil");
			}
			this.typePermis = permis.getTypePermis();
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new EvenementCivilException(e.getMessage(), e);
		}

		try {
			// on récupère la commune de l'adresse principale en gérant les fractions
			// à utiliser pour déterminer le numeroOFS si besoin d'ouvrir un nouveau for vaudois
			final Adresse adressePrincipale = getAdressePrincipale();
			if (context.getServiceInfra().estDansLeCanton(adressePrincipale)) {
				final Commune communePrincipale = context.getServiceInfra().getCommuneByAdresse(adressePrincipale, date);
				if (communePrincipale == null) {
					throw new EvenementCivilException("Incohérence dans l'adresse principale");
				}
				this.numeroOfsCommunePrincipale = communePrincipale.getNoOFS();
			}
			else {
				this.numeroOfsCommunePrincipale = 0;
			}
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException("Echec de résolution de l'adresse principale.", e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ObtentionPermis(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Integer numeroOfsCommunePrincipale,
	                          TypePermis typePermis, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, date, numeroOfsCommuneAnnonce, null, null, null, context);
		this.numeroOfsCommunePrincipale = numeroOfsCommunePrincipale;
		this.typePermis = typePermis;
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// quelque soit le permis, si l'individu correspond à un non-habitant (= ancien habitant)
		// il faut mettre à jour le permis chez nous
		final PersonnePhysique pp = getPrincipalPP();
		if (pp != null && !pp.isHabitantVD()) {
			pp.setCategorieEtranger(CategorieEtranger.valueOf(getTypePermis()));
			Audit.info(getNumeroEvenement(), String.format("L'individu %d (tiers non-habitant %s) a maintenant le permis '%s'",
					getNoIndividu(), FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), getTypePermis().name()));
		}

		/* Seul le permis C a une influence */
		if (getTypePermis() != TypePermis.ETABLISSEMENT) {
			Audit.info(getNumeroEvenement(), "Permis non C : ignoré fiscalement");
			return HandleStatus.TRAITE;
		}
		else {
			return super.handle(warnings);
		}
	}

	public TypePermis getTypePermis() {
		return typePermis;
	}

	public Integer getNumeroOfsCommunePrincipale() {
		return numeroOfsCommunePrincipale;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}
}
