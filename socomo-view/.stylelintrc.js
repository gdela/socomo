module.exports = {
	extends: ['stylelint-config-standard'],
	plugins: ['stylelint-scss'],
	rules: {
		'indentation': 'tab',
		'color-named': 'never',
		'color-no-invalid-hex': true,
		'color-hex-length': 'long',
		'property-no-unknown': true
	}
};
