<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Caracteristiques generales -->
<fieldset>
	<legend><span><fmt:message key="caracteristiques.mouvements.dossier" /></span></legend>
	
		<table border="0">
			<tr>
				<td>
					<unireg:raccourciAjouter onClick="javascript:SubmitFormEditMvt();" tooltip="label.bouton.ajouter" display="label.bouton.ajouter"/>
					<form:errors cssClass="error"/>
				</td>
			</tr>
		</table>
	
	<jsp:include page="../tiers/common/mouvement/mouvements.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
	
</fieldset>	
<!-- Fin Caracteristiques generales -->