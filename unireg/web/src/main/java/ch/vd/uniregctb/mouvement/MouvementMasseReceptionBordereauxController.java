package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import ch.vd.uniregctb.mouvement.view.BordereauEnvoiListView;
import ch.vd.uniregctb.mouvement.view.BordereauEnvoiView;

/**
 * Controlleur pour la réception des bordereaux d'envoi de mouvements de dossiers
 */
public class MouvementMasseReceptionBordereauxController extends AbstractMouvementMasseController {

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		final BordereauEnvoiListView view = new BordereauEnvoiListView();
		final Integer noCollAdmReceptrice = getNoCollAdmFiltree();
		final List<BordereauEnvoiView> bordereaux = getMouvementManager().findBordereauxAReceptionner(noCollAdmReceptrice);
		view.setBordereaux(bordereaux);
		view.setMontreDestinataire(noCollAdmReceptrice == null);
		return view;
	}

}
