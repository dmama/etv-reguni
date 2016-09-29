package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesREE;
import ch.vd.unireg.interfaces.organisation.data.InscriptionREE;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class MockDonneesREE implements DonneesREE {

	private final NavigableMap<RegDate, InscriptionREE> inscriptionREE;

	public MockDonneesREE() {
		this(new TreeMap<RegDate, InscriptionREE>());
	}

	public MockDonneesREE(NavigableMap<RegDate, InscriptionREE> inscriptionREE) {
		this.inscriptionREE = inscriptionREE;
	}

	@NotNull
	@Override
	public List<DateRanged<InscriptionREE>> getInscriptionREE() {
		return MockOrganisationHelper.getHisto(inscriptionREE);
	}

	@Override
	public InscriptionREE getInscriptionREE(RegDate date) {
		return OrganisationHelper.valueForDate(getInscriptionREE(), date);
	}

	public void changeInscriptionREE(RegDate date, InscriptionREE nouvelleInscription) {
		MockOrganisationHelper.changeRangedData(inscriptionREE, date, nouvelleInscription);
	}

	public void addInscriptionREE(RegDate dateDebut, @Nullable RegDate dateFin, InscriptionREE nouvelleInscription) {
		MockOrganisationHelper.addRangedData(inscriptionREE, dateDebut, dateFin, nouvelleInscription);
	}
}
