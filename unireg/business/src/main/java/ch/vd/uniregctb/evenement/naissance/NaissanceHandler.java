package ch.vd.uniregctb.evenement.naissance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règle métiers permettant de traiter les événements de naissance.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public class NaissanceHandler extends EvenementCivilHandlerBase {

	private static final Logger LOGGER = Logger.getLogger(NaissanceHandler.class);

	/**
	 * Vérifie que les informations sont complètes.
	 * Pour un événement de déménagement, de séparation  ou de divorce (sans séparation préalable),
	 * si l’individu est marié avec un autre individu présent dans le registre des individus,
	 * l’application recherche l’événement correspondant de l’autre membre du couple.
	 * Si ce 2e événement n’est pas trouvé, le 1er est mis en attente (en cas de déménagement d’un seul membre du couple,
	 * il y a suspicion de séparation fiscale).
	 * Les éventuels événements manquants de déménagement des enfants sont ignorés.
	 */
	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		/* Rien de spécial pour la naissance : pas de regroupement */
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.EvenementCivilHandler#validate(java.lang.Object,
	 *      java.util.List)
	 */
	@Override
	protected void validateSpecific(EvenementCivil evenementCivil, List<EvenementCivilErreur> errors, List<EvenementCivilErreur> warnings) {
		if ( FiscalDateHelper.isMajeurAt(evenementCivil.getIndividu(), RegDate.get()) ) {
			errors.add( new EvenementCivilErreur("L'individu ne devrait pas être majeur à la naissance"));
		}
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 * @throws EvenementCivilHandlerException
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenementCivil, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		LOGGER.debug("Traitement de la naissance de l'individu : " + evenementCivil.getIndividu().getNoTechnique() );

		try {
			/*
			 * Transtypage de l'événement en naissance
			 */
			final Naissance naissance = (Naissance) evenementCivil;
			final Individu individu = naissance.getIndividu();
			final RegDate dateEvenement = evenementCivil.getDate();

			/*
			 * Vérifie qu'aucun tiers n'existe encore rattaché à cet individu
			 */
			verifieNonExistenceTiers(individu.getNoTechnique());

			/*
			 *  Création d'un nouveau Tiers et sauvegarde de celui-ci
			 */
			PersonnePhysique bebe = new PersonnePhysique(true);
			bebe.setNumeroIndividu(individu.getNoTechnique());
			bebe = (PersonnePhysique)getTiersDAO().save(bebe);
			Audit.info(naissance.getNumeroEvenement(), "Création d'un nouveau tiers habitant (numéro: "+bebe.getNumero()+")");

			this.getEvenementFiscalService().publierEvenementFiscalChangementSituation(bebe, dateEvenement, bebe.getId());
			return new Pair<PersonnePhysique, PersonnePhysique>(bebe, null);
		}
		catch (Exception e) {
			LOGGER.debug("Erreur lors de la sauvegarde du nouveau tiers", e);
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.NAISSANCE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new NaissanceAdapter();
	}

}
