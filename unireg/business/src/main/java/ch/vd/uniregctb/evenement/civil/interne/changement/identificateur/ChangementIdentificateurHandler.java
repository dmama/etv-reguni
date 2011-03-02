package ch.vd.uniregctb.evenement.civil.interne.changement.identificateur;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.changement.AbstractChangementHandler;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ChangementIdentificateurHandler extends AbstractChangementHandler {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		final long noIndividu = evenement.getNoIndividu();
		Audit.info(evenement.getNumeroEvenement(), String.format("Traitement du changement d'identificateur de l'individu : %d", noIndividu));

		final PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(noIndividu, true);
		if (pp != null && !pp.isHabitantVD()) {
			// pour les non-habitants, il faut recharger les données, non?
			// quelles sont les données à recharger ? NAVS13 pour sûr !
			final Individu individu = getService().getIndividu(pp);
			final String navs13Registre = individu.getNouveauNoAVS();
			if (navs13Registre != null && navs13Registre.trim().length() > 0) {
				pp.setNumeroAssureSocial(individu.getNouveauNoAVS().trim());
			}
		}

		return super.handle(evenement, warnings);
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		/* l'existance de l'individu est vérifié dans validateCommon */
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		final Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CHGT_CORREC_IDENTIFICATION);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new ChangementIdentificateurAdapter(event, context, this);
	}
}
