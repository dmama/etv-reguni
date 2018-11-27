<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.param.periode.fiscale" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/parametrage-periode-fiscale.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="head">
		<fmt:message key="label.param.confirm.init" var="confirmInit"/>
		<fmt:message key="label.param.confirm.suppr" var="confirmSuppr"/>
		<style type="text/css">
			a.edit, div.button-add, div.checkbox {
				margin: 10px;
			}
			.information {
				width: auto;
			}
			div.checkbox {
				float: right;
			}
			div.emolument {
				text-align: center;
			}

			.colonneTitreParametres {
				width: 35%;
			}

			.colonneModele{
				width: 25%;
			}

			.colonneModeleAction {
				padding-right: 13px;
				width: 25%;
				text-align: right;
			}

			.colonneFeuille{
				width: 17%;
			}

			.colonneFeuilleAction {
				width: 15%;
				text-align: right;
			}

			input[type=checkbox] {
				vertical-align: bottom;
			}
		</style>

		<script type="text/javascript">
			$(document).ready(function() {
				/*
				 * Event Handlers
				 */
				$("select").change( function() {
					$("form").submit();
				});
				$("#initPeriodeFiscale").click( function () {
					return confirm("${confirmInit}");
				});
				$("a.delete").click( function () {
					return confirm("${confirmSuppr}");
				});
			});
		</script>

	</tiles:put>
	<tiles:put name="body">
		<form method="get" id="form" action="list.do">
			<fieldset class="information"><legend><fmt:message key="label.param.periodes"/></legend>
				<div style="margin-top: 5px">
					<fmt:message key="label.param.periode.select"/>:
					<select id="periode" name="pf">
						<%--@elvariable id="periodes" type="java.util.List"--%>
						<c:forEach var="periode" items="${periodes}">
							<c:set var="selected" value=""/>
							<%--@elvariable id="periodeSelectionnee" type="ch.vd.unireg.declaration.PeriodeFiscale"--%>
							<c:if test="${periode.id == periodeSelectionnee.id }">
								<c:set var="selected">
									selected="selected"
								</c:set>
							</c:if>
							<option value="${periode.id}" ${selected}>${periode.annee}</option>
						</c:forEach>
					</select>
				</div>

				<div class="button-add">
					<unireg:raccourciAjouter id="initPeriodeFiscale" link="init-periode.do" tooltip="label.param.init.periode" display="label.param.init.periode"/>
				</div>

				<fmt:message key="label.param.periode.arg" var="titleParametres">
					<fmt:param value="${periodeSelectionnee.annee}" />
				</fmt:message>

				<%@ include file="parametres-pf-pp-show.jsp" %>

				<%@ include file="parametres-pf-pm-show.jsp" %>

				<%@ include file="parametres-pf-snc-show.jsp" %>

				<%@ include file="params-pf-delais-online-pp-show.jsp" %>

				<%@ include file="params-pf-delais-online-pm-show.jsp" %>

				<%@ include file="modele-show.jsp" %>
			</fieldset>
		</form>
	</tiles:put>
</tiles:insert>