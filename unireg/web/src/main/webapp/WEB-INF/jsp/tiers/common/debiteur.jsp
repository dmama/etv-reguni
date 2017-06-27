<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<c:if test="${not empty command.debiteurs}">
<display:table 	name="command.debiteurs" id="debiteur" 
				pagesize="10" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator"
				requestURI="${url}" class="display">
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
		<c:if test="${page == 'visu' }">
			<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${debiteur.id}"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${!debiteur.annule}">
				<unireg:raccourciAnnuler onClick="javascript:Rapport.annulerRapport(${debiteur.id});" tooltip="Annuler"/>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
</display:table>
</c:if>