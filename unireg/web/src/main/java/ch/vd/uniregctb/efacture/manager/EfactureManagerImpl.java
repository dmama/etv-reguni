package ch.vd.uniregctb.efacture.manager;

import javax.jms.JMSException;
import java.util.Date;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;

public class EfactureManagerImpl implements EfactureManager {

	private TiersService tiersService;
	private EditiqueCompositionService editiqueCompositionService;


	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, Long idDemande, RegDate dateDemande) throws EditiqueException {
		final Tiers tiers = tiersService.getTiers(ctbId);
		final Date dateTraitement = DateHelper.getCurrentDate();
		try {
			editiqueCompositionService.imprimeDocumentEfacture(tiers, typeDocument,dateTraitement,dateDemande);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}
}
