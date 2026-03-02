export default {
	'frontend/**/*.{ts,svelte}': [
		'bash -c "cd frontend && npx eslint --fix $@" --',
		'bash -c "cd frontend && npx prettier --write $@" --',
	],
	'frontend/**/*.{js,json,css,html,md}': [
		'bash -c "cd frontend && npx prettier --write $@" --',
	],
	'e2e/**/*.ts': [
		'bash -c "cd e2e && npx eslint --fix $@" --',
		'bash -c "cd e2e && npx prettier --write $@" --',
	],
	'backend/**/*.kt': ['bash -c "cd backend && ./gradlew ktlintFormat"'],
};
