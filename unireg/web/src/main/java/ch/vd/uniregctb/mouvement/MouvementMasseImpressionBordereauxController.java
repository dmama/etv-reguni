package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

import ch.vd.uniregctb.mouvement.view.BordereauListElementView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseImpressionBordereauxView;

/**
 * Controlleur pour l'écran d'impression des bordereaux des mouvements de dossiers en masse
 */
public class MouvementMasseImpressionBordereauxController extends AbstractMouvementMasseController {

	@Override
	protected MouvementMasseImpressionBordereauxView formBackingObject(HttpServletRequest request) throws Exception {
		final Integer noCollAdmFiltrage = getNoCollAdmFiltree();
		final MouvementMasseImpressionBordereauxView view = new MouvementMasseImpressionBordereauxView();
		final List<BordereauListElementView> bordereaux = getMouvementManager().getProtoBordereaux(noCollAdmFiltrage);
		view.setBordereaux(bordereaux);
		view.setMontreExpediteur(noCollAdmFiltrage == null);
		return view;
	}
	
}
