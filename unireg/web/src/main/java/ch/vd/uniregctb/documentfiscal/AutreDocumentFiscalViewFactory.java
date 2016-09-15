package ch.vd.uniregctb.documentfiscal;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.MessageSource;

/**
 * Factory des vues d'autres documents fiscaux
 */
public abstract class AutreDocumentFiscalViewFactory {

	interface ViewFactory<T extends AutreDocumentFiscal> {
		AutreDocumentFiscalView buildView(T document, MessageSource messageSource);
	}

	private static final Map<Class<? extends AutreDocumentFiscal>, ViewFactory<?>> FACTORIES = buildFactoryMap();

	private static <T extends AutreDocumentFiscal> void addToMap(Map<Class<? extends AutreDocumentFiscal>, ViewFactory<?>> map,
	                                                             Class<T> clazz,
	                                                             ViewFactory<? super T> viewFactory) {
		map.put(clazz, viewFactory);
	}

	private static Map<Class<? extends AutreDocumentFiscal>, ViewFactory<?>> buildFactoryMap() {
		final Map<Class<? extends AutreDocumentFiscal>, ViewFactory<?>> map = new HashMap<>();
		addToMap(map, LettreBienvenue.class, new ViewFactory<LettreBienvenue>() {
			@Override
			public AutreDocumentFiscalAvecSuiviView buildView(LettreBienvenue document, MessageSource messageSource) {
				return new AutreDocumentFiscalAvecSuiviView(document,
				                                            messageSource,
				                                            "label.autre.document.fiscal.lettre.bienvenue",
				                                            "label.autre.document.fiscal.lettre.bienvenue.type." + document.getType());
			}
		});
		addToMap(map, AutorisationRadiationRC.class, new ViewFactory<AutorisationRadiationRC>() {
			@Override
			public AutreDocumentFiscalView buildView(AutorisationRadiationRC document, MessageSource messageSource) {
				return new AutreDocumentFiscalView(document,
				                                   messageSource,
				                                   "label.autre.document.fiscal.autorisation.radiation.rc",
				                                   null);
			}
		});
		addToMap(map, DemandeBilanFinal.class, new ViewFactory<DemandeBilanFinal>() {
			@Override
			public AutreDocumentFiscalView buildView(DemandeBilanFinal document, MessageSource messageSource) {
				return new AutreDocumentFiscalView(document,
				                                   messageSource,
				                                   "label.autre.document.fiscal.demande.bilan.final",
				                                   null);
			}
		});
		addToMap(map, LettreLiquidation.class, new ViewFactory<LettreLiquidation>() {
			@Override
			public AutreDocumentFiscalView buildView(LettreLiquidation document, MessageSource messageSource) {
				return new AutreDocumentFiscalView(document,
				                                   messageSource,
				                                   "label.autre.document.fiscal.lettre.liquidation",
				                                   null);
			}
		});
		return map;
	}

	public static <T extends AutreDocumentFiscal> AutreDocumentFiscalView buildView(T doc, MessageSource messageSource) {
		final Class<? extends AutreDocumentFiscal> clazz = doc.getClass();
		//noinspection unchecked
		final ViewFactory<? super T> factory = (ViewFactory<? super T>) FACTORIES.get(clazz);
		return factory.buildView(doc, messageSource);
	}
}
