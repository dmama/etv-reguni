<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Caracteristiques generales -->
<fieldset>
<FONT COLOR="#FF0000">${erreurCommunicationEditique}</FONT>
	<legend><span><fmt:message key="caracteristiques.di" /></span></legend>
		<c:if test="${command.allowedEmission}">
		<table border="0">
			<tr>
				<td>
					<form:form method="post" name="formAddDI" action="edit.do?ajouterDI=true&numero=${command.contribuable.numero}" >
						<unireg:raccourciAjouter onClick="document.formAddDI.submit();" tooltip="Ajouter" display="label.bouton.ajouter"/>
					<form:errors cssClass="error"/>
					</form:form>
				</td>
			</tr>
		</table>
		</c:if>
	
	<jsp:include page="../../tiers/common/di/dis.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
	
</fieldset>	
<!-- Fin Caracteristiques generales -->
