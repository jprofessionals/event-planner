import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';
import svelte from 'eslint-plugin-svelte';
import eslintConfigPrettier from 'eslint-config-prettier/flat';
import globals from 'globals';
import svelteConfig from './svelte.config.js';

export default tseslint.config(
	{
		ignores: ['.svelte-kit/', 'build/', 'dist/', 'node_modules/'],
	},
	eslint.configs.recommended,
	...tseslint.configs.recommendedTypeChecked,
	...svelte.configs.recommended,
	{
		languageOptions: {
			globals: {
				...globals.browser,
				...globals.node,
			},
		},
	},
	{
		languageOptions: {
			parserOptions: {
				projectService: true,
				tsconfigRootDir: import.meta.dirname,
			},
		},
	},
	{
		files: ['**/*.svelte', '**/*.svelte.ts', '**/*.svelte.js'],
		languageOptions: {
			parserOptions: {
				projectService: true,
				extraFileExtensions: ['.svelte'],
				parser: tseslint.parser,
				svelteConfig,
			},
		},
	},
	{
		files: ['**/*.js'],
		...tseslint.configs.disableTypeChecked,
	},
	...svelte.configs.prettier,
	eslintConfigPrettier,
);
