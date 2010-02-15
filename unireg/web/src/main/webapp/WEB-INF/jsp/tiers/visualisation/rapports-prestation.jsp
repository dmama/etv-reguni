<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut CTB associé + Rapports prestation -->
<c:if test="${command.allowedOnglet.RPT}">
	<table border="0">
		<tr>
			<td>
				<unireg:raccourciModifier link="../rapports-prestation/edit.do?id=${command.tiers.numero}" tooltip="Modifier les rapports de prestation" display="label.bouton.modifier"/>
			</td>
		</tr>
	</table>
</c:if>

<fieldset>
	<legend><span><fmt:message key="label.contribuable.associe" /></span></legend>
	
	<input name="rt_histo"
	type="checkbox" onClick="toggleRowsIsActif('contribuableAssocie','isCtbAssoHisto', 2);" id="isCtbAssoHisto" />
	<fmt:message key="label.historique" />
		
	<jsp:include page="../common/contribuable-associe.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.rapports.prestation" /></span></legend>
	
	<input name="rt_histo"
	type="checkbox" onClick="toggleRowsIsHisto('rapportPrestation','isRTHisto', 1);" id="isRTHisto" />
	<label for="isRTHisto"><fmt:message key="label.historique" /></label>
		
	<jsp:include page="../common/rapports-prestation.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>

<!-- Fin CTB associé + Rapports prestation -->
