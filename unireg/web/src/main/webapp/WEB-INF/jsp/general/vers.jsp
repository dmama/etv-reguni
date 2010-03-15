<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}.numeroAsString" scope="request"/>
<spring:bind path="${bind}" >
<div style="float: right;margin-right: 10px">
	<span><fmt:message key="label.ouvrir.vers" /> : </span>
	<c:if test="${urlRetour == null}">
		<select name="AppSelect" onchange="javascript:AppSelect_OnChange(this);">
			<option value="">---</option>
			<option value="<c:out value='${command.urlTaoPP}'/>"><fmt:message key="label.TAOPP" /></option>
				<option value="<c:out value='${command.urlTaoBA}'/>"><fmt:message key="label.TAOBA" /></option>
				<unireg:BridageIS>
					<option value="<c:out value='${command.urlTaoIS}'/>"><fmt:message key="label.TAOIS" /></option>
				</unireg:BridageIS>
				<option value="<c:out value='${command.urlSipf}'/>"><fmt:message key="label.SIPF" /></option>
			<option value="<c:out value='launchcat.do?numero=' /><c:out value='${tiersGeneral.numero}' />" ><fmt:message key="label.CAT" /></option>
		</select>
	</c:if>
	<c:if test="${urlRetour != null}">
		<a href="${urlRetour}<c:out value='${status.value}' />" class="detail" title="<fmt:message key="label.retour.application.appelante" />">&nbsp;</a>
	</c:if>
	
	<script type="text/javascript">
		function AppSelect_OnChange(select) {
			var value = select.options[select.selectedIndex].value;
			if ( value && value !== '') {
				//window.open(value, '_blank') ;
				window.location.href = value;
			}
		}
	</script>
</div>
</spring:bind>
