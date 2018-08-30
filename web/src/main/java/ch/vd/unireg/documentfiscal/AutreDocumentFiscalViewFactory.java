package ch.vd.unireg.documentfiscal;

import java.util.HashMap;
import java.util.Map;

import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.registrefoncier.allegement.DemandeDegrevementICIView;

/**
 * Factory des vues d'autres documents fiscaux
 */
public abstract class AutreDocumentFiscalViewFactory {

	interface ViewFactory<T extends AutreDocumentFiscal> {
		AutreDocumentFiscalView buildView(T document, ServiceInfrastructureService infraService, MessageHelper messageHelper);
	}

	private static final Map<Class<? extends AutreDocumentFiscal>, ViewFactory<?>> FACTORIES = buildFactoryMap();

	private static <T extends AutreDocumentFiscal> void addToMap(Map<Class<? extends AutreDocumentFiscal>, ViewFactory<?>> map,
	                                                             Class<T> clazz,
	                                                             ViewFactory<? super T> viewFactory) {
		map.put(clazz, viewFactory);
	}

	private static Map<Class<? extends AutreDocumentFiscal>, ViewFactory<?>> buildFactoryMap() {
		final Map<Class<? extends AutreDocumentFiscal>, ViewFactory<?>> map = new HashMap<>();
		addToMap(map, LettreBienvenue.class,                    (document, infraService, messageHelper) -> new AutreDocumentFiscalAvecSuiviView(document, infraService, messageHelper, "label.autre.document.fiscal.lettre.bienvenue", "label.autre.document.fiscal.lettre.bienvenue.type." + document.getType()));
		addToMap(map, AutorisationRadiationRC.class,            (document, infraService, messageHelper) -> new AutreDocumentFiscalView(document, infraService, messageHelper, "label.autre.document.fiscal.autorisation.radiation.rc", null));
		addToMap(map, DemandeBilanFinal.class,                  (document, infraService, messageHelper) -> new AutreDocumentFiscalView(document, infraService, messageHelper, "label.autre.document.fiscal.demande.bilan.final", null));
		addToMap(map, LettreTypeInformationLiquidation.class,   (document, infraService, messageHelper) -> new AutreDocumentFiscalView(document, infraService, messageHelper, "label.autre.document.fiscal.lettre.liquidation", null));
		addToMap(map, DemandeDegrevementICI.class,              (document, infraService, messageHelper) -> new DemandeDegrevementICIView(document, infraService, messageHelper, "label.autre.document.fiscal.formulaire.demande.degrevement.ici", null));
		return map;
	}

	public static <T extends AutreDocumentFiscal> AutreDocumentFiscalView buildView(T doc, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		final Class<? extends AutreDocumentFiscal> clazz = doc.getClass();
		//noinspection unchecked
		final ViewFactory<? super T> factory = (ViewFactory<? super T>) FACTORIES.get(clazz);
		return factory.buildView(doc, infraService, messageHelper);
	}
}
