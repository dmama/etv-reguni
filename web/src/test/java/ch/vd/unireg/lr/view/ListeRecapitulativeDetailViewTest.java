package ch.vd.unireg.lr.view;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.MockMessageSource;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.view.DelaiDocumentFiscalView;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockInfrastructureConnector;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

import static org.junit.Assert.assertEquals;

public class ListeRecapitulativeDetailViewTest {

	@Test
	public void testBuildDelais() {
		DeclarationImpotSource lr = new DeclarationImpotSource();
		Set<DelaiDeclaration> delaiDeclarationSet = new HashSet<DelaiDeclaration>();
		// Delai déclaration 01
		DelaiDeclaration delaiDeclaration01 = new DelaiDeclaration();
		delaiDeclaration01.setId(111L);
		delaiDeclaration01.setDateDemande(null);
		delaiDeclaration01.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		Declaration declaration = new DeclarationImpotSource();
		declaration.setId(11L);
		delaiDeclaration01.setDeclaration(declaration);
		// Delai déclaration 02
		DelaiDeclaration delaiDeclaration02 = new DelaiDeclaration();
		delaiDeclaration02.setId(222L);
		delaiDeclaration02.setDateDemande(RegDate.get(2016, 06, 15));
		delaiDeclaration02.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		declaration = new DeclarationImpotSource();
		declaration.setId(22L);
		delaiDeclaration02.setDeclaration(declaration);
		// Delai déclaration 03
		DelaiDeclaration delaiDeclaration03 = new DelaiDeclaration();
		delaiDeclaration03.setId(333L);
		delaiDeclaration03.setDateDemande(RegDate.get(2015, 03, 12));
		delaiDeclaration03.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		declaration = new DeclarationImpotSource();
		declaration.setId(33L);
		delaiDeclaration03.setDeclaration(declaration);
		// Delai déclaration 04
		DelaiDeclaration delaiDeclaration04 = new DelaiDeclaration();
		delaiDeclaration04.setId(444L);
		delaiDeclaration04.setDateDemande(RegDate.get(2017, 01, 23));
		delaiDeclaration04.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		declaration = new DeclarationImpotSource();
		declaration.setId(44L);
		delaiDeclaration04.setDeclaration(declaration);

		delaiDeclarationSet.add(delaiDeclaration01);
		delaiDeclarationSet.add(delaiDeclaration02);
		delaiDeclarationSet.add(delaiDeclaration03);
		delaiDeclarationSet.add(delaiDeclaration04);
		lr.setDelaisDeclaration(delaiDeclarationSet);

		ProxyServiceInfrastructureService infraService = new ProxyServiceInfrastructureService();
		infraService.setUp(new DefaultMockInfrastructureConnector() {
			@Override
			public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
				return "toto";
			}
		});

		MessageSource messageSourceMock = new MockMessageSource();
		final MessageHelper messageHelperMock = Mockito.mock(MessageHelper.class);
		Mockito.when(messageHelperMock.getMessage(Mockito.any(String.class))).thenReturn("option.etat.delai." + EtatDelaiDocumentFiscal.ACCORDE.name());

		List<DelaiDocumentFiscalView> returnedList = ListeRecapitulativeDetailView.buildDelais(lr, infraService, messageHelperMock);

		// Asserts
		assertEquals(4, returnedList.size());
		assertEquals(444L, returnedList.get(0).getId().longValue()); // 20170123
		assertEquals(222L, returnedList.get(1).getId().longValue()); // 20160615
		assertEquals(333L, returnedList.get(2).getId().longValue()); // 20150312
		assertEquals(111L, returnedList.get(3).getId().longValue()); // null
	}

}