const js = require('@eslint/js');

// reglas basicas y limite de complejidad ciclomatica en 10
module.exports = [
  js.configs.recommended,
  {
    files: ['src/**/*.js'],
    languageOptions: {
      sourceType: 'commonjs',
      globals: { require: 'readonly', module: 'writable', __dirname: 'readonly', console: 'readonly' },
    },
    rules: {
      complexity: ['error', 10],
    },
  },
];
