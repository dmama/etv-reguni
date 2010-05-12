package ch.vd.uniregctb.evenement.changement.nom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.changement.AbstractChangementHandler;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ChangementNomHandler extends AbstractChangementHandler {

	private static final Logger LOGGER = Logger.getLogger(ChangementNomHandler.class);

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		/* Rien de spécial pour le changement de nom */

	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		/* pas de validation spécifique pour le changement de nom */
		/* l'existance de l'individu est vérifié dans validateCommon */
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		final long noIndividu = evenement.getIndividu().getNoTechnique();

		LOGGER.debug("Changement de nom de l'individu : " + noIndividu);
		Audit.info(evenement.getNumeroEvenement(), "Changement de nom de l'individu : " + noIndividu);

		final PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(noIndividu, true);
		if (pp != null && !pp.isHabitant()) {
			// pour les non-habitants, il faut recharger les données, non?
			// quelles sont les données à recharger ? nom/prénom !
			final Individu individu = getService().getIndividu(pp);

			// nom / prénom
			final HistoriqueIndividu historiqueIndividu = individu.getDernierHistoriqueIndividu();
			final String nom = historiqueIndividu.getNom();
			final String prenom = historiqueIndividu.getPrenom();
			pp.setNom(nom != null ? nom.trim() : "");
			pp.setPrenom(prenom != null ? prenom.trim() : "");
		}

		return super.handle(evenement, warnings);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new ChangementNomAdapter();
	}
}
