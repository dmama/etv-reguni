<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut Immeubles -->

<div id="immeublesDiv" style="position:relative"><img src="<c:url value="/images/loading.gif"/>"/></div>

<script>
	// chargement Ajax des immeubles
	$(function() {
		$('#immeublesDiv').load('../rf/immeuble/list.do?ctb=${command.tiersGeneral.numero}&' + new Date().getTime());
	});
</script>

<!-- Fin Immeubles -->
