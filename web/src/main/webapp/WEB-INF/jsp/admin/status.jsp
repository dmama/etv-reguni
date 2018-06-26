<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.info"/></tiles:put>

  	<tiles:put name="body">
  	
  		<a href="JavaScript:location.reload(true);">
			(<fmt:message key="label.bouton.refresh"/>)
		</a>

		<fieldset>
			<legend><span><fmt:message key="label.batch.actif"/></span></legend> 
			<div id="jobsActif"></div>
		</fieldset>

		<fieldset class="info">
			<legend><span><fmt:message key="title.info.data" /></span></legend>
			<fieldset class="info">
				<legend><span><fmt:message key="label.info.tiers"/></span></legend>
				<table>
					<unireg:nextRowClass reset="1"/>
					<tr class="<unireg:nextRowClass/>"><td style="width: 80%;"><fmt:message key="label.info.db.nombre.tiers"/></td><td><span class="value"><c:out value="${tiersCount}"/></span></td></tr>
					<tr class="<unireg:nextRowClass/>"><td><fmt:message key="label.info.indexer.nombre.tiers"/></td><td><span class="value"><c:out value="${tiersIndexCount}"/></span></td></tr>
				</table>
			</fieldset>
			<fieldset class="info">
				<legend><span><fmt:message key="label.info.identifications"/></span></legend>
				<table>
					<unireg:nextRowClass reset="1"/>
					<tr class="<unireg:nextRowClass/>"><td style="width: 80%;"><fmt:message key="label.info.db.nombre.identifications"/></td><td><span class="value"><c:out value="${identCount}"/></span></td></tr>
					<tr class="<unireg:nextRowClass/>"><td><fmt:message key="label.info.indexer.nombre.identifications"/></td><td><span class="value"><c:out value="${identIndexCount}"/></span></td></tr>
				</table>
			</fieldset>
		</fieldset>

		<fieldset class="info">
			<legend><span><fmt:message key="title.info.connection" /></span></legend>
			<table class="display_table">
				<thead>
					<tr>
						<th>&nbsp;</th><th><fmt:message key="label.etat"/></th>
					</tr>
				</thead>
				<tbody>
					<unireg:nextRowClass reset="1"/>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.civil"/> :</td>
						<td id="serviceCivilStatus"><img src="<c:url value="/images/loading.gif"/>" /></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.organisation"/> :</td>
						<td id="serviceEntrepriseStatus"><img src="<c:url value="/images/loading.gif"/>" /></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.infra"/> :</td>
						<td id="serviceInfraStatus"><img src="<c:url value="/images/loading.gif"/>" /></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.securite"/> :</td>
						<td id="serviceSecuriteStatus"><img src="<c:url value="/images/loading.gif"/>" /></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.brvplus"/> :</td>
						<td id="bvrPlusStatus"><img src="<c:url value="/images/loading.gif"/>" /></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.efacture"/> :</td>
						<td id="efactureStatus"><img src="<c:url value="/images/loading.gif"/>" /></td>
					</tr>
				</tbody>
			</table>
		</fieldset>

        <fieldset id="stats_fieldset" class="info">
            <legend><span><fmt:message key="title.info.stats" /></span></legend>
            <div id="stats_output" class="asciiart"><c:out value="${serviceStats}" escapeXml="false"/></div>
        </fieldset>
        
		<fieldset class="info">
			<legend><span><fmt:message key="title.info.cache" /></span></legend>
			<c:out value="${cacheStatus}" escapeXml="false"/>
		</fieldset>
		
		<script type="application/javascript">
			$(function() {

				// Chargement asynchrone des statuts des services
				$.get('<c:url value="/admin/status/civil.do"/>?' + new Date().getTime(), function(status) {
					updateServiceStatus('#serviceCivilStatus', status);
				});
				$.get('<c:url value="/admin/status/entreprise.do"/>?' + new Date().getTime(), function(status) {
					updateServiceStatus('#serviceEntrepriseStatus', status);
				});
				$.get('<c:url value="/admin/status/infra.do"/>?' + new Date().getTime(), function(status) {
					updateServiceStatus('#serviceInfraStatus', status);
				});
				$.get('<c:url value="/admin/status/securite.do"/>?' + new Date().getTime(), function(status) {
					updateServiceStatus('#serviceSecuriteStatus', status);
				});
				$.get('<c:url value="/admin/status/bvr.do"/>?' + new Date().getTime(), function(status) {
					updateServiceStatus('#bvrPlusStatus', status);
				});
				$.get('<c:url value="/admin/status/efacture.do"/>?' + new Date().getTime(), function(status) {
					updateServiceStatus('#efactureStatus', status);
				});

				function updateServiceStatus(element, status) {
					if (status.code == 'OK') {
						$(element).html('<img title="' + status.code + '" src="<c:url value="/css/x/checkmark.png"/>"/>');
					}
					else {
						$(element).html('<span style="background: url(<c:url value="/css/x/error.gif"/>) no-repeat; vertical-align: middle; text-align: left";">' +
							'<a style="padding-left:2em" href="#" onclick="$(this).children(\'div\').dialog({title:\'Description du problème\', width:1024, modal:true}); return false;">(détails)' +
							'<div style="display:none"><p>' + status.description + '</p></div></a></span>');
					}
				}

				// Chargement des batches en cours d'exécution
				Batch.loadRunning($('#jobsActif'), "3s", true);

				// Ajuste la taille des divs *_output par rapport à celles des divs *_fieldset, ceci pour
				// contourner une limitation du style 'overflow:auto' qui ne supporte pas des tailles relatives
				var divs = ['stats'];
				for (d in divs) {
					var fieldset = $('#' + divs[d] + '_fieldset');
					var output = $('#' + divs[d] + '_output');
					output.css('width', fieldset.prop('offsetWidth') - 20);
				}
			});
		</script>

  	</tiles:put>
</tiles:insert>
