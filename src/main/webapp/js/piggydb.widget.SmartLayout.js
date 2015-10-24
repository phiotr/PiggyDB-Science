(function(module) {
	
	var _pageContent = jQuery("#page-content");
	var _container = jQuery("div.sl-container");
	var _pane = jQuery("div.sl-pane");
	
	var _object = {}
	
	_object.init = function() {
		_object.updateLayout();
	};
	
	_object.updateLayout = function() {
        _object.setVerticalLayout();
	};
	
	_object.setVerticalLayout = function() {
			_object.vertical = true;
			_pageContent.toggleClass("sl-horizontal", false);
			jQuery("body").css("overflow", "auto");
			_pane.css("width", "auto");
	};
	
	
	module.SmartLayout = _object;
	
})(piggydb.widget);
