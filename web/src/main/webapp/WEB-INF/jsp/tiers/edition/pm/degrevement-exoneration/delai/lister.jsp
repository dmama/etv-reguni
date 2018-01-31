<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="editDemandeDegrevementCommand" type="ch.vd.uniregctb.registrefoncier.allegement.EditDemandeDegrevementView"--%>

<fieldset>
	<legend><span><fmt:message key="label.delais" /></span></legend>
	<table border="0">
		<tr>
			<td>
				<unireg:linkTo name="Ajouter" title="Ajouter" action="/degrevement-exoneration/delai/ajouter.do" params="{id:${editDemandeDegrevementCommand.idDemandeDegrevement}}" link_class="add"/>
			</td>
		</tr>
	</table>
	<jsp:include page="delais.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</fieldset>
