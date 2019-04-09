module.exports = {
	extends: ['stylelint-config-standard'],
	plugins: [
		'stylelint-scss',
		'stylelint-no-unsupported-browser-features'
	],
	rules: {
		'property-no-unknown': true,
		'plugin/no-unsupported-browser-features': true,
		'indentation': 'tab',
		'rule-empty-line-before': null,
		'block-closing-brace-empty-line-before': null,
		'color-named': 'never',
		'color-no-invalid-hex': true,
		'color-hex-length': 'long'
	}
};
