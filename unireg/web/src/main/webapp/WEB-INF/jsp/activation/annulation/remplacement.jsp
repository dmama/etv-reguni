<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:url var="tiersURL" value="/activation/remplacement/list.do?type=${command.tiers.type}&TB_iframe=true&modal=true&height=350&width=800" />

<script type="text/javascript" language="Javascript">
	function nouveauTiersHandle() {
		Element.hide('tiersNouveauPanel');
		Element.show( 'tiersExistantPanel');
		tb_show("", "<c:out value="${tiersURL}" />");
	}
	function rechercheTiers() {
		tb_show("", "<c:out value="${tiersURL}" />");
	}
</script>

<div id="tiersNouveauPanel">
	<table>
		<tr>
			<td width="10%">&nbsp;</td>
			<td>
				<a id="addLink" href="javascript:;" class="add" onclick="nouveauTiersHandle();return false">&nbsp;<fmt:message key="label.recherche.tiers.remplacant"/></a>
			</td>
		</tr>
	</table>
</div>
<div id="tiersExistantPanel" style="display: none;">
	<table>
		<tr>
			<td width="10%">&nbsp;</td>
			<td>
				<a id="searchLink" href="javascript:;" class="replay" onclick="rechercheTiers();return false">&nbsp;<fmt:message key="label.recherche.tiers.remplacant"/></a>
			</td>
		</tr>
	</table>
	<jsp:include page="../../general/tiers.jsp">
		<jsp:param name="page" value="activation" />
		<jsp:param name="path" value="tiersRemplacant" />
	</jsp:include>
</div>
<script type="text/javascript" language="Javascript">
	var flagNouveauTiers=<c:out value="${command.nouveauTiers}"/>;
	if (flagNouveauTiers) {
		Element.show('tiersNouveauPanel');
		Element.hide( 'tiersExistantPanel');
	}
	else {
		Element.hide('tiersNouveauPanel');
		Element.show( 'tiersExistantPanel');
	}
</script>