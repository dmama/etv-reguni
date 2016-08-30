/*
 * jQuery UI Tooltip 1.9-milestone3
 *
 * Copyright 2010, AUTHORS.txt
 * Dual licensed under the MIT or GPL Version 2 licenses.
 * http://jquery.org/license
 *
 * http://docs.jquery.com/UI/Tooltip
 *
 * Depends:
 *	jquery.ui.core.js
 *	jquery.ui.widget.js
 *	jquery.ui.position.js
 */
(function($) {

var increments = 0;

$.widget("ui.tooltip", {
	options: {
		items: "[title]",
		content: function() {
			return $(this).attr("title");
		},
		position: {
			my: "left center",
			at: "right center",
			offset: "15 0"
		}
	},
	_create: function() {
		var self = this;
		this.tooltip = $("<div></div>")
			.attr("id", "ui-tooltip-" + increments++)
			.attr("role", "tooltip")
			.attr("aria-hidden", "true")
			.addClass("ui-tooltip ui-widget ui-corner-all ui-widget-content")
			.appendTo(document.body)
			.hide();
		this.tooltipContent = $("<div></div>")
			.addClass("ui-tooltip-content")
			.appendTo(this.tooltip);
		this.opacity = this.tooltip.css("opacity");
		this.element
			.bind("focus.tooltip mouseover.tooltip", function(event) {
				self.open( event );
			})
			.bind("blur.tooltip mouseout.tooltip", function(event) {
				self.close( event );
			});
	},

	enable: function() {
		this.options.disabled = false;
	},

	disable: function() {
		this.options.disabled = true;
	},

	destroy: function() {
		this.tooltip.remove();
		$.Widget.prototype.destroy.apply(this, arguments);
	},

	widget: function() {
		return this.element.pushStack(this.tooltip.get());
	},

	open: function(event) {
		var target = $(event && event.target || this.element).closest(this.options.items);
		// already visible? possible when both focus and mouseover events occur
		if (this.current && this.current[0] == target[0])
			return;
		var self = this;
		this.current = target;
		this.currentTitle = target.attr("title");
		var content = this.options.content.call(target[0], function(response) {
			// IE may instantly serve a cached response, need to give it a chance to finish with _show before that
			setTimeout(function() {
				// ignore async responses that come in after the tooltip is already hidden
				if (self.current == target)
					self._show(event, target, response);
			}, 13);
		});
		if (content) {
			self._show(event, target, content);
		}
	},

	_show: function(event, target, content) {
		if (!content)
			return;

		target.attr("title", "");

		if (this.options.disabled)
			return;

		this.tooltipContent.html(content);
		this.tooltip.css({
			top: 0,
			left: 0
		}).show().position( $.extend({
			of: target
		}, this.options.position )).hide();

		this.tooltip.attr("aria-hidden", "false");
		target.attr("aria-describedby", this.tooltip.attr("id"));

		// [SIFISC-5219] supprimé l'animation pour IE6
		if ($.browser.msie && /6.0/.test(navigator.userAgent)) {
			this.tooltip.stop(false, true).show();
		}
		else {
			this.tooltip.stop(false, true).fadeIn();
		}

		this._trigger( "open", event );
	},

	close: function(event) {
		if (!this.current)
			return;

		// [SIFISC-4391] avec jQuery 1.7, la méthode $.attr() ne retourne plus l'élément pointé, mais la valeur renseignée (ici le titre).
		// Pour obtenir le même résultat, il faut donc décomposer les deux opérations.
		// var current = this.current.attr("title", this.currentTitle);
		this.current.attr("title", this.currentTitle);
		var current = this.current;

		this.current = null;

		if (this.options.disabled)
			return;

		current.removeAttr("aria-describedby");
		this.tooltip.attr("aria-hidden", "true");

		// [SIFISC-5219] supprimé l'animation pour IE6
		if ($.browser.msie && /6.0/.test(navigator.userAgent)) {
			this.tooltip.stop(false, true).hide();
		}
		else {
			this.tooltip.stop(false, true).fadeOut();
		}

		this._trigger( "close", event );
	}

});

})(jQuery);
