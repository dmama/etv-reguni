<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<%--@elvariable id="command" type="ch.vd.unireg.tiers.view.TiersView"--%>
<c:if test="${not empty command.contribuablesAssocies}">
	<display:table 	name="command.contribuablesAssocies" id="contribuableAssocie" pagesize="10" 
					requestURI="${url}" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
		<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
			<a href="../tiers/visu.do?id=${contribuableAssocie.numero}"><unireg:numCTB numero="${contribuableAssocie.numero}"/></a>
		</display:column>

		<display:column sortable ="true" titleKey="label.nom.raison">
			<unireg:multiline lines="${contribuableAssocie.nomCourrier}"/>
		</display:column>

		<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${contribuableAssocie.dateDebut}"/>
		</display:column>

		<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${contribuableAssocie.dateFin}"/>
		</display:column>

		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${contribuableAssocie.id}"/>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!contribuableAssocie.annule}">
					<unireg:linkTo name="" action="/rapport/cancel.do" method="POST" params="{id:${contribuableAssocie.id}, sens:'${contribuableAssocie.sensRapportEntreTiers}'}" link_class="delete"
					               title="Annulation du rapport-entre-tiers" confirm="Voulez-vous vraiment annuler ce rapport-entre-tiers ?"/>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>
</c:if>