package ch.vd.uniregctb.webservices.tiers3.data.strategy;

import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionCode;
import ch.vd.uniregctb.webservices.tiers3.CategoriePersonnePhysique;
import ch.vd.uniregctb.webservices.tiers3.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers3.Sexe;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;

public class PersonnePhysiqueStrategy extends ContribuableStrategy<PersonnePhysique> {

	private static final Logger LOGGER = Logger.getLogger(PersonnePhysiqueStrategy.class);

	@Override
	public PersonnePhysique newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		final PersonnePhysique pp = new PersonnePhysique();
		initBase(pp, right, context);
		initParts(pp, right, parts, context);
		return pp;
	}

	@Override
	public PersonnePhysique clone(PersonnePhysique right, @Nullable Set<TiersPart> parts) {
		final PersonnePhysique pp = new PersonnePhysique();
		copyBase(pp, right);
		copyParts(pp, right, parts, CopyMode.EXCLUSIF);
		return pp;
	}

	@Override
	protected void initBase(PersonnePhysique to, ch.vd.uniregctb.tiers.Tiers from, Context context) throws WebServiceException {
		super.initBase(to, from, context);

		final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) from;
		if (!personne.isHabitantVD()) {
			to.setNom(personne.getNom());
			to.setPrenom(personne.getPrenom());
			to.setDateNaissance(DataHelper.coreToWeb(personne.getDateNaissance()));
			to.setSexe(EnumHelper.coreToWeb(personne.getSexe()));
			to.setDateDeces(DataHelper.coreToWeb(personne.getDateDeces()));
			for (ch.vd.uniregctb.tiers.IdentificationPersonne ident : personne.getIdentificationsPersonnes()) {
				if (ident.getCategorieIdentifiant() == ch.vd.uniregctb.type.CategorieIdentifiant.CH_AHV_AVS) {
					to.setAncienNumeroAssureSocial(ident.getIdentifiant());
				}
			}
			to.setNouveauNumeroAssureSocial(personne.getNumeroAssureSocial());
			to.setDateArrivee(DataHelper.coreToWeb(personne.getDateDebutActivite()));
			to.setCategorie(EnumHelper.coreToWeb(personne.getCategorieEtranger()));
		}
		else {
			final ch.vd.uniregctb.interfaces.model.Individu individu = context.serviceCivilService.getIndividu(personne.getNumeroIndividu(), null, AttributeIndividu.PERMIS);

			if (individu == null) {
				final String message = String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d", personne
						.getNumeroIndividu(), personne.getNumero());
				LOGGER.error(message);
				throw ExceptionHelper.newBusinessException(message, BusinessExceptionCode.INDIVIDU_INCONNU);
			}

			final ch.vd.uniregctb.interfaces.model.HistoriqueIndividu data = individu.getDernierHistoriqueIndividu();
			to.setNom(data.getNom());
			to.setPrenom(data.getPrenom());
			to.setDateNaissance(DataHelper.coreToWeb(individu.getDateNaissance()));
			to.setSexe((individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ));
			to.setDateDeces(DataHelper.coreToWeb(personne.getDateDeces() == null ? individu.getDateDeces() : personne.getDateDeces()));
			to.setNouveauNumeroAssureSocial(individu.getNouveauNoAVS());
			to.setAncienNumeroAssureSocial(data.getNoAVS());
			to.setDateArrivee(DataHelper.coreToWeb(data.getDateDebutValidite()));

			final ch.vd.uniregctb.interfaces.model.Permis permis = individu.getPermisActif(null);
			if (permis == null) {
				to.setCategorie(CategoriePersonnePhysique.SUISSE);
			}
			else {
				to.setCategorie(EnumHelper.coreToWeb(permis.getTypePermis()));
			}
		}

	}

	@Override
	protected void copyBase(PersonnePhysique to, PersonnePhysique from) {
		super.copyBase(to, from);
		to.setNom(from.getNom());
		to.setPrenom(from.getPrenom());
		to.setDateNaissance(from.getDateNaissance());
		to.setSexe(from.getSexe());
		to.setDateDeces(from.getDateDeces());
		to.setAncienNumeroAssureSocial(from.getAncienNumeroAssureSocial());
		to.setNouveauNumeroAssureSocial(from.getNouveauNumeroAssureSocial());
		to.setDateArrivee(from.getDateArrivee());
		to.setCategorie(from.getCategorie());
	}
}
