# VAADIN 24 FilterBar

The **_FilterBar_** component allows users to compile complex filter combinations using only a single TextField by analyzing an entered String as a term of 1->n keywords via [Collo](https://github.com/MantledIllusion/collo).

Supported features are:
* analyzing entered terms using a configurable RegEx-based **_TermAnalyzer_**
* offering examples and favorites of configured terms
* displaying recognized keyword combinations as clickable suggestions
* adding and removing analyzed terms in a list of filters
* auto-removal of non-combinable terms
* toggling between AND/OR filter concatenation
* I18N of labels, placeholders and tooltips