package ch.vd.unireg.interfaces.upi.rcpers;

import java.io.Serializable;
import java.util.List;

import ch.ech.ech0084.v1.FullName;
import ch.ech.ech0084.v1.PersonInformation;
import ch.ech.ech0084.v1.ValuesStoredUnderAhvvn;
import ch.ech.ech0085.v1.GetInfoPersonResponse;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.upi.ServiceUpiException;
import ch.vd.unireg.interfaces.upi.ServiceUpiRaw;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.Sexe;

public class ServiceUpiRCPers implements ServiceUpiRaw {

	private RcPersClient client;
	private ServiceInfrastructureRaw infraService;

	public void setClient(RcPersClient client) {
		this.client = client;
	}

	public void setInfraService(ServiceInfrastructureRaw infraService) {
		this.infraService = infraService;
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
				return buildInfo(info.getAccepted(), infraService);
			}
			else {
				throw new ServiceUpiException("La réponse ne contient ni le champ 'accepted' ni le champ 'refused'...");
			}
		}
		catch (Exception e) {
			throw new ServiceUpiException(e);
		}
	}

	private static UpiPersonInfo buildInfo(GetInfoPersonResponse.Accepted data, ServiceInfrastructureRaw infraService) {
		final String avs = EchHelper.avs13FromEch(data.getLatestAhvvn());
		final ValuesStoredUnderAhvvn stored = data.getValuesStoredUnderAhvvn();
		final PersonInformation person = stored.getPerson();
		final String prenoms = person.getFirstNames();
		final String nom = person.getOfficialName();
		final Sexe sexe = EchHelper.sexeFromEch44(person.getSex());
		final RegDate dateNaissance = EchHelper.partialDateFromEch44(person.getDateOfBirth());
		final RegDate dateDeces = XmlUtils.xmlcal2regdate(stored.getDateOfDeath());
		final Nationalite nationalite = NationaliteUpi.get(person.getNationality(), infraService);
		final NomPrenom nomPrenomMere = extractNomPrenom(stored.getMothersName());
		final NomPrenom nomPrenomPere = extractNomPrenom(stored.getFathersName());
		return new UpiPersonInfo(avs, prenoms, nom, sexe, dateNaissance, dateDeces, nationalite, nomPrenomMere, nomPrenomPere);
	}

	@Nullable
	private static NomPrenom extractNomPrenom(@Nullable FullName fullname) {
		if (fullname == null) {
			return null;
		}
		return new NomPrenom(fullname.getLastName(), fullname.getFirstNames());
	}

	/**
	 * Implémentation locale de la nationalité issue du service UPI
	 */
	private static final class NationaliteUpi implements Nationalite, Serializable {

		private final Pays pays;

		public static NationaliteUpi get(PersonInformation.Nationality nationality, ServiceInfrastructureRaw infraService) {
			if (nationality == null) {
				return null;
			}
			return new NationaliteUpi(nationality, infraService);
		}

		private NationaliteUpi(PersonInformation.Nationality nat, ServiceInfrastructureRaw infraService) {
			this.pays = initPays(nat, infraService);
		}

		private static Pays initPays(PersonInformation.Nationality nationality, ServiceInfrastructureRaw infraService) {
			final Pays p;
			final String status = nationality.getNationalityStatus();
			switch (status) {
			case "0":
				// inconnu
				p = infraService.getPays(ServiceInfrastructureRaw.noPaysInconnu, null);
				break;
			case "1":
				// apatride
				p = infraService.getPays(ServiceInfrastructureRaw.noPaysApatride, null);
				break;
			case "2":
				// ok
				final Integer noOfsPays = nationality.getCountryId();
				if (noOfsPays == null) {
					throw new IllegalArgumentException("Pays sans numéro OFS.");
				}
				final List<Pays> paysCandidats = infraService.getPaysHisto(noOfsPays);
				if (paysCandidats == null || paysCandidats.isEmpty()) {
					// TODO faudrait-il mettre le pays inconnu ici ?
					p = null;
				}
				else {
					// on prend toujours la dernière version disponible du pays en question...
					p = paysCandidats.get(paysCandidats.size() - 1);
				}
				break;
			default:
				throw new IllegalArgumentException("Code nationality status inconnu = [" + status + ']');
			}
			return p;
		}


		@Override
		public Pays getPays() {
			return pays;
		}

		@Override
		public RegDate getDateDebut() {
			return null;            // pas mieux...
		}

		@Override
		public RegDate getDateFin() {
			return null;            // pas mieux...
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return true;            // du coup...
		}
	}
}
