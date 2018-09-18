<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Debiteur Prestation Imposable -->

<fieldset>
	<legend><span><fmt:message key="label.debiteur.is" /></span></legend>
	
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<unireg:raccourciAjouter link="../tiers/debiteur/create.do?numeroCtbAss=${command.tiers.numero}" tooltip="Ajouter d&eacute;biteur" display="label.bouton.ajouter"/>
			</td>
		</tr>
	</table>

	<c:if test="${not empty command.debiteurs}">
		<display:table 	name="command.debiteurs" id="debiteur" pagesize="10" requestURI="list.do" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			<display:column sortable ="true" titleKey="label.numero.debiteur" href="visu.do" paramId="id" paramProperty="numero" sortProperty="numero" >
				<a href="../tiers/visu.do?id=${debiteur.numero}"><unireg:numCTB numero="${debiteur.numero}"/></a>
			</display:column>
			<display:column sortable ="true" titleKey="label.nom.raison" >
				<unireg:multiline lines="${debiteur.nomCourrier}"/>
				<c:if test="${debiteur.complementNom != null }">
					<br />${debiteur.complementNom}
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.categorie.is" >
				<fmt:message key="option.categorie.impot.source.${debiteur.categorieImpotSource}" />
			</display:column>
			<display:column sortable ="true" property="personneContact" titleKey="label.contact"  />
			<display:column style="action">
				<c:if test="${!debiteur.annule}">
					<unireg:linkTo name="" action="/rapport/cancel.do" method="POST"  params="{id:${debiteur.id}, sens:'${debiteur.sensRapportEntreTiers}'}" link_class="delete"
					               title="Annulation du rapport-entre-tiers" confirm="Voulez-vous vraiment annuler ce rapport-entre-tiers ?"/>
				</c:if>
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
		</display:table>
	</c:if>

</fieldset>
<!-- Fin Debiteur Prestation Imposable -->