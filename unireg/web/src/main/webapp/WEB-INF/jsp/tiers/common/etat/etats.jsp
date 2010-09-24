<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:if test="${not empty command.etats}">
<fieldset>
	<legend><span><fmt:message key="label.etats" /></span></legend>
	
	<display:table 	name="command.etats" id="etat" pagesize="10" class="display">
		<display:column titleKey="label.date.obtention" >
			<c:if test="${etat.annule}"><strike></c:if>
				<unireg:regdate regdate="${etat.dateObtention}" />
			<c:if test="${etat.annule}"></strike></c:if>
		</display:column>
 		<display:column titleKey="label.etat">
	 		<c:if test="${etat.annule}"><strike></c:if>
				<fmt:message key="option.etat.avancement.${etat.etat}" />
			<c:if test="${etat.annule}"></strike></c:if>
			<c:if test="${!etat.annule && etat.etat == 'SOMMEE'}">
				&nbsp;
				<a href="../declaration/copie-sommation.do?idEtat=${etat.id}" class="pdf" id="copie-sommation-${etat.id}" onClick="Page_ImprimerCopieSommation(${etat.id})"><img src="${pageContext.request.contextPath}/images/pdf_icon.png" style="align: top;"/></a>
				<img src="${pageContext.request.contextPath}/images/pdf_grayed_icon.png" id="disabled-copie-sommation-${etat.id}" style="display: none; align: top;"/>
			</c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${!etat.annule}">
				<img src="../images/consult_off.gif" title="${etat.logModifUser}-<fmt:formatDate value="${etat.logModifDate}" pattern="dd.MM.yyyy HH:mm:ss"/>" />
			</c:if>
			<c:if test="${etat.annule}">
				<img src="../images/consult_off.gif" title="${etat.annulationUser}-<fmt:formatDate value="${etat.annulationDate}" pattern="dd.MM.yyyy HH:mm:ss"/>" />
			</c:if>
		</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

<script type="text/javascript">

	 	function Page_ActivationImpressionCopieSommation(idEtat, actif) {
	 		var eltActif = document.getElementById("copie-sommation-" + idEtat);
	 		var eltInactif = document.getElementById("disabled-copie-sommation-" + idEtat);
	 		if (actif) {
	 			eltActif.style.display = "";
	 			eltInactif.style.display = "none";
	 		}
	 		else {
	 			eltInactif.style.display = "";
	 			eltActif.style.display = "none";
			}
	 	}

		function Page_ImprimerCopieSommation(idEtat) {
			Page_ActivationImpressionCopieSommation(idEtat, false);
			setTimeout("Page_ActivationImpressionDelai(" + idEtat + ", true);", 2000);
		}

</script>
	
</fieldset>
</c:if>