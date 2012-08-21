package ch.vd.uniregctb.evenement.civil.interne.depart;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Gère l'interprétation d'un événement de départ issu de RCPers : il peut être principal ou secondaire selon l'adresse qui se ferme.
 * <p/>
 * <b>SIFISC-5642</b> : cette stratégie accepte un décalage de <i>n</i> jours (<i>n</i> positif ou nul) entre la date de fin de l'adresse et la date de l'événement
 * (uniquement dans le cas où la date de l'événement est postérieure à la date de fin de l'adresse),
 */
public class DepartEchTranslationStrategy implements EvenementCivilEchTranslationStrategy {

	/**
	 * Décalage autorisé maximal (0..<i>n</i>) en jours entre la date de fin de l'adresse et la date de l'événement, au delà duquel
	 * on ne sait pas retrouver quelle est l'adresse qui s'arrête. En particulier un décalage de 0 signifie que les dates doivent être
	 * identiques.
	 */
	private final int decalageAutorise;

	public DepartEchTranslationStrategy(int decalageAutorise) {
		Assert.isTrue(decalageAutorise >= 0);
		this.decalageAutorise = decalageAutorise;
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		// Départ principal ou secondaire ?
		// si on trouve une adresse principale qui se termine le jour de l'événement (ou presque, voir le décalage autorisé), alors c'est un départ principal
		// sinon, si on trouve une adresse secondaire qui se termine ce jour-là, c'est un départ secondaire
		// sinon... on n'en sait rien... et boom !
		final Adresse adresseTerminee = getAdresseTerminee(event, context);
		if (adresseTerminee.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
			return new DepartPrincipalEch(event, context, options, adresseTerminee);
		}
		else {
			return new DepartSecondaireEch(event, context, options, adresseTerminee);
		}
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		return false;
	}

	@NotNull
	private Adresse getAdresseTerminee(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		try {
			final AdressesCivilesHisto adressesCivilesHisto = context.getAdresseService().getAdressesCivilesHisto(event.getNumeroIndividu(), false);
			final Adresse adresseTerminee = DepartDecaleHelper.getAdresseResidenceTerminee(event.getDateEvenement(), decalageAutorise, adressesCivilesHisto);
			if (adresseTerminee != null) {
				return adresseTerminee;
			}
			else if (decalageAutorise == 0) {
				throw new EvenementCivilException("Aucune adresse principale ou secondaire ne se termine à la date de l'événement.");
			}
			else {
				throw new EvenementCivilException(String.format("Aucune adresse principale ou secondaire ne se termine %d jour%s ou moins avant la date de l'événement.", decalageAutorise, decalageAutorise > 1 ? "s" : ""));
			}
		}
		catch (AdresseException e) {
			throw new EvenementCivilException("Erreur lors de la récupération des adresses civiles", e);
		}
	}
}
