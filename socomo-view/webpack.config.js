/* eslint-disable */

const path = require('path');

module.exports = {
	entry: './src/visualizer.js',
	output: {
		filename: 'bundle.js',
		path: path.resolve(__dirname, 'dist')
	},
	devServer: {
		port: 8086,
		overlay: {
			errors: true,
			warnings: true
		}
	},
	module: {
		rules: [
			{
				test: /\.js$/,
				include: [path.resolve(__dirname, 'src')],
				use: [
					{
						loader: 'babel-loader',
						options: {
							presets: ['@babel/preset-env']
						}
					},
					{
						loader: 'eslint-loader',
						options: {
							// none
						}
					}
				],

			},
			{
				test: /\.(scss|css)$/,
				include: [path.resolve(__dirname, 'src')],
				use: [
					'style-loader',
					'css-loader',
					'sass-loader'
				]
			}
		]
	}
};
