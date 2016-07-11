/**
 * Sadly, the text boxes that we listen to exist outside of React's world, so this component encapsulates them.
 */
define("SuggestionSourceListener", [], function() {
  return {
    getSourceTexts: function() {
       return this.suggestionSources.pluck('value').join('.\n');
    },
    init: function(cb) {
      var suggestionSourcesIds = ['indication_for_referral', 'medical_history'];
      var that = this;
      this.suggestionSources = $$('textarea[name$="' + suggestionSourcesIds.join('"], textarea[name$="') + '"]');
      this.suggestionSources.invoke('observe', 'blur', function() {
        var newText = that.getSourceTexts();
        if (newText != this.text) {
          this.text = newText;
          if(this.text) {
            cb(this.text);
          }
        }
      });
    },
  };
});

/**
 * This object interacts with the PhenoTips backend to make sure suggestions we dismiss go
 * away for good.
 */
define("SuggestionsDismisser", ["lodash"], function(_) {
  return {
    _dismissed: {},
    _existsDataStore: false,
    /**
     * Given a dictionary of suggestions, return a version without those that have been dismissed in the past.
     */
    filter: function(suggestions) {
      return _.omit(suggestions, _.keys(this._dismissed));
    },
    dismiss: function(phenotype, onDone) {
      this._dismissed[phenotype.id] = true;
      this._persist(onDone);
    },
    clear: function(onDone) {
      this._dismissed = {};
      this._persist(onDone);
    },
    _persist: function(onDone) {
      var delimited = _.keys(this._dismissed).join('|');
      var parameters = {
        'ajax': true,
        'form_token': $$("meta[name='form_token']")[0].content,
      };
      if(this._existsDataStore) {
        parameters['PhenoTips.DismissedSuggestionsClass_0_terms'] = delimited;
      } else {
        parameters.classname = 'PhenoTips.DismissedSuggestionsClass';
        parameters['PhenoTips.DismissedSuggestionsClass_terms'] = delimited;
      }
      var url = XWiki.currentDocument.getURL((this._existsDataStore ? 'save' : 'objectadd'));
      var that = this;
      new Ajax.Request(url, {
        parameters: parameters,
        onSuccess: function() {
          that._existsDataStore = true;
        },
        onComplete: onDone
      });
    },
    init: function(onDone) {
      var url = XWiki.currentDocument.getRestURL('objects/PhenoTips.DismissedSuggestionsClass/0/properties/terms');
      var that = this;
      new Ajax.Request(url, {
        method: 'get',
        requestHeaders: {'Accept': 'application/json'},
        onSuccess: function(response) {
          var keys = response.responseJSON.value.split('|');
          keys.forEach(function(e) { that._dismissed[e] = true; });
          that._existsDataStore = true;
        },
        onComplete: onDone,
      });
    },
  };
});

/**
 * This is a port to react of PhenoTips' existing YesNoPicker element.
 * It's much reduced in functionality, and only displays the buttons and provides callbacks on them,
 * without containing the inner checkboxes that the phenotips widget has.
 */
define("ReactYNPicker", ['react', 'lodash'], function(React, _) {
  return React.createClass({
    displayName: 'yn-picker',
    propTypes: {
      onButton: React.PropTypes.object.isRequired,
    },
    _supported: {
      yes : {label: 'Y', title : "$services.localization.render('phenotips.yesNoNAPicker.yes.title')"},
      no  : {label: 'N', title : "$services.localization.render('phenotips.yesNoNAPicker.no.title')"},
      na  : {label: 'NA', title : "$services.localization.render('phenotips.yesNoNAPicker.NA.title')"}
    },
	_onClick: function(key, ev) {
	  return this.props.onButton[key](ev);
	},
    render: function() {
      var that = this;
      var inner = _.filter(_.keys(this._supported).map(function(key) {
        var value = that._supported[key];
        if (!that.props.onButton[key]) {
          return null;
        }
        return React.createElement('label', {
          className: key,
          title: value.title,
          onClick: that._onClick.bind(null, key),
          key: key,
        }, value.label);
      }), _.identity);
      return React.createElement('span', {className: 'yes-no-picker'}, inner);
    }
  });
});

/**
 * A small purpose-agnostic refresh button.
 */
define("RefreshButton", ["react"], function(React) {
  return React.createClass({
    displayName: 'refresh-button',
    propTypes: {
      onRefresh: React.PropTypes.func.isRequired,
    },
    render: function() {
      return React.createElement('span',
        {id: 'annotation-refresh', className: 'fa fa-refresh xHelpButton', 'aria-hidden': 'true',
         onClick: this.props.onRefresh});
    },
  });
});

/**
 * The spinner that shows up as we load clinical text suggestions.
 */
define("SuggestionsLoading", ['react'], function(React) {
  return React.createClass({
    displayName: 'suggestions-loading',
    render: function() {
      return React.createElement('div',
        {id: 'annotation-widget-container'},
        React.createElement('div',
          {className: 'loading-container'},
          React.createElement('div', {id: 'suggestions-spinner', className: 'fa fa-lg fa-spinner fa-spin'}),
          React.createElement('div', {id: 'generating-suggestions-label'}, 'Generating Suggestions')
        )
      );
    },
  });
});

/**
 * A single suggestion drawn from the clinical notes.
 */
define("SuggestionElement", ['react', "ReactYNPicker", "lodash"], function(React, ReactYNPicker, _) {
  return React.createClass({
    displayName: 'suggestion-element',
    propTypes: {
      phenotype: React.PropTypes.object.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      onDismiss: React.PropTypes.func.isRequired,
    },
    onYes: function() {
      var phenotype = _.clone(this.props.phenotype);
      phenotype.isObserved = true;
      this.props.onSelect(phenotype);
    },
    onNo: function() {
      var phenotype = _.clone(this.props.phenotype);
      phenotype.isObserved = false;
      this.props.onSelect(phenotype);
    },
    onDismiss: function() {
      var phenotype = _.clone(this.props.phenotype);
      this.props.onDismiss(phenotype);
    },
    render: function() {
      var sentence = this.props.phenotype.sentence;
      var end = this.props.phenotype.endInSentence;
      var start = this.props.phenotype.startInSentence;
      return React.createElement('li',
          {className: 'suggestion-list-item'},
          React.createElement('div', {className: 'suggestion'},
            React.createElement('div',
              {className: 'suggestion-headline'},
              React.createElement(ReactYNPicker, {onButton: {yes: this.onYes, no: this.onNo}}),
              React.createElement('span', {className: 'suggestion-term'}, this.props.phenotype.label),
              React.createElement('span', {className: 'xHelpButton fa fa-info-circle phenotype-info',
                                           title: this.props.phenotype.id})
              ),
            React.createElement('div',
              {className: 'suggestion-quote'},
              '...' + sentence.substring(0, start),
              React.createElement('span', {className: 'quoted-term'}, sentence.substring(start, end)),
              sentence.substring(end, sentence.length) + '...'
              ),
            React.createElement('div', {className: 'hide-suggestion', onClick: this.onDismiss}, "✖")
            )
          );
    },
  });
});

/**
 * The paginated list of suggestions from clinical notes. Renders as a list full of SuggestionElement instances.
 */
define("SuggestionList", ['react', "SuggestionElement", "lodash"], function(React, SuggestionElement, _) {
  return React.createClass({
    displayName: 'suggestion-list',
    propTypes: {
      phenotypes: React.PropTypes.object.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      onDismiss: React.PropTypes.func.isRequired,
      pageSize: React.PropTypes.number,
    },
    getDefaultProps: function() {
      return {
        pageSize: 4,
      };
    },
    getInitialState: function() {
      return {
        page: 0,
      };
    },
    componentWillReceiveProps: function(nextProps) {
      /* We can't be paging past the end, so roll it back if necessary. */
      if(this.state.page >= this._maxPages(nextProps.phenotypes, nextProps.pageSize)) {
        this.setState({ page: this._maxPages(nextProps.phenotypes, nextProps.pageSize) - 1 });
      }
    },
    nextPage: function() {
      this.setState({ page: Math.min(this.state.page + 1, this._maxPages())});
    },
    prevPage: function() {
      this.setState({ page: Math.max(this.state.page - 1, 0)});
    },
    _maxPages: function(phenotypes, pageSize) {
      phenotypes = phenotypes || this.props.phenotypes;
      pageSize = pageSize || this.props.pageSize;
      return Math.ceil(_.keys(phenotypes).length / pageSize);
    },
    render: function() {
      var prev = null;
      var next = null;
      if (this.state.page !== 0) {
        prev = React.createElement('span', {className: 'fa fa-chevron-left navigation', onClick: this.prevPage, key: 'prev'});
      }
      if ((this.state.page + 1) < this._maxPages()) {
        next = React.createElement('span', {className: 'fa fa-chevron-right navigation', onClick: this.nextPage, key: 'next'});
      }
      var that = this;
      var len = _.keys(this.props.phenotypes).length;
      var start = this.state.page * this.props.pageSize;
      var end = Math.min(start + this.props.pageSize, len);
      var list = _.keys(this.props.phenotypes).slice(start, end).map(function(key) {
        var phenotype = that.props.phenotypes[key];
        return React.createElement(SuggestionElement, {
          phenotype: phenotype,
          onSelect: that.props.onSelect,
          onDismiss: that.props.onDismiss,
          key: phenotype.id,
        });
      });
      var count = 'No suggestions to display';
      if (len) {
        count = (start + 1) + " to " + end + " of " + len + " suggestions.";
      }
      var ul = React.createElement('ul', {className: 'suggestions-list', key: 'suggestions-list'}, list);
      var inner = _.filter([prev, ul, next], _.identity);
      return React.createElement('div', {id: 'annotation-widget-container'},
          React.createElement('div', {id: 'suggestions-container'}, inner),
          React.createElement('div', {id: 'suggestion-count'}, count)
          );
    },
  });
});

/**
 * The base clinical notes annotation panel. Listens for changes to the text boxes, hits the server for new annotations and re-renders accordingly.
 */
define("ClinicalNotesAnnotation", ["react", "PhenotypeSelectionUtils", "SuggestionSourceListener", "SuggestionsLoading", "SuggestionList",
                                   "RefreshButton", "SuggestionsDismisser", 'lodash'],
                           function(React, PhenotypeSelectionUtils, SuggestionSourceListener,   SuggestionsLoading,   SuggestionList,
                                    RefreshButton,  SuggestionsDismisser, _) {
  return React.createClass({
    displayName: 'clinical-notes-annotation-panel',
    onSourceBoxUpdate: function(text) {
      /* React is ajax agnostic, so let's just stick to prototype here */
      var queryString = '?outputSyntax=plain';
      var url = new XWiki.Document('AnnotationService', 'PhenoTips').getURL('get') + queryString;
      this.setState({ loading: true }, function() {
        new Ajax.Request(url, {
          parameters: {
            text: text,
          },
          onSuccess: this.setResults,
        });
      });
    },
    setResults: function(results) {
      var suggestions = SuggestionsDismisser.filter(results.responseJSON.response);
      var existing = PhenotypeSelectionUtils.getSelectedPhenotypes().map(function (o) { return o.id; });
      suggestions = _.omit(suggestions, existing);
      this.setState({
        loading: false,
        suggestions: suggestions,
      });
    },
    getInitialState: function() {
      return {
        suggestions: {},
        loading: false,
      };
    },
    componentDidMount: function() {
      SuggestionSourceListener.init(this.onSourceBoxUpdate);
      SuggestionsDismisser.init(this._forceRefresh);
    },
    onRefresh: function() {
      SuggestionsDismisser.clear(this._forceRefresh);
    },
    onSelectPhenotype: function(phenotype) {
      PhenotypeSelectionUtils.selectPhenotype(phenotype, '');
      this.onDismissPhenotype(phenotype);
    },
    onDismissPhenotype: function(phenotype) {
      var that = this;
      SuggestionsDismisser.dismiss(phenotype, function() {
        var suggestions = that.state.suggestions;
        suggestions = SuggestionsDismisser.filter(suggestions);
        that.setState({suggestions: suggestions});
      });
    },
    _forceRefresh: function() {
      this.onSourceBoxUpdate(SuggestionSourceListener.getSourceTexts());
    },
    render: function() {
      var inner = null;
      if(this.state.loading) {
        inner = React.createElement(SuggestionsLoading);
      } else {
        inner = React.createElement(SuggestionList, {
          onSelect: this.onSelectPhenotype,
          onDismiss: this.onDismissPhenotype,
          phenotypes: this.state.suggestions,
        });
      }
      return React.createElement('div',
        {className: 'sub-panel'},
        React.createElement('h3',
          {className: 'wikigeneratedheader'},
          React.createElement('span', null, React.createElement('strong', null, 'Suggestions from Clinical Notes')),
          React.createElement(RefreshButton, {onRefresh: this.onRefresh})),
        inner);
    },
  });
});

require(['react', 'react-dom', 'ClinicalNotesAnnotation'], function(React, ReactDOM, ClinicalNotesAnnotation) {
  var init = function() {
    var contain = new Element('div', {id: 'clinical-notes-container'});
    $$('.current-phenotype-selection')[0].insert({top: contain});
    ReactDOM.render(React.createElement(ClinicalNotesAnnotation, null), contain);
  };

  if(XWiki.domIsLoaded) {
    init();
  } else {
    document.observe("xwiki:dom:loaded", init);
  }
});
