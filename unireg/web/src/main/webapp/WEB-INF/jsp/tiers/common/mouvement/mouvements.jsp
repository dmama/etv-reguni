<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit-contribuable.do" />
</c:if>
<c:if test="${not empty command.mouvements}">
<display:table
		name="command.mouvements" id="mouvement" pagesize="10" 
		requestURI="${url}"
		class="display">

	<display:column sortable ="true" titleKey="label.type.mouvement">
		<c:if test="${mouvement.annule}"><strike></c:if>
			<fmt:message key="option.type.mouvement.${mouvement.typeMouvement}"/>
		<c:if test="${mouvement.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.mouvement" sortProperty="dateMouvement">
		<c:if test="${mouvement.annule}"><strike></c:if>
			<fmt:formatDate value="${mouvement.dateMouvement}" pattern="dd.MM.yyyy"/>
		<c:if test="${mouvement.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.etat.mouvement" sortProperty="etatMouvement">
		<c:if test="${mouvement.annule}"><strike></c:if>
			<fmt:message key="option.etat.mouvement.${mouvement.etatMouvement}"/>
		<c:if test="${mouvement.annule}"></strike></c:if>
	</display:column>

	<display:column sortable ="true" titleKey="label.collectivite.administrative" >
		<c:if test="${mouvement.annule}"><strike></c:if>
            ${mouvement.collectiviteAdministrative}
		<c:if test="${mouvement.annule}"></strike></c:if>
	</display:column>

	<display:column sortable ="true" titleKey="label.destination.utilisateur">
		<c:if test="${mouvement.annule}"><strike></c:if>
            ${mouvement.destinationUtilisateur}
		<c:if test="${mouvement.annule}"></strike></c:if>
	</display:column>

	<display:column style="action">
		<c:if test="${page == 'visu' }">
			<a href="#" class="detail" title="Détail d'un mouvement" onclick="return open_details_mouvement(${mouvement.id});">&nbsp;</a>
			<unireg:consulterLog entityNature="MouvementDossier" entityId="${mouvement.id}"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${mouvement.annulable}">
				<unireg:raccourciAnnuler onClick="javascript:Page_AnnulerMvt(${mouvement.id});" tooltip="Annulation de mouvement"/>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	
</display:table>

	<script type="text/javascript">
		function Page_AnnulerMvt(idMvt) {
				if(confirm('Voulez-vous vraiment annuler ce mouvement de dossier ?')) {
					var form = F$("theForm");
					form.doPostBack("annulerMvt", idMvt);
			 	}
	 	} 	
	</script>

</c:if>
