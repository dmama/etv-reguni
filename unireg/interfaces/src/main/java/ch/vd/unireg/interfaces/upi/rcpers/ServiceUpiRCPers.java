package ch.vd.unireg.interfaces.upi.rcpers;

import ch.ech.ech0084.v1.PersonInformation;
import ch.ech.ech0084.v1.ValuesStoredUnderAhvvn;
import ch.ech.ech0085.v1.GetInfoPersonResponse;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.upi.ServiceUpiException;
import ch.vd.unireg.interfaces.upi.ServiceUpiRaw;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;

public class ServiceUpiRCPers implements ServiceUpiRaw {

	private RcPersClient client;

	public void setClient(RcPersClient client) {
		this.client = client;
	}

	@Override
	public UpiPersonInfo getPersonInfo(String noAvs13) throws ServiceUpiException {
		final String error = AvsHelper.validateNouveauNumAVS(noAvs13);
		if (error != null) {
			throw new ServiceUpiException("Numéro AVS13 invalide : '" + noAvs13 + "' (" + error + ')');
		}

		final long avs13 = EchHelper.avs13ToEch(noAvs13.replaceAll("\\.", ""));
		try {
			final GetInfoPersonResponse info = client.getInfoPersonUpi(avs13);
			if (info.getRefused() != null) {
				if (info.getRefused().getReason() == 10) {       // service indisponible
					throw new ServiceUpiException("Service indisponible.");
				}
				return null;
			}
			else if (info.getAccepted() != null) {
				final GetInfoPersonResponse.Accepted accepted = info.getAccepted();
				final long lastNavs13 = accepted.getLatestAhvvn();
				final UpiPersonInfo upi = new UpiPersonInfo(EchHelper.avs13FromEch(lastNavs13));
				final ValuesStoredUnderAhvvn data = accepted.getValuesStoredUnderAhvvn();
				if (data != null) {
					final PersonInformation person = data.getPerson();
					upi.setDateNaissance(EchHelper.partialDateFromEch44(person.getDateOfBirth()));
					upi.setNom(person.getOfficialName());
					upi.setPrenoms(person.getFirstNames());
					upi.setSexe(EchHelper.sexeFromEch44(person.getSex()));
				}
				return upi;
			}
			else {
				throw new ServiceUpiException("La réponse ne contient ni le champ 'accepted' ni le champ 'refused'...");
			}
		}
		catch (Exception e) {
			throw new ServiceUpiException(e);
		}
	}
}
