package ch.vd.unireg.coordfin;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.CoordonneesFinancieres;

public class CoordonneesFinancieresServiceImpl implements CoordonneesFinancieresService {

	private IbanValidator ibanValidator;

	@Override
	public void updateCoordonneesFinancieres(@NotNull Contribuable ctb, @Nullable String titulaire, @Nullable String iban, @NotNull RegDate dateValeur, @NotNull UpdateNotifier notifier) {

		final CoordonneesFinancieres currentCoords = ctb.getCoordonneesFinancieresCourantes();

		// on récupère les valeurs courantes
		final String currentTitulaire = (currentCoords == null ? null : currentCoords.getTitulaire());
		final String currentIban = Optional.ofNullable(currentCoords)
				.map(CoordonneesFinancieres::getCompteBancaire)
				.map(CompteBancaire::getIban)
				.map(IbanHelper::normalize)
				.orElse(null);
		final String currentBicSwift = Optional.ofNullable(currentCoords)
				.map(CoordonneesFinancieres::getCompteBancaire)
				.map(CompteBancaire::getBicSwift)
				.orElse(null);

		// on détermine le nouveau titulaire
		final String newTitulaire;
		if (StringUtils.isNotBlank(titulaire)) {
			newTitulaire = titulaire;
		}
		else {
			// pas de titulaire renseigné, on prend l'existant
			newTitulaire = currentTitulaire;
		}

		// on détermine le nouvel iban
		final String newIban;
		final boolean newIbanValid;
		if (StringUtils.isNotBlank(iban)) {     // [SIFISC-8936] Un IBAN vide ne doit jamais être pris en compte
			newIbanValid = ibanValidator.isValidIban(iban);
			if (newIbanValid) {
				// le nouvel iban est valide : on le prend
				newIban = IbanHelper.normalize(iban);
			}
			else if (ibanValidator.isValidIban(currentIban)) {
				// le nouvel iban n'est pas valide mais l'ancien oui : on garde l'ancien + on notifie l'appelant qui décidera quoi faire.
				//noinspection ConstantConditions
				notifier.onInvalidNewIban(currentIban, iban);
				newIban = currentIban;
			}
			else if ("CH".equalsIgnoreCase(iban)) {
				// le nouvel iban est considéré comme nul : on garde l'ancien.
				newIban = currentIban;
			}
			else {
				// le nouvel iban n'est pas valide et l'ancien non plus : on prend le nouveau quand même...
				newIban = IbanHelper.normalize(iban);
			}
		}
		else {
			// pas d'iban renseigné, on prend l'existant
			newIban = currentIban;
			newIbanValid = ibanValidator.isValidIban(currentIban);
		}

		// est-ce qu'il y a quelque chose à faire ?
		final boolean titulaireChange = !Objects.equals(currentTitulaire, newTitulaire);
		final boolean ibanChange = !Objects.equals(currentIban, newIban);

		if ((titulaireChange && newIbanValid) || ibanChange) {  // on ne met-à-jour le titulaire que si le nouvel IBAN est valide
			// quelque chose à changé, on met-à-jour les coordonnées financières
			final String newBicSwift = (ibanChange ? null : currentBicSwift);    // on ne reprends pas le Bic Switft si l'IBAN a changé, parce qu'il n'est certainement plus valide

			// [SIFISC-20035] on historise les coordonnées financières
			if (currentCoords != null) {
				currentCoords.setDateFin(dateValeur.getOneDayBefore());
			}
			final CoordonneesFinancieres newCoords = new CoordonneesFinancieres();
			newCoords.setDateDebut(dateValeur);
			newCoords.setTitulaire(LengthConstants.streamlineField(newTitulaire, LengthConstants.TIERS_PERSONNE, true));
			newCoords.setCompteBancaire(new CompteBancaire(LengthConstants.streamlineField(newIban, LengthConstants.TIERS_NUMCOMPTE, false), newBicSwift));
			ctb.addCoordonneesFinancieres(newCoords);
		}
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}
}
