/* eslint-disable */

const path = require('path');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const StyleLintPlugin = require('stylelint-webpack-plugin');

module.exports = {
	entry: {
		index: './src/index.js',
		bundle: './src/main.js'
	},
	output: {
		filename: '[name].js',
		path: path.resolve(__dirname, 'dist')
	},
	plugins: [
		new MiniCssExtractPlugin({
			filename: '[name].css'
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
							presets: [['@babel/preset-env', { useBuiltIns: "usage", corejs: 2 }]]
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
				use: [
					MiniCssExtractPlugin.loader,
					'css-loader',
					'sass-loader'
				]
			}
		]
	},
	externals: {
		// keep in sync with external assets loading in index.js
		cytoscape: 'cytoscape'
	},
	devServer: {
		port: 8086,
		disableHostCheck: true,
		overlay: {
			errors: true,
			warnings: true
		}
	}
};
