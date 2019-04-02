/* eslint-disable */

const path = require('path');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const StyleLintPlugin = require('stylelint-webpack-plugin');

module.exports = {
	entry: './src/visualizer.js',
	devServer: {
		port: 8086,
		overlay: {
			errors: true,
			warnings: true
		}
	},
	output: {
		filename: 'bundle.js',
		path: path.resolve(__dirname, 'dist')
	},
	plugins: [
		new MiniCssExtractPlugin({
			filename: 'bundle.css'
		}),
		new StyleLintPlugin({
			context: "src",
			files: '**/*.scss',
			syntax: 'scss'
		})
	],
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
				test: /\.scss$/,
				include: [path.resolve(__dirname, 'src')],
				use: [
					MiniCssExtractPlugin.loader,
					'css-loader',
					'sass-loader'
				]
			}
		]
	},
	externals: {
		cytoscape: 'cytoscape'
	}
};
