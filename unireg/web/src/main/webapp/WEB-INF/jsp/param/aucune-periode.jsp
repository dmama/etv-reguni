<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.param.periode.fiscale" />
	</tiles:put>
	<tiles:put name="head">
		<fmt:message key="label.param.confirm.init" var="confirmInit"/>
		<style type="text/css">
			.select-maitre, a.edit, div.button-add {
				margin: 10px
			}
			.information {
				width: auto
			}
			div.button-add {
				/*float: right*/
			}
		</style>
		
		<script type="text/javascript">
		 $(document).ready(function() {
			 /*
			  * Event Handlers
			  */
				$("#initPeriodeFiscale").click( function () {
					return confirm("${confirmInit}");
				});
		 });
		</script>

	</tiles:put>
	<tiles:put name="body">
		<form method="get" id="form" action="periode.do">
		<fieldset class="information"><legend><fmt:message key="label.param.periodes"/></legend>
			
			<div class="button-add">
				<unireg:raccourciAjouter id="initPeriodeFiscale" link="init-periode.do" tooltip="label.param.init.periode" display="label.param.init.periode"/>
			</div>

		</fieldset>
		</form>
	</tiles:put>
</tiles:insert>