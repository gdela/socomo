/* global require */

const gulp = require('gulp');
const concat = require('gulp-concat');
const rename = require('gulp-rename');

const destination = '../dist';

gulp.task('js', function() {
	const linter = require('gulp-eslint');
	const minify = require('gulp-uglify-es').default;
	return gulp.src('src/main/resources/pl/gdela/socomo/visualizer/*.js')
		.pipe(linter())
		.pipe(linter.format())
		.pipe(linter.failAfterError())
		.pipe(concat('visualizer.js'))
		.pipe(gulp.dest(destination))
		.pipe(rename('visualizer.min.js'))
		.pipe(minify())
		.pipe(gulp.dest(destination));
});

gulp.task('css', function () {
	const linter = require('gulp-stylelint');
	const minify = require('gulp-clean-css');
	return gulp.src('src/main/resources/pl/gdela/socomo/visualizer/*.css')
		.pipe(linter({
			failAfterError: true,
			reporters: [{formatter: 'string', console: true}]
		}))
		.pipe(concat('visualizer.css'))
		.pipe(gulp.dest(destination))
		.pipe(rename('visualizer.min.css'))
		.pipe(minify())
		.pipe(gulp.dest(destination));
});

gulp.task('build', gulp.parallel('js', 'css'));
