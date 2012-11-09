package ch.vd.uniregctb.evenement.rapport.travail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.xml.event.rt.request.v1.MiseAJourRapportTravailRequest;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;



public class MiseAJourRapportTravailRequestHandler implements RapportTravailRequestHandler {

	private TiersService tiersService;


	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	public MiseAJourRapportTravailResponse handle(MiseAJourRapportTravailRequest request) throws ServiceException {

		//le débiteur doit exister
		final long numeroDpi = request.getIdentifiantRapportTravail().getNumeroDebiteur();
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(numeroDpi);
		if(dpi == null){
			final String msg = String.format("le débiteur %s n'existe pas",FormatNumeroHelper.numeroCTBToDisplay(numeroDpi));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		RegDate dateDebutPeriode = DataHelper.xmlToCore(request.getIdentifiantRapportTravail().getDateDebutPeriodeDeclaration());
		RegDate dateFinPeriode = DataHelper.xmlToCore(request.getIdentifiantRapportTravail().getDateFinPeriodeDeclaration());

		// Le débiteur doit être actif (For en vigueur) sur toute la période de déclaration
		validateDPI(dpi,dateDebutPeriode,dateFinPeriode);
		return null;
	}



	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/rt/rapport-travail-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Arrays.asList(new ClassPathResource("event/rt/rapport-travail-response-1.xsd"));
	}

	private void validateDPI(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws ServiceException {

		final List<ForFiscal> fors = dpi.getForsFiscauxNonAnnules(true);
		final List<DateRange> forRanges = new ArrayList<DateRange>(fors);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode, dateFinPeriode);

		final List<DateRange> periodeNonCouverte = DateRangeHelper.subtract(periodeDeclaration, forRanges);
		if (!periodeNonCouverte.isEmpty()) {
			final String msg = String.format("le débiteur (%s) ne possède pas de fors couvrant la totalité de la période de déclaration qui va du %s au %s.",
					FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()), RegDateHelper.dateToDisplayString(dateDebutPeriode), RegDateHelper.dateToDisplayString(dateFinPeriode));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.VALIDATION.name(), null));
		}
	}
}
