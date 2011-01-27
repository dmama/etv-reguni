<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:set var="tiersId" value="${param.tiersId}"/>

<div id="remarques"><img src="<c:url value="/images/loading.gif"/>"/>Chargement des remarques...</div>

<script type="text/javascript">
	// appels ajax pour charger les remarques
	XT.doAjaxAction('refreshRemarques', $('#remarques').get(0), {
		'tiersId' : ${tiersId} 
	});
</script>
