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
		
	</tiles:put>
	<tiles:put name="body">
		<form method="get" id="form" action="periode.do">
		<fieldset class="information"><legend><fmt:message key="label.param.periodes"/></legend>
			
			<div class="button-add">
				<unireg:linkTo name="label.param.init.periode" action="/param/periode/init-periode.do" confirm="${confirmInit}" link_class="add" method="POST" title="label.param.init.periode"/>
			</div>

		</fieldset>
		</form>
	</tiles:put>
</tiles:insert>