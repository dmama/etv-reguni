<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.regimes.fiscaux.VD"/></span></legend>

	<unireg:setAuth var="auth" tiersId="${command.tiers.numero}"/>
	<c:if test="${!command.tiers.annule && auth.regimesFiscaux}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../regimefiscal/edit-list.do?pmId=${command.tiers.numero}&portee=VD" tooltip="Modifier les régimes fiscaux cantonaux" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<c:if test="${not empty command.regimesFiscauxVD}">

		<input class="noprint" name="rfvd_histo" type="checkbox" onClick="History.toggleRowsIsHisto('regimesVD','rfvd_histo');" id="rfvd_histo" />
		<label class="noprint" for="rfvd_histo"><fmt:message key="label.historique" /></label>

		<display:table name="${command.regimesFiscauxVD}" id="regimesVD" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 20%;">
				<unireg:regdate regdate="${regimesVD.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" class="notShownIfNotEmpty" style="width: 20%;">
				<unireg:regdate regdate="${regimesVD.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				${regimesVD.type}
			</display:column>
			<display:column class="action" style="width: 10%;">
				<unireg:consulterLog entityNature="RegimeFiscal" entityId="${regimesVD.id}" />
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.regimes.fiscaux.CH"/></span></legend>

	<c:if test="${!command.tiers.annule && auth.regimesFiscaux}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../regimefiscal/edit-list.do?pmId=${command.tiers.numero}&portee=CH" tooltip="Modifier les régimes fiscaux fédéraux" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<c:if test="${not empty command.regimesFiscauxCH}">

		<input class="noprint" name="rfch_histo" type="checkbox" onClick="History.toggleRowsIsHisto('regimesCH','rfch_histo');" id="rfch_histo" />
		<label class="noprint" for="rfch_histo"><fmt:message key="label.historique" /></label>

		<display:table name="${command.regimesFiscauxCH}" id="regimesCH" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 20%;">
				<unireg:regdate regdate="${regimesCH.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" class="notShownIfNotEmpty" style="width: 20%;">
				<unireg:regdate regdate="${regimesCH.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				${regimesCH.type}
			</display:column>
			<display:column class="action" style="width: 10%;">
				<unireg:consulterLog entityNature="RegimeFiscal" entityId="${regimesCH.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<script type="text/javascript">

	var History = {

		toggleRowsIsHisto: function(idTable, idCheckbox) {

			var table = $('#' + idTable).get(0);
			if (table != null) {
				var showHisto = $('#' + idCheckbox).get(0).checked;
				var nbLines = table.rows.length;
				for (var index = 0 ; index < nbLines ; ++ index) {
					var line = table.rows[index];
					var visible;
					if (!showHisto) {
						if (this.hasClass(line, 'strike')) {
							visible = false;
						}
						else {
							visible = true;
							var column = $(line).find('td.notShownIfNotEmpty');
							if (column.length > 0) {
								for (var colIndex = 0 ; colIndex < column.length ; ++ colIndex) {
									visible = StringUtils.isBlank(column[colIndex].innerHTML);
									if (!visible) {
										break;
									}
								}
							}
						}
					}
					else {
						visible = true;
					}

					line.style.display = (visible ? '' : 'none');
				}

				this.computeEvenOdd(table);
			}
		},

		hasClass: function(element, clazz) {
			return (' ' + element.className + ' ').indexOf(' ' + clazz + ' ') > -1;
		},

		computeEvenOdd: function(table) {
			var rows = $(table).find('tr:visible');
			rows.removeClass('odd');
			rows.removeClass('even');
			$(table).find('tr:visible:even').addClass('even');
			$(table).find('tr:visible:odd').addClass('odd');
		}

	};

	$(function() {
		History.toggleRowsIsHisto('regimesVD','rfvd_histo');
		History.toggleRowsIsHisto('regimesCH','rfch_histo');
	});

</script>
